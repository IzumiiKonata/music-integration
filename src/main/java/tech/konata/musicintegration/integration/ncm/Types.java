package tech.konata.musicintegration.integration.ncm;

import com.google.gson.annotations.SerializedName;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * @author IzumiiKonata
 * Date: 2025/7/1 20:22
 */
@UtilityClass
public class Types {

    public enum PlayingStatusType {
        EXIT, SET_PLAYING, SET_PLAYING_POSITION, SET_PLAYING_STATUS, PRECACHE_NEXT
    }

    // Playing Status Types
    public class Artist {
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("alia")
        public List<String> alia;
        @SerializedName("alias")
        public List<String> alias;
        @SerializedName("transName")
        public String transName;
    }

    public class Album {
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("description")
        public String description;
        @SerializedName("trackCount")
        public int trackCount;
        @SerializedName("subscribedCount")
        public int subscribedCount;
        @SerializedName("commentCount")
        public int commentCount;
        @SerializedName("commentThreadId")
        public String commentThreadId;
        @SerializedName("algorithm")
        public String algorithm;
        @SerializedName("size")
        public int size;
        @SerializedName("albumName")
        public String albumName;
        @SerializedName("picId")
        public String picId;
        @SerializedName("picUrl")
        public String picUrl;
        @SerializedName("cover")
        public String cover;
        @SerializedName("alias")
        public List<String> alias;
        @SerializedName("transNames")
        public List<String> transNames;
        @SerializedName("explicit")
        public boolean explicit;
    }

    public class FreeTrialPrivilege {
        @SerializedName("resConsumable")
        public boolean resConsumable;
        @SerializedName("userConsumable")
        public boolean userConsumable;
        @SerializedName("listenType")
        public Object listenType;
        @SerializedName("cannotListenReason")
        public Object cannotListenReason;
        @SerializedName("playReason")
        public Object playReason;
        @SerializedName("freeLimitTagType")
        public Object freeLimitTagType;
    }

    public class Privilege {
        @SerializedName("id")
        public String id;
        @SerializedName("fee")
        public int fee;
        @SerializedName("payed")
        public int payed;
        @SerializedName("maxPlayBr")
        public int maxPlayBr;
        @SerializedName("maxDownBr")
        public int maxDownBr;
        @SerializedName("commentPriv")
        public int commentPriv;
        @SerializedName("cloudSong")
        public int cloudSong;
        @SerializedName("toast")
        public Boolean toast;
        @SerializedName("flag")
        public int flag;
        @SerializedName("now")
        public Long now;
        @SerializedName("maxSongBr")
        public int maxSongBr;
        @SerializedName("maxFreeBr")
        public int maxFreeBr;
        @SerializedName("sharePriv")
        public Integer sharePriv;
        @SerializedName("status")
        public int status;
        @SerializedName("subPriv")
        public int subPriv;
        @SerializedName("maxSongLevel")
        public Integer maxSongLevel;
        @SerializedName("maxDownLevel")
        public Integer maxDownLevel;
        @SerializedName("maxFreeLevel")
        public Integer maxFreeLevel;
        @SerializedName("maxPlayLevel")
        public Integer maxPlayLevel;
        @SerializedName("freeTrialPrivilege")
        public FreeTrialPrivilege freeTrialPrivilege;
    }

    public class Track {
        @SerializedName("id")
        public String id;
        @SerializedName("commentThreadId")
        public String commentThreadId;
        @SerializedName("copyrightId")
        public String copyrightId;
        @SerializedName("duration")
        public int duration;
        @SerializedName("mvid")
        public String mvid;
        @SerializedName("name")
        public String name;
        @SerializedName("status")
        public int status;
        @SerializedName("fee")
        public int fee;
        @SerializedName("songType")
        public int songType;
        @SerializedName("noCopyrightRcmd")
        public Object noCopyrightRcmd;
        @SerializedName("originCoverType")
        public int originCoverType;
        @SerializedName("mark")
        public long mark;
        @SerializedName("artists")
        public List<Artist> artists;
        @SerializedName("privilege")
        public Privilege privilege;
        @SerializedName("album")
        public Album album;
        @SerializedName("algorithm")
        public String algorithm;
        @SerializedName("transNames")
        public List<String> transNames;
    }

    public class SourceData {
        @SerializedName("id")
        public String id;
        @SerializedName("playCount")
        public int playCount;
        @SerializedName("name")
        public String name;
        @SerializedName("coverImgUrl")
        public String coverImgUrl;
        @SerializedName("trackIds")
        public List<TrackId> trackIds;
    }

    public class TrackId {

        @SerializedName("id")
        public String id;

    }

    public class FromInfo {
        @SerializedName("originalScene")
        public String originalScene;
        @SerializedName("originalResourceType")
        public String originalResourceType;
        @SerializedName("computeSourceResourceType")
        public String computeSourceResourceType;
        @SerializedName("sourceData")
        public SourceData sourceData;
        @SerializedName("trialMode")
        public int trialMode;
    }

    public class TrackIn {
        @SerializedName("id")
        public String id;
        @SerializedName("displayOrder")
        public int displayOrder;
        @SerializedName("randomOrder")
        public long randomOrder;
        @SerializedName("isPlayedOnce")
        public boolean isPlayedOnce;
        @SerializedName("ai")
        public String ai;
        @SerializedName("aiRcmd")
        public boolean aiRcmd;
        @SerializedName("scene")
        public String scene;
        @SerializedName("href")
        public String href;
        @SerializedName("text")
        public String text;
        @SerializedName("localTrack")
        public Object localTrack;
        @SerializedName("track")
        public Track track;
        @SerializedName("resourceType")
        public String resourceType;
        @SerializedName("fromInfo")
        public FromInfo fromInfo;
        @SerializedName("resourceId")
        public String resourceId;
        @SerializedName("trackId")
        public String trackId;
    }

    public class PlayingStatus {
        @SerializedName("trackIn")
        public TrackIn trackIn;
        @SerializedName("playingState")
        public long playingState;
        @SerializedName("noAddToHistory")
        public boolean noAddToHistory;
        @SerializedName("flag")
        public long flag;
        @SerializedName("fromType")
        public String fromType;
        @SerializedName("triggerScene")
        public String triggerScene;
    }

    // Detector Status Types
    public class SongDetail {
        @SerializedName("name")
        public String name;
        @SerializedName("cover")
        public String cover;
        @SerializedName("albumName")
        public String albumName;
        @SerializedName("artistNames")
        public List<String> artistNames;
    }

    public abstract class DetectorStatus {
        @SerializedName("available")
        public boolean available;
        @SerializedName("id")
        public long id;
        @SerializedName("playing")
        public boolean playing;
        @SerializedName("position")
        public double position;
        @SerializedName("duration")
        public double duration;
    }

    public class DetectorAvailableStatus extends DetectorStatus {
        @SerializedName("detail")
        public SongDetail detail;

        public DetectorAvailableStatus(long id, boolean playing, double position, double duration, SongDetail detail) {
            this.available = true;
            this.id = id;
            this.playing = playing;
            this.position = position;
            this.duration = duration;
            this.detail = detail;
        }
    }

    public class DetectorUnavailableStatus extends DetectorStatus {
        public DetectorUnavailableStatus() {
            this.available = false;
            this.id = -1;
            this.playing = false;
            this.position = 0;
            this.duration = 0;
        }
    }

}