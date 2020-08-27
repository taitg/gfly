
/**
 * Error codes and error handling functions
 */
public class Errors {
	/**
	 * Print an error message and, if devMode AND verbose, a stack trace of the
	 * error
	 * 
	 * @param e       Exception to handle
	 * @param message A message to display if devMode and verbose
	 */
	public static void handleException(Exception e, String message) {
		System.err.printf("\nERROR: %s\n", message);
		if (Config.devMode && Config.verbose) {
			e.printStackTrace();
			System.out.println();
		}
	}
}
