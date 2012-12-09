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

	protected ReentrantLock mDataLock = new ReentrantLock();

	/**
	 * @param context to use to open or create the database
	 * @param name of the database file, or null for an in-memory database
	 * @param version number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 */
	protected InMemoryDbArrayList(Context context, String name, int version) {
		super(context, name, version);
	}

	@Override
	protected synchronized ArrayList<E> getList() {
		if (mData==null)
			mData = new ArrayList<E>(0);
		return mData;
	}

	@Override
	protected void startLoadingInMemory() {
		super.startLoadingInMemory();
		mDataLock.lock();
	}

	@Override
	protected void finishLoadingInMemory() {
		mDataLock.unlock();
		super.finishLoadingInMemory();
	}

	@Override
	protected void startLoadingFromCursor(Cursor c) {
		mData.ensureCapacity(c.getCount());
	}

	public void addItem(E item) {
		// protect the data coherence
		mDataLock.lock();
		try {
			super.addItem(item);
		} finally {
			mDataLock.unlock();
		}
	};

	public boolean removeItem(E item) {
		// protect the data coherence
		mDataLock.lock();
		try {
			return super.removeItem(item);
		} finally {
			mDataLock.unlock();
		}
	};
}
