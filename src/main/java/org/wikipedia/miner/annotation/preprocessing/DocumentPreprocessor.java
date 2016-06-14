/*
 *    DocumentPreprocessor.java
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

package org.wikipedia.miner.annotation.preprocessing;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.*;
import org.wikipedia.miner.annotation.preprocessing.PreprocessedDocument.RegionTag;

/**
 * This abstract class specifies the methods required to preprocess documents so that they can be tagged by a document tagger.
 * 
 * A document preprocessor must be able to recognise and strip out any syntax that should not be considered when matching terms to potential articles in Wikipedia.
 * 
 * Often, when tagging documents, you want to tag only the first mention of each topic within each significant region (chapter, paragraph, html div, etc). To facilitate this, the document preprocessor must also be able to recognize regions which should be tagged independently of each other
 * 
 * @author David Milne 
 */
public abstract class DocumentPreprocessor {
	
	protected Pattern openPattern ;
	protected Pattern closePattern ;
	protected Pattern splitPattern ;
	
	/**
	 * Initialises a DocumentPreprocessor, so that will recognise regions specified with the given tags. 
	 * 
	 * @param openPattern a regular expression that will match tags which indicate the start of a region (e.g. a DIV start tag in a web page)
	 * @param closePattern a regular expression that will match tags which indicate the end of a region (e.g. a DIV end tag in a web page)
	 * @param splitPattern a regular expression that will match tags which indicate where a region should be split in two (e.g. a BR tag in a web page)
	 */
	public DocumentPreprocessor(Pattern openPattern, Pattern closePattern, Pattern splitPattern) {
		
		this.openPattern = openPattern ;
		this.closePattern = closePattern ;
		this.splitPattern = splitPattern ;
	}
		
	/**
	 * DocumentPreprocessors must implement this method, to take the marked-up content and 
	 * replace all tags, special characters, etc. with blank spaces. It is essential that this 
	 * method leaves the original content string unchanged and returns a new string of the same 
	 * length as content. The index of every unchanged character in the new string must be the same
	 * as it was in the original one.
	 * 
	 * @param content the string to be pre-processed
	 * @return the preprocessedString
	 */
	public abstract PreprocessedDocument preprocess(final String content) ;
	
	
	/**
	 * A convenience method that reads and preprocesses the content of a file
	 * 
	 * @param file a file to read and preprocess
	 * @return the preprocessed content of the file.
	 * @throws IOException if the file cannot be read.
	 */
	public PreprocessedDocument preprocess(File file) throws IOException {
		return preprocess(getContent(file)) ;		
	}
	
	
	/**
	 * A convenience method to read the content of a document from file 
	 * 
	 * @param file	the file to be read
	 * @return the content of the file 
	 * @throws IOException if there is a problem reading the file
	 */
	public static String getContent(File file) throws IOException {
		StringBuilder sb = new StringBuilder() ;
		
		BufferedReader reader = new BufferedReader(new FileReader(file)) ;
		String line ;
		
		while ((line=reader.readLine()) != null) {
			sb.append(line).append("\n") ;
		}
		return sb.toString() ;
	}
	
	protected String clearAllMentions(String regex, String text) {
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE + Pattern.DOTALL) ;
		Matcher m = p.matcher(text) ;
		
		int lastPos = 0 ;
		StringBuilder sb = new StringBuilder() ;
		
		while(m.find()) {
			sb.append(text.substring(lastPos, m.start())) ;
			sb.append(getSpaceString(m.group().length())) ;			
			lastPos = m.end() ;		
		}
		
		sb.append(text.substring(lastPos)) ;
		return sb.toString() ;
	}
	
	protected String getSpaceString(int length) {
		StringBuilder sb = new StringBuilder() ;
		
		for (int i=0;i<length;i++)
			sb.append(" ") ;
		
		return sb.toString() ;
	}
	
	protected ArrayList<RegionTag> getRegionTags(String markup) {
		ArrayList<RegionTag> regionTags = new ArrayList<RegionTag>() ;
		
		if (openPattern != null) {
			Matcher m = openPattern.matcher(markup) ;
			while (m.find()) 
				regionTags.add(new RegionTag(m.start(), RegionTag.REGION_OPEN)) ;
		}
		
		if (closePattern != null) {
			Matcher m = closePattern.matcher(markup) ;
			while (m.find()) 
				regionTags.add(new RegionTag(m.start(), RegionTag.REGION_CLOSE)) ;
		}
				
		if (splitPattern != null) {
			Matcher m = splitPattern.matcher(markup) ;
			while (m.find()) 
				regionTags.add(new RegionTag(m.start(), RegionTag.REGION_SPLIT)) ;
		}
		
		Collections.sort(regionTags) ;
		
		return regionTags ;
	}
}
