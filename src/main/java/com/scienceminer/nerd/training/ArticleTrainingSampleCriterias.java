package com.scienceminer.nerd.training;

import java.util.List;

/**
 * Bean for representing the selection criteria of a training sample of Wikipedia articles
 * (following the same criteria as implemented in WikipediaMiner)
 */
public class ArticleTrainingSampleCriterias {

	// the minimum number of links that must be made to an article
	private Integer minInLinks = null;

	// the minimum number of links that an article must make
	private Integer minOutLinks = null; 

	// minLinkProportion the minimum proportion of links (over total words) that articles must contain
	private Double minLinkProportion = null; 

	// maxLinkProportion the maximum proportion of links (over total words) that articles must contain
	private Double maxLinkProportion = null;

	// the minimum number of words allowed in an article
	private Integer minWordCount = null; 

	// maxWordCount the maximum number of words allowed in an article
	private Integer maxWordCount = null; 

	// a list of article ids that should not appear in the sample of articles
	private List<Integer> exclude = null;

	public Integer getMinInLinks() {
		return this.minInLinks;
	}

	public void setMinInLinks(Integer minInLinks) {
		this.minInLinks = minInLinks;
	}

	public Integer getMinOutLinks() {
		return this.minOutLinks;
	}

	public void setMinOutLinks(Integer minOutLinks) {
		this.minOutLinks = minOutLinks;
	}

	public Double getMinLinkProportion() {
		return this.minLinkProportion;
	}

	public void setMinLinkProportion(Double minLinkProportion) {
		this.minLinkProportion = minLinkProportion;
	}

	public Double getMaxLinkProportion() {
		return this.maxLinkProportion;
	}

	public void setMaxLinkProportion(Double maxLinkProportion) {
		this.maxLinkProportion = maxLinkProportion;
	}

	public Integer getMinWordCount() {
		return this.minWordCount;
	}

	public void setMinWordCount(Integer minWordCount) {
		this.minWordCount = minWordCount;
	}

	public Integer getMaxWordCount() {
		return this.maxWordCount;
	}

	public void setMaxWordCount(Integer maxWordCount) {
		this.maxWordCount = maxWordCount;
	}

	public List<Integer> getExclude() {
		return this.exclude;
	}

	public void setExclude(List<Integer> exclude) {
		this.exclude = exclude;
	}
}
