/*
 *    MarkupStripper.java
 *    Copyright (C) 2007 David Milne, d.n.milnegmail.com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.wikipedia.miner.util;

import java.util.*;
import java.util.regex.*;


/**
 * This provides tools to strip out markup from wikipedia articles, or anything else that has been written
 * in mediawiki's format. It's all pretty simple, so don't expect perfect parsing. It is particularly bad at 
 * dealing with templates (these are simply removed rather than resolved).  
 */
public class MarkupStripper {

	private Pattern linkPattern = Pattern.compile("\\[\\[(.*?:)?(.*?)(\\|.*?)?\\]\\]");
	
	private Pattern isolatedBefore = Pattern.compile("(\\s*|.*\\n(\\s*))", Pattern.DOTALL);
	private Pattern isolatedAfter = Pattern.compile("(\\s*|(\\s*)\\n.*)", Pattern.DOTALL);
	
	private EmphasisResolver emphasisResolver = new EmphasisResolver();

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
		Vector<int[]> regions = gatherSimpleRegions(markup, "\\<\\!--(.*?)--\\>");
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

		Vector<int[]> regions = gatherComplexRegions(markup, "\\[\\[", "\\]\\]");

		StringBuffer strippedMarkup = new StringBuffer();
		int lastPos = markup.length();

		//because regions are sorted by end position, we work backwards through them
		int i = regions.size();

		while (i > 0) {
			i --;

			int[] region = regions.elementAt(i);

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
		
		String resolvedMarkup = emphasisResolver.resolveEmphasis(markup);
		
		Vector<int[]> regions = gatherSimpleRegions(resolvedMarkup, "\\<\\/?[bi]\\>");
		
		StringBuffer clearedMarkup = new StringBuffer();
		int lastPos = 0;
		
		int i = regions.size();

		while (i > 0) {
			i --;

			int[] region = regions.elementAt(i);


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

		Vector<int[]> regions = gatherComplexRegions(markup, "\\[\\[", "\\]\\]");

		StringBuffer strippedMarkup = new StringBuffer();
		int lastPos = markup.length();

		//because regions are sorted by end position, we work backwards through them
		int i = regions.size();

		while (i > 0) {
			i --;

			int[] region = regions.elementAt(i);
			
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
		
		Vector<int[]> regions = new Vector<int[]>();
		
		for (String sectionName:sectionNames) 
			regions = mergeRegionLists(regions, gatherSection(markup, sectionName));

		return stripRegions(markup, regions, replacement);
	}
	
	public String stripSectionHeaders(String markup, Character replacement) {
		
		Vector<int[]> regions = this.gatherSectionHeaders(markup);
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
	public String stripRegions(String markup, Vector<int[]> regions, Character replacement) {

		StringBuffer clearedMarkup = new StringBuffer();

		int lastPos = markup.length();

		//because regions are sorted by end position, we work backwards through them
		int i = regions.size();

		while (i > 0) {
			i --;

			int[] region = regions.elementAt(i);

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


//	======================================================================================================

	/**
	 * Gathers areas within the markup which correspond to links to other wikipedia pages
	 * (as identified by [[ and ]] pairs). Note: these can be nested (e.g. for images)
	 */
	public Vector<int[]> gatherInternalLinks(String markup) {
		//currItem = "internal links";

		return gatherComplexRegions(markup, "\\[\\[", "\\]\\]");
	}
	


	/**
	 * Gathers areas within the markup which correspond to templates (as identified by {{ and }} pairs). 
	 */
	public Vector<int[]> gatherTemplates(String markup) {
		//currItem = "templates";
		return gatherComplexRegions(markup, "\\{\\{", "\\}\\}");
	}
	
	public Vector<int[]> getIsolatedRegions(Vector<int[]> regions, String markup) {
		
		Vector<int[]> isolatedRegions = new Vector<int[]>();
		
		for (int[] region:regions) {
			if (isIsolated(region, markup))
				isolatedRegions.add(region);
		};
		
		return isolatedRegions;
	}
	
	public Vector<int[]> excludeIsolatedRegions(Vector<int[]> regions, String markup) {
		
		Vector<int[]> unisolatedRegions = new Vector<int[]>();
		
		for (int[] region:regions) {
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
	public Vector<int[]> gatherTables(String markup) {
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
	public Vector<int[]> gatherHTML(String markup) {

		//currItem = "html";

		//gather and merge references
		Vector<int[]> regions = gatherReferences(markup);

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
	public Vector<int[]> gatherReferences(String markup) {

		//currItem = "references";

		//gather <ref/>
		Vector<int[]> regions = gatherSimpleRegions(markup, "\\<ref(\\s*?)([^>]*?)\\/\\>");

		//gather <ref> </ref> pairs (these shouldnt be nested, but what the hell...)
		regions = mergeRegionLists(regions, gatherComplexRegions(markup, "\\<ref(\\s*?)([^>\\/]*?)\\>", "\\<\\/ref(\\s*?)\\>"));

		return regions;
	}


	/**
	 * Gathers items which MediaWiki documentation mysteriously refers to as "majic words": e.g. __NOTOC__
	 */
	public Vector<int[]> gatherMagicWords(String markup) {

		//currItem = "magic words";
		return gatherSimpleRegions(markup, "\\_\\_([A-Z]+)\\_\\_");
	}

	/**
	 * Gathers all links to external web pages
	 */
	public Vector<int[]> gatherExternalLinks(String markup) {
		//currItem = "external links";
		return gatherSimpleRegions(markup, "\\[(http|www|ftp).*?\\]");
	}

	/**
	 * Gathers bold and italic markup
	 */
	public Vector<int[]> gatherEmphasis(String markup) {
		//currItem = "emphasis";
		return gatherSimpleRegions(markup, "'{2,}"); 
	}
	
	
	/**
	 * Gathers section headers
	 */
	public Vector<int[]> gatherSectionHeaders(String markup) {

		Vector<int[]> regions = new Vector<int[]>();
		
		Pattern p = Pattern.compile("\\n\\s*((={2,})[^=].*?\\2)[^=]");
		Matcher m = p.matcher(markup);
		
		while (m.find()) {
			int[] region = {m.start(1), m.end(1)};
			regions.add(region);
		}
		return regions;
	}
	
	
	public Vector<int[]> gatherSection(String markup, String sectionName) {
		
		Vector<int[]> regions = new Vector<int[]>();
		
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
	public Vector<int[]> gatherListAndIndentMarkers(String markup) {
		//currItem = "list and intent markers";

		Vector<int[]> regions = gatherSimpleRegions(markup, "\n( *)([//*:]+)");

		//increment start positions of all regions by one, so they don't include the newline character
		for (int[] region:regions)
			region[0]++;

		//add occurance of list item on first line (if there is one)
		regions = mergeRegionLists(regions, gatherSimpleRegions(markup, "^( *)([//*:]+)"));
		return regions;
	}
	
	private boolean isEntirelyItalicised(String line) {
		
		String resolvedLine = emphasisResolver.resolveEmphasis(line);
		
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
	public Vector<int[]> gatherMisformattedStarts(String markup) {

		//currItem = "starts";

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

		Vector<int[]> regions = new Vector<int[]>();
		regions.add(region);
		
		return regions;
	}


	/**
	 * Gathers simple regions: ones which cannot be nested within each other.
	 * 
	 *  The returned regions (an array of start and end positions) will be sorted 
	 *  by end position (and also by start position, since they can't overlap) 
	 */ 
	public Vector<int[]> gatherSimpleRegions(String markup, String regex) {

		//an array of regions we have identified
		//each region is given as an array containing start and end character indexes of the region. 
		Vector<int[]> regions = new Vector<int[]>();
		
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
	public Vector<int[]> gatherComplexRegions(String markup, String startRegex, String endRegex) {

		//an array of regions we have identified
		//each region is given as an array containing start and end character indexes of the region. 
		Vector<int[]> regions = new Vector<int[]>();

		//a stack of region starting positions
		Vector<Integer> startStack = new Vector<Integer>();
		
		
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
					int start = startStack.elementAt(startStack.size()-1);
					startStack.removeElementAt(startStack.size()-1);
					
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
	 * Collapses a region list, by discarding any regions which are contained within 
	 * other regions.
	 * 
	 * The resulting region list will be non-overlapping and sorted by end positions.
	 *//*
	private Vector<int[]> collapseRegionList(Vector<int[]> regions) {

		Vector<int[]> newRegions = new Vector<int[]>();

		int index = regions.size() -1;

		int lastPos = -1;

		while (index >= 0) {

			int[] region = regions.elementAt(index);

			if (lastPos <0 || region[1] <= lastPos) {
				newRegions.add(0, region);
				lastPos = region[0];
			}
			
			index--;
		}

		return newRegions;	
	}*/

	/**
	 * Merges two lists of regions into one sorted list. Regions that are contained
	 * within other regions are discarded.
	 * 
	 * The resulting region list will be non-overlapping and sorted by end positions.
	 */
	private Vector<int[]> mergeRegionLists(Vector<int[]> regionsA, Vector<int[]> regionsB) {

		int indexA = regionsA.size() -1;
		int indexB = regionsB.size() - 1;

		Vector<int[]> newRegions = new Vector<int[]>();

		int lastPos = -1;

		while (indexA >= 0 && indexB >= 0) {

			int[] regionA = regionsA.elementAt(indexA);
			int[] regionB = regionsB.elementAt(indexB);

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

			int[] regionA = regionsA.elementAt(indexA);

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

			int[] regionB = regionsB.elementAt(indexB);

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
	

}
