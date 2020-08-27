import com.pi4j.wiringpi.SoftTone;
import java.util.ArrayList;

/**
 * Controller class for simple tones
 */
public class Tone {

	private ToneWorker workerThread;
	private int pinNum;
	private String name;
	private boolean playing;
	private ArrayList<Integer> queue;

	/**
	 * Constructor for a tone controller object
	 */
	public Tone(int pinNum, String name) {
		this.pinNum = pinNum;
		this.name = name;
		playing = false;
		queue = new ArrayList<>();

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
				System.out.printf("Tone: %s playing freq %d\n", name, freq);
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
		queue.add(new Integer(freq));
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
				if (queue.size() > 0) {
					// use and remove last freq in queue
					int freq = (int) queue.get(queue.size() - 1);
					queue.remove(queue.size() - 1);

					if (freq > 0) {
						int pulseTime = 1 + (int) (100_000.0 / freq);
						long pulseEndTime = System.currentTimeMillis() + pulseTime;

						if (Config.verbose)
							System.out.println(String.format("Pulse %dms\n>> Off %dms", pulseTime, pulseTime));

						// play freq until time that pulse should stop
						while (System.currentTimeMillis() < pulseEndTime) {
							play(freq);
							if (queue.size() > 1) {
								freq = (int) queue.get(queue.size() - 1);
								queue.remove(queue.size() - 1);
							}
							// Util.delay(1);
						}

						// stop sound
						play(0);

						// remove all freqs except last from queue
						int remaining = queue.size();
						if (remaining > 1) {
							for (int i = 0; i < remaining - 1; i++) {
								queue.remove(0);
							}
						}

						// delay until next pulse should start
						// int offTime = 1 + (int) (pulseTime * 0.75);
						Util.delay(pulseTime);
					}
				}
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
