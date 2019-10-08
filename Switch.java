import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/**
 * Controls buttons or limit switches
 */
public class Switch {

	private GpioPinDigitalInput pin; // GPIO pin object for the switch
	private String name; // name for this switch
	private boolean invert; // flag for whether to invert (false: high = pressed)
	private boolean state; // flag for whether the switch is currently pressed
	private boolean wasPressed; // flag for whether the switch was pressed since the last check

	/**
	 * Constructor for a button controller object
	 * 
	 * @param gpio   GPIO controller
	 * @param name   Name for this switch
	 * @param pinNum GPIO pin number
	 */
	public Switch(GpioController gpio, String name, int pinNum) {
		this(gpio, name, pinNum, false);
	}

	/**
	 * Constructor for a switch controller object, including limit switches (allows
	 * inverted signal if invert = true)
	 * 
	 * @param gpio   GPIO controller
	 * @param name   Name for this switch
	 * @param pinNum GPIO pin number
	 * @param limit  True if this is a limit switch
	 */
	public Switch(GpioController gpio, String name, int pinNum, boolean limit) {

		// initialize state
		this.name = name;
		state = false;
		wasPressed = false;

		// determine signal inversion
		invert = false;
		PinPullResistance resistance = PinPullResistance.PULL_DOWN;
		// if (Config.invertSwitches) {
		// invert = true;
		// resistance = PinPullResistance.PULL_UP;
		// }
		// else
		if (limit)
			invert = true;

		// set up GPIO pin
		Pin pin = RaspiPin.getPinByAddress(pinNum);
		this.pin = gpio.provisionDigitalInputPin(pin, name, resistance);
		this.pin.setShutdownOptions(true);

		// register a listener for state change events
		// this runs in its own thread
		this.pin.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				handleEvent(event);
			}
		});
		if (Config.verbose)
			System.out.printf("Switch: %s ready\n", name);
	}

	/**
	 * Returns the CURRENT state at time of check
	 * 
	 * @return True if switch is activated
	 */
	public boolean isPressed() {
		return state;
	}

	/**
	 * Returns whether the switch was pressed since the last check
	 * 
	 * @return True if switch was activated
	 */
	public boolean wasPressed() {
		boolean result = state || wasPressed;
		wasPressed = false;
		if (Config.verbose && result)
			System.out.printf("Switch: %s was pressed\n", name);
		return result;
	}

	/**
	 * Reset the was-pressed state
	 */
	public void reset() {
		wasPressed = false;
	}

	/**
	 * Process a state change event
	 * 
	 * @param event Event to handle
	 */
	private void handleEvent(GpioPinDigitalStateChangeEvent event) {
		if (invert)
			state = event.getState().isLow();
		else
			state = event.getState().isHigh();
		if (state)
			wasPressed = true;
	}
}
