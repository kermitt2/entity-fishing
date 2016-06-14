package org.wikipedia.miner.comparison;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.util.MarkupStripper;

public class ConnectionSnippet implements Comparable<ConnectionSnippet> {

	private String _markup ;
	private final String _plainText ;
	
	private final Article _source ;
	private final Article _topic1 ;
	private final Article _topic2 ;
	
	private final int _sentenceIndex ;
	private boolean _followsHeading = false ;
	private boolean _isListItem = false ;
	
	private Double _weight ;
	
	private static final Pattern _headingPattern = Pattern.compile("\\s*={2,}(.*?)={2,}(.*)") ;
	private static final Pattern _listPattern = Pattern.compile("\\s*[*#]+(.*)") ;
	private static final MarkupStripper _stripper = new MarkupStripper() ;
	
	public ConnectionSnippet(int sentenceIndex, Article source, Article topic1, Article topic2) {
		_sentenceIndex = sentenceIndex ;
		_source = source ;
		_topic1 = topic1 ;
		_topic2 = topic2 ;
		
		_markup = _source.getSentenceMarkup(_sentenceIndex) ;
		
		Matcher m = _headingPattern.matcher(_markup) ;
		if (m.matches()) {
			_followsHeading = true ;
			_markup = m.group(2).trim() ;
		}
		
		m = _listPattern.matcher(_markup) ;
		if (m.matches()) {
			_isListItem = true ;
			_markup = m.group(1).trim() ;
		}
		
		_plainText = _stripper.stripToPlainText(_markup, null) ;
	}

	public String getMarkup() {
		
		return _markup ;
	}

	public String getPlainText() {
		return _plainText;
	}

	public Article getSource() {
		return _source;
	}

	public Article getTopic1() {
		return _topic1;
	}

	public Article getTopic2() {
		return _topic2;
	}

	public int getSentenceIndex() {
		return _sentenceIndex;
	}

	public boolean followsHeading() {
		return _followsHeading;
	}

	public boolean isListItem() {
		return _isListItem;
	}

	public Double getWeight() {
		return _weight;
	}
	
	public void setWeight(double weight) {
		_weight = weight ;
	}


	public int compareTo(ConnectionSnippet s) {
		
		int cmp = 0 ;
		
		if (s._weight != null && _weight != null && s._weight != _weight)
			cmp =  s._weight.compareTo(_weight) ; 
		
		if (cmp == 0)
			cmp = _source.compareTo(s._source) ;
		
		if (cmp == 0)
			cmp = new Integer(s._sentenceIndex).compareTo(_sentenceIndex) ;
		
		return cmp ;
	}
	
}
