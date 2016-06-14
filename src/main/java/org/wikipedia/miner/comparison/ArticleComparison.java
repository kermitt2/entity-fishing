package org.wikipedia.miner.comparison;

import org.wikipedia.miner.model.Article;

public class ArticleComparison {

	private final Article articleA ;
	private final Article articleB ;
	
	private boolean inLinkFeaturesSet= false ;
	private Double inLinkGoogleMeasure ;
	//private Double inLinkGoogleSentenceMeasure ;
	private Double inLinkVectorMeasure ;
	private Integer inLinkUnion ;
	private Double inLinkIntersectionProportion ;
	
	
	private boolean outLinkFeaturesSet = false;
	private Double outLinkGoogleMeasure ;
	//private Double outLinkGoogleSentenceMeasure ;
	private Double outLinkVectorMeasure ;
	private Integer outLinkUnion ;
	private Double outLinkIntersectionProportion ;
	
	
	public ArticleComparison(Article artA, Article artB) {
		articleA = artA ;
		articleB = artB ;
	}
	
	
	
	public Article getArticleA() {
		return articleA;
	}



	public Article getArticleB() {
		return articleB;
	}


	public boolean inLinkFeaturesSet() {
		return inLinkFeaturesSet ;
	}
	

	public Double getInLinkGoogleMeasure() {
		return inLinkGoogleMeasure;
	}
	
	//public Double getInLinkGoogleSentenceMeasure() {
	//	return inLinkGoogleSentenceMeasure;
	//}
	
	public Double getInLinkVectorMeasure() {
		return inLinkVectorMeasure ;
	}


	public Integer getInLinkUnion() {
		return inLinkUnion;
	}



	public Double getInLinkIntersectionProportion() {
		return inLinkIntersectionProportion;
	}


	public boolean outLinkFeaturesSet() {
		return outLinkFeaturesSet ;
	}

	public Double getOutLinkGoogleMeasure() {
		return outLinkGoogleMeasure;
	}
	
	//public Double getOutLinkGoogleSentenceMeasure() {
	//	return outLinkGoogleSentenceMeasure;
	//}
	
	public Double getOutLinkVectorMeasure() {
		return outLinkVectorMeasure ;
	}

	public Integer getOutLinkUnion() {
		return outLinkUnion;
	}

	public Double getOutLinkIntersectionProportion() {
		return outLinkIntersectionProportion;
	}

	public void setInLinkFeatures(Double googleMeasure, Double vectorMeasure, Integer union, Double intersectionProportion) {
		
		inLinkFeaturesSet = true ;
		inLinkGoogleMeasure = googleMeasure ;
		//inLinkGoogleSentenceMeasure = googleSentenceMeasure ;
		inLinkVectorMeasure = vectorMeasure ;
		inLinkUnion = union ;
		inLinkIntersectionProportion = intersectionProportion ;
	}
	
	public void setOutLinkFeatures(Double googleMeasure, Double vectorMeasure, Integer union, Double intersectionProportion) {
		
		outLinkFeaturesSet = true ;
		outLinkGoogleMeasure = googleMeasure ;
		//outLinkGoogleSentenceMeasure = googleSentenceMeasure ;
		outLinkVectorMeasure = vectorMeasure ;
		outLinkUnion = union ;
		outLinkIntersectionProportion = intersectionProportion ;
	}
	
	protected static double normalizeGoogleMeasure(Double googleMeasure) {
		
		if (googleMeasure == null)
			return 0 ;
		
		if (googleMeasure >= 1)
			return 0 ;
		
		return 1-googleMeasure ;
	}
	
	protected static double normalizeVectorMeasure(Double vectorMeasure) {
		
		if (vectorMeasure == null)
			return 0 ;
		
		if (vectorMeasure == (Math.PI/2))
			return 0 ;
		
		if (vectorMeasure == 0)
			return 1 ;
		
		double sr = (Math.PI/2) - vectorMeasure ; // reverse, so 0=no relation, PI/2= same
		sr = sr / (Math.PI/2) ; // normalize, so measure is between 0 and 1 rather than 0 and PI/2 ;  
		
		//ROUGH HACK: this roughly follows a power law (almost all measures are near 0) so let's log it to spread it out a bit.
		sr = Math.log((sr*1000) + 1) ;
		sr = sr/4 ;
		
		return sr ;
	}
	
}
