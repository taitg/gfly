
// import com.pi4j.io.gpio.GpioController;
// import com.pi4j.io.gpio.GpioPinDigitalOutput;
// import com.pi4j.io.gpio.PinState;
// import com.pi4j.io.gpio.RaspiPin;
// import com.pi4j.io.gpio.Pin;
import com.pi4j.wiringpi.SoftTone;

/**
 * Controller class for simple tones
 */
public class Tone {

	private int pin;
	private String name;

	/**
	 * Constructor for a tone controller object
	 * 
	 * @param gpio   The GPIO controller
	 * @param name   Name for this motor
	 * @param pinNum GPIO pin number for the motor
	 */
	public Tone(int pinNum, String name) {
		this.name = name;

		// set up GPIO pin
		int success = SoftTone.softToneCreate(pinNum);

		if (Config.verbose && success != 0)
			System.out.printf("Tone: %s ready\n", name);
	}

	/**
	 * Turn the tone off
	 */
	public void stop() {
		if (Config.verbose)
			System.out.printf("Tone: %s off\n", name);
		SoftTone.softToneStop(pin);
	}

	/**
	 * Turn the tone on
	 */
	public void play(int freq) {
		play(freq, 0);
	}

	/**
	 * Turn the tone on for a number of milliseconds
	 */
	public void play(int freq, int time) {
		if (Config.verbose)
			System.out.printf("Tone: %s playing freq %d\n", name, freq);
		SoftTone.softToneWrite(pin, freq);
		if (time > 0) {
			Util.delay(time);
			stop();
		}
	}
}
