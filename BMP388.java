import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import java.io.IOException;

public class BMP388 {

  private static byte CHIP_ID               = 0x50;
  private static byte REGISTER_CHIPID       = 0x00;
  private static byte REGISTER_STATUS       = 0x03;
  private static byte REGISTER_PRESSUREDATA = 0x04;
  private static byte REGISTER_TEMPDATA     = 0x07;
  private static byte REGISTER_CONTROL      = 0x1B;
  private static byte REGISTER_OSR          = 0x1C;
  private static byte REGISTER_ODR          = 0x1D;
  private static byte REGISTER_CONFIG       = 0x1F;
  private static byte REGISTER_CAL_DATA     = 0x31;
  private static byte REGISTER_CMD          = 0x7E;

  private I2CDevice device;
  private double[] tempCalib;
  private double[] pressureCalib;
  private double seaLevelPressure;
  
	public BMP388(I2CBus i2cBus) throws IOException {
    try {
      device = i2cBus.getDevice(0x77);
      
      byte chipId = readByte(REGISTER_CHIPID);
      if (chipId != CHIP_ID && Config.verbose) {
        System.out.println("BMP388: Failed to find chip");
        return;
      }

      readCoefficients();
      reset();
      seaLevelPressure = 1013.25;

      if (Config.verbose) System.out.println("BMP388: ready");
    }
    catch (Exception e) {
      Errors.handleException(e, "Failed to initialize BMP388");
    }
  }

  public double getPressure() {
    return read()[0] / 100;
  }

  public double getTemperature() {
    return read()[1];
  }

  public double getAltitude() {
    return 44307.7 * (1 - Math.pow(getPressure() / seaLevelPressure, 0.190284));
  }

  public double[] getPTA() {
    double[] pt = read();
    double[] result = new double[3];
    result[0] = pt[0] / 100;
    result[1] = pt[1];
    result[2] = 44307.7 * (1 - Math.pow(result[0] / seaLevelPressure, 0.190284));
    return result;
  }

  private double[] read() {
    double[] pt = new double[2];

    // perform one measurement in forced mode
    writeByte(REGISTER_CONTROL, (byte) 0x13);

    // wait for both conversions to complete
    while ((readByte(REGISTER_STATUS) & 0x60) != 0x60) {
      Util.delay(2);
    }

    // get ADC values
    byte[] data = readRegister(REGISTER_PRESSUREDATA, 6);
    long adc_p = ((data[2] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[0] & 0xff);
    long adc_t = ((data[5] & 0xff) << 16) | ((data[4] & 0xff) << 8) | (data[3] & 0xff);

    // calculate temperature
    double pd1 = (double) adc_t - tempCalib[0];
    double pd2 = pd1 * tempCalib[1];
    double temperature = pd2 + (pd1 * pd1) * tempCalib[2];

    // calculate pressure
    pd1 = pressureCalib[5] * temperature;
    pd2 = pressureCalib[6] * Math.pow(temperature, 2.0);
    double pd3 = pressureCalib[7] * Math.pow(temperature, 3.0);
    double po1 = pressureCalib[4] + pd1 + pd2 + pd3;

    pd1 = pressureCalib[1] * temperature;
    pd2 = pressureCalib[2] * Math.pow(temperature, 2.0);
    pd3 = pressureCalib[3] * Math.pow(temperature, 3.0);
    double po2 = (double) adc_p * (pressureCalib[0] + pd1 + pd2 + pd3);

    pd1 = Math.pow(adc_p, 2);
    pd2 = pressureCalib[8] + pressureCalib[9] * temperature;
    pd3 = pd1 * pd2;
    double pd4 = pd3 + pressureCalib[10] * Math.pow(adc_p, 3.0);

    double pressure = po1 + po2 + pd4;

    // pressure in Pa, temperature in deg C
    pt[0] = pressure;
    pt[1] = temperature;
    return pt;
  }

  private void readCoefficients() {
    try {
      byte[] bytes = readRegister(REGISTER_CAL_DATA, 21);
      long[] coeff = new Struct().unpack("<HHbhhbbHHbbhbb", bytes);

      tempCalib = new double[] {
        (double) coeff[0] / Math.pow(2, -8.0), // T1
        (double) coeff[1] / Math.pow(2, 30.0), // T2
        (double) coeff[2] / Math.pow(2, 48.0) }; // T3

      pressureCalib = new double[] {
        ((double) coeff[3] - Math.pow(2, 14.0)) / Math.pow(2, 20.0), // P1
        ((double) coeff[4] - Math.pow(2, 14.0)) / Math.pow(2, 29.0), // P2
        (double) coeff[5] / Math.pow(2, 32.0), // P3
        (double) coeff[6] / Math.pow(2, 37.0), // P4
        (double) coeff[7] / Math.pow(2, -3.0), // P5
        (double) coeff[8] / Math.pow(2, 6.0), // P6
        (double) coeff[9] / Math.pow(2, 8.0), // P7
        (double) coeff[10] / Math.pow(2, 15.0), // P8
        (double) coeff[11] / Math.pow(2, 48.0), // P9
        (double) coeff[12] / Math.pow(2, 48.0), // P10
        (double) coeff[13] / Math.pow(2, 65.0) }; // P11

      if (Config.verbose) {
        System.out.print("BMP388: T calib = ");
        for (int i = 0; i < 3; i++) System.out.printf("T%d: %f, \n", i+1, tempCalib[i]);
        System.out.print("\nBMP388: P calib = ");
        for (int i = 0; i < 11; i++) System.out.printf("P%d: %f, \n", i+1, pressureCalib[i]);
        System.out.println();
      }
    }
    catch (Exception e) {
      Errors.handleException(e, "Failed to read sensor coefficient data");
    }
  }
  
  private void reset() {
    writeByte(REGISTER_CMD, (byte) 0xb6);
  }

  private byte readByte(byte register) {
    return readRegister(register, 1)[0];
  }

  private byte[] readRegister(byte register, int length) {
    byte[] result = new byte[length];
    try {
      byte[] toWrite = new byte[] { (byte) (register & 0xff) };
      device.write(toWrite);
      device.read(result, 0, length);
    }
    catch (IOException e) {
      Errors.handleException(e, "Failed to read sensor data");
    }
    return result;
  }

  private void writeByte(byte register, byte value) {
    try {
      byte[] toWrite = new byte[] { (byte) (register & 0xff), (byte) (value & 0xff) };
      device.write(toWrite);
    }
    catch (IOException e) {
      Errors.handleException(e, "Failed to read sensor data");
    }
  }
}