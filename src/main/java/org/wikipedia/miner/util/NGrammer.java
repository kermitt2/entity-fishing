package org.wikipedia.miner.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

/**
 * A utility class for gathering n-grams from text. Uses an openNLP tokenizer to identify word 
 * boundaries, and optionally uses an openNLP sentence splitter to ensure that ngrams do not span 
 * sentence boundaries. Also ensures that ngrams do not span line breaks. 
 * 
 */
public class NGrammer {

	public enum CaseContext { 
		
		/**
		 * ngram is found in lower cased text.
		 */
		lower, 
		
		/**
		 * ngram is found in UPPER CASED TEXT.
		 */
		upper, 
		
		/**
		 * ngram is found in Text Where Every Word Starts With A Capital Letter
		 */
		upperFirst, 
		
		/**
		 * ngram is found in text with a Healthy mixture of capitalization (probably normal text)
		 */
		mixed
	} ;

	private Tokenizer _tokenizer ;
	private SentenceDetector _sentenceDetector ;

	private int _maxN = 10 ;


	public NGrammer(SentenceDetector sentenceDetector, Tokenizer tokenizer) {
		
		if (tokenizer == null) 
			throw new NullPointerException() ;

		_sentenceDetector = sentenceDetector ;
		_tokenizer = tokenizer ;
	}

	
	public void setMaxN(int n) {
		_maxN = n ;
	}

	public int getMaxN() {
		return _maxN ;
	}

	
	public String[] ngramDetect(String s) {

		Span spans[] = ngramPosDetect(s) ;
		String[] ngrams = new String[spans.length] ;

		for (int i=0 ; i<spans.length ; i++) 
			ngrams[i] = s.substring(spans[i].getStart(), spans[i].getEnd()) ;

		return ngrams ;
	}

	public NGramSpan[] ngramPosDetect(String s) {

		ArrayList<Span> ngramSpans = new ArrayList<Span>() ;

		int lineStart = 0 ;
		for (String line:s.split("\n")) {

			//System.out.println("l: " + line) ;

			//then split by sentence, if a splitter is available
			Span[] sentenceSpans ;	
			if (_sentenceDetector == null) {
				sentenceSpans = new Span[1] ;
				sentenceSpans[0] = new Span(0,line.length()) ;
			} else {
				sentenceSpans = _sentenceDetector.sentPosDetect(line) ;
			}

			for (Span sentenceSpan:sentenceSpans) {
				String sentence = line.substring(sentenceSpan.getStart(), sentenceSpan.getEnd()) ;
				//System.out.println(" s: " + sentence) ;
				
				Span[] tokenSpans = _tokenizer.tokenizePos(sentence) ;
				
				CaseContext caseContext = identifyCaseContext(sentence, tokenSpans) ;

				//System.out.println(" c: " + caseContext) ;

				for (int i=0 ; i<tokenSpans.length ; i++) {

					//never start an ngram with a punctuation token
					if (tokenSpans[i].length() == 1 && !Character.isLetterOrDigit(sentence.charAt(tokenSpans[i].getStart())))
						continue ;
					
					int ngramStart = tokenSpans[i].getStart();

					for (int j=Math.min(i + _maxN, tokenSpans.length-1) ; j >= i ; j--) {

						//never end an ngram with a punctuation token
						if (tokenSpans[j].length() == 1 && !Character.isLetterOrDigit(sentence.charAt(tokenSpans[j].getStart())))
							continue ;

						int globalStart = lineStart + sentenceSpan.getStart() + tokenSpans[i].getStart();
						int globalEnd = lineStart + sentenceSpan.getStart() + tokenSpans[j].getEnd();
						
						Span[] tokenSpansLocalToNgram = new Span[(j-i)+1] ;
						for (int k=0 ; k<tokenSpansLocalToNgram.length ; k++) {
							Span tokenSpan = tokenSpans[i+k] ;
							tokenSpansLocalToNgram[k] = new Span(tokenSpan.getStart()-ngramStart, tokenSpan.getEnd()-ngramStart) ;
						}

						ngramSpans.add(new NGramSpan(globalStart, globalEnd, tokenSpansLocalToNgram, caseContext, i==0)) ;

						//String ngram = s.substring(globalStart, globalEnd) ;
						//System.out.println("  n: '" + ngram + "'") ;
					}
				}
			}

			lineStart = lineStart + line.length() + 1 ;
		}

		return ngramSpans.toArray(new NGramSpan[ngramSpans.size()]) ;
	}

	private CaseContext identifyCaseContext(String text, Span[] tokenSpans) {

		boolean allUpper = true ;
		boolean allLower = true ;
		boolean allUpperFirst = true ;

		for (Span tokenSpan:tokenSpans) {

			if (!allUpper && !allLower && !allUpperFirst)
				return CaseContext.mixed ;

			String token = text.substring(tokenSpan.getStart(), tokenSpan.getEnd()) ;
			CaseContext c = identifyCaseContext(token) ;

			//System.out.println("token:" + token + " context: " + c) ;
			
			switch(c) {
			case upper:
				allLower = false ;
				break ;
			case lower:
				allUpper = false ;
				allUpperFirst = false ;
				break ;
			case upperFirst:
				allLower = false ;
				allUpper = false ;
				break ;
			case mixed:
				allLower = false ;
				allUpper = false ;
				allUpperFirst = false ;
				break ;
			}
		}

		if (allUpper)
			return CaseContext.upper ;

		if (allLower)
			return CaseContext.lower ;

		if (allUpperFirst)
			return CaseContext.upperFirst ;

		return CaseContext.mixed ;
	}

	private CaseContext identifyCaseContext(String token) {

		boolean allUpper = true ;
		boolean allLower = true ;
		boolean upperFirst = true ;

		for (int i=0 ; i<token.length(); i++) {

			if (!allUpper && !allLower) {
				if (upperFirst)
					return CaseContext.upperFirst ;
				else
					return CaseContext.mixed ;
			}

			char c = token.charAt(i) ;
			
			//System.out.println("checking char '" + c + "'") ;

			if (Character.isUpperCase(c)) 
				allLower = false ;
			
			if (Character.isLowerCase(c)) {
				allUpper = false ;
				if (i==0)
					upperFirst = false ;
			}
		}

		if (allUpper)
			return CaseContext.upper ;

		if (allLower)
			return CaseContext.lower ;

		if (upperFirst)
			return CaseContext.upperFirst ;

		return CaseContext.mixed ;
	}

	
	
	/**
	 * An extension of Span that captures the location of an ngram, as well as the individual tokens within it.
	 */
	public class NGramSpan extends Span {

		private Span[] _tokenSpans ;
		private CaseContext _caseContext ;
		private boolean _isSentenceStart ;

		private NGramSpan(int start, int end, Span[] tokenSpans, CaseContext context, boolean isSentenceStart) {
			super(start, end) ;
			_tokenSpans = tokenSpans ;
			_caseContext = context ;
			_isSentenceStart = isSentenceStart ;
		}

		public Span[] getTokenSpans() {
			return _tokenSpans ;
		}
		
		public CaseContext getCaseContext() {
			return _caseContext ;
		}
		
		public boolean isSentenceStart() {
			return _isSentenceStart ;
		}
		
		public String getNgram(String sourceText) {
			return sourceText.substring(getStart(), getEnd()) ;
		}
		
		public String getNgramUpperFirst(String sourceText) {
			
			char[] ngram = getNgram(sourceText).toLowerCase().toCharArray() ;
			
			for (Span s:_tokenSpans) 
				ngram[s.getStart()] = Character.toUpperCase(ngram[s.getStart()]) ;
			
			return new String(ngram) ;
		}
	}

}
