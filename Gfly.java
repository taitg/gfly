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
	private static long lastGPSTime;
	private static long lastLCDUpdateTime;

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
		double diff = controller.getAltitudeChange();

		if (diff > 0.5) {
			controller.setTone(440 + (int) (440 * diff));
		} else if (diff < -0.5) {
			controller.setTone(220 + (int) (110 * diff));
		} else {
			controller.setTone(0);
		}

		return diff;
	}

	private static GPSData handleGPS() {
		GPSData gps = controller.getGPSData();

		if (gps != null && System.currentTimeMillis() - lastGPSTime > 1000) {
			String gpsStr = String.format("T,%f,%f,%f,%f,%f\n", gps.getLatitude(), gps.getLongitude(), gps.getAltitude(),
					gps.getSpeed(), gps.getTrackingAngle());

			try {
				String fileName = "gps.txt";
				Files.write(Paths.get(fileName), gpsStr.getBytes(StandardCharsets.UTF_8),
						Files.exists(Paths.get(fileName)) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
			} catch (Exception e) {
				Errors.handleException(e, "cannot write GPS file");
			}
			System.out.print(gpsStr);
		}

		lastGPSTime = System.currentTimeMillis();

		return gps;
	}

	private static void main powerDown() {
		try {
			Process p = Runtime.getRuntime().exec("sudo shutdown -h now");
			p.waitFor();
		} catch (Exception e) {
			Errors.handleException(e, "could not shut down!");
		}
		break;
	}

	private static void mainLoop() {
		// read and handle commands if accepting them
		if (acceptingCommands)
			handleDevCommand();

		GPSData gps = handleGPS();
		double diff = handleAltitudeChange();

		if (System.currentTimeMillis() - lastLCDUpdateTime > 200) {
			double[] pta = controller.getPTA();
			double temp = pta[1];
			double altitude = pta[2];
			if (gps != null && gps.getAltitude() > 0)
				altitude = (altitude + gps.getAltitude()) * 0.5;

			String line1 = String.format("%-6.1fm  %4.1fkph", altitude, gps.getSpeed() * 1.852);
			String line2 = String.format("%-4.1fC   %+5.1fm/s", temp, diff);
			controller.setLCDLine(0, line1);
			controller.setLCDLine(1, line2);
			System.out.printf("%s %s\n", line1, line2);

			lastLCDUpdateTime = System.currentTimeMillis();
		}

		if (controller.getButton().isPressed()) {
			controller.setLCDLine(0, "Shutting down...");
			long pressTime = System.currentTimeMillis();
			while (controller.getButton().isPressed()) {
				long time = System.currentTimeMillis() - pressTime;
				if (time < 1000)
					controller.setLCDLine(0, " [            ] ");
				else if (time < 2000)
					controller.setLCDLine(0, " [            ] ");
				else if (time < 3000)
					controller.setLCDLine(0, " [            ] ");
				else
					controller.setLCDLine(0, " [            ] ");
				if (time > 3200) {
					powerDown();
				}
				Util.delay(200);
			}
		}

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
		lastGPSTime = 0;
		lastLCDUpdateTime = 0;

		// run the main program loop
		while (!shutdown)
			mainLoop();

		// exit the program, closing all threads
		System.exit(0);
	}
}