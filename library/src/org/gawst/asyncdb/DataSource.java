package org.gawst.asyncdb;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Created by robUx4 on 12/31/2014.
 */
public interface DataSource<E> {

	/**
	 * @return All the elements in the source with all the fields
	 */
	Cursor queryAll();

	/**
	 * Clear all the data in the source
	 * @return the number of items removed
	 */
	int clearAllData();

	/**
	 * Add a new element in the source
	 * @param element The element to add
	 * @return An object representing the added item or {@code null} if it wasn't added
	 */
	Object insert(ContentValues element) throws RuntimeException;

	/**
	 * Delete the item from the source of data
	 * @param itemToDelete
	 * @return {@code true} if the element was removed
	 */
	boolean delete(E itemToDelete);

	/**
	 * Update an element already in the database
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
