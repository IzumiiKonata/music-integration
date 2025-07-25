package tech.konata.musicintegration.integration.ncm;

import tech.konata.musicintegration.MusicIntegration;
import tech.konata.musicintegration.integration.IIntegrationProvider;
import tech.konata.musicintegration.integration.Music;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author IzumiiKonata
 * Date: 2025/7/1 20:03
 */
public class NCMProvider implements IIntegrationProvider {

    ElogListener listener = new ElogListener();
    List<SongDetailCallback> songDetailCallbacks = new ArrayList<>();
    private long currentSongId = -1;
    private String currentSongName = "";
    private String currentSongCover = "";
    private String currentSongAlbumName = "";
    private List<String> currentSongArtistNames = new ArrayList<>();
    private double currentSongDuration = 0;
    private double currentSongPosition = 0;
    private boolean currentSongPausing = false;
    private long currentSongRelativeTime = 0;
    private final List<PlayListener> playListeners = new ArrayList<>();
    private final List<StatusListener> statusListeners = new ArrayList<>();
    private final List<PositionListener> positionListeners = new ArrayList<>();

    public NCMProvider() {
        bindEvents();

//        this.addPlayListener(p -> System.out.println("Playing: " + p));
//        this.addStatusListener(s -> System.out.println("Paused: " + !s));
//        this.addPositionListener(s -> System.out.println("Position: " + s));
//        this.registerCallback(m -> {
//            System.out.println("Music changed: ");
//            System.out.println("    Id: " + m.id);
//            System.out.println("    Name: " + m.name);
//            System.out.println("    Artists: " + m.artists);
//            System.out.println("    Cover: " + m.cover);
//        });
    }

    private void emitPlay(long songId) {
        for (PlayListener listener : playListeners) {
            listener.onPlay(songId);
        }
    }

    private void emitStatus(boolean playing) {
        for (StatusListener listener : statusListeners) {
            listener.onStatus(playing);
        }

        this.callbacks.forEach(cb -> cb.onStatusChanged(playing));
    }

    private void emitPosition(double position) {
        for (PositionListener listener : positionListeners) {
            listener.onPosition(position);
        }

        this.callbacks.forEach(cb -> cb.onPositionChanged(position));
    }

    @Override
    public void startMonitoring() {
        try {
            String[] lines = listener.start();
            preloadLines(lines);
        } catch (FileNotFoundException e) {
            callbacks.forEach(cb -> cb.onError("无法读取网易云音乐信息! 请确保您的网易云音乐客户端版本高于 3.0.0 且正在运行"));
            MusicIntegration.CONFIG.enabled = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopMonitoring() {
        this.listener.stop();
    }

    private void preloadLines(String[] lines) {
        long now = System.currentTimeMillis();
        List<String> records = new ArrayList<>();

        long songId = -1;
        long songPlayTime = 0;
        double songPosition = 0;
        boolean songPausing = false;
        Types.PlayingStatus songTrackDetail = null;

        // 反向遍历行
        List<String> reversedLines = Arrays.asList(lines);
        Collections.reverse(reversedLines);

        lineLoop:
        for (String line : reversedLines) {
            ElogAnalysis.LogHeader headers = ElogAnalysis.getHeader(line);

            if (headers == null) {
                continue;
            }

            records.add(0, line);

            Types.PlayingStatusType type = ElogAnalysis.getType(line);
            if (type == null) continue;

            switch (type) {
                case EXIT:
                    resetState();
                    return;
                case SET_PLAYING:
                    Types.PlayingStatus data = ElogMatches.SET_PLAYING.parseArgs(line);
                    if (data != null) {
                        songId = Long.parseLong(data.trackIn.track.id);
                        songPlayTime = headers.timestamp;
                        songTrackDetail = data;
                    }
                    break lineLoop;
            }
        }

        long lastActionTime = songPlayTime;

        for (String line : records) {
            ElogAnalysis.LogHeader headers = ElogAnalysis.getHeader(line);

            if (headers == null) {
                continue;
            }

            Types.PlayingStatusType type = ElogAnalysis.getType(line);
            if (type == null) continue;

            switch (type) {
                case SET_PLAYING_POSITION:
                    Double position = ElogMatches.SET_PLAYING_POSITION.parseArgs(line);
                    if (position != null) {
                        songPosition = position;
                        lastActionTime = headers.timestamp;
                    }
                    break;

                case SET_PLAYING_STATUS:
                    Integer status = ElogMatches.SET_PLAYING_STATUS.parseArgs(line);
                    if (status != null) {
                        long offset = headers.timestamp - lastActionTime;
                        lastActionTime = headers.timestamp;
                        songPausing = status == 2;

                        if (songPausing) {
                            songPosition += offset / 1000.0;
                        }
                    }
                    break;
            }
        }

        if (!songPausing) {
            songPosition += (now - lastActionTime) / 1000.0;
        }

        boolean bShouldCallCallbacks = this.currentSongId != songId;

        this.currentSongId = songId;
        this.currentSongPausing = songPausing;
        this.currentSongPosition = songPausing ? songPosition : 0;
        this.currentSongRelativeTime = songPausing ? 0 : now - (long) (songPosition * 1000);

        if (songTrackDetail != null) {
            refreshCurrentSongDetail(songTrackDetail);

            if (bShouldCallCallbacks) {
                Types.Track track = songTrackDetail.trackIn.track;
                Music music = Music.builder()
                        .id(String.valueOf(songId))
                        .name(track.name)
                        .artists(track.artists.stream().map(ar -> ar.name).collect(Collectors.toList()))
                        .cover(songTrackDetail.trackIn.track.album.cover)
                        .coverType(Music.CoverType.URL)
                        .duration(songTrackDetail.trackIn.track.duration)
                        .build();
                //.transName + ")" : "")).collect(Collectors.toList()), songTrackDetail.trackIn.track.album.cover);

                this.callbacks.forEach(cb -> cb.onMusicChanged(music));
            }
        }

    }

    private void bindEvents() {
        listener.addLineListener(line -> {
            ElogAnalysis.LogHeader headers = ElogAnalysis.getHeader(line);
//            System.out.println(line);
            if (headers == null) {
                return;
            }

            long now = System.currentTimeMillis();
            double offset = (now - headers.timestamp) / 1000.0;

            Types.PlayingStatusType type = ElogAnalysis.getType(line);
            if (type == null) return;

            switch (type) {
                case EXIT:
                    resetState();
                    emitPlay(this.currentSongId);
                    return;

                case SET_PLAYING:
                    Types.PlayingStatus data = ElogMatches.SET_PLAYING.parseArgs(line);

                    if (data != null) {
                        this.currentSongId = Long.parseLong(data.trackIn.track.id);
                        this.currentSongPausing = true;
                        this.currentSongPosition = 0;
                        this.currentSongRelativeTime = 0;
                        refreshCurrentSongDetail(data);
                        emitPlay(this.currentSongId);

                        Types.Track track = data.trackIn.track;
                        Music music = Music.builder()
                                .id(String.valueOf(currentSongId))
                                .name(track.name)
                                .artists(track.artists.stream().map(ar -> ar.name).collect(Collectors.toList()))
                                .cover(data.trackIn.track.album.cover)
                                .coverType(Music.CoverType.URL)
                                .duration(data.trackIn.track.duration)
                                .build();

                        this.callbacks.forEach(cb -> cb.onMusicChanged(music));

                    }
                    break;

                case SET_PLAYING_POSITION:
                    Double position = ElogMatches.SET_PLAYING_POSITION.parseArgs(line);
                    if (position != null) {
                        this.currentSongPosition = this.currentSongPausing ? position : 0;
                        this.currentSongRelativeTime = this.currentSongPausing
                                ? 0
                                : System.currentTimeMillis() - (long) (position * 1000) - (long) (offset * 1000);

                        emitPosition(position);
                    }
                    break;

                case SET_PLAYING_STATUS:
                    Integer status = ElogMatches.SET_PLAYING_STATUS.parseArgs(line);
                    if (status != null) {
                        long newRelative = now - (long) (this.currentSongPosition * 1000);
                        double newPosition = (now - this.currentSongRelativeTime) / 1000.0;

                        this.currentSongPausing = status == 2;
                        this.currentSongPosition = this.currentSongPausing
                                ? newPosition - offset
                                : 0;
                        this.currentSongRelativeTime = this.currentSongPausing
                                ? 0
                                : newRelative - (long) (offset * 1000);

                        emitStatus(!this.currentSongPausing);
                    }
                    break;

                case PRECACHE_NEXT:
                    Long id = ElogMatches.PRECACHE_NEXT.parseArgs(line);
                    if (id != null) {
                        this.callbacks.forEach(cb -> cb.onPreloadNext(String.valueOf(id)));
                    }
                    break;
            }
        });
    }

    private void resetState() {
        this.currentSongId = -1;
        this.currentSongPosition = 0;
        this.currentSongPausing = false;
        this.currentSongRelativeTime = 0;
    }

    private void refreshCurrentSongDetail(Types.PlayingStatus trackStatus) {
        Types.Track detail = trackStatus.trackIn.track;

        this.currentSongName = detail.name;
        this.currentSongCover = detail.album.cover;
        this.currentSongAlbumName = detail.album.name;
        this.currentSongArtistNames = detail.artists.stream()
                .map(artist -> artist.name)
                .collect(Collectors.toList());
        this.currentSongDuration = detail.duration / 1000.0;

        this.songDetailCallbacks.forEach(cb -> cb.onSongDetail(trackStatus));
    }

    public Types.DetectorStatus getStatus() {
        long now = System.currentTimeMillis();

        if (this.currentSongId == -1) {
            return new Types.DetectorUnavailableStatus();
        } else {
            double position = Math.min(
                    this.currentSongPausing
                            ? this.currentSongPosition
                            : (now - this.currentSongRelativeTime) / 1000.0,
                    this.currentSongDuration
            );

            Types.SongDetail detail = new Types.SongDetail();
            detail.name = this.currentSongName;
            detail.cover = this.currentSongCover;
            detail.albumName = this.currentSongAlbumName;
            detail.artistNames = new ArrayList<>(this.currentSongArtistNames);

            return new Types.DetectorAvailableStatus(
                    this.currentSongId,
                    !this.currentSongPausing,
                    position,
                    this.currentSongDuration,
                    detail
            );
        }
    }

    @Override
    public String getCurrentSongId() {
        return String.valueOf(this.currentSongId);
    }

    @Override
    public String getCurrentSongName() {
        return this.currentSongName;
    }

    @Override
    public double getCurrentSongDuration() {
        return this.currentSongDuration;
    }

    @Override
    public double getCurrentSongPosition() {
        return this.currentSongPosition;
    }

    @Override
    public List<String> getCurrentSongArtists() {
        return this.currentSongArtistNames;
    }

    @Override
    public boolean isPlaying() {
        return !this.currentSongPausing;
    }

    public interface PlayListener {
        void onPlay(long songId);
    }

    public interface StatusListener {
        void onStatus(boolean playing);
    }

    public interface PositionListener {
        void onPosition(double position);
    }

    public interface SongDetailCallback {

        void onSongDetail(Types.PlayingStatus status);

    }

}
