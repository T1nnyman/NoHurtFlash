package net.tinnyman.nohurtflash;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = NoHurtFlash.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModConfig {
    public static final Builder BUILDER = new Builder();
    public static final BooleanValue ENABLE_GLOW;
    public static final ConfigValue<String> GLOW_COLOR_HEX;
    public static final BooleanValue RGB_MODE;
    public static final DoubleValue RGB_CYCLES_PER_SECOND;
    public static final ForgeConfigSpec SPEC;

    private static volatile int cachedR = 255;
    private static volatile int cachedG = 0;
    private static volatile int cachedB = 0;

    static {
        BUILDER.push("General Settings");

        ENABLE_GLOW = BUILDER
                .comment("Enable or disable the hurt-time glow effect")
                .define("enableGlow", true);

        GLOW_COLOR_HEX = BUILDER
                .comment("Glow color as hex. Examples: \\\"#FF0000\\\" or \\\"00FFAA\\\"")
                .define("glowColorHex", "#FF0000");

        RGB_MODE = BUILDER
                .comment("If true, glow rapidly cycles through rainbow colors")
                .define("rgbMode", false);

        RGB_CYCLES_PER_SECOND = BUILDER
                .comment("Rainbow speed (full hue cycles per second). Example: 2.0 = 2 rainbows per second")
                        .defineInRange("rgbCyclesPerSecond", 3.0, 0.1, 50.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private static void refreshCachedColor() {
        int rgb = parseHexToRgb24(GLOW_COLOR_HEX.get());
        cachedR = (rgb >> 16) & 0xFF;
        cachedG = (rgb >> 8) & 0xFF;
        cachedB = rgb & 0xFF;

        NoHurtFlash.LOGGER.info("[NoHurtFlash] Config loaded: enableGlow={}, rgbMode={}, rgbCyclesPerSecond={}, glowColorHex={} -> ({},{},{})",
                ENABLE_GLOW.get(),
                RGB_MODE.get(),
                RGB_CYCLES_PER_SECOND.get(),
                GLOW_COLOR_HEX.get(),
                cachedR, cachedG, cachedB
        );
    }

    private static boolean isOurConfig(net.minecraftforge.fml.config.ModConfig cfg) {
        return cfg != null && NoHurtFlash.MODID.equals(cfg.getModId()) && cfg.getType() == net.minecraftforge.fml.config.ModConfig.Type.CLIENT;
    }

    @SubscribeEvent
    public static void onConfigLoading(final ModConfigEvent.Loading event) {
        if (!isOurConfig(event.getConfig())) return;
        refreshCachedColor();
    }

    @SubscribeEvent
    public static void onConfigReloading(final ModConfigEvent.Reloading event) {
        if (!isOurConfig(event.getConfig())) return;
        refreshCachedColor();
    }

    public static int parseHexToRgb24(String raw) {
        if (raw == null) return 0xFF0000;

        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("#")) s = s.substring(1);

        // Allow 3-digit shorthand RGB ("f0a")
        if (s.length() == 3) {
            char r = s.charAt(0), g = s.charAt(1), b = s.charAt(2);
            s = "" + r + r + g + g + b + b;
        }

        if (s.length() != 6) return 0xFF0000;

        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return 0xFF0000;
        }
    }
}
