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
	private ArrayList<LED> leds;

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
			leds = new ArrayList<LED>();
			leds.add(new LED(gpio, "0", 4));
			leds.add(new LED(gpio, "1", 5));
			leds.add(new LED(gpio, "2", 6));
			leds.add(new LED(gpio, "3", 10));
			leds.add(new LED(gpio, "4", 11));
			leds.add(new LED(gpio, "5", 31));

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
			if (sensor != null)
				sensor.shutdown();
			if (tone != null)
				tone.shutdown();
			if (gpio != null)
				gpio.shutdown();
			if (i2cBus != null)
				i2cBus.close();
		} catch (Exception e) {
			Errors.handleException(e, "Failed to gracefully stop devices");
		}
	}

	public ArrayList<LED> getLEDs() {
		return leds;
	}

	public GPSData getGPSData() {
		GPSData lastValid = gps.getLastComplete();
		if (lastValid != null)
			return lastValid;
		return gps.getLast();
	}

	public double[] getPTA() {
		return sensor.getPTA();
	}

	public double getAltitudeChange() {
		return sensor.getAltitudeChange();
	}

	public void playTone(int freq) {
		tone.play(freq);
	}

	public void setTone(int freq) {
		tone.setFreq(freq);
	}

	public void stopTone() {
		tone.stop();
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

			else
				System.out.println("Invalid test");
		} catch (Exception e) {
			Errors.handleException(e, "Failed to run component test");
		}
	}
}