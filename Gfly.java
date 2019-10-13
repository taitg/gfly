import java.util.ArrayList;
import java.io.PrintWriter;
import java.util.Scanner;

import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Gfly {

	// system states (and test codes)
	public static final int DEV_COMMAND = -1;
	public static final int WAITING = 0;

	// device controller object
	private static DeviceController controller;

	private static int state; // current system state
	private static boolean shutdown; // flag for whether the program should shut down
	private static boolean acceptingCommands; // flag for whether the program should take commands

	private static void handleDevCommand() {
		if (!acceptingCommands)
			return;
		int previousState = state;

		// start new thread
		(new Thread() {
			public void run() {

				// don't start new threads listening for commands until this one is done
				acceptingCommands = false;

				// wait for input
				String input = null;
				String[] args = null;
				try {
					input = new Scanner(System.in).nextLine();
					args = input.split(" ");
				} catch (Exception e) {
					// if it can't read, just return, the program probably quit
					return;
				}

				// process input
				state = DEV_COMMAND;
				try {
					if (input == null || args == null)
						return;
					if (input.equals("quit"))
						shutdown = true;
					else if (args[0].equals("test"))
						controller.testComponent(args);
					else
						Config.handleConfigLine(input);
				} catch (Exception e) {
					Errors.handleException(e, "Could not execute command");
				}

				// start listening for commands again
				acceptingCommands = true;
				state = previousState;
			}
		}).start();
	}

	private static double handleAltitudeChange() {
		for (LED led : controller.getLEDs())
			led.off();

		double diff = controller.getAltitudeChange();
		if (diff > 0.5) {
			controller.setTone(440 + (int) (440 * diff));
			controller.getLEDs().get(2).on();
			if (diff > 1)
				controller.getLEDs().get(3).on();
			if (diff > 1.5)
				controller.getLEDs().get(4).on();
			if (diff > 2)
				controller.getLEDs().get(5).on();
		} else if (diff < -0.5) {
			controller.setTone(220 + (int) (110 * diff));
			controller.getLEDs().get(1).on();
			if (diff < -1)
				controller.getLEDs().get(0).on();
		} else {
			controller.setTone(0);
		}

		System.out.printf("Alt diff: %f\n", diff);
		return diff;
	}

	private static GPSData handleGPS() {
		GPSData gps = controller.getGPSData();
		String gpsStr = String.format("T,%f,%f,%f,%f,%f\n", gps.getLatitude(), gps.getLongitude(), gps.getAltitude(),
				gps.getSpeed(), gps.getTrackingAngle());

		try {
			String fileName = "gps.txt";
			Files.write(Paths.get(fileName), gpsStr.getBytes(StandardCharsets.UTF_8),
					Files.exists(Paths.get(fileName)) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
		} catch (Exception e) {
			Errors.handleException(e, "cannot write GPS file");
		}

		return gps;
	}

	private static void mainLoop() {
		// read and handle commands if accepting them
		if (acceptingCommands)
			handleDevCommand();

		double diff = handleAltitudeChange();

		GPSData gps = handleGPS();

		Util.delay(Config.mainLoopDelay);
	}

	public static void main(String... args) {
		// load the config values
		Config.loadFromFile();

		// wait before attempting to initialize devices
		Util.delay(Config.programStartDelay);

		// initialize the device controller, exit program if it fails
		controller = new DeviceController();
		if (!controller.init())
			System.exit(-1);

		// add a shutdown hook so that the application can trap a Ctrl-C and
		// handle it gracefully by ensuring that all components are properly shut down
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (Config.verbose)
					System.out.println("\nShutting down...");
				controller.shutdown();
			}
		});

		// initialize the program state
		if (Config.devMode)
			acceptingCommands = true;
		shutdown = false;
		state = WAITING;

		// run the main program loop
		while (!shutdown)
			mainLoop();

		// exit the program, closing all threads
		System.exit(0);
	}
}