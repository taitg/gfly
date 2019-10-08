import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.Pin;

/**
 * Controller class for simple pins
 */
public class SimplePin {

	private GpioPinDigitalOutput pin; // GPIO pin object for the motor pin
	private String name; // name for this motor

	/**
	 * Constructor for a motor controller object
	 * 
	 * @param gpio   The GPIO controller
	 * @param name   Name for this motor
	 * @param pinNum GPIO pin number for the motor
	 */
	public SimplePin(GpioController gpio, String name, int pinNum) {
		this.name = name;

		// set up GPIO pin
		Pin pin = RaspiPin.getPinByAddress(pinNum);
		this.pin = gpio.provisionDigitalOutputPin(pin, name, PinState.LOW);
		this.pin.setShutdownOptions(true, PinState.LOW);

		if (Config.verbose)
			System.out.printf("Motor: %s ready\n", name);
	}

	/**
	 * Turn the motor on
	 */
	public void on() {
		if (Config.verbose)
			System.out.printf("Motor: %s on\n", name);
		pin.high();
	}

	/**
	 * Pulse the motor on for a specified amount of time
	 * 
	 * @param milliseconds How long to run for
	 */
	public void on(int milliseconds) {
		if (Config.verbose) {
			System.out.printf("Motor: running %s %d ms\n", name, milliseconds);
		}
		pin.high();
		Util.delay(milliseconds);
		pin.low();
	}

	/**
	 * Turn the motor off
	 */
	public void stop() {
		if (Config.verbose)
			System.out.printf("Motor: %s off\n", name);
		pin.low();
	}
}
