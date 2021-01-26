import React, { Component } from "react";

import {
  AppRegistry,
  Text,
  View,
  Image,
  SafeAreaView,
  StyleSheet,
  Button,
  TouchableOpacity
} from 'react-native';

import KalturaVideo from "@kaltura-player-youi/video-component/KalturaVideo";
import VideoGallery from './VideoGallery';

export default class YiReactApp extends Component {

  state = {
    isPlaying: false,
    duration: 0,
    currentTime: 0,
    sources: null,
    media: null,
    isMuted: false,
    videoSelected: false,
    changeMedia: false,
    playbackSpeed: 1.0,
    logLevel: "VERBOSE",
    keepDeviceScreenOn: false
  }

  videoRef = React.createRef()
  pauseBtnRef = React.createRef()

  onVideoSelected = (source) => {
    this.setState({...source, videoSelected: true, changeMedia: false,});
  }

  loadBtnPressed = () => {
    if (this.state.videoSelected) {
      this.setState({changeMedia: false, sources: null, media: null, videoSelected: false, partnerId: null, initOptions: null, isMuted: false, playbackSpeed: 1.0});
    }
  }
  changeMediaPressed = () => {
    this.setState({changeMedia: true, sources: null, media: null, partnerId: null, initOptions: null});
  }

  pauseBtnPressed = () => this.state.isPlaying ? this.videoRef.current.pause() : this.videoRef.current.play();
  keepDeviceScreenOnBtnPressed = () => this.state.keepDeviceScreenOn ? this.videoRef.current.keepDeviceScreenOn(false) : this.videoRef.current.keepDeviceScreenOn(true) ;

  seekBtnPressed = () => this.videoRef.current.seek(this.state.currentTime + 10000);
  logLevelBtnPressed = () => this.setState({ logLevel:  this.state.logLevel === "VERBOSE" ? "ERROR" : "VERBOSE"}); 
  muteBtnPressed = () => this.setState({ 'isMuted': !this.state.isMuted });
  speedBtnPressed = () => this.setState({playbackSpeed: this.state.playbackSpeed == 1.0 ? 2.0 : 1.0 });

  render() {
    const {isMuted, isPlaying, keepDeviceScreenOn, videoSelected, media, sources, partnerId, initOptions, changeMedia, logLevel, playbackSpeed} = this.state;
    return (
      <SafeAreaView style={styles.container}>
        <Text style={styles.header}>Kaltura Video Sample</Text>
        {videoSelected || changeMedia ?
          <View style={styles.mainContainer}>
            <View style={changeMedia ? styles.hide : styles.videoContainer}>
              <View style={styles.videoContainer}>
                <KalturaVideo
                  ottPartnerId={partnerId}
                  initOptions={initOptions}
                  logLevel={logLevel}
                  source={sources}
                  media={media}
                  playbackSpeed={playbackSpeed}
                  muted={isMuted}
                  style={styles.video}
                  ref={this.videoRef}
                  onPreparing={() => console.log("onPreparing called.")}
                  onReady={() => console.log("onReady called.")}
                  onPlaying={() => this.setState({ isPlaying: true })}
                  onPaused={() => this.setState({ isPlaying: false })}
                  onFinalized={() => console.log("onFinalized called.")}
                  onVideoBitrateChanged={(videoBitrate) => console.log("onVideoBitrateChanged called. " + videoBitrate)}
                  onAudioBitrateChanged={(audioBitrate) => console.log("onAudioBitrateChanged called. " + audioBitrate)}
                  onTotalBitrateChanged={(totalBitrate) => console.log("onTotalBitrateChanged called. " + totalBitrate)}
                  onPlaybackComplete={() => console.log("onPlaybackComplete called.")}
                  onBufferingStarted={() => console.log("onBufferingStarted called.")}
                  onBufferingEnded={() => console.log("onBufferingEnded called.")}
                  onCurrentTimeUpdated={(currentTime) => {
                    console.log("currentTime " + currentTime)
                    this.setState({ currentTime: currentTime })
                  }}
                  onKeepDeviceScreenOnUpdated={(keepOn) => {
                    console.log("onKeepDeviceScreenOnUpdated " + keepOn)
                    this.setState({ keepDeviceScreenOn: keepOn });
                  }}
                  onDurationChanged={(duration) => this.setState({ duration: duration })}
                  onSeekingEvent={(targetPosition) => {
                    console.log("onSeekingEvent " + targetPosition)
                  }}
                  onAvailableAudioTracksChanged={(tracks) => {
                    console.log("onAvailableAudioTracksChanged")
                    console.log(tracks.nativeEvent)
                  }}
                  onAvailableClosedCaptionsTracksChanged={(tracks) => {
                    console.log("onAvailableClosedCaptionsTracksChanged")
                    console.log(tracks.nativeEvent)
                  }}
                  onAvailableVideoTracksChanged={(tracks) => {
                    console.log("onAvailableVideoTracksChanged")
                    console.log(tracks.nativeEvent)
                  }}
                  onErrorOccurred={(error) => {
                    console.log("onErrorOccurred: " + error.nativeEvent.errorCode + "-" + error.nativeEvent.nativePlayerErrorCode + "-" + error.nativeEvent.message)
                  }}/>
              </View>
              <View style={styles.buttonContainer}>
                {/*<MyTouchableOpacity text={videoSelected ? 'Unload' : 'Load'} onPress={this.loadBtnPressed} />*/}
                {/*<MyTouchableOpacity text={'Change Media'} onPress={this.changeMediaPressed} />*/}
                <MyTouchableOpacity text={isPlaying ? 'Pause' : 'Play'} onPress={this.pauseBtnPressed} disabled={!videoSelected} />
                <MyTouchableOpacity text={keepDeviceScreenOn ? 'UnLock' : 'Lock'} onPress={this.keepDeviceScreenOnBtnPressed} disabled={!videoSelected} />
                <MyTouchableOpacity text={isMuted ? 'Unmute' : 'Mute'} onPress={this.muteBtnPressed} disabled={!videoSelected}/>
                <MyTouchableOpacity text={playbackSpeed == 1.0 ? "X1" : 'X2'} onPress={this.speedBtnPressed} disabled={!videoSelected}/>
                <MyTouchableOpacity text={'Seek +10s'} onPress={this.seekBtnPressed} disabled={!videoSelected}/>
                <MyTouchableOpacity text={'LOG'} onPress={this.logLevelBtnPressed} disabled={!videoSelected}/>
              </View>
              <TouchableOpacity
                  activeOpacity={0.7}
                  onPress={this.loadBtnPressed}
                  style={{...styles.touchableOpacityStyle, ...styles.closeButton}}>
                <Image
                    source={require('./assets/close.png')}
                    style={styles.floatingButtonStyle}
                />
              </TouchableOpacity>
              <TouchableOpacity
                  activeOpacity={0.7}
                  onPress={this.changeMediaPressed}
                  style={{...styles.touchableOpacityStyle, ...styles.changeMediaButton}}>
                <Image
                    source={require('./assets/menu_icon.png')}
                    style={styles.floatingButtonStyle}
                />
              </TouchableOpacity>
            </View>
            {changeMedia ?  <VideoGallery onVideoSelected={this.onVideoSelected}/> : undefined}
          </View>
          : <VideoGallery onVideoSelected={this.onVideoSelected}/>}
      </SafeAreaView>
    );
  }
}

export function MyTouchableOpacity(props) {
  return <TouchableOpacity
      disabled={props.disabled}
      style={props.disabled ? { ...(props.style? props.style : styles.button), ...styles.buttonDisabled } : props.style? props.style : styles.button }
      onPress={props.onPress}>
      <Text style={styles.buttonText}>{props.text}</Text>
  </TouchableOpacity>;
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: 'black',
    flex: 1
  },
  header: {
    color: 'white',
    fontWeight: 'bold',
    textAlign: 'center',
    flex: 0.2,
    marginBottom: 1
  },
  mainContainer: {
    // backgroundColor: 'darkgrey',
    width: "100%",
    height: "100%"
  },
  videoContainer: {
    flex: 10,
    backgroundColor: 'black',
  },
  hide: {
    display: 'none'
  },
  video: {
    width: "100%",
    height: "100%"
  },
  buttonContainer: {
    flex: 2,
    flexDirection: 'row',
    zIndex: 1000000
  },
  button: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'white',
    margin: 2,
    borderRadius: 3,
    maxWidth: 100
  },
  buttonDisabled: {
    backgroundColor: "#ccc",
    color: "#999"
  },
  buttonText: {
  },
  touchableOpacityStyle: {
    position: 'absolute',
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  floatingButtonStyle: {
    resizeMode: 'contain',
    width: 40,
    height: 40,
    //backgroundColor:'black'
  },
  closeButton: {
    left: 0,
    top: 0,
  },
  changeMediaButton: {
    right: 0,
    top: 0,
  }
})

AppRegistry.registerComponent("YiReactApp", () => YiReactApp);
