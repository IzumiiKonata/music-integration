package tech.konata.musicintegration;

import lombok.Getter;
import lombok.Setter;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.konata.musicintegration.config.ModConfig;
import tech.konata.musicintegration.integration.IIntegrationProvider;
import tech.konata.musicintegration.integration.ncm.NCMProvider;
import tech.konata.musicintegration.integration.smtc.SMTCProvider;
import tech.konata.musicintegration.rendering.Interpolations;
import tech.konata.musicintegration.rendering.MusicIntegrationRenderer;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class MusicIntegration implements ClientModInitializer {
    public static final String MOD_ID = "music-integration";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ConfigHolder<ModConfig> CONFIG_HOLDER;
    public static ModConfig CONFIG;
    static List<Runnable> autoRegisterCallbacks = new ArrayList<>();
    @Getter
    @Setter
    private static IIntegrationProvider provider;

    public static boolean isProvider(ModConfig.ProviderType providerType) {
        return CONFIG.providerType.equals(providerType);
    }

    public static boolean isEnabled() {
        return CONFIG.enabled;
    }

    public static void refreshProvider() {

        IIntegrationProvider newProvider;

        switch (CONFIG.providerType) {

            case NCM: {
                newProvider = new NCMProvider();
                break;
            }

            case SMTC: {
                newProvider = new SMTCProvider();
                break;
            }

            default: {
                throw new IllegalArgumentException("Unknown provider: " + CONFIG.providerType.name());
            }

        }

        if (isEnabled() && provider != null)
            provider.stopMonitoring();

        setProvider(newProvider);

        if (isEnabled())
            provider.startMonitoring();

        autoRegisterCallbacks.forEach(Runnable::run);
    }

    public static void autoRegisterCallback(Runnable runnable) {
        autoRegisterCallbacks.add(runnable);
        runnable.run();
    }

    public static void autoRegister() {
        autoRegisterCallbacks.forEach(Runnable::run);
    }

    @Override
    public void onInitializeClient() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        CONFIG_HOLDER = AutoConfig.getConfigHolder(ModConfig.class);
        CONFIG_HOLDER.load();
        CONFIG = CONFIG_HOLDER.getConfig();

        refreshProvider();
        Interpolations.init();
        MusicIntegrationRenderer.register();
        PlayingMusicData.register();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            CONFIG_HOLDER.save();
        }));
    }

}