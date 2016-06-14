/*
 *    Cleaner.java
 *    Copyright (C) 2007 David Milne, d.n.milne@gmail.com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.wikipedia.miner.util.text;


/**
 * This class provides very conservative morphology, and is intended to only resolve small variations
 * in capitalization and punctuation usage. It casefolds all terms and discards unneeded punctuation. 
 * 
 * This involves adding spaces when underscores or camelcasing is used, converting characters to 
 * lowercase, and discarding whitespace and disambiguation information (the text found within
 * parentheses in many wikipedia titles).
 */	
public class Cleaner extends TextProcessor{
	
	private final boolean disallowInternalPeriods = false ;
		
	/**
	 * Returns a cleaned copy of the argument text, with capitalization and stopwords removed. 
	 * 
	 * @param text	the text to be processed.
	 * @return	the processed version of this text.
	 */
        @Override
	public String processText(String text) {
		String t = text ;
		
		t = cleanPunctuation(t).replace('\n', ' ') ;
		t = t.replaceAll("\\'", ""); //aly added
		return t.replace('\"', ' ').trim().toLowerCase() ;
	}
	
	private String cleanPunctuation(String text) {
		
		StringBuilder resultStr = new StringBuilder();
		int j = 0;
		
		boolean phraseStart = true;
		boolean seenNewLine = false;
		boolean haveSeenHyphen = false;
		boolean haveSeenSlash = false;
		
		while (j < text.length()) {
			boolean isWord = false;
			boolean potNumber = false;
			int startj = j;
			while (j < text.length()) {
				char ch = text.charAt(j);
				if (Character.isLetterOrDigit(ch)) {
					potNumber = true;
					isWord = true;
					//aly: allowing digits as words
					if (Character.isLetter(ch)) {
					 isWord = true;
					}
					
					j++;
				} else if ((!disallowInternalPeriods && (ch == '.')) ||
						(ch == '@') ||
						(ch == '_') ||
						(ch == '&') ||
						(ch == '/') ||
						(ch == '-')) {
					if ((j > 0) && (j  + 1 < text.length()) &&
							Character.isLetterOrDigit(text.charAt(j - 1)) &&
							Character.isLetterOrDigit(text.charAt(j + 1))) {
						j++;
					} else {
						break;
					}
				} else if (ch == '\'') {
					if ((j > 0) &&
							Character.isLetterOrDigit(text.charAt(j - 1))) {
						j++;
					} else {
						break;
					}
				} else {
					break;
				}
			}
			if (isWord == true) {
				if (!phraseStart) {
					if (haveSeenHyphen) {
						resultStr.append('-');
					} else if (haveSeenSlash) {
						resultStr.append('/');
					} else {
						resultStr.append(' ');
					}
				}
				resultStr.append(text.substring(startj, j));
				if (j == text.length()) {
					break;
				}
				phraseStart = false;
				seenNewLine = false;
				haveSeenHyphen = false;
				haveSeenSlash = false;
				if (Character.isWhitespace(text.charAt(j))) {
					if (text.charAt(j) == '\n') {
						seenNewLine = true;
					} 
				} else if (text.charAt(j) == '-') {
					haveSeenHyphen = true;
				} else if (text.charAt(j) == '/') {
					haveSeenSlash = true;
				} else {
					phraseStart = true;
					resultStr.append('\n');
				}
				j++;
			} else if (j == text.length()) {
				break;
			} else if (text.charAt(j) == '\n') {
				if (seenNewLine) {
					if (phraseStart == false) {
						resultStr.append('\n');
						phraseStart = true;
					}
				} else if (potNumber) {
					if (phraseStart == false) {
						phraseStart = true;
						resultStr.append('\n');
					}
				}
				seenNewLine = true;
				j++;
			} else if (Character.isWhitespace(text.charAt(j))) {
				if (potNumber) {
					if (phraseStart == false) {
						phraseStart = true;
						resultStr.append('\n');
					}
				}
				j++;
			} else {
				if (phraseStart == false) {
					resultStr.append('\n');
					phraseStart = true;
				}
				j++;
			}
		}
		
		return resultStr.toString() ;
	}
	
}
