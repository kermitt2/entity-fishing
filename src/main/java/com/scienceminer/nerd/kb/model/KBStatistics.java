package com.scienceminer.nerd.kb.model;


import java.util.HashMap;
import java.util.Map;

public class KBStatistics {

    public static final String CONCEPTS ="concepts";
    public static final String LABELS ="labels";
    public static final String STATEMENTS ="statements";

    private final Map<String, Long> upperKnowledgeBaseStatisticsCount = new HashMap<>();
    private final Map<String, Integer> lowerKnowledgeBaseStatisticsCount = new HashMap<>();

    public Map<String, Long> getUpperKnowledgeBaseStatisticsCount() {
        return upperKnowledgeBaseStatisticsCount;
    }

    public Map<String, Integer> getLowerKnowledgeBaseStatisticsCount() {
        return lowerKnowledgeBaseStatisticsCount;
    }
}
