package com.scienceminer.nerd.utilities;

public class StringProcessor {
    public static String removeInvalidUtf8Chars(String inString) {
        if (inString == null) return null;

        StringBuilder newString = new StringBuilder();
        char ch;

        for (int i = 0; i < inString.length(); i++){
            ch = inString.charAt(i);
            if ((ch < 0x00FD && ch > 0x001F) || ch == '\t' || ch == '\n' || ch == '\r') {
                newString.append(ch);
            }
        }
        return newString.toString();

    }
}
