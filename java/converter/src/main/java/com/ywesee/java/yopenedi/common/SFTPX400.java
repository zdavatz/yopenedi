package com.ywesee.java.yopenedi.common;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;

public class SFTPX400 {
    public String host;
    public String username;
    public String privateKeyPath;
    public String knownHosts;
    public String messageFolder;

    public SFTPX400(File file) throws IOException, ParseException {
        String str = FileUtils.readFileToString(file, "UTF-8");
        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject)parser.parse(str);
        this.host = (String)obj.get("host");
        this.username = (String)obj.get("username");
        this.privateKeyPath = (String)obj.get("privateKeyPath");
        this.knownHosts = (String)obj.getOrDefault("knownHosts", null);
        this.messageFolder = (String)obj.getOrDefault("messageFolder", null);
    }
}
