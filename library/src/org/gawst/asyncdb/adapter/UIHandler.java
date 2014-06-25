package org.gawst.asyncdb.adapter;

import android.os.Handler;
import android.os.Looper;

/**
 * A {@link android.os.Handler Handler} that runs the code in the UI thread
 */
public class UIHandler extends Handler {

	public UIHandler() {
		super(Looper.getMainLooper());
	}

	/**
	 * Run the {@code Runnable} in the UI thread
	 * @param r The Runnable that will be executed.
	 */
	public void runOnUiThread(Runnable r) {
		if (isUIThread())
			r.run();
		else
			post(r);
	}

	/**
	 * Indicates the current thread is the UI thread
	 * @return {@code true} if the current Thread is the UI thread
	 */
	public static boolean isUIThread() {
		return Thread.currentThread()==Looper.getMainLooper().getThread();
	}

	/**
	 * Assert that we are running in the UI thread
	 */
	public static void assertUIThread() throws IllegalThreadStateException {
		if (!isUIThread())
			throw new IllegalThreadStateException();
	}

	/**
	 * Assert that we are NOT running in the UI thread
	 */
	public static void assertNotUIThread() throws IllegalThreadStateException {
		if (isUIThread())
			throw new IllegalThreadStateException();
	}
}
