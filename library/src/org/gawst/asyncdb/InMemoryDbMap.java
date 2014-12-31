package org.gawst.asyncdb;

import java.util.Map;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public abstract class InMemoryDbMap<K, V, H extends Map<K, V>> extends AsynchronousDbHelper<MapEntry<K,V>> {

	/**
	 * @param db The already created {@link android.database.sqlite.SQLiteOpenHelper} to use as storage
	 * @param context Used to open or create the database
	 * @param name Database filename on disk
	 * @param logger The {@link Logger} to use for all logs (can be null for the default Android logs)
	 * @param initCookie Cookie to pass to {@link #preloadInit(Object, Logger)}
	 */
	protected InMemoryDbMap(MapDataSource<K,V> db, Context context, String name, Logger logger, Object initCookie) {
		super(db, context, name, logger, initCookie);
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

	protected abstract ContentValues getValuesFromData(K key, V value) throws RuntimeException;

	@Override
	protected final ContentValues getValuesFromData(MapEntry<K, V> data) throws RuntimeException {
		return getValuesFromData(data.getKey(), data.getValue());
	}

	/**
	 * the where clause that should be used to update/delete the item
	 * <p> see {@link #getKeySelectArgs(Object)}
	 * @param itemKey the key of the item about to be selected in the database
	 * @return a string for the whereClause in {@link SQLiteDatabase#update(String, ContentValues, String, String[])} or {@link SQLiteDatabase#delete(String, String, String[])}
	 */
	protected abstract String getKeySelectClause(K itemKey);
	/**
	 * the where arguments that should be used to update/delete the item
	 * <p> see {@link #getKeySelectClause(Object)}
	 * @param itemKey the key of the  item about to be selected in the database
	 * @return a string array for the whereArgs in {@link SQLiteDatabase#update(String, ContentValues, String, String[])} or {@link SQLiteDatabase#delete(String, String, String[])}
	 */
	protected abstract String[] getKeySelectArgs(K itemKey);

	protected void putEntry(K key, V value) {
		final H map = getMap();
		map.put(key, value);
	}

	@Override
	protected final String getItemSelectClause(MapEntry<K, V> itemToSelect) {
		return getKeySelectClause(itemToSelect.getKey());
	}

	@Override
	protected final String[] getItemSelectArgs(MapEntry<K, V> itemToSelect) {
		return getKeySelectArgs(itemToSelect.getKey());
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
