package com.scienceminer.nerd.erd;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Diego Ceccarelli <diego.ceccarelli@isti.cnr.it>
 *		   modified by Patrice Lopez
 * 
 *         Created on Mar 15, 2014
 */
public class WikipediaLabelToFreebaseRecord {

	private static final Logger logger = LoggerFactory.getLogger(WikipediaLabelToFreebaseRecord.class);

	private String freebaseId;
	private String label;
	private String wikipediaLabel;

	public WikipediaLabelToFreebaseRecord() {
		super();
	}

	public String getCleanWikipediaLabel() {
		return convert(wikipediaLabel).replaceAll("/wikipedia/en_title/", "");
	}

	public String getFreebaseId() {
		return freebaseId;
	}

	public void setFreebaseId(String freebaseId) {
		this.freebaseId = freebaseId;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getWikipediaLabel() {
		return wikipediaLabel;
	}

	public void setWikipediaLabel(String wikipediaLabel) {
		this.wikipediaLabel = wikipediaLabel;
	}

	final static private Pattern quotedCharPattern = Pattern
			.compile("\\$([0-9A-Fa-f]{4})");

	protected String convert(String s) {
		// see
		// https://github.com/OpenRefine/OpenRefine/blob/master/extensions/freebase/src/com/google/refine/freebase/expr/MqlKeyUnquote.java
		StringBuffer sb = new StringBuffer();

		int last = 0;
		Matcher m = quotedCharPattern.matcher(s);
		while (m.find()) {
			int start = m.start();
			int end = m.end();
			if (start > last) {
				sb.append(s.substring(last, start));
			}
			last = end;

			sb.append((char) Integer.parseInt(s.substring(start + 1, end), 16));
		}

		if (last < s.length()) {
			sb.append(s.substring(last));
		}

		return sb.toString();
	}

	public static WikipediaLabelToFreebaseRecord parse(String tsvString) {
		Scanner scanner = new Scanner(tsvString).useDelimiter("\t");
		WikipediaLabelToFreebaseRecord record = new WikipediaLabelToFreebaseRecord();
		record.setFreebaseId(scanner.next());
		String label = scanner.next();
		// removes the first " and the final "@en
		if (!label.isEmpty()) {
			label = label.substring(1, label.length() - 4);
		} else {
			// empty label
			logger.warn("not label for line \n {}", tsvString);
		}
		record.setLabel(label);
		String wikilabel = scanner.next();
		wikilabel = wikilabel.substring(1, wikilabel.length() - 1);
		record.setWikipediaLabel(wikilabel);
		return record;
	}

	protected String decode(String utf8endodedLabel) {
		return convert(utf8endodedLabel);
	}

}
