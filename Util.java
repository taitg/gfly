import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

/**
 * Class for utility functions
 */
public class Util {
			
	/**
	 * Delay for a set amount of time
	 * 
	 * @param milliseconds How long to wait for
	 */
	public static void delay(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			Errors.handleException(e, "Thread interrupted");
		}
	}
	
	/**
	 * Rounds to a number of decimal places
	 * 
	 * @param original The original number
	 * @param places How many decimal places to round to
	 * @return The rounded number
	 */
	public static double round(double original, int places) {
		if (places < 0) return original;
		long factor = (long) Math.pow(10, places);
		return (double) Math.round(original * factor) / factor;
	}
	
	/**
	 * Convert an object to JSON
	 * 
	 * @param obj Object to convert to JSON
	 * @param pretty Whether to use pretty printing for nice formatting
	 * @return Object as a JSON string
	 */
	public static String toJSON(Object obj, boolean pretty) {
		try {
			// use pretty printing
			if (pretty) {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				return gson.toJson(obj);
			}

			// normal formatting
			return new Gson().toJson(obj);
		}
		catch (Exception e) {
			Errors.handleException(e, "Unable to convert object to JSON");
		}
		return null;
	}
	
	/**
	 * Convert an object to JSON, defaulting to normal formatting
	 * 
	 * @param obj Object to convert to JSON
	 * @return Object as a JSON string
	 */
	public static String toJSON(Object obj) {
		return toJSON(obj, false);
	}
	
	/**
	 * Write a string to a specified file, creating directories if needed
	 *
	 * @param fileName Location of the file to write
	 * @param data String to write to the file
	 * @return True if successful
	 */
	public static boolean writeFileData(String fileName, String data) {
		try {
			File f = new File(fileName);
			if (!f.exists()) f.getParentFile().mkdirs();
			
			PrintWriter writer = new PrintWriter(fileName, "UTF-8");
			writer.print(data);
			writer.close();
			return true;
		}
		catch (Exception e) {
			Errors.handleException(e, "Could not write data to " + fileName);
		}
		return false;
	}
	
	/**
	 * Compress a file into a zip archive with the same name
	 * 
	 * @param fileName The path of the file
	 */
	public static void compressFile(String fileName) {
		try {
			File fileToZip = new File(fileName);
			FileInputStream in = new FileInputStream(fileToZip);
			
			String outputFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".zip";
			FileOutputStream out = new FileOutputStream(outputFileName);
			ZipOutputStream zipOut = new ZipOutputStream(out);
			
			ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
			zipOut.putNextEntry(zipEntry);
			byte[] bytes = new byte[1024];
			int length;
			while((length = in.read(bytes)) >= 0) zipOut.write(bytes, 0, length);
			
			zipOut.close();
			in.close();
			out.close();
		}
		catch (Exception e) {
			Errors.handleException(e, "Failed to compress data file " + fileName);
		}
	}
}
