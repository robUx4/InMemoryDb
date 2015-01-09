package org.gawst.asyncdb;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * a basic helper class to keep the content of a flat database in an {@link CopyOnWriteArrayList}
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory by the {@link InMemoryDbCopyOnWriteArrayList}
 */
public abstract class InMemoryDbCopyOnWriteArrayList<E, INSERT_ID> extends InMemoryDbList<E, CopyOnWriteArrayList<E>, INSERT_ID> {

	/**
	 * the array where the data are stored, locked when writing on it
	 */
	private CopyOnWriteArrayList<E> mData;

	/**
	 * @param db The already created {@link android.database.sqlite.SQLiteOpenHelper} to use as storage
	 * @param name Database name for logs
	 * @param logger The {@link org.gawst.asyncdb.Logger} to use for all logs (can be null for the default Android logs)
	 * @param initCookie Cookie to pass to {@link AsynchronousDbHelper#preloadInit(Object)}
	 */
	protected InMemoryDbCopyOnWriteArrayList(DataSource<E, INSERT_ID> db, String name, Logger logger, Object initCookie) {
		super(db, name, logger, initCookie);
	}
	
	@Override
	protected void preloadInit(Object cookie) {
		super.preloadInit(cookie);
		mData = new CopyOnWriteArrayList<E>();
	}
	
	@Override
	protected CopyOnWriteArrayList<E> getList() {
		return mData;
	}
}
