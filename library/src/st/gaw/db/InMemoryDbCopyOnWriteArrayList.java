package st.gaw.db;

import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;

/**
 * a basic helper class to keep the content of a flat database in an {@link CopyOnWriteArrayList}
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory by the {@link InMemoryDbCopyOnWriteArrayList}
 */
public abstract class InMemoryDbCopyOnWriteArrayList<E> extends InMemoryDbList<E, CopyOnWriteArrayList<E>> {

	/**
	 * the array where the data are stored, locked when writing on it
	 */
	private CopyOnWriteArrayList<E> mData;

	/**
	 * @param context Used to open or create the database
	 * @param name Database filename on disk
	 * @param version Version number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 * @param logger The {@link Logger} to use for all logs (can be null for the default Android logs)
	 */
	protected InMemoryDbCopyOnWriteArrayList(Context context, String name, int version, Logger logger) {
		this(context, name, version, logger, null);
	}
	
	/**
	 * @param context Used to open or create the database
	 * @param name Database filename on disk
	 * @param version Version number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 * @param logger The {@link Logger} to use for all logs (can be null for the default Android logs)
	 * @param initCookie Cookie to pass to {@link #preloadInit(Object, Logger)}
	 */
	protected InMemoryDbCopyOnWriteArrayList(Context context, String name, int version, Logger logger, Object initCookie) {
		super(context, name, version, logger, initCookie);
	}
	
	@Override
	protected void preloadInit(Object cookie, Logger logger) {
		super.preloadInit(cookie, logger);
		mData = new CopyOnWriteArrayList<E>();
	}
	
	@Override
	protected CopyOnWriteArrayList<E> getList() {
		return mData;
	}
}
