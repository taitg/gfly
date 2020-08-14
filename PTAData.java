
public class PTAData {
  private double pressure;
  private double temperature;
  private double altitude;

  public PTAData(double pressure, double temperature, double altitude) {
    this.pressure = pressure;
    this.temperature = temperature;
    this.altitude = altitude;
  }

  public double getPressure() { return pressure; }

  public double getTemperature() { return temperature; }

  public double getAltitude() { return altitude; }
}