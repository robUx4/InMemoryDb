package st.gaw.db;

/**
 * class that holds the {@link Logger} for this library
 */
public class LogManager {

	static Logger logger = new LoggerDefault();
	
	public static void setLogger(Logger newLogger) {
		logger = newLogger;
	}
}
