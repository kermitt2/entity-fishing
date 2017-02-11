package org.wikipedia.miner.util;

import java.util.Vector;
import java.util.regex.Pattern;

import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Wikipedia;

public class ArticleSetBuilder {
	
	private Integer minInLinks = null;
	private Integer minOutLinks = null; 
	
	private Double minLinkProportion = null; 
	private Double maxLinkProportion = null; 
	
	private Integer minWordCount = null; 
	private Integer maxWordCount = null; 
	
	private Double maxListProportion = null; 
	
	private Pattern mustMatch = null; 
	private Pattern mustNotMatch = null; 
	
	private ArticleSet exclude = null;

	public ArticleSetBuilder setMinInLinks(Integer minInLinks) {
		this.minInLinks = minInLinks;
		return this;
	}

	public ArticleSetBuilder setMinOutLinks(Integer minOutLinks) {
		this.minOutLinks = minOutLinks;
		return this;
	}

	public ArticleSetBuilder setMinLinkProportion(Double minLinkProportion) {
		this.minLinkProportion = minLinkProportion;
		return this;
	}

	public ArticleSetBuilder setMaxLinkProportion(Double maxLinkProportion) {
		this.maxLinkProportion = maxLinkProportion;
		return this;
	}

	public ArticleSetBuilder setMinWordCount(Integer minWordCount) {
		this.minWordCount = minWordCount;
		return this;
	}

	public ArticleSetBuilder setMaxWordCount(Integer maxWordCount) {
		this.maxWordCount = maxWordCount;
		return this;
	}

	public ArticleSetBuilder setMaxListProportion(Double maxListProportion) {
		this.maxListProportion = maxListProportion;
		return this;
	}

	public ArticleSetBuilder setMustMatchPattern(Pattern mustMatch) {
		this.mustMatch = mustMatch;
		return this;
	}

	public ArticleSetBuilder setMustNotMatchPattern(Pattern mustNotMatch) {
		this.mustNotMatch = mustNotMatch;
		return this;
	}

	public ArticleSetBuilder setExclude(ArticleSet exclude) {
		this.exclude = exclude;
		return this;
	}
		
	public ArticleSet build(int size, Wikipedia wikipedia) {
		return new ArticleSet(wikipedia, size, minInLinks, minOutLinks, minLinkProportion, 
			maxLinkProportion, minWordCount, maxWordCount, maxListProportion, mustMatch, 
			mustNotMatch, null, exclude);
	}
	
	public ArticleSet[] buildExclusiveSets(int[] sizes, Wikipedia wikipedia) {
		ArticleSet sets[] = new ArticleSet[sizes.length];
		
		ArticleSet exclude = new ArticleSet();
		
		if (this.exclude != null)
			exclude.addAll(this.exclude);
		
		Vector<Article> candidates = null;//ArticleSet.getRoughCandidates(wikipedia, minInLinks, minOutLinks);

		for (int i=0; i<sizes.length; i++) {
			sets[i] = new ArticleSet(wikipedia, sizes[i], minInLinks, minOutLinks, minLinkProportion, 
				maxLinkProportion, minWordCount, maxWordCount, maxListProportion, mustMatch, mustNotMatch, 
				candidates, exclude);			
			exclude.addAll(sets[i]);
		}
		
		return sets;
	}
}
