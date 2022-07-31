package com.ywesee.java.yopenedi.converter;

import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class Utility {

    public static String concatStrings(String separator, String... strings) {
        String str = "";
        for (String s : strings) {
            if (s == null) continue;
            if (str.length() != 0) {
                str += separator;
            }
            str += s;
        }
        return str;
    }

    public static String shortestInList(List<String> strs) {
        int length = 0;
        String current = null;
        for (String str : strs) {
            if (length == 0 || str.length() < length) {
                current = str;
                length = current.length();
            }
        }
        return current;
    }

    public static String longestInList(List<String> strs) {
        int length = 0;
        String current = null;
        for (String str : strs) {
            if (length == 0 || str.length() > length) {
                current = str;
                length = current.length();
            }
        }
        return current;
    }

    public static String formatDateISO(Date date) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        return df.format(date);
    }

    public static Date dateFromISOString(String str) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            df.setTimeZone(tz);
            return df.parse(str);
        } catch (ParseException e) {
        }
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            df.setTimeZone(tz);
            return df.parse(str);
        } catch (ParseException e) {
        }
        return null;
    }

    public static boolean notNullOrEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    public static <T> T getIndexOrNull(List<T> list, int index) {
        if (list.size() > index) {
            return list.get(index);
        }
        return null;
    }

    public static boolean isAllDigit(String s) {
        char[] chars = s.toCharArray();
        for (char c : chars) {
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public static ArrayList<String> splitStringIntoParts(String input, int lengthLimit, int maxNumOfParts) {
        input = Normalizer.normalize(input, Normalizer.Form.NFC);
        ArrayList<String> arr = new ArrayList<>();
        String[] parts = input.split(" ");
        for (String part : parts) {
            String cur;
            boolean createNew;
            if (arr.size() > 0) {
                cur = arr.get(arr.size() - 1);
                createNew = false;
            } else {
                cur = "";
                createNew = true;
            }
            String newStr;
            if (arr.size() == 0 || (cur.length() + part.length() + 1 > lengthLimit && arr.size() < maxNumOfParts)) {
                cur = "";
                createNew = true;
                newStr = part;
            } else {
                newStr = cur + " " + part;
            }
            if (createNew) {
                arr.add(newStr);
            } else {
                arr.set(arr.size() - 1, newStr);
            }
        }
        return arr;
    }
}
