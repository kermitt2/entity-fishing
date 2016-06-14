/*
 *    ArticleCleaner.java
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

package org.wikipedia.miner.annotation;

import org.wikipedia.miner.util.MarkupStripper;
import org.wikipedia.miner.model.Article;

/**
 *	A utility class for cleaning up Wikipedia articles so that they can be used to train and test disambiguation, link detection, etc.
 *
 * @author David Milne
 */
public class ArticleCleaner {
	
	public enum SnippetLength { full, firstSentence, firstParagraph } ;	
	
	private MarkupStripper stripper ;
	
	/**
	 * Initializes a new ArticleCleaner
	 */
	public ArticleCleaner() {
		stripper = new MarkupStripper() ;
	}
	
	
	/**
	 * @param article the article to clean
	 * @param length the portion of the article that is to be extracted and cleaned (ALL, FIRST_SENTENCE, or FIRST_PARAGRAPH)
	 * @return the content (or snippet) of the given article, with all markup removed except links to other articles.  
	 * @throws Exception
	 */
	public String getMarkupLinksOnly(Article article, SnippetLength length) throws Exception {
		
		String markup ;
		
		switch (length) {
		
		case firstSentence :
			markup = article.getSentenceMarkup(0) ;
			break ;
		case firstParagraph :
			markup = article.getFirstParagraphMarkup() ;
			break ;
		default : 
			markup = article.getMarkup() ;
			break ;
		}
				
		markup = stripper.stripAllButInternalLinksAndEmphasis(markup, null) ;
		markup = stripper.stripNonArticleInternalLinks(markup, null) ;
		
				
		return markup ;
		
		
		
	}
	
	/**
	 * @param article the article to clean
	 * @param length the portion of the article that is to be extracted and cleaned (ALL, FIRST_SENTENCE, or FIRST_PARAGRAPH)
	 * @return the content of the given article, with all markup removed.  
	 * @throws Exception
	 */
	public String getCleanedContent(Article article, SnippetLength length) throws Exception{
		
		
		String markup ;
		
		switch (length) {
		
		case firstSentence :
			markup = article.getSentenceMarkup(0) ;
			break ;
		case firstParagraph :
			markup = article.getFirstParagraphMarkup() ;
			break ;
		default : 
			markup = article.getMarkup() ;
			break ;
		}
		
		markup = stripper.stripToPlainText(markup, null) ;
				
		return markup ;
	}
}
