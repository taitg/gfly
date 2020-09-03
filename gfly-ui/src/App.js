import React from 'react';
import axios from 'axios';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  componentDidMount() {
    setInterval(() => axios.get('http://192.168.4.1:8080/gps').then(({ data }) => this.setState({ gps: data })), 1000);
  }

  render() {
    return (
      <div>
        {this.state.gps ? <React.Fragment>
          <div>Altitude: {this.state.gps.altitude} m / {this.state.gps.pressureAltitude} m</div>
          <div>Latitude: {this.state.gps.latitude}</div>
          <div>Longitude: {this.state.gps.longitude}</div>
          <div>Speed: {this.state.gps.speed} knots</div>
          <div>VSpeed: {this.state.gps.verticalSpeed} m/s</div>
          <div>Temp: {this.state.gps.temperature} C</div>
        </React.Fragment> : 'loading'}
      </div>
    );
  }
}

export default App;
