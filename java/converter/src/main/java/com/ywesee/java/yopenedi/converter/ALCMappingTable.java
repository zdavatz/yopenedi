package com.ywesee.java.yopenedi.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class ALCMappingTable {
    private static ALCMappingTable singleton = new ALCMappingTable();
    private HashMap<String, String> rexelMap = new HashMap<>();
    private HashMap<String, String> vrgMap = new HashMap<>();

    static public ALCMappingTable getInstance() {
        return singleton;
    }

    private ALCMappingTable() {
        try {
            this.readTable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readTable() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("ALC_mapping_table.csv");
        int lineIndex = -1;
        String line;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
        while(true) {
            lineIndex++;
            if (lineIndex == 0) {
                continue;
            }
            if (!((line = bufferedReader.readLine()) != null)) break;
            String[] parts = line.split(";");
            if (parts.length != 3) {
                continue;
            }
            String code = parts[0];
            String rexel = parts[1];
            String vrg = parts[2];
            this.rexelMap.put(code, rexel);
            this.vrgMap.put(code, vrg);
        }
    }

    public String getRexel(String code) {
        return this.rexelMap.getOrDefault(code, null);
    }

    public String getVrg(String code) {
        return this.vrgMap.getOrDefault(code, null);
    }
}
