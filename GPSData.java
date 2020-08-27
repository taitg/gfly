
/**
 * Class for GPS data
 */
public class GPSData {

	private boolean valid; // flag for whether the module reported it had a GPS fix
	private String time; // time in HH.MM.SS
	private String date; // date in YYYY-MM-DD
	private double latitude; // latitude as a decimal
	private double longitude; // longitude as a decimal
	private double altitude; // altitude in m as a decimal
	private double speed; // speed in knots as a decimal
	private double trackingAngle; // tracking angle as a decimal
	private boolean switchPressed; // flag for whether the GPS switch was pressed

	/**
	 * Constructor for a GPS data object
	 * 
	 * @param data          Data string
	 * @param switchPressed Whether the switch was pressed when the reading was
	 *                      obtained
	 */
	public GPSData(String data, boolean switchPressed) {
		String[] parts = data.split(",");
		try {
			// process the information from the raw serial data string
			if (data.contains("GPRMC")) {
				valid = parts[2].equals("A");
				time = processTime(parts[1]);
				date = processDate(parts[9]);
				latitude = processLatitude(parts[3], parts[4]);
				longitude = processLongitude(parts[5], parts[6]);
				speed = Double.parseDouble(parts[7]);
				trackingAngle = Double.parseDouble(parts[8]);
			} else if (data.contains("GPGGA")) {
				valid = Integer.parseInt(parts[6]) > 0;
				time = processTime(parts[1]);
				latitude = processLatitude(parts[2], parts[3]);
				longitude = processLongitude(parts[4], parts[5]);
				altitude = parts[9] == null || parts[9].equals("") ? 0.0 : Double.parseDouble(parts[9]);
			}
			this.switchPressed = switchPressed;
		} catch (Exception e) {
			Errors.handleException(e, "Bad GPS data");
		}
	}

	/**
	 * Constructor for only time and date (used for no GPS fix)
	 * 
	 * @param time Time of the reading
	 * @param date Date of the reading
	 */
	public GPSData(String time, String date) {
		this.time = time;
		this.date = date;
		valid = true;
		latitude = -1000;
		longitude = -1000;
		altitude = -1000;
		speed = -1000;
		trackingAngle = -1000;
	}

	/**
	 * Returns true if data is valid (GPS has a fix)
	 * 
	 * @return True if GPS module had a fix
	 */
	public boolean isValid() {
		return valid;
	}

	public boolean isComplete() {
		return valid && altitude > 0;
	}

	/**
	 * Returns time
	 * 
	 * @return Time in HH.MM.SS
	 */
	public String getTime() {
		return time;
	}

	/**
	 * Returns date
	 * 
	 * @return Date in YYYY-MM-DD
	 */
	public String getDate() {
		return date;
	}

	/**
	 * Returns date and time in a single string
	 * 
	 * @return Date and time in YYYY-MM-DDTHH.MM.SS
	 */
	public String getDateTime() {
		return date + "T" + time;
	}

	/**
	 * Returns latitude
	 * 
	 * @return Latitude as a decimal
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Returns longitude
	 * 
	 * @return Longitude as a decimal
	 */
	public double getLongitude() {
		return longitude;
	}

	public double getAltitude() {
		return altitude;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	};

	/**
	 * Returns speed in knots
	 * 
	 * @return Speed as a decimal
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * Returns speed in km/h
	 * 
	 * @return Speed as a decimal
	 */
	public double getSpeedKMH() {
		return speed * 1.852;
	}

	/**
	 * Returns tracking angle
	 * 
	 * @return Tracking angle as a decimal
	 */
	public double getTrackingAngle() {
		return trackingAngle;
	}

	/**
	 * Return GPS switch status
	 * 
	 * @return True if switch was pressed
	 */
	public boolean switchWasPressed() {
		return switchPressed;
	}

	/**
	 * Prints GPS data
	 */
	public void print() {
		System.out.printf("\nTime:  %s %s\n", time, date);
		System.out.printf("Valid: %s\n", valid);
		System.out.printf("Lat.:  %f\n", latitude);
		System.out.printf("Long.: %f\n", longitude);
		System.out.printf("Alt.: %f\n", altitude);
		System.out.printf("Speed: %f at angle %f\n\n", speed, trackingAngle);
	}

	/**
	 * Processes raw time data - from HHMMSS.MMM (GMT)
	 * 
	 * @param time Time in HHMMSS.MMM
	 * @return Time in HH.MM.SS
	 */
	private String processTime(String time) {
		if (time.length() < 6)
			return "";
		return String.format("%s.%s.%s", time.substring(0, 2), time.substring(2, 4), time.substring(4, 6));
	}

	/**
	 * Processes raw date data - from DDMMYY
	 * 
	 * @param date Date in DDMMYY
	 * @return Date in YYYY-MM-DD
	 */
	private String processDate(String date) {
		if (date.length() < 6)
			return "";
		return String.format("20%s-%s-%s", date.substring(4, 6), date.substring(2, 4), date.substring(0, 2));
	}

	/**
	 * Processes raw latitude data - from DDMM.MMM where: D = degrees, M = decimal
	 * minutes
	 * 
	 * @param latitude  Latitude in DDMM.MMM
	 * @param direction N or S
	 * @return Latitude as a decimal
	 */
	private double processLatitude(String latitude, String direction) {
		if (latitude.length() < 3)
			return -1000.0;
		double degrees = Double.parseDouble(latitude.substring(0, 2));
		double minutes = Double.parseDouble(latitude.substring(2, latitude.length()));
		double result = degrees + minutes / 60.0;
		if (direction.equals("S"))
			result *= -1.0;
		return result;
	}

	/**
	 * Processes raw longitude data - from DDDMM.MMM where: D = degrees, M = decimal
	 * minutes
	 * 
	 * @param longitude Longitude in DDDMM.MMM
	 * @param direction E or W
	 * @return Longitude as a decimal
	 */
	private double processLongitude(String longitude, String direction) {
		if (longitude.length() < 4)
			return -1000.0;
		double degrees = Double.parseDouble(longitude.substring(0, 3));
		double minutes = Double.parseDouble(longitude.substring(3, longitude.length()));
		double result = degrees + minutes / 60.0;
		if (direction.equals("W"))
			result *= -1.0;
		return result;
	}
}
