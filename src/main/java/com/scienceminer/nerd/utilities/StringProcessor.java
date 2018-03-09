package com.scienceminer.nerd.utilities;

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.TextUtilities;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.lowerCase;

public class StringProcessor {
    /**
     * Adjust the case along with these rules:
     *  - should the title be fully upper case, we lowercase it
     *  - should all the tokens of the title starts with the uppercase letter, we lowercase it
     *
     *  @return a modified copy of the initial LayoutToken
     */
    public static List<LayoutToken> adjustLetterCase(List<LayoutToken> titleTokens) {
        //working on a copy 
        List<LayoutToken> result = new ArrayList<>(titleTokens);
        
        final String titleString = LayoutTokensUtil.toText(titleTokens);
        if(isAllUpperCase(titleString)) {
            for (LayoutToken token : result) {
                token.setText(lowerCase(token.getText()));
            }
        } else {
            int count = 0;
            int total = 0;
            for (LayoutToken token : result) {
                final String tokenText = token.getText();
                if(!TextUtilities.delimiters.contains(tokenText)) {
                    total++;

                    if (tokenText.length() == 1) {
                        if (TextUtilities.isAllUpperCase(tokenText)) {
                            count++;
                        }
                    }else if(tokenText.length() > 1) {
                        if(Character.isUpperCase(tokenText.charAt(0))
                                && TextUtilities.isAllLowerCase(tokenText.substring(1, tokenText.length()))) {
                            count++;
                        }
                    }
                }
            }
            if(count == total) {
                boolean first = true;
                for (LayoutToken token : titleTokens) {
                    if(first) {
                        first = false;
                        continue;
                    }
                    token.setText(lowerCase(token.getText()));
                }
            }
        }

        return result;
    }

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

    //Could be replaced by StringUtils.join(words, start, end)
    public static String concat(List<String> words, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append(words.get(i));
        }
        return sb.toString();
    }

    public static boolean isAllUpperCase(String text) {
        if (text.equals(text.toUpperCase()))
            return true;
        else
            return false;
    }

    public static boolean isAllLowerCase(String text) {
        if (text.equals(text.toLowerCase()))
            return true;
        else
            return false;
    }
}
