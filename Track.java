import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Track {

	private TrackWorker workerThread;
	private DeviceController controller;

	private boolean running;
	private String filename;
	private GPSData initialGPS;
	private GPSData lastDistancePoint;
	private ArrayList<GPSData> gpsList;

	private long lastGPSTime;
	private long lastStatsTime;
	private long lastDistanceTime;
	private long trackStartTime;
	private long trackStopTime;

	private double distanceTravelled;
	private double distance;
	private double maxDistance;
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
			initialGPS = controller.getGPSData();
			lastDistancePoint = null;
			running = false;
			lastGPSTime = 0;
			lastStatsTime = 0;
			lastDistanceTime = 0;
			trackStartTime = 0;
			trackStopTime = 0;
			distanceTravelled = 0;
			distance = 0;
			maxDistance = 0;
			maxSpeed = 0;
			maxAltitude = 0;
			maxPressureAltitude = 0;
			maxClimb = 0;
			maxSink = 0;

			startWorker();

			if (Config.verbose)
				System.out.println("Track: ready");
		} else if (Config.verbose)
			System.out.println("Track: failed to initialGPSize");
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

			trackStartTime = System.currentTimeMillis();
			running = true;
		} catch (Exception e) {
			Errors.handleException(e, "Failed to start tracking");
		}
	}

	public void stop() {
		trackStopTime = System.currentTimeMillis();
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
		return initialGPS;
	}

	public double getDistance() {
		return distance;
	}

	public double getMaxDistance() {
		return maxDistance;
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

	public long getStartTime() {
		return trackStartTime;
	}

	public long getStopTime() {
		return trackStopTime;
	}

	public void resetOrigin() {
		distance = 0;
		maxDistance = 0;
		distanceTravelled = 0;
		initialGPS = controller.getGPSData();
	}

	public void resetStats() {
		distanceTravelled = 0;
		maxDistance = 0;
		maxSpeed = 0;
		maxAltitude = 0;
		maxPressureAltitude = 0;
		maxClimb = 0;
		maxSink = 0;
	}

	private void updateStats(GPSData gps, PTAData pta) {
		if (gps != null) {
			double speed = gps.getSpeedKMH();
			double altitude = gps.getAltitude();

			if (gps.isValid()) {
				if (initialGPS != null && initialGPS.isValid()) {
					distance = Util.vincentyDistance(initialGPS.getLatitude(), initialGPS.getLongitude(), gps.getLatitude(),
							gps.getLongitude());
				} else {
					distance = 0;
				}

				if (distance > maxDistance)
					maxDistance = distance;
				if (speed > maxSpeed)
					maxSpeed = speed;

				if (gps.isComplete()) {
					if (maxAltitude == 0 || altitude > maxAltitude)
						maxAltitude = altitude;
					if (minAltitude == 0 || altitude < minAltitude)
						minAltitude = altitude;
				}
			}
		}

		if (pta != null) {
			double pressureAltitude = pta.getAltitude();
			double vSpeed = controller.getAltitudeChange();

			if (maxPressureAltitude == 0 || pressureAltitude > maxPressureAltitude)
				maxPressureAltitude = pressureAltitude;
			if (minPressureAltitude == 0 || pressureAltitude < minPressureAltitude)
				minPressureAltitude = pressureAltitude;
			if (maxClimb == 0 || vSpeed > maxClimb)
				maxClimb = vSpeed;
			if (maxSink == 0 || vSpeed < maxSink)
				maxSink = vSpeed;
		}
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

		/**
		 * Main worker loop
		 */
		@Override
		public void run() {
			while (!shutdown) {
				GPSData gps = null;
				PTAData pta = null;

				// update GPS every 1s
				if (System.currentTimeMillis() - lastGPSTime > 1000) {
					gps = controller.getGPSData();
					// PTAData pta = controller.getPTA();

					if (gps != null && gps.isValid()) {
						// if initialGPS GPS data is not valid, overwrite with new data
						if (!initialGPS.isValid() || !initialGPS.isComplete())
							initialGPS = gps;

						// add GPS data to list
						// gpsList.add(gps);

						// save GPS data to file
						if (running) {
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
					}

					lastGPSTime = System.currentTimeMillis();
				}

				// update stats every 0.2s
				if (System.currentTimeMillis() - lastStatsTime > 200) {
					if (gps == null)
						gps = controller.getGPSData();
					if (pta == null)
						pta = controller.getPTA();

					updateStats(gps, pta);
					lastStatsTime = System.currentTimeMillis();
				}

				// update distance travelled every 10s
				if (System.currentTimeMillis() - lastDistanceTime > 10000) {
					if (gps == null)
						gps = controller.getGPSData();
					// if (pta == null) pta = controller.getPTA();

					if (gps.isValid()) {
						GPSData lastPoint = lastDistancePoint == null ? initialGPS : lastDistancePoint;
						double d = Util.vincentyDistance(lastPoint.getLatitude(), lastPoint.getLongitude(), gps.getLatitude(),
								gps.getLongitude());
						if (d > 0.1) {
							distanceTravelled += d;
							lastDistancePoint = gps;
						}
						lastDistanceTime = System.currentTimeMillis();
					}
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