package tech.konata.musicintegration.integration;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author IzumiiKonata
 * Date: 2025/7/1 20:37
 */
@Data
@Builder
public class Music {

    public final String id;
    public final String name;
    public final List<String> artists;
    public final String cover;
    public final CoverType coverType;
    /**
     * Duration, in milliseconds
     */
    public final double duration;

    public enum CoverType {
        URL,
        Base64
    }

}
