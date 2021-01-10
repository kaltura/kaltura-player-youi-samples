package tv.youi.kalturaplayertest;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.netkit.utils.NKLog;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.PKAdErrorType;
import com.kaltura.playkit.player.LoadControlBuffers;
import com.kaltura.playkit.player.PKHttpClientManager;
import com.kaltura.playkit.player.PKPlayerErrorType;
import com.kaltura.playkit.player.PKTracks;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.broadpeak.BroadpeakConfig;
import com.kaltura.playkit.plugins.broadpeak.BroadpeakEvent;
import com.kaltura.playkit.plugins.broadpeak.BroadpeakPlugin;
import com.kaltura.playkit.plugins.ima.IMAConfig;
import com.kaltura.playkit.plugins.ima.IMAPlugin;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsConfig;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsPlugin;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsEvent;
import com.kaltura.playkit.plugins.youbora.YouboraPlugin;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.tvplayer.KalturaOttPlayer;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.PlayerInitOptions;
import com.npaw.youbora.lib6.YouboraLog;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tv.youi.kalturaplayertest.model.InitOptions;
import tv.youi.kalturaplayertest.model.MediaAsset;
import tv.youi.kalturaplayertest.model.NetworkSettings;
import tv.youi.kalturaplayertest.model.WrapperIMAConfig;
import tv.youi.kalturaplayertest.model.WrapperPhoenixAnalyticsConfig;
import tv.youi.kalturaplayertest.model.WrapperYouboraConfig;
import tv.youi.kalturaplayertest.model.tracks.AudioTrack;
import tv.youi.kalturaplayertest.model.tracks.TextTrack;
import tv.youi.kalturaplayertest.model.tracks.TracksInfo;
import tv.youi.kalturaplayertest.model.tracks.VideoTrack;
import tv.youi.youiengine.CYIActivity;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.kaltura.playkit.plugins.youbora.pluginconfig.YouboraConfig.KEY_HOUSEHOLD_ID;
import static com.npaw.youbora.lib6.plugin.Options.KEY_ACCOUNT_CODE;
import static com.npaw.youbora.lib6.plugin.Options.KEY_APP_NAME;
import static com.npaw.youbora.lib6.plugin.Options.KEY_APP_RELEASE_VERSION;
import static com.npaw.youbora.lib6.plugin.Options.KEY_CUSTOM_DIMENSION_1;
import static com.npaw.youbora.lib6.plugin.Options.KEY_CUSTOM_DIMENSION_2;
import static com.npaw.youbora.lib6.plugin.Options.KEY_ENABLED;
import static com.npaw.youbora.lib6.plugin.Options.KEY_USERNAME;
import static com.npaw.youbora.lib6.plugin.Options.KEY_USER_EMAIL;
import static com.npaw.youbora.lib6.plugin.Options.KEY_USER_TYPE;

@SuppressWarnings("unused")
public class PKPlayerWrapper {

    private static final PKLog log = PKLog.get("PKPlayerWrapper");
    private static final String YOUBORA_ACCOUNT_CODE = "accountCode";

    private static KalturaPlayer player;   // singleton player
    private static CYIActivity activity = CYIActivity.getCurrentActivity();
    private static CYIActivity.LifecycleListener lifecycleListener;
    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private static Class self = PKPlayerWrapper.class;

    private static OnEventListener onEventListener;

    private static void runOnUiThread(Runnable runnable) {
        if (mainHandler == null) {
            return;
        }
        mainHandler.post(runnable);
    }

    private static boolean initialized;
    private static boolean bringToFront;
    private static long reportedDuration = Consts.TIME_UNSET;
    static ByteBuffer bridgeInstance;

    static void sendEvent(String name, String payload) {
        if (bridgeInstance == null) {
            log.e("bridgeInstance is null");
            return;
        }
        nativeSendEvent(bridgeInstance, name, (payload != null ? payload : ""));
    }

    private static native void nativeSendEvent(ByteBuffer instance, String name, String payload);


    @SuppressWarnings("unused") // Called from C++
    public static void setup(ByteBuffer instance, int partnerId, String jsonOptionsStr) {

        log.d("setup:" + partnerId  + ", " + jsonOptionsStr);

        if (initialized) {
            log.d("*** initialized:" + partnerId  + ", " + jsonOptionsStr);
            return;
        }

        if (self == null) {
            self = PKPlayerWrapper.class;
        }

        if (activity == null) {
            activity = CYIActivity.getCurrentActivity();
        }

        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }

        activity.addLifecycleListener(getLifeCycleListener());
        bridgeInstance = instance;

        Context context = activity;

        Gson gson = new Gson();
        InitOptions initOptionsModel = gson.fromJson(jsonOptionsStr, InitOptions.class);
        if (initOptionsModel == null) {
            return;
        }

        if (initOptionsModel.warmupUrls != null && !initOptionsModel.warmupUrls.isEmpty()) {
            PKHttpClientManager.setHttpProvider("okhttp");
            PKHttpClientManager.warmUp((initOptionsModel.warmupUrls).toArray((new String[0])));
        }

        runOnUiThread(() -> {

            // load the player and put it in the main frame
            KalturaOttPlayer.initialize(context, partnerId, initOptionsModel.serverUrl);
            PKPluginConfigs pluginConfigs = new PKPluginConfigs();
            if (initOptionsModel.plugins != null) {
                if (initOptionsModel.plugins.ima != null) {
                    createIMAPlugin(pluginConfigs, initOptionsModel.plugins.ima); //DEFAULT
                }

                if (initOptionsModel.plugins.youbora != null) {
                    JsonObject accountCode = initOptionsModel.plugins.youbora;
                    if (accountCode.has(YOUBORA_ACCOUNT_CODE) && accountCode.get(YOUBORA_ACCOUNT_CODE) != null) {
                        createYouboraPlugin(pluginConfigs, new WrapperYouboraConfig().setAccountCode(accountCode.get(YOUBORA_ACCOUNT_CODE).getAsString()));
                    }
                }

                if (initOptionsModel.plugins.ottAnalytics != null) {
                    createPhoenixAnalyticsPlugin(pluginConfigs, initOptionsModel.plugins.ottAnalytics); //DEFAULT
                }

                if (initOptionsModel.plugins.broadpeak != null) {
                    JsonObject broadpeakJsonObject = initOptionsModel.plugins.broadpeak;

                    BroadpeakConfig broadpeakConfig = new Gson().fromJson(broadpeakJsonObject.toString(), BroadpeakConfig.class);
                    createBroadpeakPlugin(pluginConfigs, broadpeakConfig);
                }
            }

            final PlayerInitOptions initOptions = new PlayerInitOptions(partnerId);
            initOptions.setAutoPlay(initOptionsModel.autoplay);
            initOptions.setPreload(initOptionsModel.preload);
            initOptions.setAllowCrossProtocolEnabled(initOptionsModel.allowCrossProtocolRedirect);
            initOptions.setKs(initOptionsModel.ks);
            initOptions.setReferrer(initOptionsModel.referrer);
            initOptions.setAbrSettings(initOptionsModel.abrSettings);
            initOptions.setPreferredMediaFormat(initOptionsModel.preferredMediaFormat);
            initOptions.setSecureSurface(initOptionsModel.secureSurface);
            initOptions.setAspectRatioResizeMode(initOptionsModel.aspectRatioResizeMode);
            initOptions.setAllowClearLead(initOptionsModel.allowClearLead);
            initOptions.setEnableDecoderFallback(initOptionsModel.enableDecoderFallback);
            initOptions.setAdAutoPlayOnResume(initOptionsModel.adAutoPlayOnResume);
            initOptions.setIsVideoViewHidden(initOptionsModel.isVideoViewHidden);
            initOptions.forceSinglePlayerEngine(initOptionsModel.forceSinglePlayerEngine);
            initOptions.setTunneledAudioPlayback(initOptionsModel.isTunneledAudioPlayback);
            initOptions.setMaxAudioBitrate(initOptionsModel.maxAudioBitrate);
            initOptions.setMaxAudioChannelCount(initOptionsModel.maxAudioChannelCount);
            initOptions.setMaxVideoBitrate(initOptionsModel.maxVideoBitrate);
            initOptions.setMaxVideoSize(initOptionsModel.maxVideoSize);
            initOptions.setHandleAudioBecomingNoisy(initOptionsModel.handleAudioBecomingNoisyEnabled);

            NetworkSettings networkSettings = initOptionsModel.networkSettings;
            if (initOptionsModel.networkSettings != null && initOptionsModel.networkSettings.preferredForwardBufferDuration > 0) {
                initOptions.setLoadControlBuffers(new LoadControlBuffers().setMaxPlayerBufferMs(initOptionsModel.networkSettings.preferredForwardBufferDuration));
            }

            if (initOptionsModel.trackSelection != null && initOptionsModel.trackSelection.audioLanguage != null && initOptionsModel.trackSelection.audioMode != null) {
                initOptions.setAudioLanguage(initOptionsModel.trackSelection.audioLanguage, initOptionsModel.trackSelection.audioMode);
            }
            if (initOptionsModel.trackSelection != null && initOptionsModel.trackSelection.textLanguage != null && initOptionsModel.trackSelection.textMode != null) {
                initOptions.setTextLanguage(initOptionsModel.trackSelection.textLanguage, initOptionsModel.trackSelection.textMode);
            }

            //initOptions.setVideoCodecSettings(appPlayerInitConfig.videoCodecSettings)
            //initOptions.setAudioCodecSettings(appPlayerInitConfig.audioCodecSettings)
            initOptions.setPluginConfigs(pluginConfigs);

            player = KalturaOttPlayer.create(context, initOptions);

            ViewGroup parent = activity.getMainLayout();

            player.setPlayerView(MATCH_PARENT, MATCH_PARENT);
            parent.addView(player.getPlayerView(), 0);
            addKalturaPlayerListeners();

            initialized = true;

            sendPlayerEvent("playerInitialized");
            if (onEventListener != null) {
                onEventListener.onKalturaPlayerInitialized();
            }
        });
    }

    private static CYIActivity.LifecycleListener getLifeCycleListener() {
        if (lifecycleListener == null) {
            lifecycleListener = new CYIActivity.LifecycleListener() {
                @Override
                public void onActivityStarted(CYIActivity cyiActivity) {
                    log.d("addLifecycleListener onResume");
                    if (player != null) {
                        player.onApplicationResumed();
                    }
                }

                @Override
                public void onActivityStopped(CYIActivity cyiActivity) {
                    log.d("addLifecycleListener onPause");
                    if (player != null) {
                        player.onApplicationPaused();
                    }
                }
            };
        }
        return lifecycleListener;
    }

    private static void addKalturaPlayerListeners() {
        log.d("addKalturaPlayerListeners");

        if (player == null) {
            return;
        }

        player.addListener(self, PlayerEvent.canPlay, event -> sendPlayerEvent("canPlay"));
        player.addListener(self, PlayerEvent.playing, event -> sendPlayerEvent("playing"));
        player.addListener(self, PlayerEvent.play, event -> sendPlayerEvent("play"));
        player.addListener(self, PlayerEvent.pause, event -> sendPlayerEvent("pause"));
        player.addListener(self, PlayerEvent.ended, event -> sendPlayerEvent("ended"));
        player.addListener(self, PlayerEvent.stopped, event -> sendPlayerEvent("stopped"));
        player.addListener(self, PlayerEvent.replay, event -> sendPlayerEvent("replay"));

        player.addListener(self, PlayerEvent.durationChanged, event -> sendPlayerEvent("durationChanged", "{ \"duration\": " +  (event.duration / Consts.MILLISECONDS_MULTIPLIER_FLOAT) + " }"));
        player.addListener(self, PlayerEvent.playheadUpdated, new PKEvent.Listener<PlayerEvent.PlayheadUpdated>() {
            @Override
            public void onEvent(PlayerEvent.PlayheadUpdated event) {
                sendPlayerEvent("timeUpdate", "{ \"position\": " + (event.position / Consts.MILLISECONDS_MULTIPLIER_FLOAT) +
                        ", \"bufferPosition\": " + (event.bufferPosition / Consts.MILLISECONDS_MULTIPLIER_FLOAT) +
                        " }");
                if (reportedDuration != event.duration && event.duration > 0) {
                    reportedDuration = event.duration;
                    if (player.getMediaEntry() != null && player.getMediaEntry().getMediaType() != PKMediaEntry.MediaEntryType.Vod /*|| player.isLive()*/) {
                        sendPlayerEvent("loadedTimeRanges", "{\"timeRanges\": [ { \"start\": " + 0 +
                                ", \"end\": " + (event.duration / Consts.MILLISECONDS_MULTIPLIER_FLOAT) +
                                " } ] }");
                    }
                }
            }
        });
        player.addListener(self, PlayerEvent.stateChanged, event -> sendPlayerEvent("stateChanged", "{ \"newState\": \"" + event.newState.name() + "\" }"));
        player.addListener(self, PlayerEvent.volumeChanged, event -> sendPlayerEvent("volumeChanged", "{ \"volume\": \"" + event.volume + "\" }"));
        player.addListener(self, PlayerEvent.tracksAvailable, event -> { sendPlayerEvent("tracksAvailable", new Gson().toJson(getTracksInfo(event.tracksInfo))); });
        player.addListener(self, PlayerEvent.playbackRateChanged, event -> { sendPlayerEvent("playbackRateChanged", "{ \"playbackRate\": \"" + event.rate + "\" }"); });

        player.addListener(self, PlayerEvent.videoTrackChanged, event -> {
            final com.kaltura.playkit.player.VideoTrack newTrack = event.newTrack;
            VideoTrack videoTrack = new VideoTrack(newTrack.getUniqueId(), newTrack.getWidth(), newTrack.getHeight(), newTrack.getBitrate(), true, newTrack.isAdaptive());
            sendPlayerEvent("videoTrackChanged", new Gson().toJson(videoTrack));
        });

        player.addListener(self, PlayerEvent.audioTrackChanged, event -> {
            final com.kaltura.playkit.player.AudioTrack newTrack = event.newTrack;
            AudioTrack audioTrack = new AudioTrack(newTrack.getUniqueId(), newTrack.getBitrate(), newTrack.getLanguage(), newTrack.getLabel(), newTrack.getChannelCount(), true);
            sendPlayerEvent("audioTrackChanged", new Gson().toJson(audioTrack));
        });

        player.addListener(self, PlayerEvent.textTrackChanged, event -> {
            final com.kaltura.playkit.player.TextTrack newTrack = event.newTrack;
            TextTrack textTrack = new TextTrack(newTrack.getUniqueId(), newTrack.getLanguage(), newTrack.getLabel(), true);
            sendPlayerEvent("textTrackChanged", new Gson().toJson(textTrack));
        });

        player.addListener(self, PlayerEvent.playbackInfoUpdated, event -> {
            long videoBitrate = (event.playbackInfo.getVideoBitrate() > 0) ? event.playbackInfo.getVideoBitrate() : 0;
            long audioBitrate = (event.playbackInfo.getAudioBitrate() > 0) ? event.playbackInfo.getAudioBitrate() : 0;
            long totalBitrate = (videoBitrate + audioBitrate);
            sendPlayerEvent("playbackInfoUpdated", "{ \"videoBitrate\": " + event.playbackInfo.getVideoBitrate() +
                    ", \"audioBitrate\": " + event.playbackInfo.getAudioBitrate() +
                    ", \"totalBitrate\": " + totalBitrate  +
                    " }");
        });

        player.addListener(self, PlayerEvent.seeking, event -> sendPlayerEvent("seeking", "{ \"targetPosition\": " + (event.targetPosition / Consts.MILLISECONDS_MULTIPLIER_FLOAT) + " }"));
        player.addListener(self, PlayerEvent.seeked, event -> sendPlayerEvent("seeked"));
        player.addListener(self, PlayerEvent.error, event -> {
            if (event.error.isFatal()) {
                sendPlayerEvent("error", getErrorJson(event.error));
            }
        });

        player.addListener(self, PhoenixAnalyticsEvent.bookmarkError, event -> sendPlayerEvent("bookmarkError", "{ \"errorMessage\": \"" + event.errorMessage + "\" }"));
        player.addListener(self, PhoenixAnalyticsEvent.concurrencyError, event -> sendPlayerEvent("concurrencyError", "{ \"errorMessage\": \"" + event.errorMessage + "\" }"));

        player.addListener(self, BroadpeakEvent.error, event -> {
            PKError bpError = new PKError(PKPlayerErrorType.SOURCE_ERROR, PKError.Severity.Fatal, "BroadpeakError:" + event.errorCode + "-" + event.errorMessage, null);
            sendPlayerEvent("error", getErrorJson(bpError));
        });

        player.addListener(self, AdEvent.adProgress, event -> sendPlayerEvent("adProgress", "{ \"currentAdPosition\": " + (event.currentAdPosition / Consts.MILLISECONDS_MULTIPLIER_FLOAT) + " }"));
        player.addListener(self, AdEvent.cuepointsChanged, event -> sendPlayerEvent("adCuepointsChanged", getCuePointsJson(event.cuePoints)));
        player.addListener(self, AdEvent.started, event -> sendPlayerEvent("adStarted"));
        player.addListener(self, AdEvent.completed, event -> sendPlayerEvent("adCompleted"));
        player.addListener(self, AdEvent.paused, event -> sendPlayerEvent("adPaused"));
        player.addListener(self, AdEvent.resumed, event -> sendPlayerEvent("adResumed"));
        player.addListener(self, AdEvent.adBufferStart, event -> sendPlayerEvent("adBufferStart"));
        player.addListener(self, AdEvent.adClickedEvent, event -> sendPlayerEvent("adClicked", "{ \"clickThruUrl\": \"" + event.clickThruUrl + "\" }"));
        player.addListener(self, AdEvent.skipped, event -> sendPlayerEvent("adSkipped"));
        player.addListener(self, AdEvent.adRequested, event -> sendPlayerEvent("adRequested", "{ \"adTagUrl\": \"" +  event.adTagUrl + "\" }"));
        player.addListener(self, AdEvent.contentPauseRequested, event -> sendPlayerEvent("adContentPauseRequested"));
        player.addListener(self, AdEvent.contentResumeRequested, event -> sendPlayerEvent("adContentResumeRequested"));
        player.addListener(self, AdEvent.allAdsCompleted, event -> sendPlayerEvent("allAdsCompleted"));
        player.addListener(self, AdEvent.error, event -> {
            if (event.error.isFatal()) {
                sendPlayerEvent("adError", getErrorJson(event.error));
            }
        });
    }

    private static TracksInfo getTracksInfo(PKTracks pkTracksInfo) {
        List<VideoTrack> videoTracksInfo = new ArrayList<>();
        List<AudioTrack> audioTracksInfo = new ArrayList<>();
        List<TextTrack>  textTracksInfo  = new ArrayList<>();

        int videoTrackIndex = 0;
        for (com.kaltura.playkit.player.VideoTrack videoTrack : pkTracksInfo.getVideoTracks()) {
            videoTracksInfo.add(new VideoTrack(videoTrack.getUniqueId(),
                    videoTrack.getWidth(),
                    videoTrack.getHeight(),
                    videoTrack.getBitrate(),
                    pkTracksInfo.getDefaultVideoTrackIndex() == videoTrackIndex,
                    videoTrack.isAdaptive()));
            videoTrackIndex++;
        }

        int audioTrackIndex = 0;
        for (com.kaltura.playkit.player.AudioTrack audioTrack : pkTracksInfo.getAudioTracks()) {
            audioTracksInfo.add(new AudioTrack(audioTrack.getUniqueId(),
                    audioTrack.getBitrate(),
                    audioTrack.getLanguage(),
                    audioTrack.getLabel(),
                    audioTrack.getChannelCount(),
                    pkTracksInfo.getDefaultAudioTrackIndex() == audioTrackIndex));
            audioTrackIndex++;
        }

        int textTrackIndex = 0;
        for (com.kaltura.playkit.player.TextTrack textTrack : pkTracksInfo.getTextTracks()) {
            textTracksInfo.add(new TextTrack(textTrack.getUniqueId(),
                    textTrack.getLanguage(),
                    textTrack.getLabel(),
                    pkTracksInfo.getDefaultTextTrackIndex() == textTrackIndex));
            textTrackIndex++;
        }

        TracksInfo tracksInfo = new TracksInfo();
        tracksInfo.setVideoTracks(videoTracksInfo);
        tracksInfo.setAudioTracks(audioTracksInfo);
        tracksInfo.setTextTracks(textTracksInfo);
        return tracksInfo;
    }

    private static String getErrorJson(PKError error) {
        String errorCause = (error.exception != null) ? error.exception.getCause() + "" : "";
        JsonObject errorJson = new JsonObject();
        errorJson.addProperty("errorType", error.errorType.name());
        if (error.errorType instanceof PKPlayerErrorType) {
            errorJson.addProperty("errorCode", String.valueOf(((PKPlayerErrorType) error.errorType).errorCode));
        } else if (error.errorType instanceof PKAdErrorType) {
            errorJson.addProperty("errorCode", String.valueOf(((PKAdErrorType) error.errorType).errorCode));
        } else {
            errorJson.addProperty("errorCode", String.valueOf(((PKPlayerErrorType) PKPlayerErrorType.UNEXPECTED).errorCode));
        }        
        errorJson.addProperty("errorSeverity", error.severity.name());
        errorJson.addProperty("errorMessage", error.message);
        errorJson.addProperty("errorCause", errorCause);
        return new Gson().toJson(errorJson);
    }

    private static String getCuePointsJson(AdCuePoints adCuePoints) {

        if (adCuePoints == null) {
            return null;
        }

        StringBuilder cuePointsList = new StringBuilder("[ ");
        List<Long> adCuePointsArray = adCuePoints.getAdCuePoints();
        for (int i = 0; i < adCuePointsArray.size() ; i++) {
            cuePointsList.append(adCuePointsArray.get(i));
            if (i + 1 != adCuePointsArray.size()) {
                cuePointsList.append(", ");
            }
        }
        cuePointsList.append(" ]");

        return "{ " +
                "\"adPluginName\": \"" + adCuePoints.getAdPluginName() +
                "\"," +
                "\"cuePoints\": " + cuePointsList +
                ", " +
                "\"hasPreRoll\": " + adCuePoints.hasPreRoll() +
                ", " +
                "\"hasMidRoll\": " + adCuePoints.hasMidRoll() +
                "," +
                "\"hasPostRoll\": " + adCuePoints.hasPostRoll() +
                " }";
    }

    @SuppressWarnings("unused") // Called from C++
    public static void loadMedia(final String assetId, final String jsonOptionsStr) {

        log.d("loadMedia assetId: " + assetId + ", jsonOptionsStr:" + jsonOptionsStr);
        reportedDuration = Consts.TIME_UNSET;

        if (TextUtils.isEmpty(assetId) || TextUtils.isEmpty(jsonOptionsStr)) {
            return;
        }

        if (!initialized) {
            onEventListener = () -> {
                log.d("delayed loadMedia assetId: " + assetId + ", jsonOptionsStr:" + jsonOptionsStr);
                loadMediaInUIThread(assetId, jsonOptionsStr);

            };
            return;
        }

        log.d("regular loadMedia assetId: " + assetId + ", jsonOptionsStr:" + jsonOptionsStr);
        loadMediaInUIThread(assetId, jsonOptionsStr);
    }

    private static void loadMediaInUIThread(String assetId, String jsonOptionsStr) {
        runOnUiThread(() -> {
            log.d("load media in UI thread");
            if (bringToFront) {
                setZIndex(1);
                bringToFront = false;
            }

            Gson gson = new Gson();
            MediaAsset mediaAsset = gson.fromJson(jsonOptionsStr, MediaAsset.class);
            if (mediaAsset == null || player == null) {
                return;
            }
            if (mediaAsset.getPlugins() != null) {
                if (mediaAsset.getPlugins().ima != null) {
                    updatgeIMAPlugin(mediaAsset.getPlugins().ima);
                }

                if (mediaAsset.getPlugins().youbora != null) {
                    updateYouboraPlugin(mediaAsset.getPlugins().youbora);
                }

                if (mediaAsset.getPlugins().ottAnalytics != null) {
                    updatePhoenixAnalyticsPlugin(mediaAsset.getPlugins().ottAnalytics);
                }
            }
            final PKMediaEntry localPlaybackEntry = PKDownloadWrapper.getLocalPlaybackEntry(assetId);
            if (localPlaybackEntry != null) {
                player.setMedia(localPlaybackEntry);
            } else {
                player.loadMedia(mediaAsset.buildOttMediaOptions(assetId, player.getKS()), (entry, error) -> {
                    if (error != null) {
                        log.d("ott media load error: " + error + " assetId = " + assetId);
                        //code, extra, message, name
                        sendPlayerEvent("loadMediaFailed", gson.toJson(error));

                    } else {
                        log.d("ott media load success name = " + entry.getName() + " id = " + assetId);
                        sendPlayerEvent("loadMediaSuccess", "{ \"id\": \"" + entry.getId() + "\" }");
                    }
                });
            }
        });
    }

    @SuppressWarnings("unused") // Called from C++
    public static void setMedia(final String contentUrl) {

        log.d("setMedia contentUrl: " + contentUrl);
        reportedDuration = Consts.TIME_UNSET;

        if (TextUtils.isEmpty(contentUrl)) {
            return;
        }

        if (!initialized) {
            onEventListener = () -> {
                log.d("delayed setMedia contentUrl: " + contentUrl);
                setMediaInUIThread(contentUrl);
            };
            return;
        }

        log.d("regular setMedia contentUrl: " + contentUrl);
        setMediaInUIThread(contentUrl);
    }

    private static void setMediaInUIThread(String contentUrl) {
        log.d("setMedia in UI thread");

        runOnUiThread(() -> {
            if (bringToFront) {
                setZIndex(1);
                bringToFront = false;
            }

            PKMediaEntry pkMediaEntry = new PKMediaEntry();
            pkMediaEntry.setMediaType(PKMediaEntry.MediaEntryType.Vod);
            pkMediaEntry.setId("1234");
            PKMediaSource pkMediaSource = new PKMediaSource();
            pkMediaSource.setId("1234");
            pkMediaSource.setUrl(contentUrl);
            pkMediaEntry.setSources(Collections.singletonList(pkMediaSource));
            player.setMedia(pkMediaEntry);
            sendPlayerEvent("loadMediaSuccess", new Gson().toJson(pkMediaEntry));
        });
    }

    private static void updatgeIMAPlugin(WrapperIMAConfig wrapperIMAConfig) {
        if (player != null) {
            runOnUiThread(() -> player.updatePluginConfig(IMAPlugin.factory.getName(), getIMAConfig(wrapperIMAConfig)));
        }
    }

    private static void updateYouboraPlugin(WrapperYouboraConfig wrapperYouboraConfig) {
        if (player != null) {
            runOnUiThread(() -> player.updatePluginConfig(YouboraPlugin.factory.getName(), getYouboraBundle(wrapperYouboraConfig)));
        }
    }

    private static void updatePhoenixAnalyticsPlugin(WrapperPhoenixAnalyticsConfig wrapperPhoenixAnalyticsConfig) {
        if (player != null && wrapperPhoenixAnalyticsConfig != null) {
            runOnUiThread(() -> player.updatePluginConfig(PhoenixAnalyticsPlugin.factory.getName(), wrapperPhoenixAnalyticsConfig.toJson()));
        }
    }

    private static void createIMAPlugin(PKPluginConfigs pluginConfigs, JsonObject imaConfigJson) {

        PlayKitManager.registerPlugins(activity, IMAPlugin.factory);
        if (pluginConfigs != null) {
            if (imaConfigJson == null) {
                pluginConfigs.setPluginConfig(IMAPlugin.factory.getName(), new IMAConfig());
            } else {
                pluginConfigs.setPluginConfig(IMAPlugin.factory.getName(), imaConfigJson);
            }
        }
    }

    private static IMAConfig getIMAConfig(WrapperIMAConfig wrapperIMAConfig) {
        IMAConfig imaConfig = new IMAConfig();

        if (wrapperIMAConfig != null) {
            String adTagUrl = (wrapperIMAConfig.getAdTagUrl() != null) ? wrapperIMAConfig.getAdTagUrl() : "";
            if (!TextUtils.isEmpty(wrapperIMAConfig.getAdTagResponse()) && TextUtils.isEmpty(adTagUrl)) {
                imaConfig.setAdTagResponse(wrapperIMAConfig.getAdTagResponse());
            } else {
                imaConfig.setAdTagUrl(adTagUrl);
            }
            imaConfig.setVideoBitrate(wrapperIMAConfig.getVideoBitrate());
            imaConfig.enableDebugMode(wrapperIMAConfig.isEnableDebugMode());
            imaConfig.setVideoMimeTypes(wrapperIMAConfig.getVideoMimeTypes());
            imaConfig.setAdLoadTimeOut(wrapperIMAConfig.getAdLoadTimeOut());
        }
        return imaConfig;
    }

    private static void createYouboraPlugin(PKPluginConfigs pluginConfigs, WrapperYouboraConfig wrapperYouboraConfig) {

        PlayKitManager.registerPlugins(activity, YouboraPlugin.factory);
        if (pluginConfigs != null) {
            pluginConfigs.setPluginConfig(YouboraPlugin.factory.getName(), getYouboraBundle(wrapperYouboraConfig));
        }
    }

    private static Bundle getYouboraBundle(WrapperYouboraConfig wrapperYouboraConfig) {
        Bundle optBundle = new Bundle();
        if (wrapperYouboraConfig != null) {
            //Youbora config bundle. Main config goes here.
            optBundle.putString(KEY_ACCOUNT_CODE, wrapperYouboraConfig.getAccountCode());
            if (wrapperYouboraConfig.getUsername() != null) {
                optBundle.putString(KEY_USERNAME, wrapperYouboraConfig.getUsername());
            }
            if (wrapperYouboraConfig.getUserEmail() != null) {
                optBundle.putString(KEY_USER_EMAIL, wrapperYouboraConfig.getUserEmail());
            }
            if (wrapperYouboraConfig.getUserType() != null) {
                optBundle.putString(KEY_USER_TYPE, wrapperYouboraConfig.getUserType());
            }
            if (wrapperYouboraConfig.getAppName() != null) {
                optBundle.putString(KEY_APP_NAME, wrapperYouboraConfig.getAppName());
            }
            if (wrapperYouboraConfig.getAppReleaseVersion() != null) {
                optBundle.putString(KEY_APP_RELEASE_VERSION, wrapperYouboraConfig.getAppReleaseVersion());
            }
            if (wrapperYouboraConfig.getHouseHoldId() != null) {
                optBundle.putString(KEY_HOUSEHOLD_ID, wrapperYouboraConfig.getHouseHoldId());
            }
            if (wrapperYouboraConfig.getExtraParam1() != null) {
                optBundle.putString(KEY_CUSTOM_DIMENSION_1, wrapperYouboraConfig.getExtraParam1());
            }
            if (wrapperYouboraConfig.getExtraParam2() != null) {
                optBundle.putString(KEY_CUSTOM_DIMENSION_2, wrapperYouboraConfig.getExtraParam2());
            }
            optBundle.putBoolean(KEY_ENABLED, true);
        }
        return optBundle;
    }

    private static void createBroadpeakPlugin(PKPluginConfigs pluginConfigs, BroadpeakConfig broadpeakConfig) {
        PlayKitManager.registerPlugins(activity, BroadpeakPlugin.factory);
        if (pluginConfigs != null) {
            pluginConfigs.setPluginConfig(BroadpeakPlugin.factory.getName(), broadpeakConfig);
        }
    }

    private static void createPhoenixAnalyticsPlugin(PKPluginConfigs pluginConfigs, JsonObject phoenixAnalyticsConfigJson) {
        PlayKitManager.registerPlugins(activity, PhoenixAnalyticsPlugin.factory);
        if (pluginConfigs != null) {
            if (phoenixAnalyticsConfigJson == null) {
                pluginConfigs.setPluginConfig(PhoenixAnalyticsPlugin.factory.getName(), new PhoenixAnalyticsConfig(-1, "", "", Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH_SEC));
            } else {
                pluginConfigs.setPluginConfig(PhoenixAnalyticsPlugin.factory.getName(), phoenixAnalyticsConfigJson);
            }
        }
    }

    @SuppressWarnings("unused") // Called from C++
    public static void prepare() {

        log.d("prepare");
        reportedDuration = Consts.TIME_UNSET;
        if (player != null) {
            runOnUiThread(() -> player.prepare());
        }
    }

    @SuppressWarnings("unused") // Called from C++
    public static void play() {

        log.d("play");
        if (player != null) {
            runOnUiThread(() -> player.play());
        }
    }

    @SuppressWarnings("unused") // Called from C++
    public static void pause() {

        log.d("pause");
        if (player != null) {
            runOnUiThread(() -> player.pause());
        }
    }

    @SuppressWarnings("unused") // Called from C++
    public static void replay() {

        log.d("replay");
        if (player != null) {
            runOnUiThread(() -> player.replay());
        }
    }

    @SuppressWarnings("unused") // Called from C++
    public static void seekTo(float position) {

        long posMS = (long)(position * Consts.MILLISECONDS_MULTIPLIER);
        log.d("seekTo:" + posMS);
        if (player != null) {
            runOnUiThread(() -> player.seekTo(posMS));
        }
    }

    @SuppressWarnings("unused") // Called from C++
    public static void changeTrack(String uniqueId) {

        log.d("changeTrack:" + uniqueId);
        if (player != null) {
            runOnUiThread(() -> player.changeTrack(uniqueId));
        }
    }

    @SuppressWarnings("unused") // Called from C++
    public static void changePlaybackRate(float playbackRate) {

        log.d("changePlaybackRate:" + playbackRate);
        if (player != null) {
            runOnUiThread(() -> player.setPlaybackRate(playbackRate));
        }
    }

    @SuppressWarnings("unused") // Called from C++
    public static void stop() {

        log.d("stop");
        if (player != null) {
            runOnUiThread(() -> player.stop());
        }
    }

    @SuppressWarnings("unused") // Called from C++
    public static void destroy() {

        log.d("destroy");

        if (lifecycleListener != null) {
            activity.removeLifecycleListener(lifecycleListener);
            lifecycleListener = null;
        }

        runOnUiThread(() -> {
            if (player != null) {
                player.removeListeners(self);
                player.pause();
                player.destroy();
                player = null;
            }
        });

        onEventListener = null;
        activity = null;
        mainHandler = null;
        bridgeInstance = null;
        initialized = false;
        bringToFront = false;
        reportedDuration = Consts.TIME_UNSET;
        self = null;
    }

    @SuppressWarnings("unused") // Called from C++
    public static void setAutoplay(boolean autoplay) {

        log.d("setAutoplay: " + autoplay);
        if (player != null) {
            runOnUiThread(() -> player.setAutoPlay(autoplay));
        }
    }

    @SuppressWarnings("unused") // Called from C++
    public static void setKS(String ks) {

        log.d("setKS: " + ks);
        if (player != null) {
            runOnUiThread(() -> player.setKS(ks));
        }
    }

    @SuppressWarnings("unused") // Called from C++
    public static void setZIndex(float index) {

        log.d("setZIndex: " + index);

        if (!initialized) {
            log.d("delayed setZIndex index: " + index);
            bringToFront = true;
            return;
        }

        log.d("regular setZIndex index: " + index);
        if (player != null && player.getPlayerView() != null) {
            runOnUiThread(() -> player.getPlayerView().setZ(index));
        }
    }

    @SuppressWarnings("unused") // Called from C++
    public static void setFrame(int playerViewWidth, int playerViewHeight, int playerViewPosX, int playerViewPosY) {

        log.d("setFrame " + playerViewWidth + "/" + playerViewHeight + " " + playerViewPosX + "/" + playerViewPosY);

        if (player != null && player.getPlayerView() != null) {
            runOnUiThread(() -> {
                ViewGroup.LayoutParams layoutParams = player.getPlayerView().getLayoutParams();

                if (layoutParams != null) {
                    layoutParams.width = playerViewWidth >= 0 ? playerViewWidth : MATCH_PARENT;
                    layoutParams.height = playerViewHeight >= 0 ? playerViewHeight : MATCH_PARENT;
                    player.getPlayerView().setLayoutParams(layoutParams);
                }

                if (playerViewPosX > 0) {
                    player.getPlayerView().setTranslationX(playerViewPosX);
                } else {
                    player.getPlayerView().setTranslationX(0);
                }

                if (playerViewPosY > 0) {
                    player.getPlayerView().setTranslationY(playerViewPosY);
                } else {
                    player.getPlayerView().setTranslationY(0);
                }
            });
        }
    }

    @SuppressWarnings("unused") // Called from C++
    public static void setVolume(float volume) {

        log.d("setVolume: " + volume);
        if (volume < 0) {
            volume = 0f;
        } else if (volume > 1) {
            volume = 1.0f;
        }

        if (player != null) {
            float finalPlayerVolume = volume;
            runOnUiThread(() -> player.setVolume(finalPlayerVolume));
        }
    }

    @SuppressWarnings("unused") // Called from C++
    public static void setLogLevel(String logLevel) {

        if (TextUtils.isEmpty(logLevel)) {
            return;
        }
        logLevel = logLevel.toUpperCase();
        String logMessage = "setLogLevel: " + logLevel;
        switch (logLevel) {
            case "VERBOSE":
                PKLog.setGlobalLevel(PKLog.Level.verbose);
                NKLog.setGlobalLevel(NKLog.Level.verbose);
                YouboraLog.setDebugLevel(YouboraLog.Level.VERBOSE);
                log.v(logMessage);
                break;
            case "DEBUG":
                PKLog.setGlobalLevel(PKLog.Level.debug);
                NKLog.setGlobalLevel(NKLog.Level.debug);
                YouboraLog.setDebugLevel(YouboraLog.Level.DEBUG);
                log.d(logMessage);
                break;
            case "WARN":
                PKLog.setGlobalLevel(PKLog.Level.warn);
                NKLog.setGlobalLevel(NKLog.Level.warn);
                YouboraLog.setDebugLevel(YouboraLog.Level.WARNING);
                log.w(logMessage);
                break;
            case "INFO":
                PKLog.setGlobalLevel(PKLog.Level.info);
                NKLog.setGlobalLevel(NKLog.Level.info);
                YouboraLog.setDebugLevel(YouboraLog.Level.NOTICE);
                log.i(logMessage);
                break;
            case "ERROR":
                PKLog.setGlobalLevel(PKLog.Level.error);
                NKLog.setGlobalLevel(NKLog.Level.error);
                YouboraLog.setDebugLevel(YouboraLog.Level.ERROR);
                log.e(logMessage);
                break;
            case "OFF":
                PKLog.setGlobalLevel(PKLog.Level.off);
                NKLog.setGlobalLevel(NKLog.Level.off);
                YouboraLog.setDebugLevel(YouboraLog.Level.SILENT);
                break;
            default:
                log.e("setLogLevel unknown level: " + logLevel);
        }
    }

    static void sendPlayerEvent(String type, String payload) {
        sendEvent(type, payload);
    }

    static void sendPlayerEvent(String type) {
        sendPlayerEvent(type, null);
    }
}
