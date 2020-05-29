package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.kb.model.Article;

import java.util.Objects;

public class ArticlePair {
    private final Article articleA, artticleB;
    private final Long key;

    public ArticlePair(Article a, Article b){
        articleA = a;
        artticleB = b;
        key = computeKey(a, b);
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }

    public static long computeKey(Article art1, Article art2) {
        //generate unique key for the pair of articles
        int min = Math.min(art1.getId(), art2.getId());
        int max = Math.max(art1.getId(), art2.getId());
        //long key = min + (max << 30);
//System.out.println(min + " / " + max);
        return (((long) min) << 32) | (max & 0xffffffffL);
    }


    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if (!(obj instanceof ArticlePair)){
            return false;
        }
        ArticlePair otherPair = (ArticlePair) obj;

        return getKey().equals(otherPair.getKey());
    }


    public Article getArtticleB() {
        return artticleB;
    }


    public Article getArticleA() {
        return articleA;
    }

    public Long getKey() {
        return key;
    }
}
