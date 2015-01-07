package org.gawst.asyncdb.purge;

import org.gawst.asyncdb.AsynchronousDbHelper;

/**
* Created by robUx4 on 07/01/2015.
*/
public interface PurgeHandler {
	void onElementsAdded(AsynchronousDbHelper<?, ?> db);
}
