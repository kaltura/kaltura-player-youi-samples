package tv.youi.kalturaplayertest.model.tracks;

import java.util.ArrayList;
import java.util.List;

public class TracksInfo {
    List<VideoTrack> video = new ArrayList<>();
    List<TextTrack> text = new ArrayList<>();
    List<AudioTrack> audio = new ArrayList<>();

    public TracksInfo setVideoTracks(List<VideoTrack> video) {
        this.video = video;
        return this;
    }

    public TracksInfo setTextTracks(List<TextTrack> text) {
        this.text = text;
        return this;
    }

    public TracksInfo setAudioTracks(List<AudioTrack> audio) {
        this.audio = audio;
        return this;
    }
}
