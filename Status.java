public class Status {

  private boolean gpsHasFix;
  private boolean varioAudioOn;
  private boolean isTrackRunning;

  private long initialSystemTime;
  private long currentSystemTime;
  private long trackStartTime;
  private long trackStopTime;

  private String initialDate;
  private String initialTime;
  private double initialAltitude;
  private double initialLatitude;
  private double initialLongitude;
  private double initialHeading;
  private double initialSpeed;
  private double distance;

  private String date;
  private String time;
  private double altitude;
  private double latitude;
  private double longitude;
  private double heading;
  private double speed;

  private double pressure;
  private double pressureAltitude;
  private double verticalSpeed;
  private double temperature;

  private double maxSpeed;
  private double maxAltitude;
  private double minAltitude;
  private double maxPressureAltitude;
  private double minPressureAltitude;
  private double maxClimb;
  private double maxSink;
  private double maxDistance;

  public Status(DeviceController controller, Track track) {
    varioAudioOn = Config.varioAudioOn;
    isTrackRunning = track.isRunning();

    initialSystemTime = controller.getInitialTime();
    currentSystemTime = System.currentTimeMillis();
    trackStartTime = track.getStartTime();
    trackStopTime = track.getStopTime();

    GPSData gps = controller.getGPSData();
    if (gps != null) {
      date = gps.getDate();
      time = gps.getTime();
      altitude = gps.getAltitude();
      latitude = gps.getLatitude();
      longitude = gps.getLongitude();
      heading = gps.getTrackingAngle();
      speed = gps.getSpeedKMH();
      gpsHasFix = gps.isValid() && gps.isComplete();
    } else {
      date = "";
      time = "";
      altitude = 0;
      latitude = 0;
      longitude = 0;
      heading = 0;
      speed = 0;
      gpsHasFix = false;
    }

    GPSData initial = track.getInitialGPS();
    if (initial != null) {
      initialDate = initial.getDate();
      initialTime = initial.getTime();
      initialAltitude = initial.getAltitude();
      initialLatitude = initial.getLatitude();
      initialLongitude = initial.getLongitude();
      initialHeading = initial.getTrackingAngle();
      initialSpeed = initial.getSpeedKMH();
    } else {
      initialDate = "";
      initialTime = "";
      initialAltitude = 0;
      initialLatitude = 0;
      initialLongitude = 0;
      initialHeading = 0;
      initialSpeed = 0;
    }

    PTAData pta = controller.getPTA();
    if (pta != null) {
      pressure = pta.getPressure();
      pressureAltitude = pta.getAltitude();
      verticalSpeed = controller.getAltitudeChange();
      temperature = pta.getTemperature();
    } else {
      pressure = 0;
      pressureAltitude = 0;
      verticalSpeed = 0;
      temperature = 0;
    }

    distance = track.getDistance();
    maxDistance = track.getMaxDistance();
    maxSpeed = track.getMaxSpeed();
    maxAltitude = track.getMaxAltitude();
    minAltitude = track.getMinAltitude();
    maxPressureAltitude = track.getMaxPressureAltitude();
    minPressureAltitude = track.getMinPressureAltitude();
    maxClimb = track.getMaxClimb();
    maxSink = track.getMaxSink();
  }
}