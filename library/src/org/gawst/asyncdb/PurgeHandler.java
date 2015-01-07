package org.gawst.asyncdb;

/**
* Created by robUx4 on 07/01/2015.
*/
public interface PurgeHandler {
	void onElementsAdded(AsynchronousDbHelper<?, ?> db);
}
