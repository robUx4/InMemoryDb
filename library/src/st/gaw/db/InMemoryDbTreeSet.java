package st.gaw.db;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.database.Cursor;

/**
 * a basic helper class to keep the content of a flat database in an {@link TreeSet}
 * <p>
 * you should use the {@link #mDataLock} when iterating through the {@link TreeSet}
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory by the {@link InMemoryDbTreeSet}
 */
public abstract class InMemoryDbTreeSet<E> extends InMemoryDbSet<E, TreeSet<E>> {

	/**
	 * the array where the data are stored, locked when writing on it
	 */
	private TreeSet<E> mData;

	private final Comparator<E> comparator;

	/**
	 * ReentrantLock used to protect {@link #mData} when reading/writing/iterating it
	 */
	protected ReentrantLock mDataLock;

	/**
	 * @param context to use to open or create the database
	 * @param name of the database file, or null for an in-memory database
	 * @param version number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 * @param logger the {@link Logger} to use for all logs (can be null for the default Android logs)
	 * @param comparator comparator to sort the elements 
	 */
	protected InMemoryDbTreeSet(Context context, String name, int version, Logger logger, Comparator<E> comparator) {
		super(context, name, version, logger);
		this.comparator = comparator;
	}

	@Override
	protected void preloadInit() {
		super.preloadInit();
		mDataLock = new ReentrantLock();
	}

	@Override
	protected synchronized TreeSet<E> getSet() {
		if (!mDataLock.isHeldByCurrentThread()) throw new IllegalStateException("we need a lock on mDataLock to access mData in "+this);
		if (mData==null)
			mData = new TreeSet<E>(comparator);
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
	protected void startLoadingFromCursor(Cursor c) {
		if (mData==null)
			mData = new TreeSet<E>(comparator);
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
	};

	@Override
	public boolean add(E item) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.add(item);
		} finally {
			mDataLock.unlock();
		}
	};
	
	@Override
	public boolean replace(E item) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.replace(item);
		} finally {
			mDataLock.unlock();
		}
	};

	@Override
	public boolean remove(E item) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.remove(item);
		} finally {
			mDataLock.unlock();
		}
	};

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
			return mData==null ? 0 : mData.size();
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
}
