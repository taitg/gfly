import React from 'react';
import axios from 'axios';
import { Menu } from 'antd';
import { get } from 'lodash';
import styled from 'styled-components';

const serverURL = 'http://192.168.4.1:8080';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      menuSelection: 'current',
    };
  }

  componentDidMount() {
    setInterval(() => this.updateStatus(), 1000);
  }

  updateStatus() {
    if (this.state.updatingStatus) return;
    this.setState({ updatingStatus: true });
    return axios
      .get(`${serverURL}/status`)
      .then(({ data }) => this.setState({ status: data, updatingStatus: false }))
      .catch(() => this.setState({ updatingStatus: false }));
  }

  onMenuClick(e) {
    const pages = ['current', 'maxmin', 'track'];
    if (pages.includes(e.key))
      this.setState({ menuSelection: e.key });
  }

  renderMenu() {
    const { status, menuSelection } = this.state;
    return (
      <Menu theme="dark" mode="horizontal" selectedKeys={[menuSelection]} onClick={e => this.onMenuClick(e)}>
        <Menu.Item key="current">Current</Menu.Item>
        <Menu.Item key="maxmin">Max/Min</Menu.Item>
        <Menu.Item key="track">Track</Menu.Item>
        <Menu.SubMenu key="control" title="Control">
          <Menu.Item onClick={() => axios.get(`${serverURL}/toggletrack`)}>
            Turn tracking {status && status.isTrackRunning ? 'off' : 'on'}
          </Menu.Item>
          <Menu.Item onClick={() => axios.get(`${serverURL}/toggleaudio`)}>
            Turn audio {status && status.varioAudioOn ? 'off' : 'on'}
          </Menu.Item>
          <Menu.Divider />
          <Menu.Item>Restart</Menu.Item>
          <Menu.Item>Shut down</Menu.Item>
        </Menu.SubMenu>
      </Menu>
    );
  }

  renderCurrent() {
    const { status } = this.state;
    return (
      <DataList>
        <DataItem>
          <DataNumber style={{ color: !status.gpsHasFix ? 'dimgrey' : ''}}>
            {get(status, 'altitude', 0).toFixed(1)} m
          </DataNumber>
          <DataNumber>{get(status, 'pressureAltitude', 0).toFixed(1)} m</DataNumber>
          <DataName>Altitude</DataName>
        </DataItem>
        <DataItem>
          <div />
          <DataNumber style={{ color: get(status, 'verticalSpeed', 0) < 0 ? 'red' : 'limegreen' }}>
            {get(status, 'verticalSpeed', 0).toFixed(2)} m/s
          </DataNumber>
          <DataName>Vert Speed</DataName>
        </DataItem>
        <DataItem>
          <DataNumber style={{ color: !status.gpsHasFix ? 'dimgrey' : ''}}>
            {get(status, 'distance', 0).toFixed(1)} km
          </DataNumber>
          <DataName>Distance</DataName>
        </DataItem>
        <DataItem>
          <DataNumber style={{ color: !status.gpsHasFix ? 'dimgrey' : ''}}>
            {get(status, 'speed', 0).toFixed(1)} km/h
          </DataNumber>
          <DataName>Speed</DataName>
        </DataItem>
        <DataItem>
          <a
            href={status.gpsHasFix ? `https://www.google.com/maps/place/${status.latitude}${status.longitude}` : '#'}
            target="_blank"
            rel="noopener noreferrer"
          >
            <DataNumber style={{ color: !status.gpsHasFix ? 'dimgrey' : ''}}>
              {get(status, 'latitude', 0).toFixed(4)}
            </DataNumber>
          </a>
          <DataName>Latitude</DataName>
        </DataItem>
        <DataItem>
          <a
            href={status.gpsHasFix ? `https://www.google.com/maps/place/${status.latitude}${status.longitude}` : '#'}
            target="_blank"
            rel="noopener noreferrer"
          >
            <DataNumber style={{ color: !status.gpsHasFix ? 'dimgrey' : ''}}>
              {get(status, 'longitude', 0).toFixed(4)}
            </DataNumber>
          </a>
          <DataName>Longitude</DataName>
        </DataItem>
        <DataItem>
          <DataNumber>{get(status, 'temperature', 0).toFixed(1)} C</DataNumber>
          <DataName>Temp</DataName>
        </DataItem>
        <DataItem>

        </DataItem>
      </DataList>
    );
  }

  renderMaxMin() {
    const { status } = this.state;
    return (
      <DataList>
        <DataItem>
          <DataNumber>{get(status, 'maxAltitude', 0).toFixed(1)} m</DataNumber>
          <DataNumber>{get(status, 'maxPressureAltitude', 0).toFixed(1)} m</DataNumber>
          <DataName>Max Altitude</DataName>
        </DataItem>
        <DataItem>
          <DataNumber>{get(status, 'minAltitude', 0).toFixed(1)} m</DataNumber>
          <DataNumber>{get(status, 'minPressureAltitude', 0).toFixed(1)} m</DataNumber>
          <DataName>Min Altitude</DataName>
        </DataItem>
        <DataItem>
          <DataNumber style={{ color: 'limegreen' }}>
            {get(status, 'maxClimb', 0).toFixed(2)} m/s
          </DataNumber>
          <DataName>Max Climb</DataName>
        </DataItem>
        <DataItem>
          <DataNumber style={{ color: 'red' }}>
            {get(status, 'maxSink', 0).toFixed(2)} m/s
          </DataNumber>
          <DataName>Max Sink</DataName>
        </DataItem>
        <DataItem>
          <DataNumber>{get(status, 'maxDistance', 0).toFixed(1)} km</DataNumber>
          <DataName>Max Distance</DataName>
        </DataItem>
        <DataItem>
          <DataNumber>{get(status, 'maxSpeed', 0).toFixed(1)} km/h</DataNumber>
          <DataName>Max Speed</DataName>
        </DataItem>
      </DataList>
    );
  }

  render() {
    const { status, menuSelection } = this.state;
    return (
      <div>
        {this.renderMenu()}
        <div style={{ padding: '0em' }}>
          {status ? <React.Fragment>
            {menuSelection === 'current' && this.renderCurrent()}
            {menuSelection === 'maxmin' && this.renderMaxMin()}
          </React.Fragment> : 'loading'}
        </div>
      </div>
    );
  }
}

export default App;

const DataList = styled.div`
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  font-size: 1.5em;
`;

const DataItem = styled.div`
  width: 50%;
  display: flex;
  justify-content: space-between;
  flex-direction: column;
  align-items: center;
  padding: 0.5em 0;
  border: dotted darkslategrey 1px;
`;

const DataName = styled.div`
  padding: 0.5em 0 0.5em 0;
`;

const DataNumber = styled.div`
  font-size: 1.5em;
  color: gold;
`;
