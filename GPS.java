import com.pi4j.io.serial.*;
import com.pi4j.util.Console;
import java.io.IOException;
import java.util.ArrayList;
import com.pi4j.io.gpio.GpioController;

/**
 * Controller for the Adafruit Ultimate GPS module and a status LED.
 *
 * Converts serial data to GPSData objects, which are stored as a list of the
 * last 15 to be created. If any of the last 15 data are valid (meaning the GPS
 * module says it has a fix), and if data has been received more recently than the
 * timeout (currently 15sec), then the LED will be on.
 */
public class GPS {
	
	private String buffer; // buffer for incoming serial data
	private ArrayList<GPSData> dataList; // list of up to 15 previous GPS readings
	private long lastDataTime; // the last time serial data was received
	private Switch button; // GPS switch controller
	private LED led; // GPS LED controller
		
	/**
	 * Constructor for a GPS controller object
	 * 
	 * @param gpio GPIO controller
	 * @param gpsLedPinNum GPIO pin number for GPS LED
	 * @param gpsSwitchPinNum GPIO pin number for GPS switch
	 */
	public GPS(GpioController gpio, int gpsLedPinNum, int gpsSwitchPinNum) {
		button = new Switch(gpio, "gpsSwitch", gpsSwitchPinNum, true);
		led = new LED(gpio, "gpsFix", gpsLedPinNum);
		dataList = new ArrayList<>();
		init();
	}
	
	/**
	 * Start serial communication
	 */
	public void init() {
		lastDataTime = 0;
		buffer = "";
		if (Config.gpsSource.equals("NONE")) return;
		
        try {
			// create an instance of the serial communications class
			final Serial serial = SerialFactory.createInstance();

			// create and register the serial data listener
			// this will run in its own thread
			serial.addListener(new SerialDataEventListener() {
				@Override
				public void dataReceived(SerialDataEvent event) {

					// NOTE! - It is extremely important to read the data received from the
					// serial port.  If it does not get read from the receive buffer, the
					// buffer will continue to grow and consume memory.

					// read and handle the serial data
					try {
						handleSerialData(event.getAsciiString());
					} catch (IOException e) {
						Errors.handleException(e, "Serial I/O failure");
					}
				}
			});
			
            // create serial config object
            SerialConfig config = new SerialConfig();
            
            // *** need to enable serial in Raspberry Pi config ***
            // set serial I/O settings
            String device = "/dev/ttyS0"; // SerialPort.getDefaultPort(); // should return /dev/ttyS0 for UART
            if (Config.gpsSource.equals("USB")) device = "/dev/ttyUSB0";
            config.device(device)
                  .baud(Baud._9600)
                  .dataBits(DataBits._8)
                  .parity(Parity.NONE)
                  .stopBits(StopBits._1)
                  .flowControl(FlowControl.NONE);

            // display connection details
            if (Config.verbose) {
				System.out.printf("GPS: Opening serial connection to: %s\n", config.toString());
			}

            // open the default serial device/port with the configuration settings
            serial.open(config);
            
			if (Config.verbose) System.out.println("GPS: ready");
        }
        catch(Exception e) {
            Errors.handleException(e, "Serial setup failed");
        }
	}
		
	/**
	 * Get last VALID gps data
	 * 
	 * @return Last valid GPSData or null if none are valid
	 */
	public GPSData getLastValid() {
		if (dataList.size() < 1) return null;
		for (int i = dataList.size() - 1; i >= 0; i--) {
			if (dataList.get(i) != null && dataList.get(i).isValid()) {
				GPSData result = dataList.get(i);
				if (button != null && button.isPressed()) return filter(result);
				return result;
			}
		}
		return null;
	}

	public GPSData getLastComplete() {
		if (dataList.size() < 1) return null;
		GPSData result = dataList.get(0);
		for (int i = dataList.size() - 1; i >= 0; i--) {
			GPSData item = dataList.get(i);
			if (item != null && item.isValid() && item.getDate() != null) {
				result = item;
				break;
			}
		}
		for (int i = dataList.size() - 1; i >= 0; i--) {
			GPSData item = dataList.get(i);
			if (item != null && item.isValid() && item.getAltitude() > 0) {
				result.setAltitude(item.getAltitude());
				break;
			}
		}
		return result;
	}
		
	/**
	 * Get last GPS data
	 * 
	 * @return Last GPSData or null if there is no data
	 */
	public GPSData getLast() {
		if (dataList.size() < 1) return null;
		GPSData result = dataList.get(dataList.size() - 1);
		if (button != null && button.isPressed()) return filter(result);
		return result;
	}
		
	/**
	 * Get all GPS data (only keeps the last 15)
	 * - not currently in use by the program
	 * 
	 * @return List of all retained GPSData
	 */
	public ArrayList<GPSData> getAllData() { return dataList; }
	
	/**
	 * Update the LED, on if any of the last 15 are valid
	 */
	public void updateLED() {
		boolean hasValidData = getLastValid() != null;
		boolean gpsActive = button == null || !button.isPressed();
		boolean timedOut = System.currentTimeMillis() - lastDataTime > Config.gpsDataTimeout;

		if (hasValidData && gpsActive && !timedOut) led.on();
		else led.off();
		/* disable attempting to reinitialize after timeout
		if (timedOut) {
			init();
			lastDataTime = System.currentTimeMillis();
		}
		*/
	}
	
	/**
	 * Shut down LED controller
	 */
	public void shutdown() {
		if (led != null) led.shutdown();
	}
		
	/**
	 * Returns the GPS LED controller
	 * 
	 * @return GPS LED
	 */
	public LED getLED() { return led; }
	
	/**
	 * Returns the GPS Switch controller
	 * 
	 * @return GPS Switch
	 */
	public Switch getSwitch() { return button; }
	
	/**
	 * Returns the system time of the last data received
	 * 
	 * @return Time of the last GPS data in Unix time
	 */
	public long getLastDataTime() { return lastDataTime; }
		
	/**
	 * Process GPS information from serial data
	 * 
	 * @param serialData String of data to process
	 */
	private void handleSerialData(String serialData) {
		lastDataTime = System.currentTimeMillis();
		boolean pressed = false;
		if (buffer.length() > 1024) buffer = "";
		if (button != null && button.isPressed()) pressed = true;
		
		buffer += serialData;
		if (buffer.contains("$GPRMC")) {
			buffer = buffer.substring(buffer.indexOf("$GPRMC"));
			
			if (buffer.split(",").length > 10) {
				dataList.add(new GPSData(buffer, pressed));
				if (dataList.size() > 15) dataList.remove(0);
				updateLED();
				buffer = "";
			}
		}
		else
		if (buffer.contains("$GPGGA")) {
			buffer = buffer.substring(buffer.indexOf("$GPGGA"));
			
			if (buffer.split(",").length > 10) {
				dataList.add(new GPSData(buffer, pressed));
				if (dataList.size() > 15) dataList.remove(0);
				updateLED();
				buffer = "";
			}
		}
	}
	
	/**
	 * Filter out everything except date and time (for when switch is pressed)
	 * 
	 * @param input Original GPSData
	 * @return GPSData with only date and time
	 */
	private GPSData filter(GPSData input) {
		return new GPSData(input.getTime(), input.getDate());
	}
}
