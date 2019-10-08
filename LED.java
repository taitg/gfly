import java.util.ArrayList;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.Pin;

/**
 * Controller for LEDs with 1, 3 or 4 pins.
 * 
 * Has a queue for LED events (either set or flash). Functions "on", "off", and "flash" add the
 * corresponding event to the queue as one of the colour codes. A worker thread constantly processes
 * the queue in the background while the rest of the program is running. That way the program 
 * doesn't sit and wait while it flashes the LED. If multiple flash commands come in at the same
 * time, they will happen one after the other in the order that they came in.
 *
 * PI4J includes functions like pulse() and blink() which should have done all of this in 
 * a much simpler way, but they turned out to be unreliable in that the LEDs wouldn't consistently
 * flash at the same time. Trying to flash white (RGB) would noticeably turn red on first, then
 * green, then blue, instead of all of them simultaneously.
 */
public class LED {
	
	// maximum value for an LED
	// in the future LED brightness could potentially be controllable by software PWM
	private static final int PIN_MAX = 1;
	
	// colour codes
	public static final int OFF    = -1;
	public static final int RED     = 0; // needs to be 0
	public static final int GREEN   = 1; // needs to be 1
	public static final int BLUE    = 2; // needs to be 2
	public static final int WHITE   = 3; // needs to be 3
	public static final int YELLOW  = 4;
	public static final int MAGENTA = 5;
	public static final int CYAN    = 6;
	public static final int ALL     = 7;
	
	// flashing colour codes: need to be colour+10
	// not for use outside of the class: use the flash function instead
	private static final int RED_FLASH     = 10;
	private static final int GREEN_FLASH   = 11;
	private static final int BLUE_FLASH    = 12;
	private static final int WHITE_FLASH   = 13;
	private static final int YELLOW_FLASH  = 14;
	private static final int MAGENTA_FLASH = 15;
	private static final int CYAN_FLASH    = 16;
	private static final int ALL_FLASH     = 17;
	
	private ArrayList<GpioPinDigitalOutput> pins; // list of GPIO pins
	private ArrayList<Integer> queue; // LED event queue (of colour codes)
	private LEDWorker workerThread; // handles then removes events from the queue (set or flash)
	private int status; // last solid colour (or OFF) the LED was set to - returns to this after flashing
	private String name; // name of this LED
		
	/**
	 * Constructor for a 1 pin LED controller object
	 * 
	 * @param gpio GPIO controller
	 * @param name Name of this LED
	 * @param pinNum GPIO pin number
	 */
	public LED(GpioController gpio, String name, int pinNum) {
		// initialize state
		init(name);

		// set up GPIO pin
		Pin pin = RaspiPin.getPinByAddress(pinNum);
		pins.add(gpio.provisionDigitalOutputPin(pin, name, PinState.LOW));		
		pins.get(0).setShutdownOptions(true, PinState.LOW);
		
		// start worker thread
		startWorker();
	}
		
	/**
	 * Constructor for a multiple (3 or 4) pin LED controller object
	 * 
	 * @param gpio GPIO controller
	 * @param name Name of this LED
	 * @param pinNums Array of GPIO pin numbers
	 */
	public LED(GpioController gpio, String name, int[] pinNums) {
		if (pinNums.length < 3) return;
		
		// initialize state
		init(name);

		// set up GPIO pins
		Pin pinR = RaspiPin.getPinByAddress(pinNums[0]);
		Pin pinG = RaspiPin.getPinByAddress(pinNums[1]);
		Pin pinB = RaspiPin.getPinByAddress(pinNums[2]);
		
		pins.add(gpio.provisionDigitalOutputPin(pinR, name + "Red", PinState.LOW));
		pins.add(gpio.provisionDigitalOutputPin(pinG, name + "Green", PinState.LOW));
		pins.add(gpio.provisionDigitalOutputPin(pinB, name + "Blue", PinState.LOW));
		
		if (pinNums.length >= 4) {
			Pin pinW = RaspiPin.getPinByAddress(pinNums[3]);
			pins.add(gpio.provisionDigitalOutputPin(pinW, name + "White", PinState.LOW));
		}
		
		for (GpioPinDigitalOutput pin : pins) {
			pin.setShutdownOptions(true, PinState.LOW);
		}
		
		// start worker thread
		startWorker();
	}
		
	/**
	 * Shut down the controller
	 */
	public void shutdown() {
		if (workerThread != null) workerThread.shutdown();
	}
	
	/**
	 * Full on
	 */
	public void on() {
		on(ALL);
	}
		
	/**
	 * Set to a colour
	 * 
	 * @param colourCode Colour code
	 */
	public void on(int colourCode) {
		if (status != colourCode) {
			if (Config.verbose) System.out.printf("LED: %s => %s\n", name, getColourName(colourCode));
			queue.add(new Integer(colourCode));
			status = colourCode;
		}
	}
		
	/**
	 * Turn off
	 */
	public void off() {
		if (status != OFF) {
			if (Config.verbose) System.out.printf("LED: %s => off\n", name);
			queue.add(new Integer(OFF));
			status = OFF;
		}
	}
	
	/**
	 * Flash full-on a number of times
	 * 
	 * @param numTimes Number of times to flash
	 */
	public void flash(int numTimes) {
		flash(ALL, numTimes);
	}
		
	/**
	 * Flash a colour a number of times
	 * 
	 * @param colourCode Colour code
	 * @param numTimes Number of times to flash
	 */
	public void flash(int colourCode, int numTimes) {
		if (Config.verbose) {
			System.out.printf("LED: %s => flash %s x %d\n", name, getColourName(colourCode), numTimes);
		}
		for (int i = 0; i < numTimes; i++) {
			queue.add(new Integer(colourCode + 10));
		}
	}
		
	/**
	 * Flash each colour once in a rainbow sequence
	 * 
	 * @param numTimes Number of times to do sequence
	 */
	public void rainbow(int numTimes) {
		for (int i = 0; i < numTimes; i++) {
			flash(RED, 1);
			flash(YELLOW, 1);
			flash(GREEN, 1);
			flash(CYAN, 1);	
			flash(BLUE, 1);
			flash(MAGENTA, 1);
		}
	}
	
	/**
	 * Get the name of a colour from its code
	 * (used for console output)
	 * 
	 * @param colourCode Colour code
	 * @return Name of the colour
	 */
	public static String getColourName(int colourCode) {
		switch (colourCode) {
			case OFF: return "off";
			case RED: return "red";
			case GREEN: return "green";
			case BLUE: return "blue";
			case WHITE: return "white";
			case YELLOW: return "yellow";
			case MAGENTA: return "magenta";
			case CYAN: return "cyan";
			case ALL: return "full on";
		}
		return "unknown";
	}
		
	/**
	 * Initialize state
	 * 
	 * @param name Name of the LED
	 */
	private void init(String name) {
		this.name = name;
		pins = new ArrayList<>();
		queue = new ArrayList<>();
		status = OFF;
	}
		
	/**
	 * Start worker thread
	 */
	private void startWorker() {
		workerThread = new LEDWorker();
		(new Thread(workerThread)).start();
		if (Config.verbose) System.out.printf("LED: %s ready\n", name);
	}
	
	/**
	 * Set colour using code
	 * 
	 * @param colourCode Colour code
	 */
	private void set(int colourCode) {
		switch (colourCode) {
			case OFF:     set(0, 0, 0, 0); break;
			case RED:     set(PIN_MAX, 0, 0, 0); break;
			case GREEN:   set(0, PIN_MAX, 0, 0); break;
			case BLUE:    set(0, 0, PIN_MAX, 0); break;
			case YELLOW:  set(PIN_MAX, PIN_MAX, 0, 0); break;
			case MAGENTA: set(PIN_MAX, 0, PIN_MAX, 0); break;
			case CYAN:    set(0, PIN_MAX, PIN_MAX, 0); break;
			case WHITE: 
				if (pins.size() > 3) set(0, 0, 0, PIN_MAX);
				else set(PIN_MAX, PIN_MAX, PIN_MAX, 0);
				break;
			case ALL:
				set(PIN_MAX, PIN_MAX, PIN_MAX, PIN_MAX); break;
			default:
				off();
				break;
		}
	}
		
	/**
	 * Set colour using individual values
	 * 
	 * @param r Red value
	 * @param g Green value
	 * @param b Blue value
	 * @param w White value
	 */
	private void set(int r, int g, int b, int w) {		
		if (r == 0) pins.get(RED).low();
		else pins.get(RED).high();
		
		if (pins.size() < 3) return;
		
		if (g == 0) pins.get(GREEN).low();
		else pins.get(GREEN).high();
		
		if (b == 0) pins.get(BLUE).low();
		else pins.get(BLUE).high();
		
		if (pins.size() < 4) return;
		
		if (w == 0) pins.get(WHITE).low();
		else pins.get(WHITE).high();
	}
	
	/**
	 * Worker thread class
	 */
	public class LEDWorker implements Runnable {
		
		// flag for whether the worker should shut down
		private boolean shutdown;
	 	
		/**
		 * Constructor
		 */
		public LEDWorker() {
			shutdown = false;
		}
			
		/**
		 * Main worker loop
		 */
		@Override
		public void run() {
			while (!shutdown) {
				processQueue();
				Util.delay(Config.ledRefreshTime);
			}
		}
			
		/**
		 * Shut down the worker
		 */
		public void shutdown() {
			shutdown = true;
		}
	 	
		/**
		 * Process the LED's queue
		 */
		private void processQueue() {
			if (queue.size() > 0) {
				switch (queue.get(0).intValue()) {
					case OFF:     processSet(OFF); break;
					case ALL:     processSet(ALL); break;
					case RED:     processSet(RED); break;
					case GREEN:   processSet(GREEN); break;
					case BLUE:    processSet(BLUE); break;
					case WHITE:   processSet(WHITE); break;
					case YELLOW:  processSet(YELLOW); break;
					case MAGENTA: processSet(MAGENTA); break;
					case CYAN:    processSet(CYAN); break;
					case ALL_FLASH:     processFlash(ALL); break;
					case RED_FLASH:     processFlash(RED); break;
					case GREEN_FLASH:   processFlash(GREEN); break;
					case BLUE_FLASH:    processFlash(BLUE); break;
					case WHITE_FLASH:   processFlash(WHITE); break;
					case YELLOW_FLASH:  processFlash(YELLOW); break;
					case MAGENTA_FLASH: processFlash(MAGENTA); break;
					case CYAN_FLASH:    processFlash(CYAN); break;
					default: break;
				}
			}
		}
			
		/**
		 * Handle a SET from the queue
		 * 
		 * @param colourCode Colour code
		 */
		private void processSet(int colourCode) {
			set(colourCode);
			status = colourCode;
			queue.remove(0);
		}
			
		/**
		 * Handle a FLASH from the queue
		 * 
		 * @param colourCode Colour code
		 */
		private void processFlash(int colourCode) {
			set(colourCode);
			Util.delay(Config.ledFlashTime);
			set(OFF);
			Util.delay(Config.ledFlashOffTime);
			queue.remove(0);
			if (queue.size() == 0) set(status);
		}
	}
}
