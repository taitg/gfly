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
		if (Config.verbose) System.out.println("Initializing components...");
		try {
			// initialize I/O
			i2cBus = I2CFactory.getInstance(I2CBus.BUS_1);
			gpio = GpioFactory.getInstance();
			
			// initialize component controllers
			gps = new GPS(gpio, Config.gpsLedPin, Config.gpsSwitchPin);
			// sensor = new Sensor(i2cBus, Config.sensorAddress);
			// mixer = new SimpleMotor(gpio, "mixer", Config.mixerPin);
			// pump = new SimpleMotor(gpio, "pump", Config.pumpPin);
			// rgbwLED = new LED(gpio, "rgbw", Config.rgbwPins);
			// statusLED = new LED(gpio, "status", Config.rgbPins);
			// titrationButton = new Switch(gpio, "titration", Config.titrationButtonPin);
			// endpointButton = new Switch(gpio, "endpoint", Config.endpointButtonPin);
			// resetButton = new Switch(gpio, "reset", Config.resetButtonPin);
			// indicatorStepper = new Stepper(gpio, "indicator", Config.stepper1Pins, Config.limit1Pin);
			// reagentStepper = new Stepper(gpio, "reagent", Config.stepper2Pins, Config.limit2Pin);
			// usb = new USB(Config.usbPollingInterval);
			
			// initialize state
			// setErrorStatus(Errors.NO_ERR);
			
			if (Config.verbose) System.out.println("Done initializing components");
			return true;
		}
		catch (Exception e) {
			Errors.handleException(e, "Failed to initialize components");
		}
		return false;
  }

	public void shutdown() {
		if (Config.verbose) System.out.println("Stopping devices...");
		try {
			// if (statusLED != null) statusLED.shutdown();
			// if (rgbwLED != null) rgbwLED.shutdown();
			if (gps != null) gps.shutdown();
			if (gpio != null) gpio.shutdown();
			if (i2cBus != null) i2cBus.close();
		}
		catch (Exception e) {
			Errors.handleException(e, "Failed to gracefully stop devices");
		}
  }

	public GPSData getGPSData() {
		GPSData lastValid = gps.getLastValid();
		if (lastValid != null) return lastValid;
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
				}
				else System.out.println("NO GPS DATA");
			}
			
			else System.out.println("Invalid test");
		}
		catch (Exception e) {
			Errors.handleException(e, "Failed to run component test");
		}
	}
}