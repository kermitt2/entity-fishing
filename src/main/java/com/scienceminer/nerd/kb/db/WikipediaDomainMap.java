package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.model.Article;
import com.scienceminer.nerd.kb.model.Page;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.ArrayUtils;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static java.nio.ByteBuffer.allocateDirect;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.lmdbjava.EnvFlags.MDB_NOTLS;

/**
 * Persistent mapping between Wikipedia page and GRISP domain taxonomy based on Wikipedia categories.
 */
public class WikipediaDomainMap {
    /**
     * The class Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaDomainMap.class);

    // LMDB metadata
    protected Env<ByteBuffer> environment;
    protected Dbi<ByteBuffer> db;
    protected String envFilePath = null;
    protected boolean isLoaded = false;
    private String database_name = "domains";

    // an in-memory cache - map a Wikipedia page id to a list of domain IDs
    //private ConcurrentMap<Integer, int[]> domainsCache = null;

    // domain id map
    private Map<Integer, String> id2domain = null;

    // domain label map  
    private Map<String, Integer> domain2id = null;

    // wikipedia main categories (pageId of the category) to grisp domains
    private Map<Integer, List<Integer>> wikiCat2domains = null;

    private LowerKnowledgeBase wikipedia = null;
    private String lang = null;

    private static String grispDomains = "data/grisp/domains.txt";
    private static String wikiGrispMapping = "data/wikipedia/mapping.txt";

    public WikipediaDomainMap(String lang, String envPath) {
        this.lang = lang;
        this.envFilePath = envPath + "/" + database_name;

        File thePath = new File(this.envFilePath);
        if (!thePath.exists()) {
            thePath.mkdirs();
            isLoaded = false;
            LOGGER.info("domains " + lang + " / isLoaded: " + isLoaded);
        } else {
            // we assume that if the DB files exist, it has been already loaded
            isLoaded = true;
            LOGGER.info("domains " + lang + " / isLoaded: " + isLoaded);
        }
        this.environment = Env.create()
                .setMapSize(100L * 1024L * 1024L * 1024L)
                .open(thePath, MDB_NOTLS);
        db = this.environment.openDbi(database_name, DbiFlags.MDB_CREATE);
    }

    public void setWikipedia(LowerKnowledgeBase wikipedia) {
        this.wikipedia = wikipedia;
        try {
            loadGrispMapping();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getLang() {
        return this.lang;
    }

    private void loadGrispMapping() throws Exception {
        importDomains();
        wikiCat2domains = readMapping(wikiGrispMapping);
    }

    private int[] createMapping(Article page) {
        List<Integer> theDomains = null;
        com.scienceminer.nerd.kb.model.Category[] categories = page.getParentCategories();

/*System.out.println("pageId:" + page.getId());
for(int l=0; l<categories.length;l++){
    System.out.println(categories[l].getId());
}*/

        // expand the categories (hope there is no cycle!) 
        Set<Integer> allCategories = new HashSet<Integer>();
        Set<Integer> newCategories = new HashSet<Integer>();
        for (int i = 0; i < categories.length; i++) {
            allCategories.add(new Integer(categories[i].getId()));
            newCategories.add(new Integer(categories[i].getId()));
            //break;
        }

        int size = newCategories.size();
        while (size != 0) {
/*System.out.println(size + " / " + allCategories.size());
for(Integer cat : newCategories) {
    org.wikipedia.miner.model.Category theCategory = (org.wikipedia.miner.model.Category)wikipedia.getPageById(cat.intValue());
    System.out.print(theCategory.getTitle() + ", ");
}
System.out.print("\n");*/
            Set<Integer> nextCategories = new HashSet<Integer>();
            for (Integer category : newCategories) {
                if (wikiCat2domains.get(category) != null) {
                    if (theDomains == null)
                        theDomains = new ArrayList<Integer>();
                    List<Integer> grispDomains = wikiCat2domains.get(category);
                    for (Integer grispDomain : grispDomains) {
                        if (!theDomains.contains(grispDomain))
                            theDomains.add(grispDomain);
                    }
                }
                com.scienceminer.nerd.kb.model.Category theCategory = (com.scienceminer.nerd.kb.model.Category) wikipedia.getPageById(category.intValue());
                categories = theCategory.getParentCategories();
                for (int i = 0; i < categories.length; i++) {
                    if (!nextCategories.contains(new Integer(categories[i].getId())))
                        nextCategories.add(categories[i].getId());
                }
            }

            newCategories = new HashSet<Integer>();
            for (Integer category : nextCategories) {
                if (!allCategories.contains(category)) {
                    newCategories.add(category);
                    allCategories.add(category);
                    //break;
                }
            }
            size = newCategories.size();
            if ((theDomains != null) && theDomains.size() > 0)
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
        if (isLoaded)
            return;
        // for each page id in wikipedia we get the list of domain id
        PageIterator iterator = wikipedia.getPageIterator(Page.PageType.article);
        int p = 0;
        int nbToAdd = 0;
        Txn<ByteBuffer> tx = environment.txnWrite();
        while (iterator.hasNext()) {
            if ((p % 10000) == 0)
                System.out.println(p);

            if (nbToAdd == 10000) {
                tx.commit();
                tx.close();
                nbToAdd = 0;
                tx = environment.txnWrite();
            }

            // add to the persistent map
            Page page = iterator.next();
            int pageId = page.getId();
            // conservative check 
            if (page instanceof Article) {
                int[] theDomains = createMapping((Article) page);
                if (theDomains != null) {
                    try {
                        final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());
                        keyBuffer.put(KBEnvironment.serialize(pageId)).flip();
                        final byte[] serializedValue = KBEnvironment.serialize(theDomains);
                        final ByteBuffer valBuffer = allocateDirect(serializedValue.length);
                        valBuffer.put(serializedValue).flip();
                        db.put(tx, keyBuffer, valBuffer);
                        nbToAdd++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //domainsCache.put(new Integer(pageId), theDomains);
                }
            }
            p++;
        }
        tx.commit();
        tx.close();
        iterator.close();

        isLoaded = true;
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
                com.scienceminer.nerd.kb.model.Category theCategory = wikipedia.getCategoryByTitle(category);
                if (theCategory == null)
                    LOGGER.warn(category + " is not a category found in Wikipedia.");
                else {
                    categoryId = theCategory.getId();
                    if (domains.get(categoryId) != null) {
                        LOGGER.warn(category + " is already defined in " + mappingFilePath);
                    }
                }
            }
            if (categoryId != -1) {
                List<Integer> dom = new ArrayList<Integer>();
                while (st.hasMoreTokens()) {
                    String domain = st.nextToken();
                    if (domain2id.get(domain) == null)
                        LOGGER.warn(domain + " is an invalid GRISP domain label in " + mappingFilePath);
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

    // standard LMDB retrieval
    public List<String> getDomains(int pageId) {
        int[] list = null;
        final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());

        ByteBuffer cachedData = null;
        try (Txn<ByteBuffer> tx = environment.txnRead()) {
            keyBuffer.put(KBEnvironment.serialize(pageId)).flip();

            cachedData = db.get(tx, keyBuffer);
            if (cachedData != null) {
                list = (int[]) KBEnvironment.deserialize(cachedData);
            }
        } catch (Exception e) {
            LOGGER.error("Cannot fetch data " + pageId, e);
        }


        List<String> result = new ArrayList<>();
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                String domain = id2domain.get(new Integer(list[i]));
                if (domain != null)
                    result.add(domain);
            }
        }
        return result;
    }

    public void close() {
        if (db != null)
            db.close();
        if (environment != null)
            environment.close();
    }

}