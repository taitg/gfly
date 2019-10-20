import java.util.ArrayList;

import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Track {

  private TrackWorker workerThread;
  private DeviceController controller;
  private boolean running;
  private long lastGPSTime;

  public Track(DeviceController controller) {
    if (controller != null) {
      this.controller = controller;
      running = false;
      lastGPSTime = 0;
      
      startWorker();

      if (Config.verbose) System.out.println("Track: ready");
    }
    else if (Config.verbose) System.out.println("Track: failed to initialize");
	}

	/**
	 * Shut down the controller
	 */
	public void shutdown() {
		if (workerThread != null)
			workerThread.shutdown();
  }
  
  public void run() {
    running = true;
  }

  public void stop() {
    running = false;
  }

  public void toggle() {
    running = !running;
  }

  public boolean isRunning() {
    return running;
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
				if (running && System.currentTimeMillis() - lastGPSTime > 1000) {
          GPSData gps = controller.getGPSData();

          if (gps != null && gps.isValid()) {
            String gpsStr = String.format("T,%f,%f,%f,%f,%f\n", gps.getLatitude(), gps.getLongitude(), gps.getAltitude(),
                gps.getSpeed(), gps.getTrackingAngle());
      
            try {
              String fileName = "gps.txt";
              Files.write(Paths.get(fileName), gpsStr.getBytes(StandardCharsets.UTF_8),
                  Files.exists(Paths.get(fileName)) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
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