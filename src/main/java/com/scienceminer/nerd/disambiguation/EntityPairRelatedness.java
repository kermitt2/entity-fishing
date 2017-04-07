package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.kb.model.Article;

/**
 * Represent the relatedness values between a pair of entities, 
 * following in particular Milne and Witten measurement. 
 */
public class EntityPairRelatedness {

	private final Article articleA;
	private final Article articleB;
	
	private boolean inLinkFeaturesSet = false;
	private double inLinkMilneWittenMeasure = 0.0;
	private int inLinkUnion = 0;
	private double inLinkIntersectionProportion = 0.0;
	
	private boolean outLinkFeaturesSet = false;
	private double outLinkMilneWittenMeasure = 0.0;
	private int outLinkUnion = 0;
	private double outLinkIntersectionProportion = 0.0;
	
	public EntityPairRelatedness(Article artA, Article artB) {
		articleA = artA;
		articleB = artB;
	}
	
	public Article getArticleA() {
		return articleA;
	}

	public Article getArticleB() {
		return articleB;
	}

	public boolean inLinkFeaturesSet() {
		return inLinkFeaturesSet;
	}

	public double getInLinkMilneWittenMeasure() {
		return inLinkMilneWittenMeasure;
	}

	public int getInLinkUnion() {
		return inLinkUnion;
	}

	public double getInLinkIntersectionProportion() {
		return inLinkIntersectionProportion;
	}

	public boolean outLinkFeaturesSet() {
		return outLinkFeaturesSet;
	}

	public double getOutLinkMilneWittenMeasure() {
		return outLinkMilneWittenMeasure;
	}

	public int getOutLinkUnion() {
		return outLinkUnion;
	}

	public double getOutLinkIntersectionProportion() {
		return outLinkIntersectionProportion;
	}

	public void setInLinkFeatures(double milneWittenMeasure, int union, double intersectionProportion) {
		inLinkFeaturesSet = true;
		inLinkMilneWittenMeasure = milneWittenMeasure;
		inLinkUnion = union;
		inLinkIntersectionProportion = intersectionProportion;
	}
	
	public void setOutLinkFeatures(double milneWittenMeasure, int union, double intersectionProportion) {
		outLinkFeaturesSet = true;
		outLinkMilneWittenMeasure = milneWittenMeasure;
		outLinkUnion = union;
		outLinkIntersectionProportion = intersectionProportion;
	}
	
	protected static double normalizeMilneWittenMeasure(double milneWittenMeasure) {
		if (milneWittenMeasure >= 1)
			return 0;
		
		return 1.0 - milneWittenMeasure;
	}	
}
