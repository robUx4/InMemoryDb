package st.gaw.db;

import java.util.Map;

public class MapEntry<K, V> implements Map.Entry<K, V> {

	private final K mKey;
	private V mVal;

	public MapEntry(K key, V val) {
		if (null==key) throw new NullPointerException();
		mKey = key;
		mVal = val;
	}

	@Override
	public K getKey() {
		return mKey;
	}

	@Override
	public V getValue() {
		return mVal;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		if (!(o instanceof MapEntry)) return false;
		return mKey.equals(((MapEntry<?, ?>) o).mKey);
	}
	
	@Override
	public int hashCode() {
		return mKey.hashCode();
	}

	@Override
	public V setValue(V newVal) {
		mVal = newVal;
		return mVal;
	}
	
	@Override
	public String toString() {
		return mKey.toString()+':'+(null==mVal ? null : mVal.toString());
	}
}