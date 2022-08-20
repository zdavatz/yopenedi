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

    /**
     * This function split a string into multiple parts, according to a length limit of each part.
     * It first tries to split "nicely" by looking at spaces, so it doesn't split in the middle of a word.
     * If it doesn't go well (e.g. a word is longer than lengthLimit, or a string is too long to fit inside maxNumOfParts)
     * it will fall back to the "simple" way, see: splitStringIntoPartsSimply
     *
     * Umlauts are considered as length 2, see https://github.com/zdavatz/yopenedi/issues/244
     * @param input
     * @param lengthLimit
     * @param maxNumOfParts
     * @return
     */
    public static ArrayList<String> splitStringIntoParts(String input, int lengthLimit, int maxNumOfParts) {
        input = Normalizer.normalize(input, Normalizer.Form.NFC);
        ArrayList<String> arr = new ArrayList<>();
        String[] parts = input.split(" ");
        boolean fallbackToSimple = false;
        for (String part : parts) {
            int partLength = stringLengthWithUmlautAsDouble(part);
            if (partLength > lengthLimit) {
                fallbackToSimple = true;
                break;
            }

            if (arr.size() == 0) {
                arr.add(part);
                continue;
            }

            String prev = arr.get(arr.size() - 1);
            if (stringLengthWithUmlautAsDouble(prev) + partLength + 1 <= lengthLimit) {
                arr.set(arr.size() - 1, prev + " " + part);
            } else {
                arr.add(part);
            }
            if (arr.size() > maxNumOfParts) {
                fallbackToSimple = true;
                break;
            }
        }
        if (fallbackToSimple) {
            return splitStringIntoPartsSimply(input, lengthLimit, maxNumOfParts);
        }
        return arr;
    }

    /**
     * This function split a string into multiple parts, according to a length limit of each part.
     * This function is not space/punctuation-aware, it cut through words.
     *
     * Umlauts are considered as length 2, see https://github.com/zdavatz/yopenedi/issues/244
     * @param input
     * @param lengthLimit
     * @param maxNumOfParts
     * @return
     */
    public static ArrayList<String> splitStringIntoPartsSimply(String input, int lengthLimit, int maxNumOfParts) {
        if (input == null) return new ArrayList<>();
        input = Normalizer.normalize(input, Normalizer.Form.NFC);
        ArrayList<String> arr = new ArrayList<>();
        String currentString = "";
        int currentStringLength = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int thisCharLength = (c == 'ä' || c == 'ö' || c == 'ü') ? 2 : 1;
            if (currentStringLength + thisCharLength > lengthLimit) {
                arr.add(currentString);
                currentString = "";
                currentStringLength = 0;
                if (arr.size() >= maxNumOfParts) {
                    break;
                }
            }
            currentString += c;
            currentStringLength += thisCharLength;
        }
        arr.add(currentString);
        return arr;
    }

    /// Umlauts are considered as length 2, see https://github.com/zdavatz/yopenedi/issues/244
    public static int stringLengthWithUmlautAsDouble(String str) {
        if (str == null) return 0;
        int length = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == 'ä' || c == 'ö' || c == 'ü') {
                length += 2;
            } else {
                length += 1;
            }
        }
        return length;
    }

    /// Umlauts are considered as length 2, see https://github.com/zdavatz/yopenedi/issues/244
    public static String leftWithUmlautAsDouble(String str, int limit) {
        if (str == null) return null;
        int length = 0;
        String resultString = "";
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int thisCharLength = (c == 'ä' || c == 'ö' || c == 'ü') ? 2 : 1;
            if (length + thisCharLength > limit) {
                return resultString;
            }
            resultString += c;
            length += thisCharLength;
        }
        return resultString;
    }
}
