package st.gaw.db;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class InMemoryDbMap<K, V, H extends Map<K, V>> extends AsynchronousDbHelper<Map.Entry<K,V>> {

	protected InMemoryDbMap(Context context, String name, int version, Logger logger) {
		super(context, name, version, logger);
	}

	/**
	 * return the object used to the in-memory elements
	 * @return
	 */
	abstract protected H getMap();

	protected void onDataCleared() {}

	@Override
	protected void addCursorInMemory(Cursor c) {
		final Map.Entry<K, V> entry = getEntryFromCursor(c);
		if (entry!=null)
			putEntry(entry);
	}

	protected abstract Map.Entry<K, V> getEntryFromCursor(Cursor c);

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

	protected void putEntry(Map.Entry<K, V> entry) {
		final H map = getMap();
		if (null==map) throw new NullPointerException();
		map.put(entry.getKey(), entry.getValue());
	}

	@Override
	protected final String getItemSelectClause(Entry<K, V> itemToSelect) {
		return getKeySelectClause(itemToSelect.getKey());
	}

	@Override
	protected final String[] getItemSelectArgs(Entry<K, V> itemToSelect) {
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
