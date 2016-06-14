/*
 *    SimpleStemmer.java
 *    Copyright (C) 2007 Olena Medelyan, olena@cs.waikato.ac.nz
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

package org.wikipedia.miner.util.text.english;

import org.wikipedia.miner.util.text.Cleaner;
import org.wikipedia.miner.util.text.TextProcessor;

/**
 * A basic stemmer that only performs the first step of the 
 * PorterStemmer algorithm: removing of the plural endings.
 * @author olena
 *
 */
public class SimpleStemmer extends TextProcessor{
	
	Cleaner cleaner = new Cleaner() ;
	
	/**
	 * Returns a copy of the argument text, where each term within it is stemmed and cleaned. 
	 * 
	 * @param text	the text to be processed.
	 * @return	the processed version of this text.
	 */
	public String processText(String text) {
		
		String processedText = "" ;
		String[] terms = cleaner.processText(text).split(" ") ;
		
		for (String term: terms) {
			if (!"".equals(term)) 
				processedText = processedText + " " + stem(term) ;
		}
		
		return processedText.trim() ;
	}
	
	private String stem(String str)  {
		// check for zero length
		if (str.length() > 3) {
			// all characters must be letters
			char[] c = str.toCharArray();
			for (int i = 0; i < c.length; i++) {
				if (!Character.isLetter(c[i])) {
					return str.toLowerCase();
				}
			}
		} else {            
			return str.toLowerCase();
		}
		str = step1a(str);
		return str.toLowerCase();
	} 
	
	private String step1a (String str) {
		// SSES -> SS
		if (str.endsWith("sses")) {
			return str.substring(0, str.length() - 2);
			// IES -> x (families -> famil)
		} else if (str.endsWith("ies")) {
			return str.substring(0, str.length() - 3).concat("y");
			// SS -> S
		} else if (str.endsWith("ss")) {
			return str;
			// S ->
		} else if (str.endsWith("s")) {
			return str.substring(0, str.length() - 1);
		} else {
			return str;
		}
	} 
	
}
