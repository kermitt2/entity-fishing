package org.wikipedia.miner.db;

/**
 * An entry (key,value pair) to be used for an LMDB database implementation
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class WEntry<K,V> {
	
	private K key ;
	private V value ;
	
	public WEntry(K k, V v) {
		this.key = k ;
		this.value = v ;
	}
	
	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}
}