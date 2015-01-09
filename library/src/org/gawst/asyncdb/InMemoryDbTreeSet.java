package org.gawst.asyncdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * a basic helper class to keep the content of a flat database in an {@link TreeSet}
 * <p>
 * you should use the {@link #mDataLock} when iterating through the {@link TreeSet}
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory by the {@link InMemoryDbTreeSet}
 */
public abstract class InMemoryDbTreeSet<E, INSERT_ID> extends InMemoryDbSet<E, TreeSet<E>, INSERT_ID> {

	/**
	 * The array where the data are stored, locked when writing on it
	 */
	private TreeSet<E> mData;

	/**
	 * Field to tell when the data are being reloaded from the DB
	 */
	private boolean mIsLoading;

	private final Comparator<E> comparator;

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
	 * @param name of the database file, or null for an in-memory database
	 * @param logger the {@link Logger} to use for all logs (can be null for the default Android logs)
	 * @param comparator comparator to sort the elements
	 */
	protected InMemoryDbTreeSet(DataSource<E, INSERT_ID> db, String name, Logger logger, Comparator<E> comparator) {
		this(db, name, logger, comparator, null);
	}

	/**
	 * @param db
	 * @param name of the database file, or null for an in-memory database
	 * @param logger the {@link org.gawst.asyncdb.Logger} to use for all logs (can be null for the default Android logs)
	 * @param comparator comparator to sort the elements
	 * @param initCookie Cookie to pass to {@link AsynchronousDbHelper#preloadInit(Object)}
	 */
	protected InMemoryDbTreeSet(DataSource<E, INSERT_ID> db, String name, Logger logger, Comparator<E> comparator, Object initCookie) {
		super(db, name, logger, initCookie);
		this.comparator = comparator;
	}

	@Override
	protected void preloadInit(Object cookie) {
		mDataLock = new ReentrantLock();
		dataLoaded = mDataLock.newCondition();
		super.preloadInit(cookie);
		mData = new TreeSet<E>(comparator);
	}

	@Override
	protected TreeSet<E> getSet() {
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
	public boolean contains(E object) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.contains(object);
		} finally {
			mDataLock.unlock();
		}
	}

	@Override
	public boolean add(E item) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.add(item);
		} finally {
			mDataLock.unlock();
		}
	}

	@Override
	public boolean replace(E item) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.replace(item);
		} finally {
			mDataLock.unlock();
		}
	}

	@Override
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
	public boolean removeAll(Collection<E> collection) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.removeAll(collection);
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

	public int size() {
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
	public void waitForDataLoaded() {
		mDataLock.lock();
		try {
			getSet();
		    super.waitForDataLoaded();
		} finally {
			mDataLock.unlock();
		}
	}

	@Override
	public java.util.List<E> getListCopy() {
		mDataLock.lock();
		try {
			return Collections.unmodifiableList(new ArrayList<E>(getSet()));
		} finally {
			mDataLock.unlock();
		}
	}
}
