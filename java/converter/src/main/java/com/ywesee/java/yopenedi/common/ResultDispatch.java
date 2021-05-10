package com.ywesee.java.yopenedi.common;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

public class ResultDispatch {
    class EmailDest {
        String to;
        String subject;
        EmailDest(JSONObject obj) {
            this.to = (String)obj.get("to");
            if (obj.containsKey("subject")) {
                this.subject = (String) obj.get("subject");
            } else {
                this.subject = "No subject";
            }
        }
    }
    class HTTPPost {
        String url;
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

        boolean shouldSend(String gln, String edifactType) {
            String a = null;
            if (glns != null) {
                if (!glns.contains(gln)) {
                    return false;
                }
            }
            if (edifactTypes != null) {
                if (!edifactTypes.contains(edifactType)) {
                    return false;
                }
            }
            return true;
        }
    }

    Config config;
    Filter filter;
    EmailDest emailDest;
    HTTPPost httpPost;

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
    }

    void send(String gln, String edifactType, File file, String messageId) {
        if (this.filter != null && !this.filter.shouldSend(gln, edifactType)) {
            return;
        }
        if (this.httpPost != null) {
            this.sendHTTPPost(file, messageId);
        }
        if (this.emailDest != null) {
            this.sendEmail(file);
        }
    }

    void sendHTTPPost(File file, String messageId) {
        System.out.println("Uploading file (" + file.getAbsolutePath() + ") to " + this.httpPost.url);
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
            IOUtils.copy(new FileInputStream(file), con.getOutputStream());
            String res = IOUtils.toString(con.getInputStream(), Charset.defaultCharset());
            System.out.println("Response: " + res);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendEmail(File file) {
        EmailCredential emailCreds = this.config.getEmailCredential();
        Properties prop = new Properties();
        prop.put("mail.smtp.host", emailCreds.smtpHost);
        prop.put("mail.smtp.port", emailCreds.smtpPort);
        prop.put("mail.smtp.auth", "true");
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
            DataSource source = new FileDataSource(file);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(file.getName());
            multipart.addBodyPart(messageBodyPart);

            // Send the complete message parts
            message.setContent(multipart);

            Transport.send(message);

            System.out.println("Finished sending email");

        } catch (MessagingException e) {
            System.out.println(e.toString());
            e.printStackTrace(System.out);
        }
    }
}
