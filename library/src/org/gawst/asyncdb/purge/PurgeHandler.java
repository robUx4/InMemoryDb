package org.gawst.asyncdb.purge;

import org.gawst.asyncdb.AsynchronousDbHelper;

/**
* Interface for database purge handlers.
*/
public interface PurgeHandler {
	/**
	 * Called anytime an element(s) is added to the database.
	 * <p>Called in the database worker thread.</p>
	 * @param db The {@link org.gawst.asyncdb.AsynchronousDbHelper} to purge.
	 */
	void onElementsAdded(AsynchronousDbHelper<?, ?> db);
}
