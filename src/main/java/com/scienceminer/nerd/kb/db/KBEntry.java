package com.scienceminer.nerd.kb.db;

/**
 * A key/value pair to be used for an LMDB database implementation
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class KBEntry<K,V> {
	private K key = null;
	private V value = null;
	
	public KBEntry(K k, V v) {
		this.key = k;
		this.value = v;
	}
	
	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}
}