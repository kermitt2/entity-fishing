package com.scienceminer.nerd.embeddings;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kb.model.Page;
import com.scienceminer.nerd.kb.model.Article;
import com.scienceminer.nerd.kb.Statement;
import com.scienceminer.nerd.kb.db.KBIterator;
import com.scienceminer.nerd.kb.db.KBEnvironment;
import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.utilities.mediaWiki.MediaWikiParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Generate entity description to be used for producing entity embeddings. 
 * 
 * Command for creating description:
 * mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.embeddings.EntityDescription 
 * -Dexec.args="/mnt/data/wikipedia/embeddings/entity.en.description en"
 */
public class EntityDescription {
	private static final Logger LOGGER = LoggerFactory.getLogger(EntityDescription.class);

	private UpperKnowledgeBase upperKB;

	public EntityDescription() {
		try {
			upperKB = UpperKnowledgeBase.getInstance();
		}
		catch(Exception e) {
			throw new NerdResourceException("Error instanciating the knowledge base. ", e);
		}
	}

	/**
	 * Alignment by Entities Description (Zhong and Zhang, 2015), i.e. for us by its
	 * wikipedia article textual content.
	 *
	 * This first paragraph descriptions take around 18 minutes to be generated for 
	 * the complete English Wikipedia.
	 *
	 * Huaping Zhong and Jianwen Zhang. 2015. Aligning knowledge and text embeddings by 
	 * entity descriptions. In Proceedings of Conference on Empirical Methods in Natural 
	 * Language Processing.
	 */
	public void generateDescriptionSummaries(String path, String lang, boolean full) {
		BufferedWriter writer = null;
		KBIterator iter = null;

		LowerKnowledgeBase wikipedia = null;
		try {
			wikipedia = upperKB.getWikipediaConf(lang);
			if (full)
				wikipedia.loadFullContentDB();
		}
		catch(Exception e) {
			throw new NerdResourceException("Error instanciating the knowledge base. ", e);
		}

		try {
			writer = new BufferedWriter(new FileWriter(new File(path+"/" + lang + "/description.summaries."+lang)));
			iter = upperKB.getEntityIterator();
			int n = 0;
			while(iter.hasNext()) {
				if (n > 10000) {
					writer.flush();
					n = 0;
				}
				Entry entry = iter.next();
				byte[] keyData = entry.getKey();
				byte[] valueData = entry.getValue();
				//Page p = null;
				try {
					String entityId = (String)KBEnvironment.deserialize(keyData);
					Map<String,Integer> pagesIds = (Map<String,Integer>)KBEnvironment.deserialize(valueData);
					Integer pageId = pagesIds.get(lang);
					if (pageId != null) {
						// get the description summary of the entity
						Page page = upperKB.getWikipediaConf(lang).getPageById(pageId);
						String text = null;
						if (full) {
							text = page.getFullWikiText();
						}
						else {
							text = page.getFirstParagraphWikiText();
						}
						text = normaliseDescription(text, lang);
						if (text.length() > 10) {
							writer.write(entityId + "\t" + text + "\n");
							n++;
						}
					}
				} catch(Exception e) {
					LOGGER.error("fail to write entity description", e);
				}
			}
		} catch(IOException e) {
			LOGGER.error("Error when writing entity description", e);
 		} finally {
			if (iter != null)
				iter.close();
			if (writer != null) {
				try {
					writer.flush();
					writer.close();
				} catch(Exception e) {
					LOGGER.error("fail to close the entity description file", e);
				}	
			}
		}
	}

	/**
	 * Alignment by Wikipedia Anchors (Wang et al., 2014), i.e. for us 
	 * a windows of fixed size (typically 20) surrounding mentions of the 
	 * entity in the Wikipedia articles. 
	 *
	 * Zhen Wang, Jianwen Zhang, Jianlin Feng, and Zheng Chen. 2014. 
	 * Knowledge graph and text jointly embedding. In Proceedings of the 2014 
	 * Conference on Empirical Methods in Natural Language Processing.
 	 * Association for Computational Linguistics, pages 1591–1601
	 */
	public void generateDescriptionMentionContexts(String path, String lang) {
		LowerKnowledgeBase wikipedia = null;
		try {
			wikipedia = upperKB.getWikipediaConf(lang);
		}
		catch(Exception e) {
			throw new NerdResourceException("Error instantiating the knowledge base. ", e);
		}

		// load, and possibly create if not yet done, the full text of wikipedia articles
		// database
		LOGGER.info("Loading full wikitext content - this will take a while the first time");
		wikipedia.loadFullContentDB();

		BufferedWriter writer = null;
		KBIterator iter = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(path+"/"+lang+"/description.mentions."+lang)));
			iter = upperKB.getEntityIterator();
			int n = 0;
			while(iter.hasNext()) {
				if (n > 10000) {
					writer.flush();
					n = 0;
				}
				Entry entry = iter.next();
				byte[] keyData = entry.getKey();
				byte[] valueData = entry.getValue();
				try {
					String entityId = (String)KBEnvironment.deserialize(keyData);
					Map<String,Integer> pagesIds = (Map<String,Integer>)KBEnvironment.deserialize(valueData);
					Integer pageId = pagesIds.get(lang);
					if (pageId != null) {
						// get all the pages linking to this page
						Page page = wikipedia.getPageById(pageId);
						if (page.getType() == Page.PageType.article) {
							Article article = (Article)page;
							Article[] articles = article.getLinksIn();
							StringBuilder textBuilder = new StringBuilder();
							// get all the fulltext for the "mentionning" articles
							for(int i=0; i<articles.length; i++) {
								String markup = articles[i].getFullWikiText();
								if ( (markup == null) || (markup.length() < 10) )
									continue;
								markup = MediaWikiParser.getInstance().toTextWithInternalLinksOnly(markup, lang);
								List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(markup, new Language(lang, 1.0));
								List<int[]> regions = getLinkRegions(markup);
								int l = 0;
								for(int[] region : regions) {
									if (l > 100)
										break;
									if (region.length != 2)
										continue;

									// check if this region correspond to a link to the entity entityId
									String regionString = markup.substring(region[0]+2, region[1]-2);
									String title = null;
									int ind = regionString.indexOf("|");
									if (ind == -1)
										title = regionString;
									else 
										title = regionString.substring(0,ind);
									if ( (title == null) || (title.length() < 3) )
										continue;
									if (title.indexOf("#") != -1)
										title = title.substring(0, title.indexOf("#"));
									Article articleLink = wikipedia.getArticleByTitle(title);
									if (articleLink == null)
										continue;
									int pageLinkId = articleLink.getId();
									if (pageLinkId == pageId) {
										List<LayoutToken> subTokens = Utilities.getWindow(region[0]+2, region[1]-2, tokens, 10, lang);
										for (LayoutToken token : subTokens)
											textBuilder.append(token.getText()).append(" ");
										l++;
									}
								}
							}
							if (textBuilder.length() > 1) {
								String text = normaliseDescription(textBuilder.toString(), lang);
								writer.write(entityId + "\t" + text + "\n");
							}
						}
						n++;
					}
				} catch(Exception e) {
					LOGGER.error("fail to write entity description", e);
				}
			}
		} catch(IOException e) {
			LOGGER.error("Error when writing entity description", e);
 		} finally {
			IOUtils.closeQuietly(iter, writer);
		}
	}

	/**
	 * Alignment by wikidata relations and entity names (Wang et al., 2014). 
	 * i.e. for us for each triplet (h, r, t), h (head entity) or t (tail entity) 
	 * are replaced with their corresponding textual names.
	 *
	 * Zhen Wang, Jianwen Zhang, Jianlin Feng, and Zheng Chen. 2014. 
	 * Knowledge graph and text jointly embedding. In Proceedings of the 2014 
	 * Conference on Empirical Methods in Natural Language Processing.
 	 * Association for Computational Linguistics, pages 1591–1601
	 */
	public void generateDescriptionGraphRelations(String path, String lang) {
		LowerKnowledgeBase wikipedia = null;
		try {
			wikipedia = upperKB.getWikipediaConf(lang);
		}
		catch(Exception e) {
			throw new NerdResourceException("Error instanciating the knowledge base. ", e);
		}

		// for each entity, we have the relations where the entity is head entity

		// build the reverse map giving all the relation where an entity is tail
		upperKB.loadReverseStatementDatabase(false);

		BufferedWriter writer = null;
		KBIterator iter = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(path+"/"+lang+"/description.summaries."+lang)));
			iter = upperKB.getEntityIterator();
			int n = 0;
			while(iter.hasNext()) {
				if (n > 10000) {
					writer.flush();
					n = 0;
				}

				Entry entry = iter.next();
				byte[] keyData = entry.getKey();
				byte[] valueData = entry.getValue();
				try {
					StringBuilder textBuilder = new StringBuilder();
					String entityId = (String)KBEnvironment.deserialize(keyData);

					List<Statement> statements = upperKB.getStatements(entityId);
					if (statements != null) {
						for(Statement statement : statements) {
							String value = statement.getValue();
							if (value.startsWith("Q")) {
								// we have an entity as value
								Integer pageId = upperKB.getPageIdByLang(value, lang);
								Page page = wikipedia.getPageById(pageId);

								String valueEntityString = page.getTitle();
								textBuilder.append(valueEntityString).append(" ");
							} else {
								// we have a literal value, and we only consider the textual values

							}
						}
					}

					statements = upperKB.getReverseStatements(entityId);
					if (statements == null) 
						continue;
					
					for(Statement statement : statements) {
						String headEntityId = statement.getConceptId();

						Integer pageId = upperKB.getPageIdByLang(headEntityId, lang);
						Page page = wikipedia.getPageById(pageId);

						String headEntityString = page.getTitle();
						textBuilder.append(headEntityString).append(" ");
					}

					if (textBuilder.length() > 1) {
						String text = normaliseDescription(textBuilder.toString(), lang);
						writer.write(entityId + "\t" + text + "\n");
					}
					
				} catch(Exception e) {
					LOGGER.error("fail to write entity description", e);
				}
			}
		} catch(IOException e) {
			LOGGER.error("Error when writing entity description", e);
 		} finally {
			IOUtils.closeQuietly(iter, writer);
		}


	}

	/**
	 * Normalise wikimedia texts according to embedding training requirements which
	 * is a simple sequence of words.
	 */
	private static String normaliseDescription(String wikitext, String lang) {
		String text = MediaWikiParser.getInstance().toTextOnly(wikitext, lang);
		text = text.replace("\t", " ");

		// following fastText string normalisation: 
		// 1. All punctuation-and-numeric. Things in this bucket get
        // 	  their numbers flattened, to prevent combinatorial explosions.
        //    They might be specific numbers, prices, etc.
        //    -> all numerical chars are actually all transformed to '0'
    	// 2. All letters: case-flattened.
   	    // 3. Mixed letters and numbers: a product ID? Flatten case and leave
    	//    numbers alone.

		// unicode normalization
		text = UnicodeUtil.normaliseText(text);

		// wikipedia unicode encoding to Java encoding
		// <U+00AD> -> \u00AD
		//text.replaceAll();

    	// remove all xml scories
		text = text.replaceAll("<[^>]+>", " ");

		// remove all punctuation
		text = text.replaceAll("\\p{P}", " ");

		// flatten numerical chars
		text = text.replaceAll("\\d", "0");

		text = text.replaceAll("\\|", " ");

		// lower case everything (to be evaluated!)
		text = text.toLowerCase();

		// collapse spaces
		text = StringUtils.normalizeSpace(text);

		// remove stopword
		// tokenize
		List<String> tokens = GrobidAnalyzer.getInstance().tokenize(text, new Language(lang, 1.0));
		StringBuilder textBuilder = new StringBuilder();
		for(String word : tokens) {
			try {
				if (!Stopwords.getInstance().isStopword(word, lang))
					textBuilder.append(word).append(" ");
			} catch(Exception e) {
				LOGGER.warn("Problem getting Stopwords instance", e);
				textBuilder.append(word).append(" ");
			}
		}

		//return textBuilder.toString().replaceAll("( )*"," ").trim();
		return StringUtils.normalizeSpace(textBuilder.toString());
	}

	/**
	 * Simple method for converting entity embeddings with Wikipedia identifiers
	 * into entity embeddings with Wikidata identifiers.
	 */
	public void convertIdentifierEmbeddings(String path, String lang) {
		LowerKnowledgeBase wikipedia = null;
		try {
			wikipedia = upperKB.getWikipediaConf(lang);
		}
		catch(Exception e) {
			throw new NerdResourceException("Error instanciating the knowledge base. ", e);
		}

		BufferedWriter writer = null;
		BufferedReader reader = null;
		int total = 0;
		int error = 0;
		try {
			reader = new BufferedReader(new FileReader(new File(path)));
			writer = new BufferedWriter(new FileWriter(new File(path+".out")));
			
			String line;
			int n = 0;
			boolean start = true;
		    while ((line = reader.readLine()) != null) {
		    	total++;
		    	// skip first line
		    	if (start) {
		    		start = false;
		    		continue;
		    	}
		    	if (n > 1000) {
		    		writer.flush();
		    		n = 0;
		    	}
		       	int ind = line.indexOf("\t");
		       	String title = line.substring(0, ind);
		       	Article page = wikipedia.getArticleByTitle(title);
		       	String wikidataId = null;
		       	if (page == null)
		       		System.out.println("Warning: page title not found -> " + title);
		       	else
		       		wikidataId = page.getWikidataId();

		       	if (wikidataId == null)
		       		System.out.println("Warning: wikidataID for title not found -> " + title);

		       	int ind2 = line.indexOf("\t", ind+1);
		       	String pageIdString = line.substring(ind+1, ind2);
		       	int pageId = -1;
		       	try {
		       		pageId = Integer.parseInt(pageIdString);
		       	} catch(Exception e) {
		       		e.printStackTrace();
		       	}
		       	String wikidataId2 = null;
		       	if (pageId != -1) {
		       		 Page thePage = wikipedia.getPageById(pageId);
		       		 wikidataId2 = thePage.getWikidataId();
		       		 if (wikidataId2 == null)
		       			System.out.println("Warning: wikidataID for page id not found -> " + pageId);

		       		 if (wikidataId != null && wikidataId2 != null && !wikidataId.equals(wikidataId2)) {
		       		 	System.out.println("Warning: title and page id are not matching to the same Wikidata Id: " + wikidataId + " =/= " + wikidataId2);
		       		 }
		       	}

		       	if (wikidataId != null) {
			       	writer.write(wikidataId+"\t");
			       	writer.write(line.substring(ind2+1) + "\n");
		       	}
			    else if (wikidataId2 != null) {
			    	writer.write(wikidataId2+"\t");
			    	writer.write(line.substring(ind2+1) + "\n");
			    }
			    else
			    	error++;
		       	
		       	n++;
		    }
		    writer.flush();
		} catch(IOException e) {
			LOGGER.error("Error when writing entity description", e);
 		} finally {
			IOUtils.closeQuietly(writer);
		}
		System.out.println("total of " + total + " input entity vectors, with " + error + " failed entity resolution.");
	}

	/**
	 * Gathers complex regions: ones which can potentially be nested within each other.
	 * 
	 * The returned regions (an array of start and end positions) will be either
	 * non-overlapping or cleanly nested, and sorted by end position. 
	 */ 
	public static List<int[]> getLinkRegions(String markup) {
		// each region is given as an array containing start and end character indexes of the region
		List<int[]> regions = new ArrayList<int[]>() ;

		//a stack of region starting positions
		List<Integer> startStack = new ArrayList<Integer>() ;
		Pattern p = Pattern.compile("((\\[\\[)|(\\]\\]))", Pattern.DOTALL) ;
		Matcher m = p.matcher(markup) ;
		while(m.find()) {
			Integer p1 = m.start() ;
			Integer p2 = m.end() ;  
			if (m.group(2) != null) {
				//this is the start of an item
				startStack.add(p1) ;
			} else {
				//this is the end of an item
				if (!startStack.isEmpty()) {
					int start = startStack.get(startStack.size()-1) ;
					startStack.remove(startStack.size()-1) ;
					
					int[] region = {start, p2} ;
					regions.add(region) ;
				} 
			}
		}
		return regions ;
	}

	public static void main(String args[]) throws Exception {
		File dataDir = new File(args[0]);
		String lang = args[1];
		
        if (!dataDir.exists()) {
            System.err.println("Path to the data file is not valid");
            System.exit(-1);
        }
        EntityDescription entityDescription = new EntityDescription();
        entityDescription.generateDescriptionSummaries(args[0], lang, true);

        //entityDescription.generateDescriptionMentionContexts(args[0], lang);
        //entityDescription.generateDescriptionGraphRelations(args[0], lang);
        //entityDescription.convertIdentifierEmbeddings(args[0], lang);
	}
}