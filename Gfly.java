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

	private static DeviceController controller;
	private static Track track;

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
			double[] pta = controller.getPTA();
			double temp = pta[1];
			double pressureAltitude = pta[2];
			double gpsAltitude = gps == null ? 0.0 : gps.getAltitude();
			double altitude = pressureAltitude;
			if (Config.altitudeSource == 0 && gpsAltitude > 0)
				altitude = (altitude + gpsAltitude) / 2.0;
			else if (Config.altitudeSource == 1)
				altitude = gpsAltitude;

			String line1 = String.format("%-6.1fm %5.1fkph", altitude, gps.getSpeed() * 1.852);
			String line2 = String.format("%-4.1fC %+7.1fm/s", temp, diff);
			controller.setLCDLine(0, line1);
			controller.setLCDLine(1, line2);
			System.out.printf("%s %s\n", line1, line2);

			lastLCDUpdateTime = System.currentTimeMillis();
		}
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
								if (selected == 0) controller.setLCDLine(0, "Setting tracking");
								else if (selected == 1) controller.setLCDLine(0, " Setting vario  ");
								else if (selected == 2) controller.setLCDLine(0, "Setting alt src ");
								else if (selected == 3) controller.setLCDLine(0, " Shutting down  ");
								controller.setLCDProgressBar(1, (int) time-500, 1500);
			
								// second press max time reached
								if (time > 2000) {
									if (selected == 0) {
										track.toggle();
										controller.setLCDLine(0, "  GPS TRACKING  ");
										controller.setLCDLine(1, String.format("      %s       ", track.isRunning() ? "ON " : "OFF"));
									}
									else if (selected == 1) {
										Config.varioAudioOn = !Config.varioAudioOn;
										controller.setLCDLine(0, "  VARIO AUDIO   ");
										controller.setLCDLine(1, String.format("      %s       ", Config.varioAudioOn == true ? "ON " : "OFF"));
									}
									else if (selected == 2) {
										Config.altitudeSource = (Config.altitudeSource + 1) % 3;
										controller.setLCDLine(0, "ALTITUDE SOURCE ");
										controller.setLCDLine(1, String.format("      %s       ", Config.altitudeSource == 1 ? "GPS" : Config.altitudeSource == 2 ? "PRS" : "AVG"));
									}
									else if (selected == 3) {
										powerDown();
										return;
									}

									Util.delay(1000);
									return;
								}
							}
							Util.delay(100);
						}

						// second release, if short
						if (System.currentTimeMillis() - pressTime < 1000) {
							selected = (selected + 1) % 4;
						}
						if (System.currentTimeMillis() - pressTime < 2500) {
							releaseTime = System.currentTimeMillis();
						}
					}

					// after one short press
					else {
						if (selected < 2) {
							controller.setLCDLine(0, String.format("%s TRACKING: %s ", selected == 0 ? ">" : " ", track.isRunning() ? "ON " : "OFF"));
							controller.setLCDLine(1, String.format("%s VARIO: %s    ", selected == 1 ? ">" : " ", Config.varioAudioOn == true ? "ON " : "OFF"));
						}
						else if (selected < 4) {
							controller.setLCDLine(0, String.format("%s ALT SRC: %s  ", selected == 2 ? ">" : " ", Config.altitudeSource == 1 ? "GPS" : Config.altitudeSource == 2 ? "PRS" : "AVG"));
							controller.setLCDLine(1, String.format("%s POWER DOWN    ", selected == 3 ? ">" : " "));
							// controller.setLCDLine(1, "                ");
						}
					}

					Util.delay(200);
				}
			}
		}
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
			GPSData gps = controller.getGPSData(); //handleGPSData();
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