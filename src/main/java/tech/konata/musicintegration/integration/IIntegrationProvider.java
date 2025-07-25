package tech.konata.musicintegration.integration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author IzumiiKonata
 * Date: 2025/7/1 19:56
 */
public interface IIntegrationProvider {

    List<IIntegrationCallback> callbacks = new CopyOnWriteArrayList<>();

    void startMonitoring();

    void stopMonitoring();

    String getCurrentSongId();

    String getCurrentSongName();

    List<String> getCurrentSongArtists();

    /**
     * 返回当前歌曲的总长度, 单位为秒
     *
     * @return 当前歌曲的总长度, 单位为秒
     */
    double getCurrentSongDuration();

    /**
     * 返回当前歌曲的播放进度, 单位为秒
     *
     * @return 当前歌曲的播放进度, 单位为秒
     */
    double getCurrentSongPosition();

    boolean isPlaying();

    default void registerCallback(IIntegrationCallback callback) {
        if (!this.callbacks.contains(callback))
            this.callbacks.add(callback);
    }

}
