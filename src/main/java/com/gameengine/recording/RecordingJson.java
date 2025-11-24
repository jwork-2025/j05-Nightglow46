package com.gameengine.recording;

import java.util.ArrayList;
import java.util.List;

public final class RecordingJson {
    private RecordingJson() {}

    public static String field(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        int c = json.indexOf(':', i);
        if (c < 0) return null;
        int end = c + 1;
        
        // Find the end of the value, respecting nested structures
        int depth = 0;
        boolean inQuote = false;
        for (int k = end; k < json.length(); k++) {
            char ch = json.charAt(k);
            if (ch == '"' && (k == 0 || json.charAt(k-1) != '\\')) {
                inQuote = !inQuote;
            } else if (!inQuote) {
                if (ch == '{' || ch == '[') {
                    depth++;
                } else if (ch == '}' || ch == ']') {
                    if (depth == 0) {
                        return json.substring(end, k).trim();
                    }
                    depth--;
                } else if (ch == ',' && depth == 0) {
                    return json.substring(end, k).trim();
                }
            }
        }
        return json.substring(end).trim();
    }

    public static String stripQuotes(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length()-1);
        }
        return s;
    }

    public static double parseDouble(String s) {
        if (s == null) return 0.0;
        try { return Double.parseDouble(stripQuotes(s)); } catch (Exception e) { return 0.0; }
    }

    public static String[] splitTopLevel(String arr) {
        List<String> out = new ArrayList<>();
        int depth = 0; int start = 0;
        for (int i = 0; i < arr.length(); i++) {
            char ch = arr.charAt(i);
            if (ch == '{' || ch == '[') depth++;
            else if (ch == '}' || ch == ']') depth--;
            else if (ch == ',' && depth == 0) {
                out.add(arr.substring(start, i));
                start = i + 1;
            }
        }
        if (start < arr.length()) out.add(arr.substring(start));
        return out.stream().map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    public static String extractArray(String json, int startIdx) {
        int i = startIdx;
        if (i >= json.length() || json.charAt(i) != '[') return "";
        int depth = 1;
        int begin = i + 1;
        i++;
        for (; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(begin, i);
                }
            }
        }
        return "";
    }
}
