package com.scienceminer.nerd.kb.model;


import java.util.HashMap;
import java.util.Map;

public class KBStatistics {

    public static final String CONCEPTS ="Concepts";
    public static final String LABELS ="Labels";
    public static final String STATEMENTS ="Statements";
    public static final String ARTICLES = "Articles";
    public static final String PAGES = "Pages";

    private final Map<String, Long> upperKnowledgeBaseStatisticsCount = new HashMap<>();
    private final Map<String, Map<String, Integer>> lowerKnowledgeBaseStatisticsCount = new HashMap<>();

    public Map<String, Long> getUpperKnowledgeBaseStatisticsCount() {
        return upperKnowledgeBaseStatisticsCount;
    }

    public Map<String, Map<String, Integer>> getLowerKnowledgeBaseStatisticsCount() {
        return lowerKnowledgeBaseStatisticsCount;
    }
}
