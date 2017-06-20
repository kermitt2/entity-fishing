package com.scienceminer.nerd.erd;

/**
 * 
 */
public class ErdAnnotationLong {
	private String qid;
	private int begin; // offset begin in byte
	private int end; // offset end in byte
	private String primaryId; // here FreeBase id
	private String freeField; // a free field for secondary ID which can be empty
	private String mentionText; // entity mention
	private double score1 = 0.0;
	private double score2 = 0.0;
	
	public ErdAnnotationLong() {

	}

	public String toTsv() {
		StringBuilder sb = new StringBuilder();
		sb.append(qid).append('\t');
		sb.append(begin).append('\t');
		sb.append(end).append('\t');
		sb.append(primaryId).append('\t');
		sb.append(freeField).append('\t');
		sb.append(mentionText).append('\t');
		sb.append(score1).append('\t');
		sb.append(score2);
		return sb.toString();
	}

	public String getQid() {
		return qid;
	}

	public void setQid(String qid) {
		this.qid = qid;
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

	public double getScore1() {
		return score1;
	}

	public void setScore1(double score) {
		this.score1 = score;
	}
	
	public double getScore2() {
		return score2;
	}

	public void setScore2(double score) {
		this.score2 = score;
	}
	
	public int getBegin() {
		return begin;
	}

	public void setBegin(int b) {
		begin = b;
	}

	public int getEnd() {
		return end;
	}
	
	public void setEnd(int e) {
		end = e;
	}
	
	public String getFreeField() {
		return freeField;
	}
	
	public void setFreeField(String field) {
		freeField = field;
	}
}
