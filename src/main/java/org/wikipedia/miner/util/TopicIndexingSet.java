package org.wikipedia.miner.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.wikipedia.miner.annotation.preprocessing.PreprocessedDocument;
import org.wikipedia.miner.model.Article;

public class TopicIndexingSet extends ArrayList<TopicIndexingSet.Item> {
	
	public TopicIndexingSet() {
		super();
	}
	
	public void add(PreprocessedDocument doc, HashSet<Integer> topicIds) {
		add(new Item(doc, topicIds));
	}
		
	public TopicIndexingSet getRandomSubset(int size) {
		
		if (size > size())
			throw new IllegalArgumentException("requested size " + size + " is larger than " + size());
		
		Random r = new Random();
		HashSet<Integer> usedIndexes = new HashSet<Integer>();
		
		TopicIndexingSet subset = new TopicIndexingSet();
		while (subset.size() < size) {
			
			int index = r.nextInt(size());
			
			if (usedIndexes.contains(index))
				continue;
			
			Item i = get(index);
			subset.add(i);
			usedIndexes.add(index);
		}
		
		return subset;
	}
	
	public class Item {
		
		PreprocessedDocument _doc;
		HashSet<Integer> _topicIds;
		
		public Item(PreprocessedDocument doc) {
			_doc = doc;
			_topicIds = new HashSet<Integer>();
		}
		
		public Item(PreprocessedDocument doc, HashSet<Integer> topicIds) {
			_doc = doc;
			_topicIds = topicIds;
		}
		
		public PreprocessedDocument getDocument() {
			return _doc;
		}

		public void addTopic(Article art) {
			_topicIds.add(art.getId());
		}
		
		public void addTopic(int id) {
			_topicIds.add(id);
		}
		
		public boolean isTopic(Article art) {
			return _topicIds.contains(art.getId());
		}
		
		public boolean isTopic(int id) {
			return _topicIds.contains(id);
		}
		
		public Set<Integer> getTopicIds() {
			return _topicIds;
		}
		
	}
}
