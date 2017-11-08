package com.scienceminer.nerd.kb;

import com.scienceminer.nerd.exceptions.NerdException;

import org.grobid.core.utilities.OffsetPosition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

/**
 * Common representation of the domains used in a KB.
 * 
 * 
 */
public class Domains {
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(Domains.class);
	
	// the list of domains of the KB
	public List<String> domains = null;
	
	// map the domain strings to DB DomainID
	Map<String, Integer> domainIDs = null; 
	
	// map a domain string to its list of subdomains
	Map<String, List<String>> domainsTree = null; 
	
	// IPC mappings 
	public Map<String, List<String> > ipc2domains = null; 
	public Map<String, List<String> > domain2ipc = null; 
	
	// Summon displines
	public Map<String, List<String>> domain2summon = null;
	public Map<String, List<String>> summon2domains = null;
	
	private static volatile Domains instance;
	
	public static Domains getInstance() throws Exception {
        if (instance == null) {
            //double check idiom
            // synchronized (instanceController) {
                if (instance == null)
					getNewInstance();
            // }
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
	private static synchronized void getNewInstance() throws Exception {
		LOGGER.debug("Get new instance of Domains");		
		instance = new Domains();
	}
	
	/**
     * Hidden constructor
     */
	private Domains() throws Exception {
		// load domains hierarchy
		// we build the hierarchical mapping
		domainsTree = new Hashtable();  
		String grispTree = "src/main/resources/kb/domains.all2.txt";
		InputStreamReader reader = null;
		BufferedReader bufReader = null;
		try {
			reader = new InputStreamReader(new FileInputStream(grispTree), "UTF-8");
			bufReader = new BufferedReader(reader);
			String line = null;
			while ( (line = bufReader.readLine()) != null ) {
				String head = null;
				ArrayList<String> children = null;
				StringTokenizer st = new StringTokenizer(line, " ");
				if (st.hasMoreTokens()) {
					head = st.nextToken().toLowerCase();
					children = new ArrayList<String>();
					while(st.hasMoreTokens()) {
						children.add(st.nextToken().toLowerCase());
					}
				}	
				domainsTree.put(head, children);
			}
		}
		catch(IOException e) {
			LOGGER.error("Problem accessing domain file resources: " + grispTree);
		}	
		finally {
			if (bufReader != null)
				bufReader.close();
			if (reader != null)		
				reader.close();			
		}
		
		// load IPC mapping
		ipc2domains = new HashMap<String,List<String>>();
		domain2ipc = new HashMap<String,List<String>>();
		
		String domainIPCPath = "src/main/resources/kb/ipc.txt";
		reader = null;
		bufReader = null;
		try {
			reader = new InputStreamReader(new FileInputStream(domainIPCPath), "UTF-8");
			bufReader = new BufferedReader(reader);
			String line = null;
			int i = 0;
			while ( (line = bufReader.readLine()) != null ) {
				if (line.length() == 0) continue;
				StringTokenizer st = new StringTokenizer(line, "\t");
			
				String ipc = st.nextToken();
				int index = ipc.indexOf(' ',1);
				ipc = ipc.substring(1, index);
				//System.out.println(ipc);
				List<String> doms = new ArrayList<String>();
				while(st.hasMoreTokens()) {
					String dom = st.nextToken().toLowerCase();
					//System.out.println("\t" + dom);
					if (!doms.contains(dom))
						doms.add(dom);
					List<String> theIPCs = domain2ipc.get(dom);	
					if (theIPCs== null) {
						theIPCs = new ArrayList<String>();
						theIPCs.add(ipc);
						domain2ipc.put(dom, theIPCs);
					}	
					else {
						if (!theIPCs.contains(ipc)) {
							theIPCs.add(ipc);
							domain2ipc.put(dom, theIPCs);
						}
					}
				}
			
				ipc2domains.put(ipc, doms);
			}
		}
		catch(IOException e) {
			LOGGER.error("Problem accessing ipc/domain mapping file resources: " + domainIPCPath);
		}
		finally {
			if (bufReader != null)
				bufReader.close();
			if (reader != null)	
				reader.close();			
		}
		
		// load Summon Discipline mapping
		summon2domains = new HashMap<String,List<String>>();
		domain2summon = new HashMap<String,List<String>>();
		
		String domainSummonPath = "src/main/resources/kb/SummonDiscipines.txt";
		reader = null;
		bufReader = null;
		try {
			reader = new InputStreamReader(new FileInputStream(domainSummonPath), "UTF-8");
			bufReader = new BufferedReader(reader);
			String line = null;
			int i = 0;
			while ( (line = bufReader.readLine()) != null ) {
				if (line.length() == 0) continue;
				StringTokenizer st = new StringTokenizer(line, "\t");
				
				String discipline = st.nextToken();
				int index = discipline.indexOf(' ',1);
				discipline = discipline.substring(1, index);
				//System.out.println(discipline);
				List<String> doms = new ArrayList<String>();
				while(st.hasMoreTokens()) {
					String dom = st.nextToken().toLowerCase();
					//System.out.println("\t" + dom);
					if (!doms.contains(dom))
						doms.add(dom);
					List<String> theDisciplines = domain2summon.get(dom);	
					if (theDisciplines == null) {
						theDisciplines = new ArrayList<String>();
						theDisciplines.add(discipline);
						domain2summon.put(dom, theDisciplines);
					}	
					else {
						if (!theDisciplines.contains(discipline)) {
							theDisciplines.add(discipline);
							domain2ipc.put(dom, theDisciplines);
						}
					}
				}
				
				summon2domains.put(discipline, doms);
			}
		}
		catch(IOException e) {
			LOGGER.error("Problem accessing ipc/domain mapping file resources: " + domainIPCPath);
		}
		finally {
			if (bufReader != null)
				bufReader.close();
			if (reader != null)	
				reader.close();			
		}
    }

	public List<String> getIPC2Domains(String ipc) {
		return ipc2domains.get(ipc);
	}
	
	public List<String> getDomain2IPC (String domain) {
		return domain2ipc.get(domain);
	}
	
	public List<String> allDomainSubfields(String dom) {
		return domainsTree.get(dom);
	}

	public List<String> mapIPCs(List<String> IPCs) {
		List<String> result = new ArrayList<String>();
		for(String ipc : IPCs) {
			List<String> theDomains = ipc2domains.get(ipc);
			for(String dom : theDomains) {
				if (!result.contains(dom)) {
					result.add(dom);
				}
			}
		}
		return result;
	}

	/** 
	 *  Expand a list of domains by the children subdomains, 
	 *  according to the domain hierarchy.
	 */
	public List<String> expandDomains(List<String> domains) {
		if ( (domains == null) || (domains.size() == 0) ) {
			return null;
		}
		List<String> result = new ArrayList<String>();
		for(String dom : domains) {
			result.add(dom);
			List<String> subDomains = allDomainSubfields(dom);
			if (subDomains == null) {
				continue;
			}
			System.out.println(dom + " => " + subDomains.toString());
			for(String dom2 : subDomains) {
				if (!result.contains(dom2)) {
					result.add(dom2);
				}
			}
		}
		return result;
	}

    public String toString() {
        StringBuffer buffer = new StringBuffer();
		
        return buffer.toString();
    }
}