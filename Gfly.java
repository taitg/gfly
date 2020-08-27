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

	private static DeviceController controller;
	private static Track track;

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

		System.out.println(String.format("Alt diff %f", diff));

		if (Config.varioAudioOn && diff > Config.varioClimbThreshold) {
			controller.setTone(Config.varioClimbBaseFreq + (int) (Config.varioClimbDiffFreq * diff));
		} else if (Config.varioAudioOn && diff < Config.varioSinkThreshold) {
			controller.setTone(Config.varioSinkBaseFreq + (int) (Config.varioSinkDiffFreq * diff));
		} else {
			controller.setTone(0);
		}

		return diff;
	}

	private static void handleButtonInput() {
		if (controller.getButton().isPressed()) {
			long pressTime = System.currentTimeMillis();
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

			handleAltitudeChange();

			handleButtonInput();

			Util.delay(Config.mainLoopDelay);
		} catch (Exception e) {
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

		// run the main program loop
		while (!shutdown)
			mainLoop();

		// exit the program, closing all threads
		System.exit(0);
	}
}