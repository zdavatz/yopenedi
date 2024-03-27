package com.ywesee.java.yopenedi.common;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Config {
    String path;
    boolean isTest;

    public Config(String path, boolean isTest) {
        this.path = path;
        this.isTest = isTest;
    }

    public Map<String, String> udxChannel() {
        if (this.path == null) {
            return new HashMap<>();
        }
        File f = new File(this.path, "udx-channel.json");
        try {
            String str = FileUtils.readFileToString(f, "UTF-8");
            str = str.replace("\uFEFF", "");
            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject)parser.parse(str);
            Map<String, String> map = new HashMap<>();
            for (Object key : obj.keySet()) {
                String value = (String)obj.get(key);
                map.put((String)key, value);
            }
            return map;
        } catch (Exception e) {
            System.out.println("Cannot get file: " + f.getAbsolutePath());
            System.out.println("Error: " + e.toString());
        }
        return new HashMap<>();
    }

    public EmailCredential getEmailCredential() {
        if (isTest) {
            File f = new File(this.path, "test-email-credential.json");
            System.out.println("Reading from " + f.getAbsolutePath());
            try {
                return new EmailCredential(f);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        File f = new File(this.path, "email-credential.json");
        System.out.println("Reading from " + f.getAbsolutePath());
        try {
            return new EmailCredential(f);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public SFTPX400 getSFTPX400Credential() {
        if (isTest) {
            File f = new File(this.path, "test-sftpx400-credential.json");
            System.out.println("Reading from " + f.getAbsolutePath());
            try {
                return new SFTPX400(f);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        File f = new File(this.path, "sftpx400-credential.json");
        System.out.println("Reading from " + f.getAbsolutePath());
        try {
            return new SFTPX400(f);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public ArrayList<ResultDispatch> getResultDispatches() {
        try {
            ArrayList<ResultDispatch> resultDispatches = new ArrayList<>();
            String str = null;
            if (isTest) {
                try {
                    File f = new File(this.path, "test-result-dispatch.json");
                    System.out.println("Reading from " + f.getAbsolutePath());
                    str = FileUtils.readFileToString(f, "UTF-8");
                    str = str.replace("\uFEFF", "");
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
            if (str == null) {
                File f = new File(this.path, "result-dispatch.json");
                System.out.println("Reading from " + f.getAbsolutePath());
                str = FileUtils.readFileToString(f, "UTF-8");
                str = str.replace("\uFEFF", "");
            }
            JSONParser parser = new JSONParser();
            JSONArray arr = (JSONArray) parser.parse(str);
            for (int i = 0; i < arr.size(); i++) {
                resultDispatches.add(
                    new ResultDispatch(this, (JSONObject)arr.get(i))
                );
            }
            return resultDispatches;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return new ArrayList<>();
    }

    public void dispatchResult(String gln, String edifactType, File file, String messageId) {
        ArrayList<ResultDispatch> dispatches = this.getResultDispatches();
        for (ResultDispatch dispatch : dispatches) {
            dispatch.send(gln, edifactType, file, messageId);
        }
    }

    public HashMap<String, String> getGlnOverrideMap() {
        try {
            HashMap<String, String> result = new HashMap<>();
            File f = new File(this.path, "gln-override.json");
            String str = FileUtils.readFileToString(f, "UTF-8");
            str = str.replace("\uFEFF", "");
            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse(str);
            Set<String> keys = obj.keySet();
            for (String key : keys) {
                result.put(key, (String)obj.get(key));
            }
            return result;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return new HashMap<String, String>();
    }

    public <T> void replaceGLN(MessageExchange<T> exchange) {
        String gln = exchange.getRecipientGLN();

        String name = udxChannel().get(gln);
        if (name != null) {
            String replaced = getGlnOverrideMap().get(name);
            if (replaced != null) {
                exchange.setRecipientGLNOverride(replaced);
            }
        }
    }

    public String getNameOfGLN(String gln) {
        return udxChannel().getOrDefault(gln, null);
    }
}
