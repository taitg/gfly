import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Track {

	private TrackWorker workerThread;
	private DeviceController controller;

	private boolean running;
	private long lastGPSTime;
	private String filename;
	private GPSData initial;

	private double maxSpeed;
	private double maxAltitude;
	private double minAltitude;
	private double maxPressureAltitude;
	private double minPressureAltitude;
	private double maxClimb;
	private double maxSink;

	public Track(DeviceController controller) {
		if (controller != null) {
			this.controller = controller;
			running = false;
			lastGPSTime = 0;
			initial = controller.getGPSData();
			maxSpeed = 0;
			maxAltitude = 0;
			maxPressureAltitude = 0;
			maxClimb = 0;
			maxSink = 0;

			startWorker();

			if (Config.verbose)
				System.out.println("Track: ready");
		} else if (Config.verbose)
			System.out.println("Track: failed to initialize");
	}

	/**
	 * Shut down the controller
	 */
	public void shutdown() {
		if (workerThread != null)
			workerThread.shutdown();
	}

	public void run() {
		try {
			String header = "type,latitude,longitude,alt,speed,course\n";
			filename = String.format("gps_%d.txt", System.currentTimeMillis());

			Files.write(Paths.get(filename), header.getBytes(StandardCharsets.UTF_8),
					Files.exists(Paths.get(filename)) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);

			running = true;
		} catch (Exception e) {
			Errors.handleException(e, "Failed to start tracking");
		}
	}

	public void stop() {
		running = false;
	}

	public void toggle() {
		if (running)
			stop();
		else
			run();
	}

	public boolean isRunning() {
		return running;
	}

	public GPSData getInitialGPS() {
		return initial;
	}

	public double getMaxSpeed() {
		return maxSpeed;
	}

	public double getMaxAltitude() {
		return maxAltitude;
	}

	public double getMinAltitude() {
		return minAltitude;
	}

	public double getMaxPressureAltitude() {
		return maxPressureAltitude;
	}

	public double getMinPressureAltitude() {
		return minPressureAltitude;
	}

	public double getMaxClimb() {
		return maxClimb;
	}

	public double getMaxSink() {
		return maxSink;
	}

	/**
	 * Start worker thread
	 */
	private void startWorker() {
		workerThread = new TrackWorker();
		(new Thread(workerThread)).start();
		if (Config.verbose)
			System.out.println("Track: worker ready");
	}

	/**
	 * Worker thread class
	 */
	public class TrackWorker implements Runnable {

		// flag for whether the worker should shut down
		private boolean shutdown;

		/**
		 * Constructor
		 */
		public TrackWorker() {
			shutdown = false;
		}

		private void updateStats() {
			GPSData gps = controller.getGPSData();
			PTAData pta = controller.getPTA();

			double speed = gps.getSpeedKMH();
			double altitude = gps.getAltitude();
			double pressureAltitude = pta.getAltitude();
			double vSpeed = controller.getAltitudeChange();

			if (speed > maxSpeed)
				maxSpeed = speed;
			if (altitude > maxAltitude)
				maxAltitude = altitude;
			if (altitude < minAltitude)
				minAltitude = altitude;
			if (pressureAltitude > maxPressureAltitude)
				maxPressureAltitude = pressureAltitude;
			if (pressureAltitude < minPressureAltitude)
				minPressureAltitude = pressureAltitude;
			if (vSpeed > maxClimb)
				maxClimb = vSpeed;
			if (vSpeed < maxSink)
				maxSink = vSpeed;
		}

		/**
		 * Main worker loop
		 */
		@Override
		public void run() {
			while (!shutdown) {
				if (running && System.currentTimeMillis() - lastGPSTime > 1000) {
					GPSData gps = controller.getGPSData();

					if (gps != null && gps.isValid()) {
						if (!initial.isValid() || !initial.isComplete())
							initial = gps;

						String gpsStr = String.format("T,%f,%f,%f,%f,%f\n", gps.getLatitude(), gps.getLongitude(),
								gps.getAltitude(), gps.getSpeed(), gps.getTrackingAngle());

						try {
							Files.write(Paths.get(filename), gpsStr.getBytes(StandardCharsets.UTF_8),
									Files.exists(Paths.get(filename)) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
						} catch (Exception e) {
							Errors.handleException(e, "cannot write GPS file");
						}
						System.out.print(gpsStr);
					}

					lastGPSTime = System.currentTimeMillis();
				}
				Util.delay(100);
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