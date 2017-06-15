package com.scienceminer.nerd.utilities;

import com.scienceminer.nerd.kb.model.Article;

import java.util.*;
import java.util.regex.*;

/**
 * Parse document in mediawiki format, identify the various markups.  
 */
public class MediaWikiParser {

	public enum SnippetLength { full, firstSentence, firstParagraph };

	private Pattern linkPattern = Pattern.compile("\\[\\[(.*?:)?(.*?)(\\|.*?)?\\]\\]");
	private Pattern isolatedBefore = Pattern.compile("(\\s*|.*\\n(\\s*))", Pattern.DOTALL);
	private Pattern isolatedAfter = Pattern.compile("(\\s*|(\\s*)\\n.*)", Pattern.DOTALL);
	
	//private EmphasisResolver emphasisResolver = new EmphasisResolver();

	/**
	 * @param article the article to clean
	 * @param length the portion of the article that is to be extracted and cleaned (ALL, FIRST_SENTENCE, or FIRST_PARAGRAPH)
	 * @return the content (or snippet) of the given article, with all markup removed except links to other articles.  
	 * @throws Exception
	 */
	public String getMarkupLinksOnly(Article article) throws Exception {
		return getMarkupLinksOnly(article, SnippetLength.full);
	}

	public String getMarkupLinksOnly(Article article, SnippetLength length) throws Exception {
		String markup = null;
		
		switch (length) {
			/*case firstSentence :
				markup = article.getSentenceMarkup(0);
				break;*/
			case firstParagraph :
				markup = article.getFirstParagraphMarkup();
				break;
			default : 
				markup = article.getFullMarkup();
				break;
		}
				
		markup = stripAllButInternalLinksAndEmphasis(markup, null);
		markup = stripNonArticleInternalLinks(markup, null);
		
		return markup;	
	}
	
	/**
	 * @param article the article to clean
	 * @param length the portion of the article that is to be extracted and cleaned (ALL, FIRST_SENTENCE, or FIRST_PARAGRAPH)
	 * @return the content of the given article, with all markup removed.  
	 * @throws Exception
	 */
	public String getCleanedContent(Article article) throws Exception { 
		return getCleanedContent(article, SnippetLength.full);
	}

	public String getCleanedContent(Article article, SnippetLength length) throws Exception {
		String markup;
		
		switch (length) {
			/*case firstSentence :
				markup = article.getSentenceMarkup(0);
				break;*/
			case firstParagraph :
				markup = article.getFirstParagraphMarkup();
				break;
			default : 
				markup = article.getFullMarkup();
				break;
		}
		
		markup = stripToPlainText(markup, null);
				
		return markup;
	}

	/**
	 * Returns a copy of the given markup, where all markup has been removed except for 
	 * internal links to other wikipedia pages (e.g. to articles or categories), section 
	 * headers, list markers, and bold/italic markers. 
	 * 
	 * By default, unwanted markup is completely discarded. You can optionally specify 
	 * a character to replace the regions that are discared, so that the length of the 
	 * string and the locations of unstripped characters is not modified.
	 */
	public String stripAllButInternalLinksAndEmphasis(String markup, Character replacement) {
		//deal with comments and math regions entirely seperately. 
		//Comments often contain poorly nested items that the remaining things will complain about.
		//Math regions contain items that look confusingly like templates.
		List<int[]> regions = gatherSimpleRegions(markup, "\\<\\!--(.*?)--\\>");
		regions = mergeRegionLists(regions, gatherComplexRegions(markup, "\\<math(\\s*?)([^>\\/]*?)\\>", "\\<\\/math(\\s*?)\\>"));
		String clearedMarkup = stripRegions(markup, regions, replacement);

		//deal with templates entirely seperately. They often end in |}} which confuses the gathering of tables.
		regions = gatherTemplates(clearedMarkup);
		clearedMarkup = stripRegions(clearedMarkup, regions, replacement);

		//now gather all of the other regions we want to ignore	
		regions = gatherTables(clearedMarkup);

		regions = mergeRegionLists(regions, gatherHTML(clearedMarkup));
		regions = mergeRegionLists(regions, gatherExternalLinks(clearedMarkup));
		regions = mergeRegionLists(regions, gatherMagicWords(clearedMarkup));

		//ignore these regions now (they need to be blanked before we can correctly identify the remaining regions)
		clearedMarkup = stripRegions(clearedMarkup, regions, replacement);
		
		//System.out.println("Prior to removing misformatted start: ");
		//System.out.println(" - " + clearedMarkup);

		regions = gatherMisformattedStarts(clearedMarkup);
		clearedMarkup = stripRegions(clearedMarkup, regions, replacement);
		
		return clearedMarkup;
	}


	/**
	 * Returns a copy of the given markup, where all links to wikipedia pages 
	 * (categories, articles, etc) have been removed. Links to articles are 
	 * replaced with the appropriate anchor markup. All other links are removed completely.
	 * 
	 * By default, unwanted markup is completely discarded. You can optionally specify 
	 * a character to replace the regions that are discarded, so that the length of the 
	 * string and the locations of unstripped characters is not modified.
	 */
	public String stripInternalLinks(String markup, Character replacement) {

		List<int[]> regions = gatherComplexRegions(markup, "\\[\\[", "\\]\\]");

		StringBuffer strippedMarkup = new StringBuffer();
		int lastPos = markup.length();

		//because regions are sorted by end position, we work backwards through them
		int i = regions.size();

		while (i > 0) {
			i --;

			int[] region = regions.get(i);

			//only deal with this region is not within a region we have already delt with. 
			if (region[0] < lastPos) {

				//copy everything between this region and start of last one we dealt with. 
				strippedMarkup.insert(0,markup.substring(region[1], lastPos));

				String linkMarkup = markup.substring(region[0], region[1]);

				// by default (if anything goes wrong) we will keep the link as it is
				String strippedLinkMarkup = linkMarkup;

				Matcher m = linkPattern.matcher(linkMarkup);
				if (m.matches()) {

					String prefix = m.group(1);
					String dest = m.group(2);
					String anchor = m.group(3);

					if (prefix != null) {
						// this is not a link to another article, so get rid of it entirely
						if (replacement != null) 
							strippedLinkMarkup = linkMarkup.replaceAll(".",replacement.toString());			
						else 
							strippedLinkMarkup = "";
					} else {
						if (anchor != null) {
							//this has an anchor defined, so use that but blank out everything else

							if (replacement != null) 
								strippedLinkMarkup = replacement + replacement + dest.replaceAll(".", replacement.toString()) + replacement + anchor.substring(1) + anchor.substring(1) + replacement;
							else
								strippedLinkMarkup = anchor.substring(1);

						} else {
							//this has no anchor defined, so treat dest as anchor and blank out everything else

							if (replacement != null) {
								strippedLinkMarkup = replacement + replacement + dest + replacement + replacement;
							} else {
								strippedLinkMarkup = dest;
							}
						}
					}				
				} else {
					//logProblem("our pattern for delimiting links has a problem");
				}

				strippedMarkup.insert(0,strippedLinkMarkup);
				lastPos = region[0];
			}
		}	

		if (lastPos > 0) 
			strippedMarkup.insert(0,markup.substring(0, lastPos));

		return strippedMarkup.toString(); 	
	}
	
	public String stripEmphasis(String markup, Character replacement) {
		
		String resolvedMarkup = resolveEmphasis(markup);
		
		List<int[]> regions = gatherSimpleRegions(resolvedMarkup, "\\<\\/?[bi]\\>");
		
		StringBuffer clearedMarkup = new StringBuffer();
		int lastPos = 0;
		
		int i = regions.size();
		while (i > 0) {
			i--;
			int[] region = regions.get(i);

			//only deal with this region is not within a region we have already dealt with. 
			if (region[0] < lastPos) {

				//print (" - - dealing with it\n");

				//copy markup after this region and before beginning of the last region we dealt with
				if (region[1] < lastPos) 
					clearedMarkup.insert(0, resolvedMarkup.substring(region[1], lastPos));

				if (replacement != null) {
					String tag = resolvedMarkup.substring(region[0], region[1]);
					String fill;
					if (tag.matches("\\<\\/?b\\>"))
						fill = "'''";
					else
						fill = "''";
					
					fill.replaceAll(".", replacement.toString());
					
					clearedMarkup.insert(0, fill);
				}

				lastPos = region[0];
			} else {
				//print (" - - already dealt with\n");

			}
		}

		clearedMarkup.insert(0, resolvedMarkup.substring(0, lastPos));	
		return clearedMarkup.toString();
	}
	

	/**
	 * Returns a copy of the given markup, where all links to wikipedia pages
	 * that are not articles (categories, language links, etc) have been removed.
	 * 
	 * By default, unwanted markup is completely discarded. You can optionally specify
	 * a character to replace the regions that are discarded, so that the length of the
	 * string and the locations of unstripped characters is not modified.
	 */
	public String stripNonArticleInternalLinks(String markup, Character replacement) {

		//currItem = "non-article internal links";

		List<int[]> regions = gatherComplexRegions(markup, "\\[\\[", "\\]\\]");

		StringBuffer strippedMarkup = new StringBuffer();
		int lastPos = markup.length();

		//because regions are sorted by end position, we work backwards through them
		int i = regions.size();

		while (i > 0) {
			i --;

			int[] region = regions.get(i);
			
			//System.out.println(" - - REGION: " + markup.substring(region[0], region[1]));

			//only deal with this region is not within a region we have already delt with. 
			if (region[0] < lastPos) {

				//copy everything between this region and start of last one we dealt with. 
				strippedMarkup.insert(0, markup.substring(region[1], lastPos));

				String linkMarkup = markup.substring(region[0], region[1]);

				//print("link [region[0],region[1]] = linkMarkup\n\n");

				// by default (if anything goes wrong) we will keep the link as it is
				String strippedLinkMarkup = linkMarkup;
				Matcher m = linkPattern.matcher(linkMarkup);
				if (m.matches()) {

					String prefix = m.group(1);
					//String dest = m.group(2);
					//String anchor = m.group(3);

					if (prefix != null) {
						// this is not a link to another article, so get rid of it entirely
						if (replacement != null) {
							strippedLinkMarkup = linkMarkup.replaceAll(".", replacement.toString());			
						} else {
							strippedLinkMarkup = "";
						}
					}
				} else {
					//logProblem("our pattern for delimiting links has a problem");
				}

				strippedMarkup.insert(0, strippedLinkMarkup);
				lastPos = region[0];
			}
		}	

		if (lastPos > 0) 
			strippedMarkup.insert(0, markup.substring(0, lastPos));

		return strippedMarkup.toString(); 	
	}


	/**
	 * Removes all sections (both header and content, including nested sections) with the given sectionNames
	 * 
	 * @param sectionName the name of the section (case insensitive) to remove.
	 * @param markup the markup to be stripped
	 * @return the stripped markup
	 */
	public String stripSections(String markup, String[] sectionNames, Character replacement) {
		List<int[]> regions = new ArrayList<int[]>();
		for (String sectionName : sectionNames) 
			regions = mergeRegionLists(regions, gatherSection(markup, sectionName));

		return stripRegions(markup, regions, replacement);
	}
	
	public String stripSectionHeaders(String markup, Character replacement) {
		List<int[]> regions = gatherSectionHeaders(markup);
		return stripRegions(markup, regions, replacement);
	}

	/**
	 * Convenience method which combines both of the above methods - i.e. returns a copy of the
	 * given markup, where all markup has been removed except for section headers and list markers.
	 *
	 * By default, unwanted markup is completely discarded. You can optionally specify 
	 * a character to replace the regions that are discared, so that the length of the 
	 * string and the locations of unstripped characters is not modified. 
	 */
	public String stripToPlainText(String markup, Character replacement) {
		String clearedMarkup = stripAllButInternalLinksAndEmphasis(markup, replacement);
		clearedMarkup = stripInternalLinks(clearedMarkup, replacement);

		return clearedMarkup;	
	}



	/**
	 * Returns a copy of the given markup, where the given regions have been removed. 
	 * Regions are identified using one of the gather methods.
	 * 
	 * By default, unwanted markup is completely discarded. You can optionally specify
	 * a character to replace the regions that are discared, so that the length of the 
	 * string and the locations of unstripped characters is not modified.
	 */
	public String stripRegions(String markup, List<int[]> regions, Character replacement) {
		StringBuffer clearedMarkup = new StringBuffer();

		int lastPos = markup.length();

		//because regions are sorted by end position, we work backwards through them
		int i = regions.size();
		while (i > 0) {
			i --;

			int[] region = regions.get(i);

			//only deal with this region is not within a region we have already delt with. 
			if (region[0] < lastPos) {

				//print (" - - dealing with it\n");
				//copy markup after this region and before beginning of the last region we delt with
				if (region[1] < lastPos) {
					clearedMarkup.insert(0, markup.substring(region[1], lastPos));
				}

				if (replacement != null) {
					// PL: this should be investigated - in some cases the region end position goes beyond
					// the markup limit 
					int region1pos = region[1];
					if (region1pos > markup.length())
						region1pos = markup.length();
					String fill = markup.substring(region[0],region1pos).replaceAll(".", replacement.toString());
					clearedMarkup.insert(0, fill);
				}

				lastPos = region[0];
			} else {
				//print (" - - already dealt with\n");
			}
		}

		clearedMarkup.insert(0, markup.substring(0, lastPos));	
		return clearedMarkup.toString();
	}
	
	public String stripExcessNewlines(String markup) {
		String strippedMarkup = markup.replaceAll("\n{3,}", "\n\n");
		return strippedMarkup.trim();
	}

	/**
	 * Gathers areas within the markup which correspond to links to other wikipedia pages
	 * (as identified by [[ and ]] pairs). Note: these can be nested (e.g. for images)
	 */
	public List<int[]> gatherInternalLinks(String markup) {
		//currItem = "internal links";
		return gatherComplexRegions(markup, "\\[\\[", "\\]\\]");
	}
	
	/**
	 * Gathers areas within the markup which correspond to templates (as identified by {{ and }} pairs). 
	 */
	public List<int[]> gatherTemplates(String markup) {
		//currItem = "templates";
		return gatherComplexRegions(markup, "\\{\\{", "\\}\\}");
	}
	
	public List<int[]> getIsolatedRegions(List<int[]> regions, String markup) {
		List<int[]> isolatedRegions = new ArrayList<int[]>();
		for (int[] region : regions) {
			if (isIsolated(region, markup))
				isolatedRegions.add(region);
		}
		return isolatedRegions;
	}
	
	public List<int[]> excludeIsolatedRegions(List<int[]> regions, String markup) {
		
		List<int[]> unisolatedRegions = new ArrayList<int[]>();
		
		for (int[] region : regions) {
			if (!isIsolated(region, markup))
				unisolatedRegions.add(region);
		};
		
		return unisolatedRegions;
	}
	
	private boolean isIsolated(int[] region, String markup) {
		String before = markup.substring(0, region[0]);
		String after = markup.substring(region[1]);
		
		Matcher m = isolatedBefore.matcher(before);
		if (!m.matches())
			return false;
		
		m = isolatedAfter.matcher(after);
		if(!m.matches())
			return false;
		
		return true;
	}

	/**
	 * Gathers areas within the markup which correspond to tables (as identified by {| and |} pairs). 
	 */
	public List<int[]> gatherTables(String markup) {
		//currItem = "tables";
		return gatherComplexRegions(markup, "\\{\\|", "\\|\\}");
	}
	
	/**
	 * Gathers areas within the markup which correspond to html tags. 
	 * 
	 * DIV and REF regions will enclose beginning and ending tags, and everything in between,
	 * since we assume this content is supposed to be discarded. All other regions will only include the
	 * individual tag, since we assume the content between such pairs is supposed to be retained. 
	 */
	public List<int[]> gatherHTML(String markup) {

		//currItem = "html";

		//gather and merge references
		List<int[]> regions = gatherReferences(markup);

		//gather <div> </div> pairs
		regions = mergeRegionLists(regions, gatherComplexRegions(markup, "\\<div(\\s*?)([^>\\/]*?)\\>", "\\<\\/div(\\s*?)\\>"));
		
		//gather remaining tags
		regions = mergeRegionLists(regions, gatherSimpleRegions(markup, "\\<(.*?)\\>"));
		
		return regions;
	}


	/**
	 * Gathers areas within the markup which correspond to references (markup to support claims or facts).
	 * The regions will enclose beginning and ending tags, and everything in between,
	 * since we assume this content is supposed to be discarded. 
	 */
	public List<int[]> gatherReferences(String markup) {

		//currItem = "references";

		//gather <ref/>
		List<int[]> regions = gatherSimpleRegions(markup, "\\<ref(\\s*?)([^>]*?)\\/\\>");

		//gather <ref> </ref> pairs (these shouldnt be nested, but what the hell...)
		regions = mergeRegionLists(regions, gatherComplexRegions(markup, "\\<ref(\\s*?)([^>\\/]*?)\\>", "\\<\\/ref(\\s*?)\\>"));

		return regions;
	}


	/**
	 * Gathers items which MediaWiki documentation mysteriously refers to as "majic words": e.g. __NOTOC__
	 */
	public List<int[]> gatherMagicWords(String markup) {
		//currItem = "magic words";
		return gatherSimpleRegions(markup, "\\_\\_([A-Z]+)\\_\\_");
	}

	/**
	 * Gathers all links to external web pages
	 */
	public List<int[]> gatherExternalLinks(String markup) {
		//currItem = "external links";
		return gatherSimpleRegions(markup, "\\[(http|www|ftp).*?\\]");
	}

	/**
	 * Gathers bold and italic markup
	 */
	public List<int[]> gatherEmphasis(String markup) {
		//currItem = "emphasis";
		return gatherSimpleRegions(markup, "'{2,}"); 
	}
	
	
	/**
	 * Gathers section headers
	 */
	public List<int[]> gatherSectionHeaders(String markup) {

		List<int[]> regions = new ArrayList<int[]>();
		
		Pattern p = Pattern.compile("\\n\\s*((={2,})[^=].*?\\2)[^=]");
		Matcher m = p.matcher(markup);
		
		while (m.find()) {
			int[] region = {m.start(1), m.end(1)};
			regions.add(region);
		}
		return regions;
	}
	
	
	public List<int[]> gatherSection(String markup, String sectionName) {
		
		List<int[]> regions = new ArrayList<int[]>();
		
		//find start of section
		Pattern startP = Pattern.compile("\\n\\s*(={2,})\\s*" + sectionName + "\\s*\\1", Pattern.CASE_INSENSITIVE);
		Matcher startM = startP.matcher(markup);
		
		if(startM.find()) {
			int start = startM.start(1);
			int level = startM.group(1).length();
			int end;
			
			//look for start of section that is at same level or higher
			Pattern endP = Pattern.compile("\\n\\s*(={2,"+level+"})[^=].*\\1");
			Matcher endM = endP.matcher(markup);
			
			if (endM.find(startM.end())) 
				end = endM.start();
			else
				end = markup.length() -1;
				
			int[] region = {start, end};
			regions.add(region);
		}
			
		return regions;
	}
	
	

	/**
	 * Gathers markup which indicates indented items, or numbered and unnumbered list items
	 */
	public List<int[]> gatherListAndIndentMarkers(String markup) {
		//currItem = "list and intent markers";

		List<int[]> regions = gatherSimpleRegions(markup, "\n( *)([//*:]+)");

		//increment start positions of all regions by one, so they don't include the newline character
		for (int[] region:regions)
			region[0]++;

		//add occurance of list item on first line (if there is one)
		regions = mergeRegionLists(regions, gatherSimpleRegions(markup, "^( *)([//*:]+)"));
		return regions;
	}
	
	private boolean isEntirelyItalicised(String line) {
		String resolvedLine = resolveEmphasis(line);
		Pattern p = Pattern.compile("(\\s*)\\<i\\>(.*?)\\<\\/i\\>\\.?(\\s*)");
		
		Matcher m = p.matcher(resolvedLine);
		if (m.matches()) {
			if (m.group(1).contains("</i>"))
				return false;
			else
				return true;
		} else {
			return false;
		}
	}

	/**
	 * Gathers paragraphs within the markup referred to by the given pointer, which are at the 
	 * start and either begin with an indent or are entirely encased in italics. These correspond to quotes or disambiguation and 
	 * navigation notes that the author should have used templates to identify, but didn't. 
	 * This will only work after templates, and before list markers have been cleaned out.
	 */
	public List<int[]> gatherMisformattedStarts(String markup) {
		String[] lines = markup.split("\n");
		int ignoreUntil = 0;
		for (String line:lines) {
			boolean isWhitespace = line.matches("^(\\s*)$");
			boolean isIndented = line.matches("^(\\s*):.*");
			boolean isItalicised = isEntirelyItalicised(line) ;
			boolean isImage = line.matches("^(\\s*)\\[\\[Image\\:(.*?)\\]\\](\\s*)");
			//System.out.println(" - - '" + line + "' " + isIndented + "," + isItalicised);
			
			if (isWhitespace || isIndented || isItalicised || isImage)  {
				//want to ignore this line
				ignoreUntil = ignoreUntil + line.length() + 1;	
				//print(" - - - discard\n");		
			} else {
				//print(" - - - keep\n");
				break;
			}		
		}
		
		int[] region = {0, ignoreUntil};

		List<int[]> regions = new ArrayList<int[]>();
		regions.add(region);
		
		return regions;
	}


	/**
	 * Gathers simple regions: ones which cannot be nested within each other.
	 * 
	 *  The returned regions (an array of start and end positions) will be sorted 
	 *  by end position (and also by start position, since they can't overlap) 
	 */ 
	public List<int[]> gatherSimpleRegions(String markup, String regex) {

		//an array of regions we have identified
		//each region is given as an array containing start and end character indexes of the region. 
		List<int[]> regions = new ArrayList<int[]>();
		
		Pattern p = Pattern.compile(regex, Pattern.DOTALL);
		Matcher m = p.matcher(markup);

		while(m.find()) {
			int[] region = {m.start(), m.end()};
			regions.add(region);
		}

		return regions;
	}


	/**
	 * Gathers complex regions: ones which can potentially be nested within each other.
	 * 
	 * The returned regions (an array of start and end positions) will be either
	 * non-overlapping or cleanly nested, and sorted by end position. 
	 */ 
	public List<int[]> gatherComplexRegions(String markup, String startRegex, String endRegex) {

		//an array of regions we have identified
		//each region is given as an array containing start and end character indexes of the region. 
		List<int[]> regions = new ArrayList<int[]>();

		//a stack of region starting positions
		List<Integer> startStack = new ArrayList<Integer>();
		
		Pattern p = Pattern.compile("((" + startRegex + ")|(" + endRegex + "))", Pattern.DOTALL);
		Matcher m = p.matcher(markup);
		while(m.find()) {
			Integer p1 = m.start();
			Integer p2 = m.end();  

			if (m.group(2) != null) {
				//this is the start of an item
				startStack.add(p1);
			} else {
				//this is the end of an item
				if (!startStack.isEmpty()) {
					int start = startStack.get(startStack.size()-1);
					startStack.remove(startStack.size()-1);
					
					int[] region = {start, p2};
					regions.add(region);

					//print (" - item [region[0],region[1]]: ".substr(markup, region[0], region[1]-region[0])."\n");
				} else {
					//logProblem("oops, we found the end of an item, but have no idea where it started");
				}
			}
		}

		if (!startStack.isEmpty()) {
			//logProblem("oops, we got to the end of the markup and still have items that have been started but not finished");
		}

		return regions;
	}

	/**
	 * Merges two lists of regions into one sorted list. Regions that are contained
	 * within other regions are discarded.
	 * 
	 * The resulting region list will be non-overlapping and sorted by end positions.
	 */
	private List<int[]> mergeRegionLists(List<int[]> regionsA, List<int[]> regionsB) {

		int indexA = regionsA.size() -1;
		int indexB = regionsB.size() - 1;

		List<int[]> newRegions = new ArrayList<int[]>();

		int lastPos = -1;
		while (indexA >= 0 && indexB >= 0) {
			int[] regionA = regionsA.get(indexA);
			int[] regionB = regionsB.get(indexB);

			if (lastPos >= 0 && regionA[0] >= lastPos && regionA[0] >= lastPos) {
				//both of these are inside regions that we have already dealt with, so discard them
				indexA--;
				indexB--;
			} else {
				if (regionB[1] > regionA[1]) {

					//lets see if we need to copy B across
					if ((regionB[0] >= regionA[0] && regionB[1] <= regionA[1]) || (lastPos>=0 && regionB[0] >= lastPos)) {
						//either A or the last region we dealt with completely contains B, so we just discard B
					} else {
						//deal with B now
						int[] newRegion = {regionB[0], min(regionB[1], lastPos)};
						newRegions.add(0, newRegion);
						lastPos = regionB[0];
					}

					indexB--;				
				} else {

					//lets see if we need to copy A across

					if ((regionA[0] >= regionB[0] && regionA[1] <= regionB[1]) || (lastPos>=0 && regionA[0] >= lastPos)) {
						//either B or the last region we dealt with completely contains A, so we just discard A
					} else {
						//deal with A now
						int[] newRegion = {regionA[0], min(regionA[1], lastPos)};
						newRegions.add(0, newRegion);
						lastPos = regionA[0];
					}

					indexA--;	
				}
			}
		}

		//deal with any remaining A regions
		while (indexA >= 0) {
			int[] regionA = regionsA.get(indexA);
			if (lastPos >= 0 && regionA[0] > lastPos) {
				//this is already covered, so ignore it
			} else {
				int[] newRegion = {regionA[0], min(regionA[1], lastPos)};
				newRegions.add(0, newRegion);
				lastPos = regionA[0];
			}

			indexA--;
		}

		//deal with any remaining B regions
		while (indexB >= 0) {
			int[] regionB = regionsB.get(indexB);
			if (lastPos >= 0 && regionB[0] > lastPos) {
				//this is already covered, so ignore it
			} else {
				int[] newRegion = {regionB[0], min(regionB[1], lastPos)};
				newRegions.add(0, newRegion);
				lastPos = regionB[0];
			}

			indexB--;
		}

		return newRegions;
	}

	
	private int min(int a, int b) {
		if (a>=0 && b>=0) {
			return Math.min(a,b);
		} else {
			if (a>=0)
				return a;
			else 
				return b;
		}
	}

	public String resolveEmphasis(String text) {
		
		StringBuilder sb = new StringBuilder();
		
		for (String line : text.split("\n")) {
			sb.append(resolveLine(line));
			sb.append("\n");
		}
		sb.deleteCharAt(sb.length()-1);
		
		return sb.toString();
	}

	/**
	 * This is a direct translation of the php function doAllQuotes used by the original MediaWiki software.
	 * 
	 * @param line the line to resolve emphasis within
	 * @return the line, with all emphasis markup resolved to html tags
	 */
	private String resolveLine(String line) {
		
		//System.out.println("Resolving line '" + line + "'");

		String[] arr = getSplits("$"+line);

		if (arr.length <= 1)
			return line;

		//First, do some preliminary work. This may shift some apostrophes from
		//being mark-up to being text. It also counts the number of occurrences
		//of bold and italics mark-ups.

		int numBold = 0;
		int numItalics = 0;
		for (int i=0; i<arr.length; i++) {
			if (i % 2 == 1) {
				//If there are ever four apostrophes, assume the first is supposed to
				// be text, and the remaining three constitute mark-up for bold text.
				if (arr[i].length() == 4) {
					arr[i-1] = arr[i-1] + "'";
					arr[i] = getFilledString(3);
				} else if (arr[i].length() > 5) {
					//If there are more than 5 apostrophes in a row, assume they're all
					//text except for the last 5.
					arr[i-1] = arr[i-1] + getFilledString(arr[i].length()-5);
					arr[i] = getFilledString(5);
				}

				switch(arr[i].length()) {
				case 2:
					numItalics++;
					break;
				case 3:
					numBold++;
					break;
				case 5:
					numItalics++;
					numBold++;
				}
			}
		}

		//If there is an odd number of both bold and italics, it is likely
		//that one of the bold ones was meant to be an apostrophe followed
		//by italics. Which one we cannot know for certain, but it is more
		//likely to be one that has a single-letter word before it.
		if ((numBold%2==1) && (numItalics%2==1)) {
			int i= 0;
			int firstSingleLetterWord = -1;
			int firstMultiLetterWord = -1;
			int firstSpace = -1;

			for (String r:arr) {
				if ((i%2==1) && r.length()==3) {
					//added these checks to avoid string out of bounds exceptions
					if (i==0) continue;
					if (arr[i-1].length() < 2) continue;
					
					char x1 = arr[i-1].charAt(arr[i-1].length()-1);
					char x2 = arr[i-1].charAt(arr[i-1].length()-2);

					if (x1==' ') {
						if (firstSpace == -1)
							firstSpace = i;
					} else if (x2==' ') {
						if (firstSingleLetterWord == -1)
							firstSingleLetterWord = i;
					} else {
						if (firstMultiLetterWord == -1)
							firstMultiLetterWord = i;
					}
				}
				i++;
			}

			// If there is a single-letter word, use it!
			if (firstSingleLetterWord > -1) {
				arr[firstSingleLetterWord] = "''";
				arr[firstSingleLetterWord-1] = arr[firstSingleLetterWord] + "'";
			} else if (firstMultiLetterWord > -1) {
				// If not, but there's a multi-letter word, use that one.
				arr[firstMultiLetterWord] = "''";
				arr[firstMultiLetterWord-1] = arr[firstMultiLetterWord] + "'";
			} else if (firstSpace > -1) {
				// ... otherwise use the first one that has neither.
				// (notice that it is possible for all three to be -1 if, for example,
				// there is only one pentuple-apostrophe in the line)
				arr[firstSpace] = "''";
				arr[firstSpace-1] = arr[firstSpace] + "'";
			}

		}

		StringBuilder output = new StringBuilder();
		StringBuffer buffer = new StringBuffer();
		String state = "";
		int i = 0;
		for (String r : arr) {
			if (i%2==0) {
				if (state.equals("both")) 
					buffer.append(r);
				else
					output.append(r);
			} else {
				if (r.length() == 2) {
					if ( state.equals("i")) {
						output.append("</i>"); 
						state = "";
					} else if (state.equals("bi")) {
						output.append("</i>"); 
						state = "b";
					} else if ( state.equals("ib")) {
						output.append("</b></i><b>"); 
						state = "b";
					} else if (state.equals("both")) {
						output.append("<b><i>");
						output.append(buffer.toString());
						output.append("</i>");
						state = "b";
					} else { 
						output.append("<i>"); 
						state = state + "i";
					}
				} else if (r.length() == 3) {
					if ( state.equals("b") ) {
						output.append("</b>"); 
						state = "";
					} else if (state.equals("bi")) {
						output.append("</i></b><i>"); 
						state = "i";
					} else if (state.equals("ib")) {
						output.append("</b>"); 
						state = "i";
					} else if (state.equals("both")) {
						output.append("<i><b>");
						output.append(buffer);
						output.append("</b>");
						state = "i";
					} else { 
						//$state can be "i" or ""
						output.append("<b>"); 
						state = state + "b";
					}
				} else if (r.length() == 5) {
					if ( state.equals("b")) {
						output.append("</b><i>");
						state = "i";
					} else if (state.equals("i")) {
						output.append("</i><b>"); 
						state = "b";
					} else if (state.equals("bi")) {
						output.append("</i></b>"); 
						state = "";
					} else if (state.equals("ib")) {
						output.append("</b></i>"); 
						state = "";
					} else if (state.equals("both")) {
						output.append("<i><b>");
						output.append(buffer);
						output.append("</b></i>"); 
						state = "";
					} else { 
						buffer = new StringBuffer();
						state = "both";
					}
				}
			}
			i++;
		}

		//Now close all remaining tags.  Notice that the order is important.
		if ( state.equals("b") || state.equals("ib")) {
			output.append("</b>");
		}
		if ( state.equals("i") || state.equals("bi") || state.equals("ib") ) {
			output.append("</i>");
		}
		if ( state.equals("bi") ) {
			output.append("</b>");
		}
		// There might be lonely ''''', so make sure we have a buffer
		if ( state.equals("both") && buffer.length() > 0 ) {
			output.append("<b><i>");
			output.append(buffer);
			output.append("</i></b>");
		}
		
		//remove leading $
		output.deleteCharAt(0);

		return output.toString();

	}
	
	/*
	 * Does the same job as php function preg_split 
	 */
	private String[] getSplits(String text) {
		List<String> splits = new ArrayList<String>() ;

		Pattern p = Pattern.compile("\\'{2,}");
		Matcher m = p.matcher(text);
		int lastCopyIndex = 0;
		while (m.find()) {
			if (m.start() > lastCopyIndex)
				splits.add(text.substring(lastCopyIndex, m.start()));

			splits.add(m.group());
			lastCopyIndex = m.end();
		}

		if (lastCopyIndex < text.length()-1) {
			splits.add(text.substring(lastCopyIndex));
		}

		return splits.toArray(new String[splits.size()]);
	}

	private String getFilledString(int length) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<length; i++)
			sb.append("'");

		return sb.toString();
	}
}
