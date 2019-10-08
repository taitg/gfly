import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

/**
 * Config values and functions
 * 
 * *** IMPORTANT NOTE: Values below will be IGNORED during run if they are set
 * in the config file.
 * 
 * They can also be entered while the program is running if devMode is true.
 */
public class Config {

	// name of the config file
	// entries in the file need to be in the form variableName=value (no spaces or
	// quotes)
	// all of the variables below, unless otherwise noted, can be set in the config
	// file
	private static final String configFile = "settings.conf";

	// location of the folder where test data will be saved
	// if a writeable USB drive is connected, this folder will be created on it
	// it will also be created in the current directory
	public static String dataFolder = "data";

	// naming for test data files
	public static String dataPrefix = "FREDt_";
	public static String dataSuffix = ".fcv";

	// if true, program accepts console commands and saves additional debug data
	public static boolean devMode = false;

	// if true, give extra console output
	public static boolean verbose = false;

	// time zone string to save with data
	public static String timeZone = "GMT"; // GPS sends time in GMT timezone

	// if using USB for GPS serial connection, use "USB"
	// if disconnected, use "NONE"
	// otherwise use default (UART)
	public static String gpsSource = "USB";

	// if no data is received for this number of milliseconds, consider GPS fix lost
	public static int gpsDataTimeout = 15000;

	// delay time (in milliseconds) between checking USB devices
	public static int usbPollingInterval = 5000;

	// milliseconds to wait before the next main loop iteration
	public static int mainLoopDelay = 100;

	// milliseconds to wait on startup before attempting to initialize device
	public static int programStartDelay = 0; // (defaulted to no delay)

	// LED settings
	public static int ledErrorFlashes = 3; // number of flashes when showing an error
	public static int ledFlashTime = 500; // milliseconds that LED should be on for during a flash
	public static int ledFlashOffTime = 500; // milliseconds that LED should be off for after a flash
	public static int ledRefreshTime = 100; // milliseconds between checks for if LED should be updated
	public static int ledSensorDelayTime = 50; // milliseconds to delay sensor reading after LED is fired
	public static int ledExtraFlashColour = -1; // one extra flash per reading - use a colour code from LED.java

	// GPIO pin assignments (wiringPi numbering)
	// pin 8 is for sensor SDA (I2C)
	// pin 9 is for sensor SCL (I2C)
	// pins 15 and 16 are UART
	public static int titrationButtonPin = 27;
	public static int endpointButtonPin = 28;
	public static int resetButtonPin = 29;

	public static int rgbwRedPin = 23;
	public static int rgbwGreenPin = 22;
	public static int rgbwBluePin = 21;
	public static int rgbwWhitePin = 30;
	public static int[] rgbwPins; // set when program is run

	public static int rgbRedPin = 14;
	public static int rgbGreenPin = 13;
	public static int rgbBluePin = 12;
	public static int[] rgbPins; // set when program is run

	public static int gpsLedPin = 2;
	public static int gpsSwitchPin = 3;

	public static int pumpPin = 0;
	public static int mixerPin = 7;

	public static int limit1Pin = 25;
	public static int stepper1Pin1 = 1;
	public static int stepper1Pin2 = 4;
	public static int stepper1Pin3 = 5;
	public static int stepper1Pin4 = 6;
	public static int[] stepper1Pins; // set when program is run

	public static int limit2Pin = 24;
	public static int stepper2Pin1 = 10;
	public static int stepper2Pin2 = 11;
	public static int stepper2Pin3 = 31;
	public static int stepper2Pin4 = 26;
	public static int[] stepper2Pins; // set when program is run

	// I2C address for the sensor
	// NOT SETTABLE IN CONFIG FILE
	public static final int sensorAddress = 0x29;

	/**
	 * Load config values from file
	 */
	public static void loadFromFile() {
		try {
			Scanner scanner = new Scanner(new File(configFile));
			while (scanner.hasNext())
				handleConfigLine(scanner.next());
			updateArrays();
			if (verbose)
				System.out.printf("Loaded config file (%s)\n", configFile);
		} catch (Exception e) {
			Errors.handleException(e, "Failed to load config file (" + configFile + ")");
		}
	}

	/**
	 * Parses a line from the config file and puts the value into the appropriate
	 * variable
	 * 
	 * @param line The string to parse
	 */
	public static void handleConfigLine(String line) {
		try {
			String[] parts = line.split("=");
			String a = parts[0];
			String b = parts[1];
			if (a.equals("devMode"))
				devMode = b.equals("true");
			else if (a.equals("verbose"))
				verbose = b.equals("true");
			else if (a.equals("dataFolder"))
				dataFolder = b;
			else if (a.equals("dataPrefix"))
				dataPrefix = b;
			else if (a.equals("dataSuffix"))
				dataSuffix = b;
			else if (a.equals("timeZone"))
				timeZone = b;
			else if (a.equals("gpsSource"))
				gpsSource = b;
			else if (a.equals("gpsDataTimeout"))
				gpsDataTimeout = Integer.parseInt(b);
			else if (a.equals("usbPollingInterval"))
				usbPollingInterval = Integer.parseInt(b);
			else if (a.equals("mainLoopDelay"))
				mainLoopDelay = Integer.parseInt(b);
			else if (a.equals("programStartDelay"))
				programStartDelay = Integer.parseInt(b);

			else if (a.equals("ledErrorFlashes"))
				ledErrorFlashes = Integer.parseInt(b);
			else if (a.equals("ledFlashTime"))
				ledFlashTime = Integer.parseInt(b);
			else if (a.equals("ledFlashOffTime"))
				ledFlashOffTime = Integer.parseInt(b);
			else if (a.equals("ledRefreshTime"))
				ledRefreshTime = Integer.parseInt(b);
			else if (a.equals("ledSensorDelayTime"))
				ledSensorDelayTime = Integer.parseInt(b);
			else if (a.equals("ledExtraFlashColour"))
				ledExtraFlashColour = Integer.parseInt(b);

			else if (a.equals("titrationButtonPin"))
				titrationButtonPin = Integer.parseInt(b);
			else if (a.equals("endpointButtonPin"))
				endpointButtonPin = Integer.parseInt(b);
			else if (a.equals("resetButtonPin"))
				resetButtonPin = Integer.parseInt(b);
			else if (a.equals("limit1Pin"))
				limit1Pin = Integer.parseInt(b);
			else if (a.equals("limit2Pin"))
				limit2Pin = Integer.parseInt(b);
			else if (a.equals("rgbwRedPin"))
				rgbwRedPin = Integer.parseInt(b);
			else if (a.equals("rgbwGreenPin"))
				rgbwGreenPin = Integer.parseInt(b);
			else if (a.equals("rgbwBluePin"))
				rgbwBluePin = Integer.parseInt(b);
			else if (a.equals("rgbwWhitePin"))
				rgbwWhitePin = Integer.parseInt(b);
			else if (a.equals("rgbRedPin"))
				rgbRedPin = Integer.parseInt(b);
			else if (a.equals("rgbGreenPin"))
				rgbGreenPin = Integer.parseInt(b);
			else if (a.equals("rgbBluePin"))
				rgbBluePin = Integer.parseInt(b);
			else if (a.equals("gpsLedPin"))
				gpsLedPin = Integer.parseInt(b);
			else if (a.equals("gpsSwitchPin"))
				gpsSwitchPin = Integer.parseInt(b);
			else if (a.equals("pumpPin"))
				pumpPin = Integer.parseInt(b);
			else if (a.equals("mixerPin"))
				mixerPin = Integer.parseInt(b);
			else if (a.equals("stepper1Pin1"))
				stepper1Pin1 = Integer.parseInt(b);
			else if (a.equals("stepper1Pin2"))
				stepper1Pin2 = Integer.parseInt(b);
			else if (a.equals("stepper1Pin3"))
				stepper1Pin3 = Integer.parseInt(b);
			else if (a.equals("stepper1Pin4"))
				stepper1Pin4 = Integer.parseInt(b);
			else if (a.equals("stepper2Pin1"))
				stepper2Pin1 = Integer.parseInt(b);
			else if (a.equals("stepper2Pin2"))
				stepper2Pin2 = Integer.parseInt(b);
			else if (a.equals("stepper2Pin3"))
				stepper2Pin3 = Integer.parseInt(b);
			else if (a.equals("stepper2Pin4"))
				stepper2Pin4 = Integer.parseInt(b);

			else
				throw new Exception();
			if (verbose)
				System.out.printf("Updated: %s\n", line);
		} catch (Exception e) {
			if (verbose)
				System.out.printf("Invalid: %s\n", line);
		}
	}

	/**
	 * Save the current acidity and iron test counts to the config file
	 */
	public static void saveTestCounts() {
		try {
			List<String> config = new ArrayList<>(Files.readAllLines(Paths.get(configFile), StandardCharsets.UTF_8));
			Files.write(Paths.get(configFile), config, StandardCharsets.UTF_8);
		} catch (Exception e) {
			Errors.handleException(e, "Could not save test counts");
		}
	}

	/**
	 * Update pin arrays
	 */
	private static void updateArrays() {
		rgbwPins = new int[] { rgbwRedPin, rgbwGreenPin, rgbwBluePin, rgbwWhitePin };
		rgbPins = new int[] { rgbRedPin, rgbGreenPin, rgbBluePin };
		stepper1Pins = new int[] { stepper1Pin1, stepper1Pin2, stepper1Pin3, stepper1Pin4 };
		stepper2Pins = new int[] { stepper2Pin1, stepper2Pin2, stepper2Pin3, stepper2Pin4 };
	}
}
