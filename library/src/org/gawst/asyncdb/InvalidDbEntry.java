package org.gawst.asyncdb;

import android.support.annotation.NonNull;

/**
 * Exception to indicate a DB entry is no longer valid/usable, it should be removed from the DB
 *
 * @author Created by Steve Lhomme on 05/01/2015.
 */
public class InvalidDbEntry extends Exception {
	private final InvalidEntry invalidEntry;

	public InvalidDbEntry(@NonNull InvalidEntry invalidEntry) {
		this.invalidEntry = invalidEntry;
	}

	public @NonNull InvalidEntry getInvalidEntry() {
		return invalidEntry;
	}
}
