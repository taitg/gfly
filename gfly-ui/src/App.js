import React from 'react';
import axios from 'axios';
import { get } from 'lodash';
import styled from 'styled-components';
import { Alert, Menu, Spin } from 'antd';
import { IoMdSpeedometer } from 'react-icons/io';
import { GiPathDistance } from 'react-icons/gi';
import { AiFillSetting } from 'react-icons/ai';
import { FaInfoCircle } from 'react-icons/fa';
import { ImStatsBars } from 'react-icons/im';

import { headingToString } from './util';

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
    const pages = ['current', 'maxmin', 'track', 'info'];
    if (pages.includes(e.key))
      this.setState({ menuSelection: e.key });
  }

  renderMenu() {
    const { status, menuSelection } = this.state;
    return (
      <Menu theme="dark" mode="horizontal" selectedKeys={[menuSelection]} onClick={e => this.onMenuClick(e)}>
        <Menu.Item key="current"><IconWrapper><IoMdSpeedometer /></IconWrapper></Menu.Item>
        <Menu.Item key="maxmin"><IconWrapper><ImStatsBars /></IconWrapper></Menu.Item>
        <Menu.Item key="track"><IconWrapper><GiPathDistance /></IconWrapper></Menu.Item>
        <Menu.Item key="info"><IconWrapper><FaInfoCircle /></IconWrapper></Menu.Item>
        <Menu.SubMenu key="control" title={<IconWrapper><AiFillSetting /></IconWrapper>}>
          <Menu.Item onClick={() => axios.get(`${serverURL}/resetstats`)}>
            Reset stats
          </Menu.Item>
          <Menu.Item onClick={() => axios.get(`${serverURL}/resetorigin`)}>
            Reset origin point
          </Menu.Item>
          <Menu.Divider />
          <Menu.Item onClick={() => axios.get(`${serverURL}/toggletrack`)}>
            Turn tracking {status && status.isTrackRunning ? 'off' : 'on'}
          </Menu.Item>
          <Menu.Item onClick={() => axios.get(`${serverURL}/toggleaudio`)}>
            Turn audio {status && status.varioAudioOn ? 'off' : 'on'}
          </Menu.Item>
          <Menu.Divider />
          <Menu.Item onClick={() => axios.get(`${serverURL}/reboot`)}>
            Restart
          </Menu.Item>
          <Menu.Item onClick={() => axios.get(`${serverURL}/powerdown`)}>
            Shut down
          </Menu.Item>
        </Menu.SubMenu>
      </Menu>
    );
  }

  renderCurrent() {
    const { status } = this.state;
    const hasFix = status.gpsHasFix;
    return (
      <DataList>
        <DataItem>
          <div />
          <DataNumber style={{ color: get(status, 'verticalSpeed', 0) < 0 ? 'red' : 'limegreen' }}>
            {get(status, 'verticalSpeed', 0).toFixed(2)} m/s
          </DataNumber>
          <DataName>Vertical Speed</DataName>
        </DataItem>
        <DataItem>
          <DataNumber>{get(status, 'pressureAltitude', 0).toFixed(1)} m</DataNumber>
          <DataNumber style={{ color: !hasFix ? 'dimgrey' : ''}}>
            {!hasFix ? '-' : `${get(status, 'altitude', 0).toFixed(1)} m`}
          </DataNumber>
          <DataName>Altitude</DataName>
        </DataItem>
        <DataItem>
          <DataNumber style={{ color: !hasFix ? 'dimgrey' : ''}}>
            {!hasFix ? '-' : `${get(status, 'speed', 0).toFixed(1)} km/h`}
          </DataNumber>
          <DataName>Speed</DataName>
        </DataItem>
        <DataItem>
          <DataNumber style={{ color: !hasFix ? 'dimgrey' : ''}}>
            {!hasFix ? '-' : headingToString(status.heading)}
          </DataNumber>
          <DataName>Direction</DataName>
        </DataItem>
        <DataItem>
          <DataNumber style={{ color: !hasFix ? 'dimgrey' : ''}}>
            {!hasFix ? '-' : `${get(status, 'distance', 0).toFixed(1)} km`}
          </DataNumber>
          <DataName>Distance</DataName>
        </DataItem>
        <DataItem>
          <DataNumber>{get(status, 'temperature', 0).toFixed(1)} C</DataNumber>
          <DataName>Temperature</DataName>
        </DataItem>
        <DataItem>
          <DataNumber style={{ color: !hasFix ? 'dimgrey' : ''}}>
            {!hasFix ? '-' : get(status, 'latitude', 0).toFixed(4)}
          </DataNumber>
          <DataName>Latitude</DataName>
        </DataItem>
        <DataItem>
          <DataNumber style={{ color: !hasFix ? 'dimgrey' : ''}}>
            {!hasFix ? '-' : get(status, 'longitude', 0).toFixed(4)}
          </DataNumber>
          <DataName>Longitude</DataName>
        </DataItem>
      </DataList>
    );
  }

  renderMaxMin() {
    const { status } = this.state;
    return (
      <DataList>
        <DataItem>
          <DataNumber>{get(status, 'maxPressureAltitude', 0).toFixed(1)} m</DataNumber>
          <DataNumber>{get(status, 'maxAltitude', 0).toFixed(1)} m</DataNumber>
          <DataName>Max Altitude</DataName>
        </DataItem>
        <DataItem>
          <DataNumber>{get(status, 'minPressureAltitude', 0).toFixed(1)} m</DataNumber>
          <DataNumber>{get(status, 'minAltitude', 0).toFixed(1)} m</DataNumber>
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
        <DataItem>
          <DataNumber>{get(status, 'distanceTravelled', 0).toFixed(1)} km</DataNumber>
          <DataName>Dist Travelled</DataName>
        </DataItem>
        <DataItem></DataItem>
      </DataList>
    );
  }

  render() {
    const { status, menuSelection } = this.state;
    return (
      <div>
        {this.renderMenu()}
        <div style={{ padding: '0em' }}>
          {status ?
            <React.Fragment>
              {!status.gpsHasFix && <Alert type="error" message="No GPS fix" showIcon />}
              {menuSelection === 'current' && this.renderCurrent()}
              {menuSelection === 'maxmin' && this.renderMaxMin()}
            </React.Fragment>
          : <SpinWrapper><Spin size="large" /></SpinWrapper>}
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

const SpinWrapper = styled.div`
  width: 100%;
  height: 80vh;
  display: flex;
  justify-content: center;
  align-items: center;
`;

const IconWrapper = styled.div`
  /* display: flex;
  height: 100%;
  justify-content: center;
  align-items: flex-start;
  flex-grow: 1; */
  padding-top: 0.2em;
  font-size: 2.2em;
`;
