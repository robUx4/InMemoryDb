package st.gaw.db;

import android.content.ContentValues;

/**
 * interface used to receive errors from the {@link InMemoryDbHelper}
 * @see InMemoryDbHelper#setDbErrorHandler(InMemoryDbErrorHandler)
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory by the {@link InMemoryDbHelper}
 */
public interface InMemoryDbErrorHandler<E> {

	/**
	 * called when the database failed to store the item
	 * @param db the database that tried to store
	 * @param item the data that failed to store
	 * @param values the values that failed to be added
	 * @param cause an exception explaining why it failed
	 */
	void onAddItemFailed(InMemoryDbHelper<E> db, E item, ContentValues values, Throwable cause);
	
	/**
	 * called when the database failed to update an item
	 * @param db the database that tried to store
	 * @param item the data that failed to store
	 * @param cause an exception explaining why it failed
	 */
	void onUpdateItemFailed(InMemoryDbHelper<E> db, E item, Throwable cause);
	
	/**
	 * called when the database failed to delete an item
	 * @param db the database that tried to store
	 * @param item the data that failed to store
	 * @param cause an exception explaining why it failed
	 */
	void onRemoveItemFailed(InMemoryDbHelper<E> db, E item, Throwable cause);
	
	/**
	 * called when the database failed to replace an item
	 * @param db the database that tried to store
	 * @param original the item that failed to be updated
	 * @param replacement the data that should be in the item
	 * @param cause an exception explaining why it failed
	 */
	void onReplaceItemFailed(InMemoryDbHelper<E> db, E original, E replacement, Throwable cause);
	
}
