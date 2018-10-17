package com.scienceminer.nerd.disambiguation;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.scienceminer.nerd.kb.model.Article;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class RelatednessTest {
    private Relatedness relatedness;

    @Test
    public void whenCacheMissValueIsComputed(){
        CacheLoader<String, String> loader;
        loader = new CacheLoader<String, String>() {
            @Override
            public String load(String key) throws Exception {
                return key.toUpperCase();
            }
        };

        LoadingCache<String, String> cache;
        cache = CacheBuilder.newBuilder().build(loader);
        assertEquals(0, cache.size());
        assertEquals("TEST",cache.getUnchecked("test"));
        assertEquals(1,cache.size());
    }

    @Test
    public void whenCacheReachMaxSizeRemove(){
        CacheLoader<String, String> loader;
        loader = new CacheLoader<String, String>() {
            @Override
            public String load(String key) throws Exception {
                return key.toUpperCase();
            }
        };
        LoadingCache<String, String> cache;
        cache = CacheBuilder.newBuilder().maximumSize(3).build(loader);

        cache.getUnchecked("one");
        cache.getUnchecked("two");
        cache.getUnchecked("three");
        cache.getUnchecked("four");

        assertEquals(3, cache.size());

        assertEquals(null, cache.getIfPresent("one"));
        assertEquals("FOUR", cache.getIfPresent("four"));
    }

    @Test
    public void refreshWhenTimeLiveEnd(){
        CacheLoader<String, String> loader;
        loader = new CacheLoader<String, String>() {
            @Override
            public String load(String key) throws Exception {
                return key.toUpperCase();
            }
        };
        LoadingCache<String, String> cache;
        cache = CacheBuilder.newBuilder()
                .refreshAfterWrite(1, TimeUnit.MINUTES)
                .build(loader);
    }

}