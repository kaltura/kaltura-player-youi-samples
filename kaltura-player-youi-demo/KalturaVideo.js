import React from 'react';
import { findNodeHandle, NativeModules, NativeEventEmitter } from 'react-native';
import { Video } from '@youi/react-native-youi';

const PlayerEventEmitter = new NativeEventEmitter(NativeModules.KalturaVideo);

export default class KalturaVideo extends React.Component {

  constructor(props) {
    super(props)
    this.playerEventEmitter = null
    this.videoRef = React.createRef();
    this.sanitizeProps(props)
  }

  sanitizeProps(props) {
    this.childProps = { ...props };
    delete this.childProps.source;
  }

  componentDidMount() {
    // Must be called before any other method on the native module
    NativeModules.KalturaVideo.ConnectToPlayer(findNodeHandle(this.videoRef.current));
    
    this.eventEmitter = PlayerEventEmitter.addListener('KALTURA_REPLAY_EVENT', () => {
      if (this.props.onReplayEvent) {
        this.props.onReplayEvent();
      }
    })

    this.eventEmitter = PlayerEventEmitter.addListener('KALTURA_STOPPED_EVENT', () => {
      if (this.props.onStoppedEvent) {
        this.props.onStoppedEvent();
      }
    })
    
    this.eventEmitter = PlayerEventEmitter.addListener('KALTURA_PLAYBACK_RATE_CHANGED_EVENT', (rate) => {
      if (this.props.onPlaybackRateChangedEvent) {
        this.props.onPlaybackRateChangedEvent(rate);
      }
    })

    this.eventEmitter = PlayerEventEmitter.addListener('KALTURA_SEEKED_EVENT', () => {
      if (this.props.onSeekedEvent) {
        this.props.onSeekedEvent();
      }
    })

    this.eventEmitter = PlayerEventEmitter.addListener('KALTURA_SEEKING_EVENT', (targetPosition) => {
      if (this.props.onSeekingEvent) {
        this.props.onSeekingEvent(targetPosition);
      }
    })

    this.eventEmitter = PlayerEventEmitter.addListener('KALTURA_LOAD_MEDIA_SUCCESS', (event) => {
      if (this.props.onLoadMediaSuccessEvent) {
        this.props.onLoadMediaSuccessEvent(event);
      }
    })

    this.eventEmitter = PlayerEventEmitter.addListener('KALTURA_VOLUME_CHANGED', (volume) => {
      if (this.props.onVolumeChanged) {
        this.props.onVolumeChanged(volume);
      }
    })

    this.eventEmitter = PlayerEventEmitter.addListener('KALTURA_AVAILABLE_VIDEO_TRACKS_CHANGED', (event) => {
      if (this.props.onAvailableVideoTracksChanged) {
        this.props.onAvailableVideoTracksChanged(event);
      }
    })

    this.eventEmitter = PlayerEventEmitter.addListener('KALTURA_BUFFER_TIME_UPDATED', (bufferPosition) => {
      if (this.props.onBufferTimeUpdated) {
        this.props.onBufferTimeUpdated(bufferPosition);
      }
    })

    this.eventEmitter = PlayerEventEmitter.addListener('KALTURA_KEEP_SCREEN_ON_CHANGED', (keepOn) => {
      if (this.props.onKeepDeviceScreenOnUpdated) {
        this.props.onKeepDeviceScreenOnUpdated(keepOn);
      }
    })

    this.videoRef.current.getPlayerInformation().then((playerInformation) => {
      console.log({
        name: playerInformation.name,
        version: playerInformation.version
      })
     })
     
    if (this.props.logLevel) {
       NativeModules.KalturaVideo.SetLogLevel(this.props.logLevel)
    }

    NativeModules.KalturaVideo.Setup(this.props.ottPartnerId, this.props.initOptions)
    
    if (this.props.media) {
      this.loadMedia(this.props.media.id, this.props.media.asset);
    } else if (this.props.source) {
      this.setMedia(this.props.source.uri);
    }

    if (this.props.playbackSpeed) {
      NativeModules.KalturaVideo.ChangePlaybackRate(this.props.playbackSpeed)
    }
  }

  componentDidUpdate(prevProps) {
    //Pass along updated props
    // let newChildProps = {...this.props};
    // delete newChildProps.source;
    // this.childProps = {...newChildProps};


    //Handle custom props
    if (this.props.selectedVideoTrack !== prevProps.selectedVideoTrack) {
      NativeModules.KalturaVideo.SelectVideoTrack(this.props.selectedVideoTrack);
    }

    if (this.props.media !== prevProps.media && this.props.media) {
      this.loadMedia(this.props.media.id, this.props.media.asset);
    }

    if (this.props.source !== prevProps.source && this.props.source) {
      this.setMedia(this.props.source.uri);
    }

    if (this.props.playbackSpeed != prevProps.playbackSpeed && this.props.playbackSpeed) {
        NativeModules.KalturaVideo.ChangePlaybackRate(this.props.playbackSpeed)
    }
    
    if (this.props.logLevel !== prevProps.logLevel && this.props.logLevel) {
      NativeModules.KalturaVideo.SetLogLevel(this.props.logLevel)
    }
  }

  render() {
    this.sanitizeProps(this.props);
    return <Video ref={this.videoRef} {...this.childProps} />
  }

  // Kaltura custom functions
  loadMedia = (assetId, options) => {
    NativeModules.KalturaVideo.LoadMedia(assetId, options)
  }

  setMedia = (url) => {
    NativeModules.KalturaVideo.SetMedia(url)
  }

  // Passthrough functions
  play = () => {
    this.videoRef.current.play()
  }

  pause = () => {
    this.videoRef.current.pause()
  }

  stop = () => {
    this.videoRef.current.stop()
  }

  replay = () => {
    NativeModules.KalturaVideo.Replay()
  }

  seek = (time) => {
    this.videoRef.current.seek(time)
  }

  keepDeviceScreenOn = (keepOn) => {
    NativeModules.KalturaVideo.KeepDeviceScreenOn(keepOn)
  }

  getStatistics = () => {
    return this.videoRef.current.getStatistics()
  };

  getPlayerInformation = () => {
    return this.videoRef.current.getPlayerInformation()
  };

  getBufferLength = () => {
    return this.videoRef.current.getBufferLength()
  };

  getLiveSeekableRanges = () => {
    return this.videoRef.current.getLiveSeekableRanges()
  };
}
