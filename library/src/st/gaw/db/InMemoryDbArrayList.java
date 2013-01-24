package st.gaw.db;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.database.Cursor;

/**
 * a basic helper class to keep the content of a flat database in an {@link ArrayList}
 * <p>
 * you should use the {@link #mDataLock} when iterating through the {@link ArrayList}
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory by the {@link InMemoryDbArrayList}
 */
public abstract class InMemoryDbArrayList<E> extends InMemoryDbList<E, ArrayList<E>> {

	/**
	 * the array where the data are stored, locked when writing on it
	 */
	private ArrayList<E> mData;

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
	 */
	protected InMemoryDbArrayList(Context context, String name, int version, Logger logger) {
		super(context, name, version, logger);
	}

	@Override
	protected void preloadInit() {
		super.preloadInit();
		mDataLock = new ReentrantLock();
	}

	@Override
	protected synchronized ArrayList<E> getList() {
		if (!mDataLock.isHeldByCurrentThread()) throw new IllegalStateException("we need a lock on mDataLock to access mData in "+this);
		if (mData==null)
			mData = new ArrayList<E>(0);
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
			mData = new ArrayList<E>(c.getCount());
		else
			mData.ensureCapacity(c.getCount());
	}

	public boolean add(E item) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.add(item);
		} finally {
			mDataLock.unlock();
		}
	};

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
	};

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
	};

	@Override
	public boolean swap(int positionA, int positionB) {
		mDataLock.lock();
		try {
			return super.swap(positionA, positionB);
		} finally {
			mDataLock.unlock();
		}
	}
}
