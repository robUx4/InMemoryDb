package org.gawst.asyncdb;

import android.content.ContentValues;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class InMemoryLruCache<K,V, INSERT_ID> extends AsynchronousDbHelper<MapEntry<K,V>, INSERT_ID> {

	/**
	 * the cache where the data are stored, locked when writing on it
	 */
	private LruCache<K,V> mData;

	/**
	 * Field to tell when the data are being reloaded from the DB, between {@link #startLoadingInMemory()} and {@link #finishLoadingInMemory()}
	 */
	private boolean mIsLoading;

	/**
	 * ReentrantLock used to protect {@link #mData} when reading/writing/iterating it
	 */
	protected ReentrantLock mDataLock;

	/**
	 * Condition to block the {@link #mData} access before the data are loaded
	 */
	private Condition dataLoaded;

	private final boolean constructorPassed;

	protected final boolean DEBUG_LOCK = false;

	/**
	 * @param db The already created {@link android.database.sqlite.SQLiteOpenHelper} to use as storage
	 * @param name Database name for logs
	 * @param maxSize for caches that do not override {@link #sizeOf}, this is the maximum number of entries in the cache. For all other caches, this is the maximum sum of the sizes of the entries in this cache
	 * @param logger The {@link org.gawst.asyncdb.Logger} to use for all logs (can be null for the default Android logs)
	 */
	protected InMemoryLruCache(MapDataSource<K, V, INSERT_ID> db, String name, final int maxSize, Logger logger) {
		super(db, name, logger, maxSize);
		this.constructorPassed = true;
	}
	
	@Override
	protected void preloadInit(Object cookie) {
		mDataLock = new ReentrantLock();
		dataLoaded = mDataLock.newCondition();
		super.preloadInit(cookie);
		mData = new LruCache<K, V>((Integer) cookie) {
			@Override
			protected int sizeOf(K key, V value) {
				return InMemoryLruCache.this.sizeOf(key, value);
			}

			@Override
			protected void entryRemoved(boolean evicted, K key, V oldValue, V newValue) {
				InMemoryLruCache.this.entryRemoved(evicted, key, oldValue, newValue);
			}
		};
	}

	@Override
	public void addItemInMemory(MapEntry<K, V> entry) {
		if (entry!=null)
			putEntry(entry);
	}

	protected LruCache<K, V> getLruCache() {
		if (!mDataLock.isHeldByCurrentThread()) throw new IllegalStateException("we need a lock on mDataLock to access mData in "+this);
		boolean waited = false;
		if (!isDataLoaded() && !mIsLoading)
			try {
				waited = true;
				// we're trying to read the data but they are not loading yet
				LogManager.logger.v(STARTUP_TAG, "waiting data loaded in "+this);
				long now = System.currentTimeMillis();
				dataLoaded.await();
				LogManager.logger.v(STARTUP_TAG, "waiting data loaded in "+this+" finished after "+(System.currentTimeMillis()-now));
				//Thread.sleep(1000);
			} catch (InterruptedException ignored) {
			}
		if (null==mData) throw new NullPointerException("no HashMap, waited:"+waited+" mIsLoading:"+mIsLoading+" constructorPassed:"+constructorPassed);
		return mData;
	}


	protected void putEntry(MapEntry<K, V> entry) {
		final LruCache<K, V> map = getLruCache();
		map.put(entry.first, entry.second);
	}

	protected abstract ContentValues getValuesFromData(K key, V value) throws RuntimeException;

	@Override
	protected final ContentValues getValuesFromData(MapEntry<K, V> data) throws RuntimeException {
		return getValuesFromData(data.getKey(), data.getValue());
	}

	/**
	 * Returns the size of the entry for {@code key} and {@code value} in
	 * user-defined units.  The default implementation returns 1 so that size
	 * is the number of entries and max size is the maximum number of entries.
	 *
	 * <p>An entry's size must not change while it is in the cache.
	 */
	protected int sizeOf(K key, V value) {
		return 1;
	}

	/**
	 * Called for entries that have been evicted or removed. This method is
	 * invoked when a value is evicted to make space, removed by a call to
	 * {@link #remove}, or replaced by a call to {@link #put}. The default
	 * implementation does nothing.
	 *
	 * <p>The method is called without synchronization: other threads may
	 * access the cache while this method is executing.
	 *
	 * @param evicted true if the entry is being removed to make space, false
	 *     if the removal was caused by a {@link #put} or {@link #remove}.
	 * @param newValue the new value for {@code key}, if it exists. If non-null,
	 *     this removal was caused by a {@link #put}. Otherwise it was caused by
	 *     an eviction or a {@link #remove}.
	 */
	protected void entryRemoved(boolean evicted, K key, V oldValue, V newValue) {
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock entryRemoved");
		mDataLock.lock();
		try {
			if (null!=newValue)
				scheduleUpdateOperation(new MapEntry<K,V>(key, newValue));
			else
				scheduleRemoveOperation(new MapEntry<K,V>(key, oldValue));
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock entryRemoved");
			mDataLock.unlock();
		}
	}

	@Override
	protected void startLoadingInMemory() {
		mDataLock.lock();
		mData.evictAll();
		mIsLoading = true;
		super.startLoadingInMemory();
	}

	@Override
	protected void finishLoadingInMemory() {
		super.finishLoadingInMemory();
		mIsLoading = false;
		dataLoaded.signalAll();
		mDataLock.unlock();
	}

	/**
	 * Returns the value for {@code key} if it exists in the cache or can be
	 * created by {@link #create}. If a value was returned, it is moved to the
	 * head of the queue. This returns null if a value is not cached and cannot
	 * be created.
	 */
	public V get(K key) {
		// protect the data coherence
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock get");
		mDataLock.lock();
		try {
			return getLruCache().get(key);
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock get");
			mDataLock.unlock();
		}
	}

	/**
	 * Caches {@code value} for {@code key}. The value is moved to the head of
	 * the queue.
	 *
	 * @return the previous value mapped by {@code key}.
	 */
	public V put(K key, V value) {
		// protect the data coherence
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock put");
		mDataLock.lock();
		try {
			V oldValue = getLruCache().put(key, value);
			if (oldValue==null)
				scheduleAddOperation(new MapEntry<K,V>(key, value));
			else
				scheduleUpdateOperation(new MapEntry<K,V>(key, value)); // TODO: is this called twice ?
			return oldValue;
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock put");
			mDataLock.unlock();
		}
	}

	/**
	 * Remove the eldest entries until the total of remaining entries is at or
	 * below the requested size.
	 *
	 * @param maxSize the maximum size of the cache before returning. May be -1
	 *            to evict even 0-sized elements.
	 */
	public void trimToSize(int maxSize) {
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock trimToSize");
		mDataLock.lock();
		try {
			getLruCache().trimToSize(maxSize);
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock trimToSize");
			mDataLock.unlock();
		}
	}

	/**
	 * Removes the entry for {@code key} if it exists.
	 *
	 * @return the previous value mapped by {@code key}.
	 */
	public final V remove(K key) {
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock remove");
		mDataLock.lock();
		try {
			return getLruCache().remove(key);
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock remove");
			mDataLock.unlock();
		}
	}

	/**
	 * Called after a cache miss to compute a value for the corresponding key.
	 * Returns the computed value or null if no value can be computed. The
	 * default implementation returns null.
	 *
	 * <p>The method is called without synchronization: other threads may
	 * access the cache while this method is executing.
	 *
	 * <p>If a value for {@code key} exists in the cache when this method
	 * returns, the created value will be released with {@link #entryRemoved}
	 * and discarded. This can occur when multiple threads request the same key
	 * at the same time (causing multiple values to be created), or when one
	 * thread calls {@link #put} while another is creating a value for the same
	 * key.
	 */
	protected V create(K key) {
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock create");
		mDataLock.lock();
		try {
			return null;
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock create");
			mDataLock.unlock();
		}
	}

	/**
	 * Clear the cache, calling {@link #entryRemoved} on each removed entry.
	 */
	public final void evictAll() {
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock evictAll");
		mDataLock.lock();
		try {
			getLruCache().evictAll();
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock evictAll");
			mDataLock.unlock();
		}
	}

	/**
	 * For caches that do not override {@link #sizeOf}, this returns the number
	 * of entries in the cache. For all other caches, this returns the sum of
	 * the sizes of the entries in this cache.
	 */
	public synchronized final int size() {
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock size");
		mDataLock.lock();
		try {
			return getLruCache().size();
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock size");
			mDataLock.unlock();
		}
	}

	/**
	 * For caches that do not override {@link #sizeOf}, this returns the maximum
	 * number of entries in the cache. For all other caches, this returns the
	 * maximum sum of the sizes of the entries in this cache.
	 */
	public synchronized final int maxSize() {
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock maxSize");
		mDataLock.lock();
		try {
			return getLruCache().maxSize();
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock maxSize");
			mDataLock.unlock();
		}
	}

}
