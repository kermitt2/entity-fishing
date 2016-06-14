package org.wikipedia.miner.db;

/**
 * An entry (key,value pair) from a WDatabase
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class WEntry<K,V> {
	
	private K key ;
	private V value ;
	
	/**
	 * Creates a new WEntry, with the given key and value
	 * 
	 * @param k the key
	 * @param v the value
	 */
	public WEntry(K k, V v) {
		this.key = k ;
		this.value = v ;
	}
	
	/**
	 * Returns the key
	 * 
	 * @return the key
	 */
	public K getKey() {
		return key;
	}

	/**
	 * Returns the value
	 * 
	 * @return the value
	 */
	public V getValue() {
		return value;
	}
}