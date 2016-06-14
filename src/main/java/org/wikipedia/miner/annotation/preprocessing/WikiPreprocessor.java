/*
 *    WikiPreprocessor.java
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
import java.util.HashSet;
import java.util.List;
import java.util.regex.*;

import org.wikipedia.miner.annotation.preprocessing.PreprocessedDocument.RegionTag;
import org.wikipedia.miner.model.*;

/**
 * This class prepares documents in MediaWiki markup format so that they can be tagged by a document tagger.
 * 
 * @author David Milne
 */
public class WikiPreprocessor extends DocumentPreprocessor {

	private final Wikipedia wikipedia ;

	/**
	 * Initializes a new WikiPreprocessor. This will treat all section headers (==header==) as separate regions, and
	 * will ban all topics that have already been linked to in the markup. 
	 * 
	 * @param wikipedia an instance of wikipedia
	 */
	public WikiPreprocessor(Wikipedia wikipedia) {
		super(null, null, Pattern.compile("={2,}([^=]+)={2,}") ) ;
		this.wikipedia = wikipedia ;
	}

        @Override
	public PreprocessedDocument preprocess(String content) {

		StringBuffer context = new StringBuffer() ;
		ArrayList<RegionTag> regionTags = getRegionTags(content) ;
		HashSet<Integer> bannedTopics = new HashSet<Integer>() ;

		String temp = blankTemplates(content) ;
		temp = blankTables(temp) ;
		temp = blankLinks(temp, context, bannedTopics) ;
		temp = blankSectionHeaders(temp, context) ;
		
		temp = clearAllMentions("(?s)\\<\\!\\-\\-(.*?)\\-\\-\\>", temp) ; //strip comments
		
		temp = clearAllMentions("<ref\\\\>", temp) ;   //remove simple ref tags
		temp = clearAllMentions("(?s)<ref>(.*?)</ref>", temp) ; //remove ref tags and all content between them. 
		temp = clearAllMentions("(?s)<ref\\s(.*?)>(.*?)</ref>", temp) ; //remove ref tags and all content between them (with attributes).
		
		temp = clearAllMentions("<(.*?)>", temp) ; // remove remaining html tags ;	
		
		temp = clearAllMentions("\\[(http|www)(.*?)\\]", temp) ;  //remove external links ;
		
		temp = clearAllMentions("'{2,}", temp) ;  //remove all bold and italic markup ;
		
		temp = clearAllMentionsRetainFirstCharacter("\n:+", temp) ; //remove indents.
		
		temp = clearAllMentionsRetainFirstCharacter("\n([\\*\\#]+)", temp) ; //remove list markers.
		
		temp = clearAllMentions("&\\w{2,6};", temp) ;  // remove entities
		
		return new PreprocessedDocument(content, temp, context.toString(), regionTags, bannedTopics) ;
	}

	
	private String blankSectionHeaders(String markup, StringBuffer context) {
		
		Pattern p = Pattern.compile("(={2,})([^=]+)\\1") ;
		Matcher m = p.matcher(markup) ;
		
		int lastPos = 0 ;
		StringBuilder sb = new StringBuilder() ;
		
		while(m.find()) {
			sb.append(markup.substring(lastPos, m.start())) ;
			sb.append(getSpaceString(m.group().length())) ;
			
			String title = m.group(2).trim() ;
			
			if (!title.equalsIgnoreCase("see also") && !title.equalsIgnoreCase("external links") && !title.equalsIgnoreCase("references") && !title.equalsIgnoreCase("further reading"))
				context.append("\n").append(title) ;
			
			lastPos = m.end() ;		
		}
		
		sb.append(markup.substring(lastPos)) ;
		return sb.toString() ;
	}

	private String blankTemplates(String markup) {
		List<Integer> templateStack = new ArrayList<Integer>() ; 

		Pattern p = Pattern.compile("(\\{\\{|\\}\\})") ;
		Matcher m = p.matcher(markup) ;

		StringBuilder sb = new StringBuilder() ;
		int lastIndex = 0 ;

		while (m.find()) {

			String tag = markup.substring(m.start(), m.end()) ;

			if (tag.equals("{{"))
				templateStack.add(m.start()) ;
			else {
				if (!templateStack.isEmpty()) {
					int templateStart = templateStack.size()-1;
					templateStack.remove(templateStack.size()-1) ;

					if (templateStack.isEmpty()) {
						sb.append(markup.substring(lastIndex, templateStart)) ;

						//we have the whole template, with other templates nested inside		
						for (int i=templateStart; i<m.end() ; i++)
							sb.append(" ") ;

						lastIndex = m.end() ;
					}
				}
			}
		}

		if (!templateStack.isEmpty())
			System.err.println("WikiPreprocessor | Warning: templates were not well formed, so we cannot guarantee that they were stripped out correctly. ") ;

		sb.append(markup.substring(lastIndex)) ;
		return sb.toString() ;
	}

	private String blankTables(String markup) {	
		List<Integer> tableStack = new ArrayList<Integer>() ; 

		Pattern p = Pattern.compile("(\\{\\||\\|\\})") ;
		Matcher m = p.matcher(markup) ;

		StringBuilder sb = new StringBuilder() ;
		int lastIndex = 0 ;

		while (m.find()) {

			String tag = markup.substring(m.start(), m.end()) ;

			if (tag.equals("{|"))
				tableStack.add(m.start()) ;
			else {
				if (!tableStack.isEmpty()) {
					int templateStart = tableStack.size()-1 ;
					tableStack.remove(tableStack.size()-1) ;
					if (tableStack.isEmpty()) {
						sb.append(markup.substring(lastIndex, templateStart)) ;

						for (int i=templateStart; i<m.end() ; i++)
							sb.append(" ") ;
						
						lastIndex = m.end() ;
					}
				}
			}
		}

		if (!tableStack.isEmpty())
			System.err.println("WikiPreprocessor | Warning: tables were not well formed, so we cannot guarantee that they were stripped out correctly. ") ;

		sb.append(markup.substring(lastIndex)) ;		
		return sb.toString() ;
	}




	private String blankLinks(String markup, StringBuffer context, HashSet<Integer> bannedTopics) {

		List<Integer> linkStack = new ArrayList<Integer>() ; 

		Pattern p = Pattern.compile("(\\[\\[|\\]\\])") ;
		Matcher m = p.matcher(markup) ;

		StringBuilder sb = new StringBuilder() ;
		int lastIndex = 0 ;

		while (m.find()) {
			String tag = markup.substring(m.start(), m.end()) ;

			if (tag.equals("[["))
				linkStack.add(m.start()) ;
			else {
				if (!linkStack.isEmpty()) {
					int linkStart = linkStack.size()-1 ;
					linkStack.remove(linkStack.size()-1) ;

					if (linkStack.isEmpty()) {
						sb.append(markup.substring(lastIndex, linkStart)) ;

						//we have the whole link, possibly with other links nested inside.
						for (int i=linkStart; i<m.end() ; i++)
							sb.append(" ") ;

						processLink(markup.substring(linkStart+2, m.start()), context, bannedTopics) ;

						lastIndex = m.end() ;
					}
				}
			}
		}

		if (!linkStack.isEmpty()) {
			System.err.println("WikiPreprocessor| Warning: links were not well formed, so we cannot guarantee that they were stripped out correctly. ") ;
		}

		sb.append(markup.substring(lastIndex)) ;
		return sb.toString() ;
	}

	private String clearAllMentionsRetainFirstCharacter(String regex, String text) {
		
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE + Pattern.DOTALL) ;
		Matcher m = p.matcher(text) ;
		
		int lastPos = 0 ;
		StringBuilder sb = new StringBuilder() ;
		
		while(m.find()) {
			sb.append(text.substring(lastPos, m.start())) ;
			
			sb.append(text.charAt(m.start())) ;
			
			for (int i=1 ; i <m.group().length() ; i++)
				sb.append(" ") ;
			
			lastPos = m.end() ;		
		}
		
		sb.append(text.substring(lastPos)) ;
		return sb.toString() ;
	}
	
	
	

	private void processLink(String markup, StringBuffer context, HashSet<Integer> bannedTopics) {

		//ignore everything that is not in main namespace
		if (markup.indexOf(":") > 0) 
			return ;

		String anchor = markup ;
		String dest = markup ;

		int pos = markup.lastIndexOf("|") ;	
		if (pos>0) {
			anchor = markup.substring(pos+1) ;
			dest = markup.substring(0, pos) ;
		}

		context.append("\n").append(anchor) ;

		Article art = wikipedia.getArticleByTitle(dest) ;
		if (art != null) {
			bannedTopics.add(art.getId()) ;
		}
	}

}
