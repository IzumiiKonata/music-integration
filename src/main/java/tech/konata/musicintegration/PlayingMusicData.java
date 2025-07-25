package tech.konata.musicintegration;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import tech.konata.musicintegration.integration.IIntegrationCallback;
import tech.konata.musicintegration.integration.IIntegrationProvider;
import tech.konata.musicintegration.integration.Music;

/**
 * @author IzumiiKonata
 * Date: 2025/7/7 20:46
 */
@UtilityClass
public class PlayingMusicData {
    @Getter
    Music playingMusic = null;
    @Getter
    boolean playing = false;
    double position = 0;
    public final IIntegrationCallback callback = new IIntegrationCallback() {

        @Override
        public void onPositionChanged(double position) {
            // 秒 -> 毫秒
            PlayingMusicData.position = position * 1000;
        }

        @Override
        public void onStatusChanged(boolean playing) {
            PlayingMusicData.playing = playing;
        }

        @Override
        public void onMusicChanged(Music music) {
            playingMusic = music;
            position = 0;
        }

        @Override
        public void onError(String error) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.literal(error), true);
            }
        }

    };
    long lastNano = System.nanoTime();

    public double getPosition() {
        long l = System.nanoTime();

        if (playing) {
            position += (l - lastNano) / 1000000.0;
        }

        lastNano = l;

        return position;
    }

    public static void register() {
        MusicIntegration.autoRegisterCallback(() -> {
            IIntegrationProvider provider = MusicIntegration.getProvider();
            provider.registerCallback(callback);
        });
    }
}
