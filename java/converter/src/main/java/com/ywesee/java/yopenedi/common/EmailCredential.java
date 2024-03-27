package com.ywesee.java.yopenedi.common;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;

public class EmailCredential {
    public String smtpHost;
    public String smtpPort;
    public String imapHost;
    public String imapPort;
    public String user;
    public String password;
    public Boolean secure;

    EmailCredential(File file) throws IOException, ParseException {
        String str = FileUtils.readFileToString(file, "UTF-8");
        str = str.replace("\uFEFF", "");
        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject)parser.parse(str);
        this.smtpHost = (String)obj.get("smtp-host");
        this.smtpPort = (String)obj.get("smtp-port");
        this.imapHost = (String)obj.get("imap-host");
        this.imapPort = (String)obj.get("imap-port");
        this.user = (String)obj.get("user");
        this.password = (String)obj.get("password");
        this.secure = (Boolean)obj.get("secure");
    }
}
