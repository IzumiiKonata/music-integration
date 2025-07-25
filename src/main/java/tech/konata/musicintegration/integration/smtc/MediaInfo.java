package tech.konata.musicintegration.integration.smtc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public class MediaInfo {
    public String title;
    public String artist;
    public String album;
    public byte[] thumbnailData;
    public long positionMs;
    public long durationMs;
    public double playbackRate;
    public boolean isPlaying;

    public BufferedImage getThumbnail() {
        if (thumbnailData != null && thumbnailData.length > 0) {
            try {
                return ImageIO.read(new ByteArrayInputStream(thumbnailData));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}