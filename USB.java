import net.samuelcampos.usbdrivedetector.*;
import net.samuelcampos.usbdrivedetector.events.IUSBDriveListener;
import net.samuelcampos.usbdrivedetector.events.USBStorageEvent;
import java.util.ArrayList;

/**
 * Detects attached USB storage devices
 */
public class USB {

	// controller for detecting USB drives and events
	private USBDeviceDetectorManager driveDetector;

	/**
	 * Constructor for a USB controller object
	 * 
	 * @param pollingInterval How often to check status of USB devices
	 */
	public USB(int pollingInterval) {
		driveDetector = new USBDeviceDetectorManager((long) pollingInterval);
		driveDetector.addDriveListener(new USBListener());
		if (Config.verbose) {
			for (USBStorageDevice device : getDevices())
				System.out.printf("USB: %s\n", device);
			System.out.println("USB: ready");
		}
	}

	/**
	 * Get a list of writeable root folders of attached USB devices
	 * 
	 * @return List of root folders
	 */
	public ArrayList<String> getFolders() {
		ArrayList<String> folders = new ArrayList<>();
		for (USBStorageDevice device : getDevices()) {
			if (device.canWrite())
				folders.add(device.getRootDirectory().getPath());
		}
		return folders;
	}

	/**
	 * Get a list of attached USB devices
	 * 
	 * @return List of USBStorageDevice objects
	 */
	public ArrayList<USBStorageDevice> getDevices() {
		return new ArrayList<>(driveDetector.getRemovableDevices());
	}

	/**
	 * Listens for changes to attached USB devices, runs in its own thread
	 */
	private class USBListener implements IUSBDriveListener {

		/**
		 * Handles USB storage device events
		 * 
		 * @param event USB device event
		 */
		@Override
		public void usbDriveEvent(USBStorageEvent event) {
			if (Config.verbose)
				System.out.printf("USB: %s\n", event);
		}
	}
}
