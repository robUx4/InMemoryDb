package org.gawst.asyncdb;

import android.content.ContentValues;

/**
 * Interface to define a data source with {@link E} elements.
 *
 * @param <E> Elements read/written by the data source.
 * @param <INSERT_ID> Type of the object returned by {@link #insert(android.content.ContentValues)}
 * @author Created by robUx4 on 12/31/2014.
 */
public interface DataSource<E, INSERT_ID> {

	/**
	 * Internal interface to handle elements when the whole database is read.
	 *
	 * @param <E>
	 */
	public interface BatchReadingCallback<E> {
		/**
		 * Add the element in the memory storage
		 *
		 * @param item The object to add
		 */
		void addItemInMemory(E item);

		/**
		 * Called when we have the cursor to read the data from.
		 * <p/>
		 * Useful to prepare the amount of data needed or get the index of the column we need.
		 *
		 * @param elementCount The amount of elements that are about to be read from the database.
		 */
		void startLoadingAllItems(int elementCount);

		void removeInvalidEntry(InvalidEntry invalidEntry);
	}

	/**
	 * Internal call to read the whole database.
	 *
	 * @param readingCallback
	 * @return All the elements in the source with all the fields
	 */
	void queryAll(BatchReadingCallback<E> readingCallback);

	/**
	 * Clear all the data in the source
	 *
	 * @return the number of items removed
	 */
	int clearAllData();

	/**
	 * Add a new element in the source
	 *
	 * @param element The element to add
	 * @return An object representing the added item or {@code null} if it wasn't added
	 */
	INSERT_ID insert(ContentValues element) throws RuntimeException;

	/**
	 * Delete the item from the source of data
	 *
	 * @param itemToDelete
	 * @return {@code true} if the element was removed
	 */
	boolean delete(E itemToDelete);

	/**
	 * Delete the data specified by the {@code invalidEntry} from the source.
	 *
	 * @param invalidEntry
	 * @return
	 */
	boolean deleteInvalidEntry(InvalidEntry invalidEntry);

	/**
	 * Update an element already in the database
	 *
	 * @param itemToUpdate
	 * @param updateValues
	 * @return {@code true} if the element was updated
	 */
	boolean update(E itemToUpdate, ContentValues updateValues);

	/**
	 * Completely delete the data source, likely because it's corrupted beyond repair
	 */
	void eraseSource();
}
