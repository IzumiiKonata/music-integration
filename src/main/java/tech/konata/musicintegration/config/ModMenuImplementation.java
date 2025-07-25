package tech.konata.musicintegration.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import tech.konata.musicintegration.MusicIntegration;

/**
 * @author IzumiiKonata
 * Date: 2025/7/24 22:23
 */
public class ModMenuImplementation implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent);
            builder.setSavingRunnable(() -> MusicIntegration.CONFIG_HOLDER.save());

            builder.setTitle(Component.translatable("music_integration.title"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            ConfigCategory general = builder.getOrCreateCategory(Component.translatable("music_integration.category.general"));
            ModConfig config = MusicIntegration.CONFIG;
            general.addEntry(
                    entryBuilder.startBooleanToggle(Component.translatable("music_integration.enabled"), config.enabled)
                            .setDefaultValue(true)
                            .setSaveConsumer(b -> {
                                if (config.enabled != b) {
                                    if (b) {
                                        MusicIntegration.getProvider().startMonitoring();
                                    } else {
                                        MusicIntegration.getProvider().stopMonitoring();
                                    }
                                }

                                config.enabled = b;
                            })
                            .build()
            );

            general.addEntry(
                    entryBuilder.startEnumSelector(Component.translatable("music_integration.provider"), ModConfig.ProviderType.class, config.providerType)
                            .setDefaultValue(ModConfig.ProviderType.SMTC)
                            .setSaveConsumer(v -> {
                                config.providerType = v;
                                MusicIntegration.refreshProvider();
                            })
                            .build()
            );

            general.addEntry(
                    entryBuilder.startIntSlider(Component.translatable("music_integration.scale"), (int) (config.scale * 10), 5, 20)
                            .setDefaultValue(10)
                            .setMax(20)
                            .setMin(5)
                            .setTextGetter(i -> Component.literal(String.format("%.1f", i / 10.0)))
                            .setSaveConsumer(val -> config.scale = val / 10.0)
                            .build()
            );

            return builder.build();
        };
    }
}
