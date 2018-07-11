package com.scienceminer.nerd.kb;

/* main class for building the Nerd-Kid LMDB databases containing 1. Wikidata Ids and 2. Their classes predicted by Nerd-Kid
    For example:
        Wikidata Id: Q91; predicted class: PERSON
* */

public class NerdKidDataBuilder {
    public static void main(String[] args) throws Exception {
        UpperKnowledgeBase upperKnowledgeBase = UpperKnowledgeBase.getInstance();
        upperKnowledgeBase.close();
    }
}
