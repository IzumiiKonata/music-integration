package tech.konata.musicintegration.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import tech.konata.musicintegration.MusicIntegration;

/**
 * @author IzumiiKonata
 * Date: 2025/7/24 22:09
 */
@Config(name = MusicIntegration.MOD_ID)
public class ModConfig implements ConfigData {

    public boolean enabled = true;

    public ProviderType providerType = ProviderType.SMTC;

    public double scale = 1.0;

    @ConfigEntry.Gui.Excluded
    public UIPosition uiPosition = new UIPosition();

    public enum ProviderType {
        SMTC,
        NCM
    }

    public static class UIPosition {
        public int posX = 0, posY = 0;
    }

}
