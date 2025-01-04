package com.scienceminer.nerd.kb.model;


import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KBStatistics {

    public static final String CONCEPTS ="Concepts";
    public static final String LABELS ="Labels";
    public static final String STATEMENTS ="Statements";

    private final Map<String, Long> upperKnowledgeBaseStatisticsCount = new HashMap<>();
    private final Map<String, List<Pair<String, Integer>>> lowerKnowledgeBaseStatisticsCount = new HashMap<>();

    public Map<String, Long> getUpperKnowledgeBaseStatisticsCount() {
        return upperKnowledgeBaseStatisticsCount;
    }

    public Map<String, List<Pair<String, Integer>>> getLowerKnowledgeBaseStatisticsCount() {
        return lowerKnowledgeBaseStatisticsCount;
    }
}
