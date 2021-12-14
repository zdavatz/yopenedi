/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.ywesee.java.yopenedi.emailfetcher;

import com.ywesee.java.yopenedi.common.Config;
import com.ywesee.java.yopenedi.common.EmailCredential;
import com.ywesee.java.yopenedi.converter.Converter;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.util.BASE64DecoderStream;
import com.ywesee.java.yopenedi.converter.Pair;
import com.ywesee.java.yopenedi.converter.Writable;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import java.io.*;
import java.util.Properties;

public class App {
    static File edifactFolder;
    static File openTransFolder;
    static String confPath;

    static String mailboxName = "inbox";

    static boolean skipSeenMessage;
    static boolean markMessageAsSeen;
    static boolean showDebugMessages;
    static boolean isTestEnvironment;

    static boolean setupCliFromArgs(String[] args) {
        Options options = new Options();

        Option edifactOption = new Option(
                null,
                "edifact",
                true,
                "The path to save edifact files."
        );
        edifactOption.setType(String.class);
        options.addOption(edifactOption);

        Option openTransOption = new Option(null, "opentrans", true, "The path to save OpenTrans files.");
        openTransOption.setType(String.class);
        options.addOption(openTransOption);

        Option mailboxOption = new Option(null, "mailbox", true, "Which folder in the mailbox to read. Case insensitive. Default: inbox.");
        mailboxOption.setType(String.class);
        options.addOption(mailboxOption);

        Option skipSeenOption = new Option(null, "skip-seen", false, "Skip seen message?");
        options.addOption(skipSeenOption);

        Option markAsSeenOption = new Option(null, "mark-as-seen", false, "Mark message as seen after processing?");
        options.addOption(markAsSeenOption);

        Option helpOption = new Option("h", "help", false, "Display help message");
        options.addOption(helpOption);

        Option debugOption = new Option(null, "debug", false, "Show debug messages");
        options.addOption(debugOption);

        Option testOption = new Option(null, "test", false, "Add test environment message to OpenTrans file");
        options.addOption(testOption);

        Option confOption = new Option("c", "conf", true, "Config folder");
        confOption.setType(String.class);
        options.addOption(confOption);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            boolean showHelp = false;
            if (cmd.hasOption("h")) {
                showHelp = true;
            }
            if (!showHelp) {
                if (!cmd.hasOption("edifact")) {
                    System.err.println("Missing Option: --edifact");
                    showHelp = true;
                }
                if (!cmd.hasOption("opentrans")) {
                    System.err.println("Missing Option: --opentrans");
                    showHelp = true;
                }
            }
            if (showHelp) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "openedi", options);
                return false;
            }
            edifactFolder = new File(cmd.getOptionValue("edifact"));
            openTransFolder = new File(cmd.getOptionValue("opentrans"));
            if (cmd.hasOption("mailbox")) {
                mailboxName = cmd.getOptionValue("mailbox");
            }
            skipSeenMessage = cmd.hasOption("skip-seen");
            markMessageAsSeen = cmd.hasOption("mark-as-seen");
            showDebugMessages = cmd.hasOption("debug");
            isTestEnvironment = cmd.hasOption("test");
            confPath = cmd.getOptionValue("conf", "./conf");
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        if (!setupCliFromArgs(args)) {
            return;
        }
        if (!edifactFolder.exists()) {
            edifactFolder.mkdirs();
        }
        if (!openTransFolder.exists()) {
            openTransFolder.mkdirs();
        }

        Config config = new Config(confPath, isTestEnvironment);
        ArrayList<File> filesFromEmail = fetchEmail(config);
        ArrayList<File> files = new ArrayList<>(filesFromEmail);
        for (File file : files) {
            File outFolder;
            String inFilePath = file.getAbsolutePath();
            String inFileName = FilenameUtils.getName(inFilePath);
            if (inFilePath.startsWith(openTransFolder.getAbsolutePath())) {
                outFolder = edifactFolder;
            } else {
                outFolder = openTransFolder;
            }

            Converter converter = new Converter(config);
            converter.shouldMergeContactDetails = true;

            Pair<Converter.FileType, Writable> converted = converter.run(new FileInputStream(file));

            File targetFile = new File(outFolder, inFileName + ".xml");
            FileOutputStream outputStream = new FileOutputStream(targetFile);

            String recipientGLN = null;
            String orderId = null;
            String edifactType = null;
            if (converted.snd instanceof com.ywesee.java.yopenedi.OpenTrans.Order) {
                com.ywesee.java.yopenedi.OpenTrans.Order otOrder = (com.ywesee.java.yopenedi.OpenTrans.Order)converted.snd;
                otOrder.isTestEnvironment = isTestEnvironment;
                System.out.println("Outputting order(id=" + otOrder.id + ") to " + targetFile.getAbsolutePath());
                com.ywesee.java.yopenedi.OpenTrans.Party recipient = otOrder.getRecipient();
                if (recipient != null) {
                    recipientGLN = recipient.id;
                }
                orderId = otOrder.id;
                edifactType = "ORDERS";
            } else if (converted.snd instanceof com.ywesee.java.yopenedi.Edifact.OrderResponse) {
                com.ywesee.java.yopenedi.Edifact.OrderResponse ediOrderResponse = (com.ywesee.java.yopenedi.Edifact.OrderResponse)converted.snd;
                com.ywesee.java.yopenedi.Edifact.Party recipient = ediOrderResponse.getRecipient();
                if (recipient != null) {
                    recipientGLN = recipient.id;
                }
                edifactType = "ORDRSP";
            } else if (converted.snd instanceof com.ywesee.java.yopenedi.Edifact.Invoice) {
                com.ywesee.java.yopenedi.Edifact.Invoice ediInvoice = (com.ywesee.java.yopenedi.Edifact.Invoice)converted.snd;
                com.ywesee.java.yopenedi.Edifact.Party recipient = ediInvoice.getRecipient();
                if (recipient != null) {
                    recipientGLN = recipient.id;
                }
                edifactType = "INVOIC";
            } else if (converted.snd instanceof com.ywesee.java.yopenedi.Edifact.DespatchAdvice) {
                com.ywesee.java.yopenedi.Edifact.DespatchAdvice ediDispatchAdvice = (com.ywesee.java.yopenedi.Edifact.DespatchAdvice)converted.snd;
                com.ywesee.java.yopenedi.Edifact.Party recipient = ediDispatchAdvice.getRecipient();
                if (recipient != null) {
                    recipientGLN = recipient.id;
                }
                edifactType = "DESADV";
            }
            converted.snd.write(outputStream, config);
            outputStream.close();

            config.dispatchResult(recipientGLN, edifactType, targetFile, orderId);
        }
        System.out.println("Done");
    }

    static ArrayList<File> fetchEmail(Config config) throws Exception {
        EmailCredential emailCreds = config.getEmailCredential();

        final Properties properties = new Properties();
        if (emailCreds.secure) {
            properties.put("mail.imap.ssl.enable", "true");
        }
        properties.setProperty("mail.imap.host", emailCreds.imapHost);
        properties.setProperty("mail.imap.port", emailCreds.imapPort);
        properties.setProperty("mail.imap.connectiontimeout", "5000");
        properties.setProperty("mail.imap.timeout", "5000");

        Session imapSession = Session.getInstance(properties, null);
        if (showDebugMessages) {
            imapSession.setDebug(true);
        }
        Store imapStore = imapSession.getStore("imap");

        imapStore.connect(emailCreds.imapHost, emailCreds.user, emailCreds.password);

        Folder defaultFolder = imapStore.getDefaultFolder();
        Folder[] folders = defaultFolder.list();

        IMAPFolder inbox = null;
        for (Folder f : folders) {
            if (f.getFullName().equalsIgnoreCase(mailboxName)) {
                inbox = (IMAPFolder) f;
                break;
            }
        }
        if (inbox == null) {
            System.err.println("Cannot find mailbox named " + mailboxName + ". Available folders are:");
            for (Folder f : folders) {
                System.err.println(f.getFullName());
            }
            throw new Exception("Cannot find mailbox.");
        }

        inbox.open(Folder.READ_WRITE);

        Message[] ms = skipSeenMessage
                ? inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false))
                : inbox.getMessages();
        System.out.println("Found " + ms.length + " messages");

        ArrayList<File> files = new ArrayList<>();

        for (Message message : ms) {
            long uid = inbox.getUID(message);
            System.out.println("Found message. UID=" + uid);
            System.out.println("Subject: " + message.getSubject());

            System.out.println("Getting attachment");
            Object content = message.getContent();
            BASE64DecoderStream stream = null;
            if (content instanceof BASE64DecoderStream) {
                stream = (BASE64DecoderStream) content;
            } else {
                System.err.println("Attachment is not base64: " + content.toString());
                System.err.println("Skipping");
                continue;
            }
            files.add(saveStream(stream, "" + uid));

            if (markMessageAsSeen) {
                System.out.println("Marking message as seen.");
                message.setFlag(Flags.Flag.SEEN, true);
            }
        }
        inbox.close(false);
        return files;
    }

    static File saveStream(InputStream stream, String filename) throws Exception {
        Pair<InputStream, Converter.FileType> detected = Converter.detectFileType(stream);
        File inFolder;
        switch (detected.snd) {
            case OpenTrans:
                // Detected OpenTrans
                inFolder = openTransFolder;
                break;
            case Edifact:
            default:
                // Detected Edifact
                inFolder = edifactFolder;
                break;
        }

        File f = new File(inFolder, filename);
        FileOutputStream fos = new FileOutputStream(f);
        IOUtils.copy(detected.fst, fos);

        System.out.println("Saved file to " + f.getAbsolutePath());
        return f;
    }

                }
            }
        }
    }
}
