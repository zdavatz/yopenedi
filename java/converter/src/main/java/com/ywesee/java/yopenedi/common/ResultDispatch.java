package com.ywesee.java.yopenedi.common;

import com.jcraft.jsch.*;
import com.ywesee.java.yopenedi.converter.Writable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ResultDispatch {
    class EmailDest {
        String to;
        String subject;
        Charset encoding;
        EmailDest(JSONObject obj) {
            this.to = (String)obj.get("to");
            if (obj.containsKey("subject")) {
                this.subject = (String) obj.get("subject");
            } else {
                this.subject = "No subject";
            }
            if (obj.containsKey("encoding") && obj.get("encoding").equals("UTF-8")) {
                encoding = StandardCharsets.UTF_8;
            } else {
                encoding = StandardCharsets.ISO_8859_1;
            }
        }
    }
    class HTTPPost {
        String url;
        Charset encoding;
        HashMap<String, String> headers = new HashMap<>();
        HTTPPost(JSONObject obj) {
            this.url = (String)obj.get("url");
            if (obj.containsKey("headers")) {
                JSONObject headersObj = (JSONObject)obj.get("headers");
                Set<String> keys = (Set<String>)headersObj.keySet();
                for (String key : keys) {
                    this.headers.put(key, (String)headersObj.get(key));
                }
            }
            if (obj.containsKey("encoding") && obj.get("encoding").equals("UTF-8")) {
                encoding = StandardCharsets.UTF_8;
            } else {
                encoding = StandardCharsets.ISO_8859_1;
            }
        }
    }
    class SFTPX400 {
        // e.g. "cn=xxxxx; s=yyyyy; o=zzzzzz-gmbh; p=AAAAA; a=VIAT; c=DE"
        String toAddress;
        String toUserId;
        Charset encoding;
        SFTPX400(JSONObject obj) {
            this.toAddress = (String)obj.getOrDefault("toAddress", null);
            this.toUserId = (String)obj.getOrDefault("toUserId", null);
            if (obj.containsKey("encoding") && obj.get("encoding").equals("UTF-8")) {
                encoding = StandardCharsets.UTF_8;
            } else {
                encoding = StandardCharsets.ISO_8859_1;
            }
        }
    }
    class Filter {
        ArrayList<String> glns = null;
        ArrayList<String> edifactTypes = null;

        Filter(JSONObject obj) {
            if (obj.containsKey("glns")) {
                glns = new ArrayList<>();
                JSONArray glns = (JSONArray) obj.get("glns");
                for (int i = 0; i < glns.size(); i++) {
                    this.glns.add((String) glns.get(i));
                }
            }
            if (obj.containsKey("edifact_types")) {
                edifactTypes = new ArrayList<>();
                JSONArray types = (JSONArray) obj.get("edifact_types");
                for (int i = 0; i < types.size(); i++) {
                    this.edifactTypes.add((String) types.get(i));
                }
            }
        }

        String reasonForNotSending(String gln, String edifactType) {
            String reason = "";
            if (glns != null) {
                if (!glns.contains(gln)) {
                    reason = "The gln is " + gln + ", not one of the following: " + glns.toString() + "\n";
                }
            }
            if (edifactTypes != null) {
                if (!edifactTypes.contains(edifactType)) {
                    reason += "The edifact type is " + edifactType + ", not one of the following: " + edifactTypes.toString() + "\n";
                }
            }
            if (reason.length() > 0) {
                return reason;
            }
            return null;
        }
    }

    Config config;
    Filter filter;
    EmailDest emailDest;
    HTTPPost httpPost;
    SFTPX400 sftpx400;

    ResultDispatch(Config config, JSONObject obj) {
        this.config = config;
        if (obj.containsKey("filters")) {
            this.filter = new Filter((JSONObject)obj.get("filters"));
        }
        if (obj.containsKey("http")) {
            this.httpPost = new HTTPPost((JSONObject)obj.get("http"));
        }
        if (obj.containsKey("email")) {
            this.emailDest = new EmailDest((JSONObject)obj.get("email"));
        }
        if (obj.containsKey("sftp-x400")) {
            this.sftpx400 = new SFTPX400((JSONObject) obj.get("sftp-x400"));
        }
    }

    /**
     * @return Null if it sends, otherwise the reason for not sending
     */
    String send(String gln, String edifactType, Writable writable, String messageId) {
        if (this.filter != null) {
            String reasonForNotSending = this.filter.reasonForNotSending(gln, edifactType);
            if (reasonForNotSending != null) return reasonForNotSending + "----------\n";
        }
        if (this.httpPost != null) {
            this.sendHTTPPost(writable, messageId);
        }
        if (this.sftpx400 != null) {
            this.sendSFPTX400(writable, messageId);
        }
        if (this.emailDest != null) {
            this.sendEmail(writable);
        }
        return null;
    }

    void sendHTTPPost(Writable writable, String messageId) {
        System.out.println("Uploading file to " + this.httpPost.url);
        try {
            URL url = new URL(this.httpPost.url);
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection)con;
            http.setRequestMethod("POST");
            http.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            for (String key : this.httpPost.headers.keySet()) {
                http.setRequestProperty(key, this.httpPost.headers.get(key));
            }
            http.setRequestProperty("Message-ID", messageId);
            http.setDoInput(true);
            http.setDoOutput(true);
            writable.write(con.getOutputStream(), config, this.httpPost.encoding);
            String res = IOUtils.toString(con.getInputStream(), Charset.defaultCharset());
            System.out.println("Response: " + res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendEmail(Writable writable) {
        EmailCredential emailCreds = this.config.getEmailCredential();
        Properties prop = new Properties();
        prop.put("mail.smtp.host", emailCreds.smtpHost);
        prop.put("mail.smtp.port", emailCreds.smtpPort);
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.protocols", "TLSv1.2 TLSv1.3");
        if (emailCreds.secure) {
            prop.put("mail.smtp.starttls.enable", "true"); //TLS
        }

        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(emailCreds.user, emailCreds.password);
                    }
                });
        try {

            System.out.println("Sending email from " + emailCreds.user + " to " + emailDest.to);

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailCreds.user));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(emailDest.to)
            );
            message.setSubject(emailDest.subject);
            Multipart multipart = new MimeMultipart();
            BodyPart messageBodyPart = new MimeBodyPart();

            File file = File.createTempFile("temp", null);
            FileOutputStream outStream = new FileOutputStream(file);
            writable.write(outStream, config, this.emailDest.encoding);

            DataSource source = new FileDataSource(file);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(file.getName());
            multipart.addBodyPart(messageBodyPart);

            // Send the complete message parts
            message.setContent(multipart);

            Transport.send(message);

            System.out.println("Finished sending email");

        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace(System.out);
        }
    }

    void sendSFPTX400(Writable writable, String messageId) {
        try {
            com.ywesee.java.yopenedi.common.SFTPX400 sftpConfig = this.config.getSFTPX400Credential();
            if (sftpConfig == null) {
                System.out.println("Credential for SFTP X.400 not found, skipping");
            }
            JSch jsch = new JSch();
            String privateKeyPath = new File(sftpConfig.privateKeyPath).getAbsolutePath();
            System.out.println("SFTP X.400 privateKeyPath: " + privateKeyPath);
            jsch.addIdentity(privateKeyPath);
            jsch.setKnownHosts(IOUtils.toInputStream(sftpConfig.knownHosts, StandardCharsets.UTF_8));

            com.jcraft.jsch.Session session = jsch.getSession(sftpConfig.username, sftpConfig.host);
            session.connect();
            ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();

            String randomMessageFilename = RandomStringUtils.randomNumeric(9);
            String pwd = sftp.pwd();
            File messageFile = this.tempFileForX400Message(writable, messageId);
            sftp.put(messageFile.getAbsolutePath(), pwd + "/M_" + randomMessageFilename + ".TMP");
            System.out.println("Uploaded as: " + pwd + "/M_" + randomMessageFilename + ".TMP");

            sftp.rename(pwd + "/M_" + randomMessageFilename + ".TMP", pwd + "/M_" + randomMessageFilename + ".IN");
            System.out.println("Renamed to: " + pwd + "/M_" + randomMessageFilename + ".IN");
            sftp.exit();
            session.disconnect();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public File tempFileForX400Message(Writable writable, String messageId) throws IOException {
        File tempFile = File.createTempFile("temp", null);
        FileOutputStream outStream = new FileOutputStream(tempFile);
        String boundaryString = RandomStringUtils.randomAlphabetic(10);
        try {
            outStream.write(("To: \"" + (this.sftpx400.toAddress == null ? "" : this.sftpx400.toAddress) + "\" ").getBytes(StandardCharsets.UTF_8));
            outStream.write(("<" + (this.sftpx400.toUserId == null ? "x" : this.sftpx400.toUserId) + "@viat.de>\n").getBytes(StandardCharsets.UTF_8));
            if (messageId != null) {
                outStream.write(("Message-ID: " + messageId + "\n").getBytes(StandardCharsets.UTF_8));
            }
            outStream.write("Disposition-Notification-To: \"\"\n".getBytes(StandardCharsets.UTF_8));
            outStream.write(("Content-Type: multipart/mixed; boundary=\"boundary"+ boundaryString +"\"\n\n\n").getBytes(StandardCharsets.UTF_8));
            outStream.write(("--boundary"+ boundaryString +"\n").getBytes(StandardCharsets.UTF_8));

            outStream.write("Content-Type: application/octet-stream\n".getBytes(StandardCharsets.UTF_8));
            outStream.write("Content-Disposition: attachment; filename=\"orders.txt\"\n".getBytes(StandardCharsets.UTF_8));
            outStream.write("Content-Transfer-Encoding: binary\n\n".getBytes(StandardCharsets.UTF_8));

            outStream.write("".getBytes(StandardCharsets.UTF_8));
            byte[] buffer = new byte[1024];
            int length;
            writable.write(outStream, config, this.sftpx400.encoding);
            outStream.write(("\n--boundary"+ boundaryString +"—").getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outStream.close();
        }
        tempFile.deleteOnExit();
        return tempFile;
    }
}
