package org.gawst.asyncdb;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class InMemoryHashmapDb<K, V, INSERT_ID> extends InMemoryDbMap<K, V, HashMap<K,V>, INSERT_ID> {

	/**
	 * the array where the data are stored, locked when writing on it
	 */
	private HashMap<K,V> mData;

	/**
	 * Field to tell when the data are being reloaded from the DB, between {@link #startLoadingInMemory()} and {@link #finishLoadingInMemory()}
	 */
	private boolean mIsLoading;

	/**
	 * ReentrantLock used to protect {@link #mData} when reading/writing/iterating it
	 */
	protected ReentrantLock mDataLock;
	
	protected final boolean DEBUG_LOCK = false;

/*
	private static class FakeLock extends ReentrantLock {
		@Override
		public void lock() {}
		
		@Override
		public void unlock() {}
		
		@Override
		public boolean isHeldByCurrentThread() {
			return true;
		}
	}
*/
	/**
	 * Condition to block the {@link #mData} access before the data are loaded
	 */
	private Condition dataLoaded;
	
	private final boolean constructorPassed;

	/**
	 * @param db The already created {@link android.database.sqlite.SQLiteOpenHelper} to use as storage
	 * @param name Database name for logs
	 * @param logger The {@link Logger} to use for all logs (can be null for the default Android logs)
	 */
	protected InMemoryHashmapDb(MapDataSource<K, V, INSERT_ID> db, String name, Logger logger) {
		this(db, name, logger, null);
	}

	/**
	 * @param db
	 * @param name Database name for logs
	 * @param logger The {@link org.gawst.asyncdb.Logger} to use for all logs (can be null for the default Android logs)
	 * @param initCookie Cookie to pass to {@link AsynchronousDbHelper#preloadInit(Object)}
	 */
	protected InMemoryHashmapDb(MapDataSource<K, V, INSERT_ID> db, String name, Logger logger, Object initCookie) {
		super(db, name, logger, initCookie);
		this.constructorPassed = true;
	}

	@Override
	protected void preloadInit(Object cookie) {
		mDataLock = new ReentrantLock();
		dataLoaded = mDataLock.newCondition();
		super.preloadInit(cookie);
		mData = new HashMap<K,V>();
	}

	@Override
	protected HashMap<K, V> getMap() {
		if (!mDataLock.isHeldByCurrentThread()) throw new IllegalStateException("we need a lock on mDataLock to access mData in "+this);
		boolean waited = false;
		if (!isDataLoaded() && !mIsLoading)
			try {
				waited = true;
				// we're trying to read the data but they are not loading yet
				LogManager.logger.v(STARTUP_TAG, "waiting data loaded in "+this);
				long now = System.currentTimeMillis();
				dataLoaded.await(10, TimeUnit.SECONDS);
				LogManager.logger.v(STARTUP_TAG, "waiting data loaded in "+this+" finished after "+(System.currentTimeMillis()-now));
				//Thread.sleep(1000);
			} catch (InterruptedException ignored) {
				LogManager.logger.w(STARTUP_TAG, "timed out waiting for data loaded in "+this);
			}
		if (null==mData) throw new NullPointerException("no HashMap, waited:"+waited+" mIsLoading:"+mIsLoading+" constructorPassed:"+constructorPassed);
		return mData;
	}

	@Override
	protected void startLoadingInMemory() {
		mDataLock.lock();
		mData.clear();
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

	@Override
	protected void clearDataInMemory() {
		// protect the data coherence
		mDataLock.lock();
		try {
			super.clearDataInMemory();
		} finally {
			mDataLock.unlock();
		}
	}

	@Override
	public boolean containsKey(K key) {
		// protect the data coherence
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock containsKey");
		mDataLock.lock();
		try {
			return super.containsKey(key);
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock containsKey");
			mDataLock.unlock();
		}
	}

	@Override
	public K getStoredKey(K key) {
		// protect the data coherence
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock getStoredKey");
		mDataLock.lock();
		try {
			return super.getStoredKey(key);
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock getStoredKey");
			mDataLock.unlock();
		}
	}
	
	@Override
	public V remove(K key) {
		// protect the data coherence
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock remove");
		mDataLock.lock();
		try {
			return super.remove(key);
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock remove");
			mDataLock.unlock();
		}
	}

	@Override
	public V put(K key, V value) {
		// protect the data coherence
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock put");
		mDataLock.lock();
		try {
			return super.put(key, value);
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock put");
			mDataLock.unlock();
		}
	}

	@Override
	public V get(K key) {
		// protect the data coherence
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock get");
		mDataLock.lock();
		try {
			return super.get(key);
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock get");
			mDataLock.unlock();
		}
	}

	@Override
	public int size() {
		// protect the data coherence
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock size");
		mDataLock.lock();
		try {
			return null==mData ? 0 : mData.size();
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock size");
			mDataLock.unlock();
		}
	}

	@Override
	public void waitForDataLoaded() {
		if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" lock waitForDataLoaded");
		mDataLock.lock();
		try {
			getMap();
		    super.waitForDataLoaded();
		} finally {
			if (DEBUG_LOCK) LogManager.getLogger().i(TAG, this+" unlock waitForDataLoaded");
			mDataLock.unlock();
		}
	}
}
