package tech.konata.musicintegration.integration.smtc;

import tech.konata.musicintegration.integration.IIntegrationCallback;
import tech.konata.musicintegration.integration.IIntegrationProvider;
import tech.konata.musicintegration.integration.Music;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author IzumiiKonata
 * Date: 2025/7/11 23:26
 */
public class SMTCProvider implements IIntegrationProvider {

    String musicTitle = "";
    String musicArtist = "";
    String musicAlbum = "";
    double musicPosition = 0;
    double musicDuration = 0;
    boolean playing = false;
    byte[] thumbnail = null;
    private final int watchInterval = 100; // 毫秒
    private ScheduledExecutorService scheduler;

    public void refreshStatus() {
        try {
            MediaInfo currentMediaInfo = SMTCNative.getCurrentMediaInfo();

            if (this.isNull(currentMediaInfo)) {
                this.playing = false;
                this.callbacks.forEach(cb -> cb.onStatusChanged(false));
                return;
            }

            double currentMusicPosition = currentMediaInfo.positionMs / 10000_000.0;
            final boolean needToCallCallback = this.musicPosition != currentMusicPosition;
            this.musicPosition = currentMusicPosition;
            this.musicDuration = currentMediaInfo.durationMs / 10000.0;
            this.playing = currentMediaInfo.isPlaying;

            this.callbacks.forEach(cb -> {
                if (needToCallCallback) {
                    cb.onPositionChanged(this.musicPosition);
                }
                cb.onStatusChanged(this.playing);
            });

            if (!this.isChanged(currentMediaInfo)) {
                boolean thumbnailChanged = !Arrays.equals(this.thumbnail, currentMediaInfo.thumbnailData);
                this.thumbnail = currentMediaInfo.thumbnailData;

                if (thumbnailChanged) {
                    if (thumbnail != null) {
                        BufferedImage img = currentMediaInfo.getThumbnail();

                        String b64 = this.encodeImgToBase64(img);

                        if (b64 != null) {
                            for (IIntegrationCallback cb : this.callbacks) {
                                cb.onCoverChanged(b64, Music.CoverType.Base64);
                            }
                        }
                    }

                }
            } else {

//                System.out.println("Changed: [\n" +
//                        "    Title: " + currentMediaInfo.title + "\n" +
//                        "    Artist: " + currentMediaInfo.artist + "\n" +
//                        "    Album: " + currentMediaInfo.album + "\n" +
//                        "]");

                this.musicTitle = currentMediaInfo.title;
                this.musicArtist = currentMediaInfo.artist;
                this.musicAlbum = currentMediaInfo.album;
                BufferedImage thumbnail = currentMediaInfo.getThumbnail();
                String coverData = this.encodeImgToBase64(thumbnail);

                Music music = Music.builder()
                        .id(this.getCurrentSongId())
                        .name(this.getCurrentSongName())
                        .artists(this.getCurrentSongArtists())
                        .duration(this.getCurrentSongDuration())
                        .cover(coverData)
                        .coverType(Music.CoverType.Base64)

                        .build();

                this.thumbnail = currentMediaInfo.thumbnailData;

                this.callbacks.forEach(cb -> cb.onMusicChanged(music));
            }

        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("Error occurred while refreshing status!");
        }
    }

    private String encodeImgToBase64(BufferedImage img) {

        if (img == null)
            return null;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", stream);
            return Base64.getEncoder().encodeToString(stream.toByteArray());
        } catch (IOException e) {
            System.err.println("Failed to encode image!");
        }

        return null;
    }

    private boolean isNull(MediaInfo info) {
        return info == null || ((info.title == null || info.artist == null || info.album == null) && info.thumbnailData == null && info.durationMs == 0 && info.positionMs == 0);
    }

    private boolean isChanged(MediaInfo info) {
        return !(Objects.equals(this.musicTitle, info.title) && Objects.equals(this.musicArtist, info.artist) && Objects.equals(this.musicAlbum, info.album));
    }

    @Override
    public void startMonitoring() {
        // 启动定时监听
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                this::refreshStatus,
                watchInterval,
                watchInterval,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }

    @Override
    public String getCurrentSongId() {
        return String.format(
                "smtc_%s_%s_%s",
                this.musicTitle,
                this.musicAlbum,
                this.musicArtist
        ).toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
    }

    @Override
    public List<String> getCurrentSongArtists() {

        if (this.musicArtist == null)
            return Collections.emptyList();

        return Arrays.asList(this.musicArtist.split("/"));
    }

    @Override
    public String getCurrentSongName() {
        return this.musicTitle;
    }

    @Override
    public double getCurrentSongDuration() {
        return this.musicDuration;
    }

    @Override
    public double getCurrentSongPosition() {
        return this.musicPosition;
    }

    @Override
    public boolean isPlaying() {
        return this.playing;
    }

}
