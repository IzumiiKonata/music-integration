package tech.konata.musicintegration.integration.smtc;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.List;

public class MediaInfoC extends Structure {
    public Pointer title;
    public Pointer artist;
    public Pointer album;
    public Pointer thumbnailData;
    public int thumbnailSize;
    public long positionMs;
    public long durationMs;
    public double playbackRate;
    public boolean isPlaying;

    public MediaInfoC(Pointer p) {
        super(p);
    }

    public MediaInfoC() {
    }

    public String getTitleString() {
        return title == null ? null : title.getString(0, "UTF-8");
    }

    public String getArtistString() {
        return artist == null ? null : artist.getString(0, "UTF-8");
    }

    public String getAlbumString() {
        return album == null ? null : album.getString(0, "UTF-8");
    }

    @Override
    @SneakyThrows
    protected List<String> getFieldOrder() {

        return Arrays.asList(
                MediaInfoC.class.getDeclaredField("title").getName(),
                MediaInfoC.class.getDeclaredField("artist").getName(),
                MediaInfoC.class.getDeclaredField("album").getName(),
                MediaInfoC.class.getDeclaredField("thumbnailData").getName(),
                MediaInfoC.class.getDeclaredField("thumbnailSize").getName(),
                MediaInfoC.class.getDeclaredField("positionMs").getName(),
                MediaInfoC.class.getDeclaredField("durationMs").getName(),
                MediaInfoC.class.getDeclaredField("playbackRate").getName(),
                MediaInfoC.class.getDeclaredField("isPlaying").getName()
        );
    }

    public static class ByReference extends MediaInfoC implements Structure.ByReference {
    }
}