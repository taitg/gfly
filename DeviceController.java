import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;

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

	private SimplePin redButtonPower;
	private Switch redButton;
	private LED redButtonLed;
	private SimplePin yellowButtonPower;
	private Switch yellowButton;
	private LED yellowButtonLed;
	private SimplePin greenButtonPower;
	private Switch greenButton;
	private LED greenButtonLed;

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
			redButtonLed = new LED(gpio, "redLed", Config.redButtonLedPin);
			tone = new Tone(Config.piezoPin, "piezo");

			redButtonLed.flash(2);
			if (Config.varioAudioOn)
				tone.setFreq(110);

			sensor = new BMP388(i2cBus);
			yellowButtonLed = new LED(gpio, "yellowLed", Config.yellowButtonLedPin);

			yellowButtonLed.flash(2);
			if (Config.varioAudioOn)
				tone.setFreq(220);

			gps = new GPS(gpio, Config.gpsLedPin, Config.gpsSwitchPin);
			greenButtonLed = new LED(gpio, "greenLed", Config.greenButtonLedPin);

			greenButtonLed.flash(2);
			if (Config.varioAudioOn)
				tone.setFreq(440);

			redButton = new Switch(gpio, "redButton", Config.redButtonInPin);
			redButtonPower = new SimplePin(gpio, "redButtonPower", Config.redButtonOutPin);
			yellowButton = new Switch(gpio, "yellowButton", Config.yellowButtonInPin);
			yellowButtonPower = new SimplePin(gpio, "yellowButtonPower", Config.yellowButtonOutPin);
			greenButton = new Switch(gpio, "greenButton", Config.greenButtonInPin);
			greenButtonPower = new SimplePin(gpio, "greenButtonPower", Config.greenButtonOutPin);

			redButtonPower.on();
			yellowButtonPower.on();
			greenButtonPower.on();

			greenButtonLed.flash(2);
			yellowButtonLed.flash(2);
			redButtonLed.flash(2);

			redButtonLed.on();
			if (!Config.varioAudioOn)
				yellowButtonLed.off();
			else
				yellowButtonLed.on();
			greenButtonLed.off();

			if (Config.varioAudioOn)
				tone.setFreq(880);

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
			if (redButtonLed != null)
				redButtonLed.shutdown();
			if (yellowButtonLed != null)
				yellowButtonLed.shutdown();
			if (greenButtonLed != null)
				greenButtonLed.shutdown();
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

	public GPSData getGPSData() {
		GPSData lastValid = gps.getLastComplete();
		if (lastValid != null)
			return lastValid;
		return gps.getLast();
	}

	public PTAData getPTA() {
		return sensor.getLastData();
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

	public Switch getRedButton() {
		return redButton;
	}

	public LED getRedLed() {
		return redButtonLed;
	}

	public Switch getYellowButton() {
		return yellowButton;
	}

	public LED getYellowLed() {
		return yellowButtonLed;
	}

	public Switch getGreenButton() {
		return greenButton;
	}

	public LED getGreenLed() {
		return greenButtonLed;
	}

	public GPS getGPS() {
		return gps;
	}

	public BMP388 getSensor() {
		return sensor;
	}

	public Status getStatus() {
		return new Status(this);
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
					int max = 2048;

					for (int i = initial; i < max; i++) {
						playTone(i);
						Util.delay(1);
					}
					for (int i = max; i > initial; i--) {
						playTone(i);
						Util.delay(1);
					}
				}
			}

			else if (args[1].equals("beep")) {
				for (int i = 0; i < 40; i++) {
					setTone(Integer.parseInt(args[2]));
					Util.delay(100);
				}
			}

			// test pt sensor
			else if (args[1].equals("pt")) {
				while (true) {
					PTAData data = sensor.getPTA();
					System.out.printf("\nTemperature: %.02f", data.getTemperature());
					System.out.printf("\nPressure: %.02f", data.getPressure());
					System.out.printf("\nAltitude: %.02f\n", data.getAltitude());
					Util.delay(500);
				}
			}

			else
				System.out.println("Invalid test");
		} catch (Exception e) {
			Errors.handleException(e, "Failed to run component test");
		}
	}

	public class Status {

		private String date;
		private String time;
		private double altitude;
		private double latitude;
		private double longitude;
		private double heading;
		private double speed;
		private double pressure;
		private double pressureAltitude;
		private double verticalSpeed;
		private double temperature;

		public Status(DeviceController controller) {
			GPSData gps = controller.getGPSData();
			if (gps != null) {
				date = gps.getDate();
				time = gps.getTime();
				altitude = gps.getAltitude();
				latitude = gps.getLatitude();
				longitude = gps.getLongitude();
				heading = gps.getTrackingAngle();
				speed = gps.getSpeedKMH();
			} else {
				date = "";
				time = "";
				altitude = 0;
				latitude = 0;
				longitude = 0;
				heading = 0;
				speed = 0;
			}
			PTAData pta = controller.getPTA();
			if (pta != null) {
				pressure = pta.getPressure();
				pressureAltitude = pta.getAltitude();
				verticalSpeed = controller.getAltitudeChange();
				temperature = pta.getTemperature();
			} else {
				pressure = 0;
				pressureAltitude = 0;
				verticalSpeed = 0;
				temperature = 0;
			}
		}
	}
}