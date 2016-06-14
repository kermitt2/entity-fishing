/*
 *    Article.java
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

package org.wikipedia.miner.model;

import java.util.Collections;
import org.wikipedia.miner.db.WEnvironment;
import org.wikipedia.miner.db.struct.DbIntList;
import org.wikipedia.miner.db.struct.DbPage;

/**
 * Represents categories in Wikipedia; the pages that exist to hierarchically organise other pages
 */
public class Category extends Page {

	/**
	 * Initialises a newly created Category so that it represents the category given by <em>id</em>.
	 * 
	 * @param env	an active WikipediaEnvironment
	 * @param id	the unique identifier of the article
	 */
	public Category(WEnvironment env, int id) {
		super(env, id) ;
	}
	
	protected Category(WEnvironment env, int id, DbPage pd) {
		super(env, id, pd) ;
	}
	
	/**
	 * Returns an array of Categories that this category belongs to. These are the categories 
	 * that are linked to at the bottom of any Wikipedia category. 
	 * 
	 * @return	an array of Categories (sorted by id)
	 */
	public Category[] getParentCategories() {
		DbIntList tmpParents = env.getDbCategoryParents().retrieve(id) ; 
		if (tmpParents == null || tmpParents.getValues() == null) 
			return new Category[0] ;

		Category[] parentCategories = new Category[tmpParents.getValues().size()] ;

		int index = 0 ;
		for (int id:tmpParents.getValues()) {
			parentCategories[index] = new Category(env, id) ;
			index++ ;
		}

		return parentCategories ;	
	}
	
	/**
	 * Returns an array of Categories that this category contains. These are the categories 
	 * that are presented in alphabetical lists in any Wikipedia category. 
	 * 
	 * @return	an array of Categories, sorted by id
	 */
	public Category[] getChildCategories() {
		DbIntList tmpChildCats = env.getDbChildCategories().retrieve(id) ; 
		if (tmpChildCats == null || tmpChildCats.getValues() == null) 
			return new Category[0] ;

		Category[] childCategories = new Category[tmpChildCats.getValues().size()] ;

		int index = 0 ;
		for (int id:tmpChildCats.getValues()) {
			childCategories[index] = new Category(env, id) ;
			index++ ;
		}

		return childCategories ;	
	}
	
	/**
	 * Returns true if the argument {@link Article} is a child of this category, otherwise false
	 * 
	 * @param article the article of interest
	 * @return	true if the argument article is a child of this category, otherwise false
	 */
	public boolean contains(Article article) {

		DbIntList tmpChildCats = env.getDbChildArticles().retrieve(id) ;
		if (tmpChildCats == null || tmpChildCats.getValues() == null) 
			return false ;
		
		return Collections.binarySearch(tmpChildCats.getValues(), article.getId()) >= 0 ;
	}
	
	/**
	 * Returns an array of {@link Article Articles} that belong to this category.  
	 * 
	 * @return	an array of Articles, sorted by id
	 */
	public Article[] getChildArticles() {

		DbIntList tmpChildArts = env.getDbChildArticles().retrieve(id) ;
		if (tmpChildArts == null || tmpChildArts.getValues() == null) 
			return new Article[0] ;

		Article[] childArticles = new Article[tmpChildArts.getValues().size()] ;

		int index = 0 ;
		for (int id:tmpChildArts.getValues()) {
			childArticles[index] = new Article(env, id) ;
			index++ ;
		}

		return childArticles ;	
	}
	
	
}
