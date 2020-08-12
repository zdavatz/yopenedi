package com.ywesee.java.yopenedi.converter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Config {
    String path;
    public Config(String path) {
        this.path = path;
    }

    public Map<String, String> udxChannel() {
        if (this.path == null) {
            return new HashMap<>();
        }
        File f = new File(this.path, "udx-channel.json");
        try {
            String str = FileUtils.readFileToString(f, "UTF-8");
            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject)parser.parse(str);
            Map<String, String> map = new HashMap<>();
            for (Object key : obj.keySet()) {
                String value = (String)obj.get(key);
                map.put((String)key, value);
            }
            return map;
        } catch (Exception e) {
            System.err.println("Cannot get file: " + f.getAbsolutePath());
        }
        return new HashMap<>();
    }
}
