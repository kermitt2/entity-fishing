package com.scienceminer.nerd.kb;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.utilities.NerdProperties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;  

import org.mapdb.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang.ArrayUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;

import org.wikipedia.miner.model.*;
import org.wikipedia.miner.util.*;

/**
 * Persistent mapping between Wikipedia page and GRISP domain taxonomy based on Wikipedia categories.
 * 
 * @author Patrice Lopez
 * 
 */
public class WikipediaDomainMap { 
    /**
     * The class Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaDomainMap.class);

    // mapdb
    private DB db = null;
    private String database_path = null;
    private String database_name = "domains";

    // map a Wikipedia page id to a list of domain IDs
    private ConcurrentMap<Integer, int[]> domains = null;

    // domain id map
    private Map<Integer,String> id2domain = null;
  
    // domain label map  
    private Map<String,Integer> domain2id = null;

    // wikipedia main categories (pageId of the category) to grisp domains
    private Map<Integer,List<Integer>> wikiCat2domains = null;

    private Wikipedia wikipedia = null;
    private String lang = null;

    private static String grispDomains = "data/grisp/domains.txt";
    private static String wikiGrispMapping = "data/wikipedia/mapping.txt";

    /**
     * Hidden constructor
     */
    public WikipediaDomainMap() {
    }

    public void setWikipedia(Wikipedia wikipedia) {
        this.wikipedia = wikipedia;
        try {
            loadGrispMapping();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    /**
     * Open index for wikipedia domain map creation
     */
    public void openCreate() {
        File home = null;      
        try {
            home = new File(NerdProperties.getInstance().getMapDBPath());
        } catch (Exception e) {
            throw new NerdException(e);
        }
        try {
			// for the moment we use only English categories - for non-English languages we use
			// transligual correspondences
            // open MapDB
            db = DBMaker
                    .fileDB(NerdProperties.getInstance().getMapDBPath() + "/" + database_name + "-" + "en" + ".db")
                    .fileMmapEnable()            // always enable mmap
                    .make();

            domains = db.hashMap(database_name)
                    .keySerializer(Serializer.INTEGER)
                    .valueSerializer(Serializer.INT_ARRAY)
                    .make();
        } catch (Exception dbe) {
            LOGGER.debug("Error when opening MapDB.");
            throw new NerdException(dbe);
        }
    }

    /**
     * Open index for wikipedia domain map usage, non transactional
     */
    public void openUse() {
        File home = null;      
        try {
            home = new File(NerdProperties.getInstance().getMapDBPath());
        } catch (Exception e) {
            throw new NerdException(e);
        }
        try {
            // open MapDB
            db = DBMaker
                    .fileDB(NerdProperties.getInstance().getMapDBPath() + "/" + database_name + "-" + lang + ".db")
                    .fileMmapEnable()            // always enable mmap
                    .readOnly()
                    .make();

            domains = db.hashMap(database_name)
                    .keySerializer(Serializer.INTEGER)
                    .valueSerializer(Serializer.INT_ARRAY)
                    .makeOrGet();
        } catch (Exception dbe) {
            LOGGER.debug("Error when opening MapDB.");
            throw new NerdException(dbe);
        }
    }

    /**
     * Close index wikipedia domain map
     */
    public void close() {
        db.close();
    }

    private void loadGrispMapping() throws Exception {
        importDomains();
        wikiCat2domains = readMapping(wikiGrispMapping);
    }

    private int[] createMapping(Article page) {
        List<Integer> theDomains = null;
        org.wikipedia.miner.model.Category[] categories = page.getParentCategories();
        // expand the categories (hope there is no cycle!) 
        Set<Integer> allCategories = new HashSet<Integer>();
        Set<Integer> newCategories = new HashSet<Integer>();
        for(int i=0;i<categories.length;i++) {
            allCategories.add(new Integer(categories[i].getId()));
            newCategories.add(new Integer(categories[i].getId()));
            //break;
        }

        int size = newCategories.size();
        while(size != 0) {
/*System.out.println(size + " / " + allCategories.size());
for(Integer cat : newCategories) {
    org.wikipedia.miner.model.Category theCategory = (org.wikipedia.miner.model.Category)wikipedia.getPageById(cat.intValue());
    System.out.print(theCategory.getTitle() + ", ");
}
System.out.print("\n");*/
            Set<Integer> nextCategories = new HashSet<Integer>();
            for(Integer category : newCategories) {
                if (wikiCat2domains.get(category) != null) {
                    if (theDomains == null) 
                        theDomains = new ArrayList<Integer>();
                    List<Integer> grispDomains = wikiCat2domains.get(category);
                    for(Integer grispDomain : grispDomains) {
                        if (!theDomains.contains(grispDomain))
                            theDomains.add(grispDomain);
                    }
                }
                org.wikipedia.miner.model.Category theCategory = (org.wikipedia.miner.model.Category)wikipedia.getPageById(category.intValue());
                categories = theCategory.getParentCategories();
                for(int i=0;i<categories.length;i++) {
                    if (!nextCategories.contains(new Integer(categories[i].getId())))
                        nextCategories.add(categories[i].getId());
                }
            }

            newCategories = new HashSet<Integer>();
            for(Integer category : nextCategories) {
                if (!allCategories.contains(category)) {
                    newCategories.add(category);
                    allCategories.add(category);
                    //break;
                }
            }
            size = newCategories.size();
            if ((theDomains != null) && theDomains.size()>0)
                break;
        }

        
        if (theDomains == null)
            return null;
        else {
            // the following requires Java 8 streams!
            //return theDomains.stream().mapToInt(i->i).toArray();

            // for Java 7 with Apache Commons
            Integer[] integers = theDomains.toArray(new Integer[theDomains.size()]);
            return ArrayUtils.toPrimitive(integers);

        }
    }

    public void createAllMappings() {
        // for each page id in wikipedia we get the list of domain id
        PageIterator iterator = wikipedia.getPageIterator(Page.PageType.article);
        int p = 0;
        while(iterator.hasNext()) {
            if ((p%10000) == 0)
                System.out.println(p);
            // add to the persistent map
            Page page = iterator.next();
            int pageId = page.getId();
            int[] theDomains =  createMapping((Article) page);
            if (theDomains != null)
                domains.put(new Integer(pageId), theDomains);
            p++;
        }
        iterator.close();
    }

    private Map<Integer, List<Integer>> readMapping(String mappingFilePath) throws IOException {
        LineIterator iterator = FileUtils.lineIterator(new File(mappingFilePath));
        Map<Integer, List<Integer>> domains = new HashMap<Integer, List<Integer>>();

        while (iterator.hasNext()) {
            String line = iterator.nextLine();
            if (isBlank(line)) {
                continue;
            }
            StringTokenizer st = new StringTokenizer(line, "\t");
            String category = null; 
            int categoryId = -1;
            if (st.hasMoreTokens()) {
                category = st.nextToken();
                org.wikipedia.miner.model.Category theCategory = wikipedia.getCategoryByTitle(category);
                if (theCategory == null)
                    System.out.println("Warning: " + category + " is not a category found in Wikipedia.");
                else {
                    categoryId = theCategory.getId();
                    if (domains.get(new Integer(categoryId)) != null) {
                        System.out.println("Warning: " + category + " is already defined in " + mappingFilePath);
                    }
                }
            }
            if (categoryId != -1) {
                List<Integer> dom = new ArrayList<Integer>();
                while (st.hasMoreTokens()) {
                    String domain = st.nextToken();
                    if (domain2id.get(domain) == null)
                        System.out.println("Warning: " + domain + " is an invalid GRISP domain label in " + mappingFilePath);
                    else {
                        Integer domainId = domain2id.get(domain);
                        dom.add(domainId);
                    }
                }
                domains.put(new Integer(categoryId), dom);
            }
        }
        LineIterator.closeQuietly(iterator);

        return domains;
    }

    /**
     * Import the GRISP general domains
     */
    private void importDomains() throws IOException {
        domain2id = new HashMap<String, Integer>();
        id2domain = new HashMap<Integer, String>();

        LineIterator domainIterator = FileUtils.lineIterator(new File(grispDomains));
        int n = 0;
        while (domainIterator.hasNext()) {
            String line = domainIterator.next();
            final String domain = line.replace('\t', ' ').trim();
            domain2id.put(domain, new Integer(n));
            id2domain.put(new Integer(n), domain);
            n++;
        }
        LineIterator.closeQuietly(domainIterator);
    }


    public List<String> getDomains(int pageId) {
        int[] list = domains.get(new Integer(pageId));
        List<String> result = null;
        if (list != null) {
            result = new ArrayList<String>();
            for(int i=0; i<list.length; i++) {
                String domain = id2domain.get(new Integer(list[i]));
                if (domain != null)
                    result.add(domain);
            }
        }
        return result;
    }

}