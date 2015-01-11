package org.gawst.asyncdb;

import org.gawst.asyncdb.adapter.InMemoryFilteredAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * a basic helper class to keep the content of a flat database in an {@link ArrayList}
 * <p>
 * you should use the {@link #mDataLock} when iterating through the {@link ArrayList}
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory by the {@link InMemoryDbArrayList}
 */
public abstract class InMemoryDbArrayList<E, INSERT_ID> extends InMemoryDbList<E, ArrayList<E>, INSERT_ID> implements InMemoryFilteredAdapter.InMemoryFilter.InMemoryFilterable<E> {

	/**
	 * the array where the data are stored, locked when writing on it
	 */
	private ArrayList<E> mData;

	/**
	 * Field to tell when the data are being reloaded from the DB
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

	/**
	 * @param db The already created {@link android.database.sqlite.SQLiteOpenHelper} to use as storage
	 * @param name Database name for logs
	 * @param logger The {@link Logger} to use for all logs (can be null for the default Android logs)
	 */
	protected InMemoryDbArrayList(DataSource<E, INSERT_ID> db, String name, Logger logger) {
		super(db, name, logger, null);
	}

	/**
	 * @param db The already created {@link android.database.sqlite.SQLiteOpenHelper} to use as storage
	 * @param name Database name for logs
	 * @param logger The {@link Logger} to use for all logs (can be null for the default Android logs)
	 * @param initCookie Cookie to pass to {@link AsynchronousDbHelper#preloadInit(Object)}
	 */
	protected InMemoryDbArrayList(DataSource<E, INSERT_ID> db, String name, Logger logger, Object initCookie) {
		super(db, name, logger, initCookie);
	}
	
	@Override
	protected void preloadInit(Object cookie) {
		mDataLock = new ReentrantLock();
		dataLoaded = mDataLock.newCondition();
		super.preloadInit(cookie);
		mData = new ArrayList<E>();
	}

	@Override
	protected ArrayList<E> getList() {
		if (!mDataLock.isHeldByCurrentThread()) throw new IllegalStateException("we need a lock on mDataLock to access mData in "+this);
		if (!isDataLoaded() && !mIsLoading)
			try {
				// we're trying to read the data but they are not loading yet
				LogManager.logger.v(STARTUP_TAG, "waiting data loaded in "+this);
				long now = System.currentTimeMillis();
				dataLoaded.await(10, TimeUnit.SECONDS);
				LogManager.logger.v(STARTUP_TAG, "waiting data loaded in "+this+" finished after "+(System.currentTimeMillis()-now));
			} catch (InterruptedException ignored) {
				LogManager.logger.w(STARTUP_TAG, "timed out waiting for data loaded in "+this);
			}
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
	public void startLoadingAllItems(int elementCount) {
		super.startLoadingAllItems(elementCount);
		getList().ensureCapacity(elementCount);
	}

	public boolean add(E item) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.add(item);
		} finally {
			mDataLock.unlock();
		}
	}

	public boolean addAll(Collection<? extends E> items) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.addAll(items);
		} finally {
			mDataLock.unlock();
		}
	}

	public boolean remove(E item) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.remove(item);
		} finally {
			mDataLock.unlock();
		}
	}

	@Override
	public boolean remove(int index) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.remove(index);
		} finally {
			mDataLock.unlock();
		}
	}

	public void notifyItemChanged(E item) {
		// protect the data coherence
		mDataLock.lock();
		try {
			super.notifyItemChanged(item);
		} finally {
			mDataLock.unlock();
		}
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
	
	public int getCount() {
		mDataLock.lock();
		try {
			return null==mData ? 0 : mData.size();
		} finally {
			mDataLock.unlock();
		}
	}

	@Override
	public E get(int position) {
		mDataLock.lock();
		try {
			return super.get(position);
		} finally {
			mDataLock.unlock();
		}
	}

	@Override
	public int indexOf(E item) {
		mDataLock.lock();
		try {
			return super.indexOf(item);
		} finally {
			mDataLock.unlock();
		}
	}

	@Override
	public E findItem(E similar) {
		mDataLock.lock();
		try {
			return super.findItem(similar);
		} finally {
			mDataLock.unlock();
		}
	}

	@Override
	public boolean replace(int index, E newData) {
		mDataLock.lock();
		try {
			return super.replace(index, newData);
		} finally {
			mDataLock.unlock();
		}
	}

	@Override
	public boolean swap(int positionA, int positionB) {
		mDataLock.lock();
		try {
			return super.swap(positionA, positionB);
		} finally {
			mDataLock.unlock();
		}
	}

	@Override
	public void waitForDataLoaded() {
		mDataLock.lock();
		try {
			getList();
		    super.waitForDataLoaded();
		} finally {
			mDataLock.unlock();
		}
	}

	@Override
	public java.util.List<E> getListCopy() {
		mDataLock.lock();
		try {
			return Collections.unmodifiableList(new ArrayList<E>(getList()));
		} finally {
			mDataLock.unlock();
		}
	}

	@Override
	public void onReplaceItemFailed(AsynchronousDbHelper<E, ?> db, E original, E replacement, Throwable cause) {
		mDataLock.lock();
		try {
			super.onReplaceItemFailed(db, original, replacement, cause);
		} finally {
			mDataLock.unlock();
		}
	}
}
