import com.pi4j.wiringpi.SoftTone;

/**
 * Controller class for simple tones
 */
public class Tone {

	private int pinNum;
	private String name;

	/**
	 * Constructor for a tone controller object
	 */
	public Tone(int pinNum, String name) {
		this.pinNum = pinNum;
		this.name = name;

		// initialize wiringPi library
		com.pi4j.wiringpi.Gpio.wiringPiSetup();

		// set up GPIO pin
		int success = SoftTone.softToneCreate(pinNum);

		if (Config.verbose) {
			if (success == 0) System.out.printf("Tone: %s ready\n", name);
			else System.out.printf("Tone: %s failed to initialize\n", name);
		}
	}

	/**
	 * Turn the tone off
	 */
	public void stop() {
		if (Config.verbose)
			System.out.printf("Tone: %s off\n", name);
		SoftTone.softToneStop(pinNum);
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

		SoftTone.softToneWrite(pinNum, freq);

		if (time > 0) {
			Util.delay(time);
			SoftTone.softToneWrite(pinNum, 0);
		}
	}
}
