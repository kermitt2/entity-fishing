/*
 *    HtmlPreprocessor.java
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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.wikipedia.miner.annotation.preprocessing.PreprocessedDocument.RegionTag;

/**
 * This class prepares html documents so that they can be tagged by a document tagger.
 * 
 * @author David Milne
 */
public class HtmlPreprocessor extends DocumentPreprocessor{
	
	static String[] defaultRegionTags = {"div", "table"} ;
	static String[] defaultSplitterTags = {"h1", "h2"} ;

	
	/**
	 * Creates an preprocessor for html documents, with the default region tags. It will only treat divs and tables as regions, and 
	 * will split regions on h1 and h2 tags. It will not ban any topics. 
	 */
	public HtmlPreprocessor() {
		super(getStartTagRegex(defaultRegionTags), getEndTagRegex(defaultRegionTags), getTagRegex(defaultSplitterTags)) ;
	}
	
	
	/**
	 * Creates an preprocessor for html documents, that will recognize the given tags. When specifying tags, just give the name. eg "div", not "&lt;div&gt;" ;
	 * 
	 * @param regionTags specifies which tags to treat as regions. e.g. "div" and "table"
	 * @param splitterTags  specifies which tags to treat as region splitters. e.g. "h1" and "h2"
	 */
	public HtmlPreprocessor(String[] regionTags, String[] splitterTags) {
		super(getStartTagRegex(regionTags), getEndTagRegex(regionTags), getTagRegex(splitterTags)) ;
	}
	
	
	/**
	 * Takes the marked-up content and replaces all tags with blank spaces. 
	 * Everything before the body tag is also replaced with blanks. 
	 * 
	 * @param content the html to be preprocessed
	 * @return the preprocessedString
	 */
        @Override
	public PreprocessedDocument preprocess(final String content) {
		
		StringBuilder context = new StringBuilder() ;
		
		String temp = content.toLowerCase() ;
		
		ArrayList<RegionTag> regionTags = getRegionTags(temp) ;
		
		
		
		//find point where body starts
		int bodyStart = temp.indexOf("<body") ;
		
		if (bodyStart < 0) 
			bodyStart = 0 ;
		
		
		//System.out.println(temp.substring(0, bodyStart)) ;
		
		
		//add title to context
		Pattern p = Pattern.compile("<title([^>]*)>(.*?)</title>", Pattern.DOTALL) ;
		Matcher m = p.matcher(temp.substring(0, bodyStart)) ;
		while (m.find()) {
			context.append(m.group(2)) ;
			context.append(".\n") ;
		}
		
		//add metadata to context
		p = Pattern.compile("<meta(.*?)/>", Pattern.DOTALL) ;
		m = p.matcher(temp.substring(0, bodyStart)) ;
		while (m.find()) {	
			String tag = m.group() ;
			String tName = getAttributeValue(tag, "name") ;
			String tContent = getAttributeValue(tag, "content") ;
			
			if ((tName.equals("keywords") || tName.equals("description")) && !tContent.equals("")) {
				context.append(tContent) ;
				context.append("\n") ;
			}
		}
		
		temp = content.substring(bodyStart) ;

		// process links, adding anchors to context
		
		p = Pattern.compile("<a([^>]*)>(.*?)</a>", Pattern.DOTALL) ;
		m = p.matcher(temp) ;
		
		int lastPos = 0 ;
		StringBuilder sbTemp = new StringBuilder() ;

		while(m.find()) {
			sbTemp.append(temp.substring(lastPos, m.start())) ;
			sbTemp.append(getSpaceString(m.group().length())) ;
			
			//links may contain other tags, lets get down to the raw text.
			String linkContent = clearAllMentions("<(.*?)>", m.group(2)).trim() ;			
			if (!linkContent.equals("")) {
				context.append(linkContent) ;
				context.append(".\n") ;
			}
			lastPos = m.end() ;		
		}

		sbTemp.append(temp.substring(lastPos)) ;
		temp = sbTemp.toString() ;
		
		// process comments
		temp = clearAllMentions("<!--(.*?)-->", temp) ;
				
		// process scripts
		temp = clearAllMentions("<script(.*?)</script>", temp) ;
		
		// process remaining tags
		temp = clearAllMentions("<(.*?)>", temp) ;
		
		// process entities
		temp = clearAllMentions("&\\w{2,6};", temp) ;
				
		return new PreprocessedDocument(content, getSpaceString(bodyStart)+temp, context.toString(), regionTags, null) ;
	}
	
	private String getAttributeValue(String tag, String attributeName) {
		
		Pattern p = Pattern.compile(attributeName + "\\W*=\\W*\"(.*?)\"", Pattern.DOTALL) ;
		Matcher m = p.matcher(tag) ;
		if (m.find()) 
			return m.group(1) ; 
		else 
			return "" ;
	}
	
	/**
	 * Convenience method for generating patterns that will match all opening tags of the given types 
	 * 
	 * @param tags the types of tags of interest
	 * @return a regular expression that will match all opening tags of the given types
	 */
	public static Pattern getStartTagRegex(String[] tags) {
		if (tags == null || tags.length == 0)
			return null ;
		
		if (tags.length == 1)
			return Pattern.compile("<" + tags[0] + "[^>]*>", Pattern.CASE_INSENSITIVE) ;
		
		StringBuilder regex = new StringBuilder() ;
		
		for (String tag:tags) 
			regex.append(tag).append("|") ;	

		regex.deleteCharAt(regex.length()-1) ;
		
		return Pattern.compile("<(" + regex.toString() + ")[^>]*>", Pattern.CASE_INSENSITIVE) ;
	}
	
	/**
	 * Convenience method for generating patterns that will match all closing tags of the given types 
	 * 
	 * @param tags the types of tags of interest
	 * @return a regular expression that will match all closing tags of the given types
	 */
	public static Pattern getEndTagRegex(String[] tags) {
		if (tags == null || tags.length == 0)
			return null ;
		
		if (tags.length == 1)
			return Pattern.compile("</" + tags[0] + "[^>]*>", Pattern.CASE_INSENSITIVE) ;
		
		StringBuilder regex = new StringBuilder() ;
		
		for (String tag:tags) 
			regex.append(tag).append("|") ;	

		regex.deleteCharAt(regex.length()-1) ;
		
		return Pattern.compile("</(" + regex.toString() + ")[^>]*>", Pattern.CASE_INSENSITIVE) ;
	}
	
	/**
	 * Convenience method for generating patterns that will match all tags (opening, closing, singular) of the given types 
	 * 
	 * @param tags the types of tags of interest
	 * @return a regular expression that will match all tags of the given types
	 */
	public static Pattern getTagRegex(String[] tags) {
		
		if (tags == null || tags.length == 0)
			return null ;
		
		if (tags.length == 1)
			return Pattern.compile("</*" + tags[0] + "[^>]*>", Pattern.CASE_INSENSITIVE) ;
		
		StringBuilder regex = new StringBuilder() ;
		
		for (String tag:tags) 
			regex.append(tag).append("|") ;	

		regex.deleteCharAt(regex.length()-1) ;
		
		return Pattern.compile("</*(" + regex.toString() + ")[^>]*>", Pattern.CASE_INSENSITIVE) ;
	}
}
