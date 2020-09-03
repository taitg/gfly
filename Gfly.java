import java.util.Scanner;

public class Gfly {

	// system states (and test codes)
	public static final int DEV_COMMAND = -1;
	public static final int WAITING = 0;

	private static int state; // current system state
	private static boolean shutdown; // flag for whether the program should shut down
	private static boolean acceptingCommands; // flag for whether the program should take commands

	private static DeviceController controller;
	private static WebServer server;
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
					Scanner scanner = new Scanner(System.in);
					input = scanner.nextLine();
					args = input.split(" ");
					scanner.close();
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

		// if (Config.verbose)
		// 	System.out.println(String.format("Alt diff %f", diff));

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
		if (controller.getRedButton().isPressed()) {
			long pressTime = System.currentTimeMillis();
			while (controller.getRedButton().isPressed()) {
				Util.delay(100);
				if (System.currentTimeMillis() - pressTime > 3000) {
					powerDown();
					break;
				}
			}
		} else if (controller.getYellowButton().wasPressed()) {
			if (!Config.varioAudioOn)
				controller.setTone(880);
			Config.varioAudioOn = !Config.varioAudioOn;
			if (Config.varioAudioOn)
				controller.getYellowLed().on();
			else
				controller.getYellowLed().off();
			Util.delay(500);
		} else if (controller.getGreenButton().isPressed()) {
			long pressTime = System.currentTimeMillis();
			while (controller.getGreenButton().isPressed()) {
				Util.delay(100);
				if (System.currentTimeMillis() - pressTime > 3000) {
					if (Config.varioAudioOn) {
						if (track.isRunning())
							controller.setTone(220);
						else
							controller.setTone(880);
					}
					track.toggle();
					break;
				}
			}
		}
	}

	private static void updateIndicators() {
		if (!controller.getGPS().hasFix()) {
			controller.getRedLed().clear();
			controller.getRedLed().flash(1);
		} else {
			controller.getRedLed().on();
		}

		if (track.isRunning()) {
			controller.getGreenLed().clear();
			controller.getGreenLed().flash(1);
		} else {
			controller.getGreenLed().off();
		}
	}

	private static void shutDown() {
		if (Config.varioAudioOn)
			controller.playTone(110);
		Util.delay(500);
		server.shutdown();
		controller.shutdown();
	}

	private static void powerDown() {
		try {
			shutDown();
			Process p = Runtime.getRuntime().exec("sudo shutdown -h now");
			p.waitFor();
		} catch (Exception e) {
			Errors.handleException(e, "could not shut down!");
		}
	}

	private static void init() {
		controller = new DeviceController();
		if (!controller.init())
			System.exit(-1);

		track = new Track(controller);

		server = new WebServer(controller, Config.serverPort);
		server.init();
	}

	private static void mainLoop() {
		try {
			handleDevCommand();
			handleAltitudeChange();
			handleButtonInput();
			updateIndicators();

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

		init();

		// add a shutdown hook so that the application can trap a Ctrl-C and
		// handle it gracefully by ensuring that all components are properly shut down
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (Config.verbose)
					System.out.println("\nShutting down...");
				shutDown();
			}
		});

		// initialize the program state
		shutdown = false;
		state = WAITING;
		if (Config.devMode)
			acceptingCommands = true;

		// run the main program loop
		while (!shutdown)
			mainLoop();

		// exit the program, closing all threads
		System.exit(0);
	}
}