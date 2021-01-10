package tv.youi.kalturaplayertest.model;

import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.player.ABRSettings;
import com.kaltura.playkit.player.PKAspectRatioResizeMode;
import com.kaltura.playkit.player.PKMaxVideoSize;


import java.util.List;

public class InitOptions {
    public String  serverUrl;
    public boolean autoplay = false;
    public boolean preload = true;
    public boolean allowCrossProtocolRedirect = true;
    public RegisteredPlugins plugins;
    public List<String> warmupUrls;
    public String ks;
    public String referrer;
    public ABRSettings abrSettings;
    public NetworkSettings networkSettings;
    public TrackSelection trackSelection;
    public PKMediaFormat preferredMediaFormat;
    public Boolean allowClearLead;
    public Boolean enableDecoderFallback;
    public Boolean secureSurface;
    public Boolean adAutoPlayOnResume;
    public Boolean isVideoViewHidden;
    public Boolean forceSinglePlayerEngine;
    public PKAspectRatioResizeMode aspectRatioResizeMode;
    public Boolean isTunneledAudioPlayback;
    public Boolean handleAudioBecomingNoisyEnabled;
    public PKMaxVideoSize maxVideoSize;
    public Integer maxVideoBitrate;
    public Integer maxAudioBitrate;
    public Integer maxAudioChannelCount;
}
