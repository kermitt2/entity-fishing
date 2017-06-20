package com.scienceminer.nerd.erd;

/**
 * 
 */
public class ErdAnnotationShort {
	private String qid;
	private Integer interpretationSet;
	private String primaryId; // here FreeBase id
	private String mentionText; // entity mention
	private double score;

	public ErdAnnotationShort() {

	}

	public String toTsv() {
		StringBuilder sb = new StringBuilder();
		sb.append(qid).append('\t');
		sb.append(interpretationSet).append('\t');
		sb.append(primaryId).append('\t');
		sb.append(mentionText).append('\t');
		sb.append(score);
		return sb.toString();
	}

	public String getQid() {
		return qid;
	}

	public void setQid(String qid) {
		this.qid = qid;
	}

	public Integer getInterpretationSet() {
		return interpretationSet;
	}

	public void setInterpretationSet(Integer interpretationSet) {
		this.interpretationSet = interpretationSet;
	}

	public String getPrimaryId() {
		return primaryId;
	}

	public void setPrimaryId(String primaryId) {
		this.primaryId = primaryId;
	}

	public String getMentionText() {
		return mentionText;
	}

	public void setMentionText(String mentionText) {
		this.mentionText = mentionText;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

}
