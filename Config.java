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

	// if true, program accepts console commands and saves additional debug data
	public static boolean devMode = false;

	// if true, give extra console output
	public static boolean verbose = false;

	// time zone string to save with data
	public static String timeZone = "GMT"; // GPS sends time in GMT timezone

	// if using USB for GPS serial connection, use "USB"
	// if disconnected, use "NONE"
	// otherwise use default (UART)
	public static String gpsSource = "UART";

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

	public static int serverPort = 8080;

	public static boolean varioAudioOn = false;
	public static double varioClimbThreshold = 0.2;
	public static int varioClimbBaseFreq = 400; // was 440 both
	public static int varioClimbDiffFreq = 480;
	public static double varioSinkThreshold = -1.0;
	public static int varioSinkBaseFreq = 220;
	public static int varioSinkDiffFreq = 110;

	public static int sensorSamples = 25;
	public static int sensorTime = 1000;

	// don't change
	public static int sensorDelay = sensorTime / sensorSamples;
	public static double sensorFactor = 1000.0 / (double) sensorTime;

	public static int toneFreqMin = 110;
	public static int toneFreqMax = 3520;
	public static double tonePulseBase = 100_000.0; // divided by freq

	// GPIO pin assignments (wiringPi numbering)
	// pin 8 is for sensor SDA (I2C)
	// pin 9 is for sensor SCL (I2C)
	// pins 15 and 16 are UART

	public static int gpsLedPin = 2; // unused
	public static int gpsSwitchPin = 0; // unused
	public static int piezoPin = 3;

	public static int redButtonLedPin = 1;
	public static int redButtonOutPin = 4;
	public static int redButtonInPin = 5;

	public static int yellowButtonLedPin = 12;
	public static int yellowButtonOutPin = 13;
	public static int yellowButtonInPin = 14;

	public static int greenButtonLedPin = 22;
	public static int greenButtonOutPin = 23;
	public static int greenButtonInPin = 24;

	/**
	 * Load config values from file
	 */
	public static void loadFromFile() {
		try {
			Scanner scanner = new Scanner(new File(configFile));
			while (scanner.hasNext())
				handleConfigLine(scanner.next());
			updateArrays();
			scanner.close();
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
			else if (a.equals("timeZone"))
				timeZone = b;
			if (a.equals("serverPort"))
				serverPort = Integer.parseInt(b);
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

			if (a.equals("varioAudioOn"))
				varioAudioOn = b.equals("true");
			if (a.equals("varioClimbThreshold"))
				varioClimbThreshold = Double.parseDouble(b);
			else if (a.equals("varioClimbBaseFreq"))
				varioClimbBaseFreq = Integer.parseInt(b);
			else if (a.equals("varioClimbDiffFreq"))
				varioClimbDiffFreq = Integer.parseInt(b);
			if (a.equals("varioSinkThreshold"))
				varioSinkThreshold = Double.parseDouble(b);
			else if (a.equals("varioSinkBaseFreq"))
				varioSinkBaseFreq = Integer.parseInt(b);
			else if (a.equals("varioSinkDiffFreq"))
				varioSinkDiffFreq = Integer.parseInt(b);

			else if (a.equals("sensorSamples"))
				sensorSamples = Integer.parseInt(b);
			else if (a.equals("sensorTime"))
				sensorTime = Integer.parseInt(b);
			else if (a.equals("varioSinkBaseFreq"))
				varioSinkBaseFreq = Integer.parseInt(b);
			else if (a.equals("varioSinkDiffFreq"))
				varioSinkDiffFreq = Integer.parseInt(b);

			// public static int = 25;
			// public static int = 1000;

			// public static int = 80;

			// public static int toneFreqMin = 110;
			// public static int toneFreqMax = 3520;
			// public static double tonePulseBase = 100_000.0; // divided by freq

			else if (a.equals("gpsLedPin"))
				gpsLedPin = Integer.parseInt(b);
			else if (a.equals("gpsSwitchPin"))
				gpsSwitchPin = Integer.parseInt(b);
			else if (a.equals("piezoPin"))
				piezoPin = Integer.parseInt(b);
			else if (a.equals("redButtonLedPin"))
				redButtonLedPin = Integer.parseInt(b);
			else if (a.equals("redButtonOutPin"))
				redButtonOutPin = Integer.parseInt(b);
			else if (a.equals("redButtonInPin"))
				redButtonInPin = Integer.parseInt(b);
			else if (a.equals("yellowButtonLedPin"))
				yellowButtonLedPin = Integer.parseInt(b);
			else if (a.equals("yellowButtonOutPin"))
				yellowButtonOutPin = Integer.parseInt(b);
			else if (a.equals("yellowButtonInPin"))
				yellowButtonInPin = Integer.parseInt(b);
			else if (a.equals("greenButtonLedPin"))
				greenButtonLedPin = Integer.parseInt(b);
			else if (a.equals("greenButtonOutPin"))
				greenButtonOutPin = Integer.parseInt(b);
			else if (a.equals("greenButtonInPin"))
				greenButtonInPin = Integer.parseInt(b);

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

	}
}
