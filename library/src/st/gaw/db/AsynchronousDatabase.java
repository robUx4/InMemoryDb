package st.gaw.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A {@link SQLiteOpenHelper} class with a worker thread for all disk work
 * 
 * @param <E> Type of Objects that are stored in the main table
 */
public abstract class AsynchronousDatabase<E> extends AsynchronousDbHelper<E> {

	/**
	 * Constructor for the SQLite database with a worker thread
	 * @param context Used to open or create the database
	 * @param name Database filename on disk
	 * @param versionVersion number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 * @param logger The {@link Logger} to use for all logs (can be null for the default Android logs)
	 */
	public AsynchronousDatabase(Context context, String name, int version, Logger logger) {
		super(context, name, version, logger, null);
	}

	@Override
	protected final void addCursorInMemory(Cursor c) {
		// do nothing
	}
	
	@Override
	protected final boolean shouldReloadAllData() {
		return false;
	}
}
