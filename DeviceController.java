import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import java.util.ArrayList;

/**
 * Controls devices and I/O
 */
public class DeviceController {

	// component controllers
	private GpioController gpio;
	private I2CBus i2cBus;
	private GPS gps;
	private Tone tone;
	private BMP388 sensor;
	// private Sensor sensor;
	// private SimpleMotor mixer;
	// private SimpleMotor pump;
	// private LED rgbwLED;
	// private LED statusLED;
	// private Switch titrationButton;
	// private Switch endpointButton;
	// private Switch resetButton;
	// private Stepper indicatorStepper;
	// private Stepper reagentStepper;
	// private USB usb;

	// last persistent error code that was thrown
	// private int errorStatus;

	/**
	 * Initialize components and I/O
	 * 
	 * @return True if everything initialized without fatal errors
	 */
	public boolean init() {
		if (Config.verbose)
			System.out.println("Initializing components...");
		try {
			// initialize I/O
			i2cBus = I2CFactory.getInstance(I2CBus.BUS_1);
			gpio = GpioFactory.getInstance();

			// initialize component controllers
			gps = new GPS(gpio, Config.gpsLedPin, Config.gpsSwitchPin);
			tone = new Tone(Config.piezoPin, "piezo");
			sensor = new BMP388(i2cBus);
			// usb = new USB(Config.usbPollingInterval);

			// initialize state
			// setErrorStatus(Errors.NO_ERR);

			if (Config.verbose)
				System.out.println("Done initializing components");
			return true;
		} catch (Exception e) {
			Errors.handleException(e, "Failed to initialize components");
		}
		return false;
	}

	public void shutdown() {
		if (Config.verbose)
			System.out.println("Stopping devices...");
		try {
			if (gps != null)
				gps.shutdown();
			if (gpio != null)
				gpio.shutdown();
			if (i2cBus != null)
				i2cBus.close();
		} catch (Exception e) {
			Errors.handleException(e, "Failed to gracefully stop devices");
		}
	}

	public GPSData getGPSData() {
		GPSData lastValid = gps.getLastComplete();
		if (lastValid != null)
			return lastValid;
		return gps.getLast();
	}

	public void testComponent(String... args) {
		try {
			// print GPS data
			if (args[1].equals("gps")) {
				GPSData data = getGPSData();
				if (data != null) {
					System.out.println("Last GPS data:");
					data.print();
				} else
					System.out.println("NO GPS DATA");
			}

			// test piezo
			else if (args[1].equals("tone")) {
				while (true) {
					int initial = 110;
					int max = 1024;

					for (int i = initial; i < max; i++) {
						tone.play(i);
						Util.delay(1);
					}
					for (int i = max; i > initial; i--) {
						tone.play(i);
						Util.delay(1);
					}
				}
			}

			// test pt sensor
			else if (args[1].equals("pt")) {
				while (true) {
					double[] data = sensor.getPTA();
					System.out.printf("\nTemperature: %.02f", data[1]);
					System.out.printf("\nPressure: %.02f", data[0]);
					System.out.printf("\nAltitude: %.02f\n", data[2]);
					Util.delay(500);
				}
			}

			// test all data
			else if (args[1].equals("data")) {
				double alt = 0;
				while (true) {
					double[] data = sensor.getPTA();
					System.out.printf("\nTemperature: %.02f", data[1]);
					System.out.printf("\nPressure: %.02f", data[0]);
					System.out.printf("\nAltitude: %.02f\n", data[2]);

					if (alt == 0) {
						alt = data[2];
						tone.play(0);
					} else {
						if (data[2] - alt > 3) {
							tone.play(880, 250);
						}
						else if (data[2] - alt > 2) {
							tone.play(440 + 220 * (data[2] - alt)), 150);
						}
						else tone.play(0);
					}


					GPSData gps = getGPSData();
					if (gps != null) {
						System.out.println("Last GPS data:");
						gps.print();
					} else
						System.out.println("NO GPS DATA");
					Util.delay(1000);
				}
			}

			else
				System.out.println("Invalid test");
		} catch (Exception e) {
			Errors.handleException(e, "Failed to run component test");
		}
	}
}