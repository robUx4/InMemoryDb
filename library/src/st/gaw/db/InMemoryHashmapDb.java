package st.gaw.db;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;

public abstract class InMemoryHashmapDb<K, V> extends InMemoryDbMap<K, V, HashMap<K,V>> {

	/**
	 * the array where the data are stored, locked when writing on it
	 */
	private HashMap<K,V> mData;

	/**
	 * ReentrantLock used to protect {@link #mData} when reading/writing/iterating it
	 */
	protected ReentrantLock mDataLock;

	protected InMemoryHashmapDb(Context context, String name, int version, Logger logger) {
		super(context, name, version, logger);
	}

	@Override
	protected void preloadInit() {
		mDataLock = new ReentrantLock();
		super.preloadInit();
	}

	@Override
	protected HashMap<K, V> getMap() {
		if (!mDataLock.isHeldByCurrentThread()) throw new IllegalStateException("we need a lock on mDataLock to access mData in "+this);
		if (mData==null)
			mData = new HashMap<K,V>(0);
		return mData;
	}

	@Override
	protected void startLoadingInMemory() {
		mDataLock.lock();
		super.startLoadingInMemory();
	}

	@Override
	protected void finishLoadingInMemory() {
		super.finishLoadingInMemory();
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
		mDataLock.lock();
		try {
			return super.containsKey(key);
		} finally {
			mDataLock.unlock();
		}
	};

	@Override
	public V remove(K key) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.remove(key);
		} finally {
			mDataLock.unlock();
		}
	};

	@Override
	public V put(K key, V value) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.put(key, value);
		} finally {
			mDataLock.unlock();
		}
	};

	@Override
	public V get(K key) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.get(key);
		} finally {
			mDataLock.unlock();
		}
	};

	@Override
	public int size() {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.size();
		} finally {
			mDataLock.unlock();
		}
	}
}
