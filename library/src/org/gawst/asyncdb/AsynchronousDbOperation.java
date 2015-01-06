package org.gawst.asyncdb;

/**
 * Interface for operations run in the internal thread of the {@link AsynchronousDbHelper} via
 * {@link AsynchronousDbHelper#scheduleCustomOperation(AsynchronousDbOperation) scheduleCustomOperation()}
 */
public interface AsynchronousDbOperation<E, INSERT_ID> {
	/**
	 * Callback of the operation to run in the internal thread of {@code db}
	 * 
	 * @param db on which this operation is processed
	 */
	void runInMemoryDbOperation(AsynchronousDbHelper<E, INSERT_ID> db);

}
