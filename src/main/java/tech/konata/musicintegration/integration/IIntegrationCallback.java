package tech.konata.musicintegration.integration;

/**
 * @author IzumiiKonata
 * Date: 2025/7/1 20:39
 */
public interface IIntegrationCallback {

    default void onMusicChanged(Music music) {

    }

    default void onStatusChanged(boolean playing) {

    }

    default void onPositionChanged(double position) {

    }

    default void onCoverChanged(String cover, Music.CoverType type) {

    }

    default void onPreloadNext(String musicId) {

    }

    default void onError(String error) {

    }

}
