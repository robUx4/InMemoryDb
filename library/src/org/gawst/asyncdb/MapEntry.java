package org.gawst.asyncdb;

import android.util.Pair;

/**
 * Basic Key/Value object.
 * @param <K> Type of the key.
 * @param <V> Type of the value.
 */
public class MapEntry<K, V> extends Pair<K,V> {

	public MapEntry(K key, V val) {
		super(key, val);
		if (null==key) throw new IllegalArgumentException();
	}

	public K getKey() {
		return first;
	}

	public V getValue() {
		return second;
	}
	
	public int hashCode() {
		return first.hashCode();
	}
	
	@Override
	public String toString() {
		return first.toString()+':'+(null==second ? null : second.toString());
	}
}