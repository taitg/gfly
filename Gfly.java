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

	private static int state; // current system state
	private static boolean shutdown; // flag for whether the program should shut down
	private static boolean acceptingCommands; // flag for whether the program should take commands
	private static long lastGPSTime;
	private static long lastDistanceTime;
	private static long lastLCDUpdateTime;

	private static DeviceController controller;
	private static Track track;
	private static double distance;
	private static GPSData gpsDelta; // for distance calculation
	private static GPSData gpsOrigin;
	private static PTAData ptaOrigin;

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

		if (Config.varioAudioOn && diff > 0.5) {
			controller.setTone(440 + (int) (440 * diff));
		} else if (Config.varioAudioOn && diff < -0.5) {
			controller.setTone(220 + (int) (110 * diff));
		} else {
			controller.setTone(0);
		}

		return diff;
	}

	private static void updateLCD(GPSData gps, double diff) {
		if (System.currentTimeMillis() - lastLCDUpdateTime > 200) {
			PTAData pta = controller.getPTA();

			double pressureAltitude = pta.getAltitude();
			double gpsAltitude = gps == null ? 0.0 : gps.getAltitude();

			double altitude = pressureAltitude;
			double altitudeOrigin = ptaOrigin != null ? ptaOrigin.getAltitude() : 0.0;

			if (Config.altitudeSource == 0) {
				altitude = (altitude + gpsAltitude) / 2.0;
				if (gpsOrigin != null)
					altitudeOrigin = (altitudeOrigin + gpsOrigin.getAltitude()) / 2.0;
			}
			else if (Config.altitudeSource == 1) {
				altitude = gpsAltitude;
				if (gpsOrigin != null)
					altitudeOrigin = gpsOrigin.getAltitude();
			}

			double speed = gps.getSpeedKMH();

			String line1 = "";
			String line2 = "";
			
			if (Config.mode == 0) {
				double temp = pta.getTemperature();
				line1 = String.format("%-7.1fm %4.1fkph", altitude, speed);
				line2 = String.format("%-4.1fC %+7.1fm/s", temp, diff);
			}
			else if (Config.mode == 1) {
				double elevationGain = altitude - altitudeOrigin;
				String direction = Util.headingToString(gps == null ? 0.0 : gps.getTrackingAngle());
				double distanceDirect = 0.0;
				// if (gps != null && gpsOrigin != null) {
				// 	distanceDirect = Util.vincentyDistance(gpsOrigin.getLatitude(), gpsOrigin.getLongitude(), gps.getLatitude(), gps.getLongitude());
				// }
				line1 = String.format("%6.2fkm %4.1fkph", distance, speed);
				line2 = String.format("%+7.1fm    %s ", elevationGain, direction);
			}

			controller.setLCDLines(line1, line2);
			
			// System.out.printf("%s %s\n", gpsOrigin.getLatitude(), gpsOrigin.getLongitude());
			// System.out.printf("%s %s\n", gps.getLatitude(), gps.getLongitude());
			// System.out.println(Util.gpsDistance(gpsOrigin.getLatitude(), gpsOrigin.getLongitude(), gps.getLatitude(), gps.getLongitude()) * 1000);
			System.out.printf("%s %s\n", line1, line2);

			lastLCDUpdateTime = System.currentTimeMillis();
		}
	}

	private static void displayMenu(int selected) {
		if (selected < 2) {
			String line1 = String.format("%s TRACKING: %s ", selected == 0 ? ">" : " ", track.isRunning() ? "ON " : "OFF");
			String line2 = String.format("%s MODE: %s    ", selected == 1 ? ">" : " ", Config.mode == 0 ? "FLY " : "HIKE");
			controller.setLCDLines(line1, line2);
		}
		else if (selected < 4) {
			String line1 = String.format("%s ALT SRC: %s  ", selected == 2 ? ">" : " ", Config.altitudeSource == 1 ? "GPS" : Config.altitudeSource == 2 ? "PRS" : "AVG");
			String line2 = String.format("%s VARIO: %s    ", selected == 3 ? ">" : " ", Config.varioAudioOn == true ? "ON " : "OFF");
			controller.setLCDLines(line1, line2);
		}
		else if (selected < 6) {
			String line1 = String.format("%s RESET ORIGIN  ", selected == 4 ? ">" : " ");
			String line2 = String.format("%s POWER DOWN    ", selected == 5 ? ">" : " ");
			controller.setLCDLines(line1, line2);
		}
	}

	private static int getNextSelection(int selected) {
		return (selected + 1) % 6;
	}

	private static void displayProgress(int selected, long time) {
		String progressTitle = "";
		if (selected == 0) progressTitle = "Setting tracking";
		else if (selected == 1) progressTitle = "  Setting mode  ";
		else if (selected == 2) progressTitle = "Setting alt src ";
		else if (selected == 3) progressTitle = " Setting vario  ";
		else if (selected == 4) progressTitle = "Resetting origin";
		else if (selected == 5) progressTitle = " Shutting down  ";
		controller.setLCDProgress(progressTitle, (int) time-500, 1500);
	}

	private static void handleSelection(int selected) {
		if (selected == 0) {
			track.toggle();
			String line2 = String.format("      %s       ", track.isRunning() ? "ON " : "OFF");
			controller.setLCDLines("  GPS TRACKING  ", line2);
		}
		else if (selected == 1) {
			Config.mode = (Config.mode + 1) % 2;
			String line2 = String.format("      %s      ", Config.mode == 0 ? "FLY " : "HIKE");
			controller.setLCDLines("      MODE      ", line2);
		}
		else if (selected == 2) {
			Config.altitudeSource = (Config.altitudeSource + 1) % 3;
			String line2 = String.format("      %s       ", Config.altitudeSource == 1 ? "GPS" : Config.altitudeSource == 2 ? "PRS" : "AVG");
			controller.setLCDLines("ALTITUDE SOURCE ", line2);
		}
		else if (selected == 3) {
			Config.varioAudioOn = !Config.varioAudioOn;
			String line2 = String.format("      %s       ", Config.varioAudioOn == true ? "ON " : "OFF");
			controller.setLCDLines("  VARIO AUDIO   ", line2);
		}
		else if (selected == 4) {
			resetOrigin();
		}
		else if (selected == 5) {
			powerDown();
			return;
		}
		Util.delay(1000);
	}

	private static void handleButtonInput() {
		int selected = 0;

		// first press
		if (controller.getButton().isPressed()) {
			long pressTime = System.currentTimeMillis();
			
			// repeat until first release
			while (controller.getButton().isPressed()) {
				Util.delay(200);
			}

			// first release, if short
			if (System.currentTimeMillis() - pressTime < 1000) {
				long releaseTime = System.currentTimeMillis();

				// repeat until release timeout
				while (System.currentTimeMillis() - releaseTime < 5000) {

					// second press
					if (controller.getButton().isPressed()) {
						pressTime = System.currentTimeMillis();
			
						// repeat until second release
						while (controller.getButton().isPressed()) {
							long time = System.currentTimeMillis() - pressTime;

							if (time > 500) {
								displayProgress(selected, time);
			
								// second press max time reached
								if (time > 2000) {
									handleSelection(selected);
									return;
								}
							}
							Util.delay(100);
						}

						// second release, if short
						if (System.currentTimeMillis() - pressTime < 1000) {
							selected = getNextSelection(selected);
						}
						if (System.currentTimeMillis() - pressTime < 2500) {
							releaseTime = System.currentTimeMillis();
						}
					}

					// after one short press
					else {
						displayMenu(selected);
					}

					Util.delay(200);
				}
			}
		}
	}

	private static void resetOrigin() {
		distance = 0.0;
		gpsDelta = null;
		gpsOrigin = null;
		ptaOrigin = null;
	}

	private static void powerDown() {
		try {
			controller.shutdown();
			Process p = Runtime.getRuntime().exec("sudo shutdown -h now");
			p.waitFor();
		} catch (Exception e) {
			Errors.handleException(e, "could not shut down!");
		}
	}

	private static void mainLoop() {
		try {
			handleDevCommand();

			GPSData gps = controller.getGPSData();

			if (gpsDelta == null && gps.isComplete()) {
				gpsDelta = gps;
				lastDistanceTime = System.currentTimeMillis();
			}

			if (lastDistanceTime > 0 && System.currentTimeMillis() - lastDistanceTime > 60000) {
				double distanceDelta = Util.vincentyDistance(gpsDelta.getLatitude(), gpsDelta.getLongitude(), gps.getLatitude(), gps.getLongitude());
				distance += distanceDelta;
				gpsDelta = gps;
				lastDistanceTime = System.currentTimeMillis();
			}

			if (gpsOrigin == null && gps.isComplete())
				gpsOrigin = gps;

			if (ptaOrigin == null)
				ptaOrigin = controller.getPTA();

			double diff = handleAltitudeChange();

			updateLCD(gps, diff);

			handleButtonInput();

			Util.delay(Config.mainLoopDelay);
		}
		catch (Exception e) {
			Errors.handleException(e, "exception in main loop");
		}
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

		track = new Track(controller);
		distance = 0.0;
		gpsDelta = null;
		gpsOrigin = null;
		ptaOrigin = null;

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
		lastDistanceTime = 0;
		lastLCDUpdateTime = 0;

		// run the main program loop
		while (!shutdown)
			mainLoop();

		// exit the program, closing all threads
		System.exit(0);
	}
}