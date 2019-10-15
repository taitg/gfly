import com.pi4j.wiringpi.SoftTone;

/**
 * Controller class for simple tones
 */
public class Tone {

	private ToneWorker workerThread;
	private int pinNum;
	private String name;
	private int freq;
	private boolean playing;

	/**
	 * Constructor for a tone controller object
	 */
	public Tone(int pinNum, String name) {
		this.pinNum = pinNum;
		this.name = name;
		playing = false;
		freq = 0;

		// set up GPIO pin
		int success = SoftTone.softToneCreate(pinNum);

		startWorker();

		if (Config.verbose) {
			if (success == 0)
				System.out.printf("Tone: %s ready\n", name);
			else
				System.out.printf("Tone: %s failed to initialize\n", name);
		}
	}

	/**
	 * Shut down the controller
	 */
	public void shutdown() {
		if (workerThread != null)
			workerThread.shutdown();
	}

	/**
	 * Start worker thread
	 */
	private void startWorker() {
		workerThread = new ToneWorker();
		(new Thread(workerThread)).start();
		if (Config.verbose)
			System.out.printf("Tone: %s worker ready\n", name);
	}

	/**
	 * Turn the tone off
	 */
	public void stop() {
		if (Config.verbose)
			System.out.printf("Tone: %s off\n", name);
		SoftTone.softToneStop(pinNum);
		playing = false;
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
		if (freq > 0) {
			if (Config.verbose)
				System.out.printf("Tone: %s playing freq %d for %dms\n", name, freq, time);
			SoftTone.softToneWrite(pinNum, Math.min(Math.max(freq, 110), 3520));
		} else
			SoftTone.softToneWrite(pinNum, 0);

		playing = freq > 0;

		if (time > 0) {
			Util.delay(time);
			play(0);
		}
	}

	public void setFreq(int freq) {
		this.freq = freq;
	}

	/**
	 * Worker thread class
	 */
	public class ToneWorker implements Runnable {

		// flag for whether the worker should shut down
		private boolean shutdown;

		/**
		 * Constructor
		 */
		public ToneWorker() {
			shutdown = false;
		}

		/**
		 * Main worker loop
		 */
		@Override
		public void run() {
			while (!shutdown) {
				if (freq > 0 && !playing) {
					int pulseTime = 1 + (int) (200000.0 / freq);
					int offTime = 1 + (int) (pulseTime / 2.0);
					long pulseEndTime = System.currentTimeMillis() + pulseTime;

					while (System.currentTimeMillis() < pulseEndTime) {
						play(freq);
						Util.delay(10);
					}
					freq = 0;
					play(0);
					Util.delay(offTime);
				} else
					Util.delay(200);
			}
		}

		/**
		 * Shut down the worker
		 */
		public void shutdown() {
			shutdown = true;
		}
	}
}
