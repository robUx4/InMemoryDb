package org.gawst.asyncdb;

import android.content.ContentValues;
import android.support.annotation.Nullable;

import java.util.Map;
import java.util.Set;

public abstract class InMemoryDbMap<K, V, H extends Map<K, V>, INSERT_ID> extends AsynchronousDbHelper<MapEntry<K,V>, INSERT_ID> {

	/**
	 * @param db The already created {@link android.database.sqlite.SQLiteOpenHelper} to use as storage
	 * @param name Database name for logs
	 * @param logger The {@link org.gawst.asyncdb.Logger} to use for all logs (can be null for the default Android logs)
	 * @param initCookie Cookie to pass to {@link AsynchronousDbHelper#preloadInit(Object)}
	 */
	protected InMemoryDbMap(MapDataSource<K, V, INSERT_ID> db, String name, Logger logger, Object initCookie) {
		super(db, name, logger, initCookie);
	}

	/**
	 * return the object used to the in-memory elements
	 * @return
	 */
	abstract protected H getMap();

	protected void onDataCleared() {}

	@Override
	public final void addItemInMemory(MapEntry<K, V> entry) {
		putEntry(entry.getKey(), entry.getValue());
	}

	/**
	 * Transform the element in memory into {@link android.content.ContentValues} that can be saved in the database.
	 * <p> you can return null and fill the database yourself if you need to.
	 * @param key Map key value of the element to insert/update.
	 * @param value Map value of the element to insert/update.
	 * @param update {@code true} if the values are for an update, not an insert, you can omit keys there.
	 * @return a ContentValues element with all data that can be used to restore the data later from the database.
	 * @throws RuntimeException
	 */
	@Nullable
	protected abstract ContentValues getValuesFromData(K key, V value, boolean update) throws RuntimeException;

	@Override
	protected final ContentValues getValuesFromData(MapEntry<K, V> data, boolean update) throws RuntimeException {
		return getValuesFromData(data.getKey(), data.getValue(), update);
	}

	protected void putEntry(K key, V value) {
		final H map = getMap();
		map.put(key, value);
	}

	@Override
	protected void clearDataInMemory() {
		getMap().clear();
		super.clearDataInMemory();
		onDataCleared();
	}

	public V remove(K key) {
		V result = getMap().remove(key);
		//if (result!=null)
			scheduleRemoveOperation(new MapEntry<K,V>(key,result));
		return result;
	}

	public V put(K key, V value) {
		V result = getMap().put(key, value);
		if (result==null)
			scheduleAddOperation(new MapEntry<K,V>(key, value));
		else
			scheduleUpdateOperation(new MapEntry<K,V>(key, value));
		return result;
	}

	public V get(K key) {
		return getMap().get(key);
	}

	public boolean containsKey(K key) {
		return getMap().containsKey(key);
	}
	
	public K getStoredKey(K key) {
		if (DEBUG_DB) LogManager.logger.d("colors", "looking for "+key);
		Set<K> keys = getMap().keySet();
		for (K k : keys ){
			if (DEBUG_DB) LogManager.logger.d("colors", " testing key "+k);
			if (k.equals(key)) {
				if (DEBUG_DB) LogManager.logger.d("colors", " using "+k);
				return k;
			}
		}
		return null;
	}

	public void notifyItemChanged(K key) {
		V value = getMap().get(key);
		if (value!=null)
			scheduleUpdateOperation(new MapEntry<K,V>(key, value));
	}

	public int size() {
		return getMap().size();
	}
}
