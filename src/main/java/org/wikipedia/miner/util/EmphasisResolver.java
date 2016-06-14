/*
 *    EmphasisResolver.java
 *    Copyright (C) 2009 David Milne, d.n.milne@gmail.com
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

package org.wikipedia.miner.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This parses MediaWiki syntax for '''bold''' and ''italic'' text with the equivalent html markup.
 * 
 * @author David Milne
 */
public class EmphasisResolver {

	public String resolveEmphasis(String text) {
		
		StringBuilder sb = new StringBuilder();
		
		for (String line:text.split("\n")) {
			sb.append(resolveLine(line));
			sb.append("\n");
		}
		
		sb.deleteCharAt(sb.length()-1);
		
		return sb.toString();
	}

	/**
	 * This is a direct translation of the php function doAllQuotes used by the original MediaWiki software.
	 * 
	 * @param line the line to resolve emphasis within
	 * @return the line, with all emphasis markup resolved to html tags
	 */
	private String resolveLine(String line) {
		
		//System.out.println("Resolving line '" + line + "'");

		String[] arr = getSplits("$"+line);

		if (arr.length <= 1)
			return line;

		//First, do some preliminary work. This may shift some apostrophes from
		//being mark-up to being text. It also counts the number of occurrences
		//of bold and italics mark-ups.

		int numBold = 0;
		int numItalics = 0;

		for (int i=0; i<arr.length; i++) {
			if (i % 2 == 1) {
				//If there are ever four apostrophes, assume the first is supposed to
				// be text, and the remaining three constitute mark-up for bold text.
				if (arr[i].length() == 4) {
					arr[i-1] = arr[i-1] + "'";
					arr[i] = getFilledString(3);
				} else if (arr[i].length() > 5) {
					//If there are more than 5 apostrophes in a row, assume they're all
					//text except for the last 5.
					arr[i-1] = arr[i-1] + getFilledString(arr[i].length()-5);
					arr[i] = getFilledString(5);
				}

				switch(arr[i].length()) {
				case 2:
					numItalics++;
					break;
				case 3:
					numBold++;
					break;
				case 5:
					numItalics++;
					numBold++;
				}
			}
		}

		//If there is an odd number of both bold and italics, it is likely
		//that one of the bold ones was meant to be an apostrophe followed
		//by italics. Which one we cannot know for certain, but it is more
		//likely to be one that has a single-letter word before it.

		if ((numBold%2==1) && (numItalics%2==1)) {
			int i= 0;
			int firstSingleLetterWord = -1;
			int firstMultiLetterWord = -1;
			int firstSpace = -1;

			for (String r:arr) {
				if ((i%2==1) && r.length()==3) {
					
					//added these checks to avoid string out of bounds exceptions
					if (i==0) continue;
					if (arr[i-1].length() < 2) continue;
					
					char x1 = arr[i-1].charAt(arr[i-1].length()-1);
					char x2 = arr[i-1].charAt(arr[i-1].length()-2);

					if (x1==' ') {
						if (firstSpace == -1)
							firstSpace = i;
					} else if (x2==' ') {
						if (firstSingleLetterWord == -1)
							firstSingleLetterWord = i;
					} else {
						if (firstMultiLetterWord == -1)
							firstMultiLetterWord = i;
					}
				}
				i++;
			}

			// If there is a single-letter word, use it!
			if (firstSingleLetterWord > -1) {
				arr[firstSingleLetterWord] = "''";
				arr[firstSingleLetterWord-1] = arr[firstSingleLetterWord] + "'";
			} else if (firstMultiLetterWord > -1) {
				// If not, but there's a multi-letter word, use that one.
				arr[firstMultiLetterWord] = "''";
				arr[firstMultiLetterWord-1] = arr[firstMultiLetterWord] + "'";
			} else if (firstSpace > -1) {
				// ... otherwise use the first one that has neither.
				// (notice that it is possible for all three to be -1 if, for example,
				// there is only one pentuple-apostrophe in the line)
				arr[firstSpace] = "''";
				arr[firstSpace-1] = arr[firstSpace] + "'";
			}

		}

		StringBuilder output = new StringBuilder();
		StringBuffer buffer = new StringBuffer();
		String state = "";
		int i = 0;
		for (String r:arr) {
			if (i%2==0) {
				if (state.equals("both")) 
					buffer.append(r);
				else
					output.append(r);
			} else {
				if (r.length() == 2 ) {
					if ( state.equals("i")) {
						output.append("</i>"); 
						state = "";
					} else if (state.equals("bi")) {
						output.append("</i>"); 
						state = "b";
					} else if ( state.equals("ib")) {
						output.append("</b></i><b>"); 
						state = "b";
					} else if ( state.equals("both")) {
						output.append("<b><i>");
						output.append(buffer.toString());
						output.append("</i>");
						state = "b";
					} else { 
						//$state can be "b" or ""
						output.append("<i>"); 
						state = state + "i";
					}
				} else if ( r.length() == 3 ) {
					if ( state.equals("b") ) {
						output.append("</b>"); 
						state = "";
					} else if ( state.equals("bi")) {
						output.append("</i></b><i>"); 
						state = "i";
					} else if ( state.equals("ib")) {
						output.append("</b>"); 
						state = "i";
					} else if ( state.equals("both")) {
						output.append("<i><b>");
						output.append(buffer);
						output.append("</b>");
						state = "i";
					} else { 
						//$state can be "i" or ""
						output.append("<b>"); 
						state = state + "b";
					}
				} else if ( r.length() == 5 ) {
					if ( state.equals("b")) {
						output.append("</b><i>");
						state = "i";
					} else if ( state.equals("i")) {
						output.append("</i><b>"); 
						state = "b";
					} else if ( state.equals("bi")) {
						output.append("</i></b>"); 
						state = "";
					} else if ( state.equals("ib")) {
						output.append("</b></i>"); 
						state = "";
					} else if ( state.equals("both")) {
						output.append("<i><b>");
						output.append(buffer);
						output.append("</b></i>"); 
						state = "";
					} else { 
						// ($state == "")
						buffer = new StringBuffer();
						state = "both";
					}
				}
			}
			i++;
		}

		//Now close all remaining tags.  Notice that the order is important.
		if ( state.equals("b") || state.equals("ib")) {
			output.append("</b>");
		}
		if ( state.equals("i") || state.equals("bi") || state.equals("ib") ) {
			output.append("</i>");
		}
		if ( state.equals("bi") ) {
			output.append("</b>");
		}
		// There might be lonely ''''', so make sure we have a buffer
		if ( state.equals("both") && buffer.length() > 0 ) {
			output.append("<b><i>");
			output.append(buffer);
			output.append("</i></b>");
		}
		
		//remove leading $
		output.deleteCharAt(0);

		return output.toString();

	}
	
	/*
	 * Does the same job as php function preg_split 
	 */
	private String[] getSplits(String text) {

		ArrayList<String> splits = new ArrayList<String>() ;

		Pattern p = Pattern.compile("\\'{2,}");

		Matcher m = p.matcher(text);
		int lastCopyIndex = 0;
		while (m.find()) {
			if (m.start() > lastCopyIndex)
				splits.add(text.substring(lastCopyIndex, m.start()));

			splits.add(m.group());

			lastCopyIndex = m.end();
		}

		if (lastCopyIndex < text.length()-1) {
			splits.add(text.substring(lastCopyIndex));
		}

		return splits.toArray(new String[splits.size()]);
	}


	private String getFilledString(int length) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<length; i++)
			sb.append("'");

		return sb.toString();
	}

	public static void main(String[] args) {

		EmphasisResolver er = new EmphasisResolver();

		String markup = "'''War''' is an openly declared state of organized [[violent]] [[Group conflict|conflict]], typified by extreme [[aggression]], [[societal]] disruption, and high [[Mortality rate|mortality]]. As a behavior pattern, warlike tendencies are found in many [[primate]] species, including [[humans]], and also found in many [[ant]] species. The set of techniques used by a group to carry out war is known as '''warfare'''.";

		//String markup = "Parsing '''MediaWiki''''s syntax for '''bold''' and ''italic'' markup is a '''''deceptively''' difficult'' task. Whoever came up with the markup scheme should be '''shot'''."; 

		System.out.println(er.resolveEmphasis(markup));
	}
}
