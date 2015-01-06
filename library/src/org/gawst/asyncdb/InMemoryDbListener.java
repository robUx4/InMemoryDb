package org.gawst.asyncdb;

/**
 * listener interface to be notified when the data changes in a {@link AsynchronousDbHelper}
 *
 * @param <E> the type of data stored by the database
 */
public interface InMemoryDbListener<E> {
	/**
	 * notify the listener that the data of the {@link AsynchronousDbHelper} just changed
	 * <p>it may be called from any thread, including the internal one
	 * @param db the database which content has changed
	 */
	void onMemoryDbChanged(AsynchronousDbHelper<E, ?> db);
}
