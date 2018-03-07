package com.scienceminer.nerd.utilities;

import org.apache.commons.lang.StringUtils;
import org.grobid.core.features.FeatureFactory;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.TextUtilities;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.lowerCase;

public class StringProcessor {
    /**
     * Adjust the case along with these rules:
     *  - should the title be fully upper case, we lowercase it
     *  - should all the tokens of the title starts with the uppercase letter, we lowercase it
     */
    public static void adjustLetterCase(List<LayoutToken> titleTokens) {
        final String titleString = LayoutTokensUtil.toText(titleTokens);
        if(StringUtils.upperCase(titleString).equals(titleString)) {
            for (LayoutToken token : titleTokens) {
                token.setText(lowerCase(token.getText()));
            }
        } else {
            int count = 0;
            int total = 0;
            for (LayoutToken token : titleTokens) {
                final String tokenText = token.getText();
                if(!TextUtilities.fullPunctuations.contains(tokenText)) {
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
}
