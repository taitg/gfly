import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import static java.lang.Math.*;

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
		if (milliseconds < 1) return;
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

	public static String headingToString(double heading) {
		if (heading >= 11.25 && heading < 33.75) return "NNE";
		if (heading >= 33.75 && heading < 56.25) return "NE ";
		if (heading >= 56.25 && heading < 78.75) return "ENE";
		if (heading >= 78.75 && heading < 101.25) return " E ";
		if (heading >= 101.25 && heading < 123.75) return "ESE";
		if (heading >= 123.75 && heading < 146.25) return "SE ";
		if (heading >= 146.25 && heading < 168.75) return "SSE";
		if (heading >= 168.75 && heading < 191.25) return " S ";
		if (heading >= 191.25 && heading < 213.75) return "SSW";
		if (heading >= 213.75 && heading < 236.25) return "SW ";
		if (heading >= 236.25 && heading < 258.75) return "WSW";
		if (heading >= 258.75 && heading < 281.25) return " W ";
		if (heading >= 281.25 && heading < 303.75) return "WNW";
		if (heading >= 303.75 && heading < 326.25) return "NW ";
		if (heading >= 326.25 && heading < 348.75) return "NNW";
		return " N ";
	}

	public static double gpsDistance(double lat1, double lon1, double lat2, double lon2) {
		final int R = 6371; // Radius of the earth
		Double latDistance = toRad(lat2-lat1);
		Double lonDistance = toRad(lon2-lon1);
		Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + 
			Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * 
			Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		return R * c;
	}

	private static double toRad(double value) {
		return value * Math.PI / 180;
	}

	public static double vincentyDistance(double lat1, double lon1, double lat2, double lon2) {
		double λ1 = toRad(lon1);
		double λ2 = toRad(lon2);

		double φ1 = toRad(lat1);
		double φ2 = toRad(lat2);

		double a = 6_378_137;
		double b = 6_356_752.314245;
		double f = 1 / 298.257223563;

		double L = λ2 - λ1;
		double tanU1 = (1 - f) * tan(φ1), cosU1 = 1 / sqrt((1 + tanU1 * tanU1)), sinU1 = tanU1 * cosU1;
		double tanU2 = (1 - f) * tan(φ2), cosU2 = 1 / sqrt((1 + tanU2 * tanU2)), sinU2 = tanU2 * cosU2;

		double λ = L, λʹ, iterationLimit = 100, cosSqα, σ, cos2σM, cosσ, sinσ, sinλ, cosλ;
		do {
				sinλ = sin(λ);
				cosλ = cos(λ);
				double sinSqσ = (cosU2 * sinλ) * (cosU2 * sinλ) + (cosU1 * sinU2 - sinU1 * cosU2 * cosλ) * (cosU1 * sinU2 - sinU1 * cosU2 * cosλ);
				sinσ = sqrt(sinSqσ);
				if (sinσ == 0) return 0;  // co-incident points
				cosσ = sinU1 * sinU2 + cosU1 * cosU2 * cosλ;
				σ = atan2(sinσ, cosσ);
				double sinα = cosU1 * cosU2 * sinλ / sinσ;
				cosSqα = 1 - sinα * sinα;
				cos2σM = cosσ - 2 * sinU1 * sinU2 / cosSqα;

				if (Double.isNaN(cos2σM)) cos2σM = 0;  // equatorial line: cosSqα=0 (§6)
				double C = f / 16 * cosSqα * (4 + f * (4 - 3 * cosSqα));
				λʹ = λ;
				λ = L + (1 - C) * f * sinα * (σ + C * sinσ * (cos2σM + C * cosσ * (-1 + 2 * cos2σM * cos2σM)));
		} while (abs(λ - λʹ) > 1e-12 && --iterationLimit > 0);

		if (iterationLimit == 0)
			return 0;
			// throw new IllegalStateException("Formula failed to converge");

		double uSq = cosSqα * (a * a - b * b) / (b * b);
		double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
		double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
		double Δσ = B * sinσ * (cos2σM + B / 4 * (cosσ * (-1 + 2 * cos2σM * cos2σM) -
						B / 6 * cos2σM * (-3 + 4 * sinσ * sinσ) * (-3 + 4 * cos2σM * cos2σM)));

		return b * A * (σ - Δσ) / 1000.0;
}
}
