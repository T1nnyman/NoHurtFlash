package net.tinnyman.nohurtflash;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.tinnyman.nohurtflash.Util.GlowColorUtil;

import java.nio.file.Path;

/**
 * Client-side configuration for NoHurtFlash
 *
 * This file defines config keys + default values and keeps a cached parsed glow color for fast use during rendering.
 */
@Mod.EventBusSubscriber(modid = NoHurtFlash.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModConfig {
    public static final Builder BUILDER = new Builder();
    public static final ForgeConfigSpec SPEC;

    /* Config Values */
    public static final BooleanValue GLOW_ENABLED;
    public static final ConfigValue<String> GLOW_COLOR_HEX_STRING;
    public static final BooleanValue RAINBOW_MODE_ENABLED;
    public static final DoubleValue RAINBOW_CYCLES_PER_SECOND;

    /* Cached parsed state */
    private static volatile int cachedR = 255;
    private static volatile int cachedG = 0;
    private static volatile int cachedB = 0;

    static {
        BUILDER.push("General Settings");

        GLOW_ENABLED = BUILDER
                .comment("Enable or disable the hurt-time glow effect")
                .define("enableGlow", true);

        GLOW_COLOR_HEX_STRING = BUILDER
                .comment("Glow color as hex. Examples: \\\"#FF0000\\\" or \\\"00FFAA\\\"")
                .define("glowColorHex", "#FF0000");

        RAINBOW_MODE_ENABLED = BUILDER
                .comment("If true, glow rapidly cycles through rainbow colors")
                .define("rgbMode", false);

        RAINBOW_CYCLES_PER_SECOND = BUILDER
                .comment("Rainbow speed (full hue cycles per second). Example: 2.0 = 2 rainbows per second")
                        .defineInRange("rgbCyclesPerSecond", 3.0, 0.1, 50.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /**
     * Re-parses the configured hex color string and updates cached RGB channels.
     * Called when the client config loads or reloads.
     */
    public static void rebuildCachedGlowColor() {
        int rgb24 = GlowColorUtil.parseRgb24FromHex(GLOW_COLOR_HEX_STRING.get(), 0xFF000);
        cachedR = (rgb24 >> 16) & 0xFF;
        cachedG = (rgb24 >> 8) & 0xFF;
        cachedB = rgb24 & 0xFF;

        NoHurtFlash.LOGGER.info("[NoHurtFlash] Config loaded: enableGlow={}, rgbMode={}, rgbCyclesPerSecond={}, glowColorHex={} -> ({},{},{})",
                GLOW_ENABLED.get(),
                RAINBOW_MODE_ENABLED.get(),
                RAINBOW_CYCLES_PER_SECOND.get(),
                GLOW_COLOR_HEX_STRING.get(),
                cachedR, cachedG, cachedB
        );
    }

    /** Set the config value (in-memory) + rebuild cache. */
    public static void setGlowColorHex(String hex) {
        GLOW_COLOR_HEX_STRING.set(hex);
        rebuildCachedGlowColor();
    }

    /** Enable/disable RGB mode (in-memory). */
    public static void setRgbMode(boolean enabled) {
        RAINBOW_MODE_ENABLED.set(enabled);
    }

    /**
     * Persist current in-memory config values to disk immediately.
     * This keeps the TOML in sync with changes made by commands/UI.
     */
    public static void saveNow() {
        Path path = clientConfigPath();

        CommentedFileConfig file = CommentedFileConfig.builder(path)
                .sync()
                .autoreload()
                .writingMode(WritingMode.REPLACE)
                .build();

        try {
            file.load();
            file.set("General Settings.enableGlow", GLOW_ENABLED.get());
            file.set("General Settings.glowColorHex", GLOW_COLOR_HEX_STRING.get());
            file.set("General Settings.rgbMode", RAINBOW_MODE_ENABLED.get());
            file.set("General Settings.rgbCyclesPerSecond", RAINBOW_CYCLES_PER_SECOND.get());
            file.save();
        } finally {
            file.close();
        }
    }

    /** Convenience: set all picker-related values + rebuild cache + save to disk. */
    public static void applyPickerSettings(boolean rgbMode, double rgbCps, String hex) {
        RAINBOW_MODE_ENABLED.set(rgbMode);
        RAINBOW_CYCLES_PER_SECOND.set(rgbCps);
        GLOW_COLOR_HEX_STRING.set(hex);

        rebuildCachedGlowColor(); // keeps your cachedR/G/B up to date
        saveNow();
    }

    /** Default Forge config filename for client config. */
    private static Path clientConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(NoHurtFlash.MODID + "-client.toml");
    }

    /**
     * Ensures we only respond to THIS mod's CLIENT config events.
     */
    private static boolean isNoHurtFlashClientConfig(net.minecraftforge.fml.config.ModConfig cfg) {
        return cfg != null && NoHurtFlash.MODID.equals(cfg.getModId()) && cfg.getType() == net.minecraftforge.fml.config.ModConfig.Type.CLIENT;
    }

    @SubscribeEvent
    public static void onConfigLoading(final ModConfigEvent.Loading event) {
        if (!isNoHurtFlashClientConfig(event.getConfig())) return;
        rebuildCachedGlowColor();
    }

    @SubscribeEvent
    public static void onConfigReloading(final ModConfigEvent.Reloading event) {
        if (!isNoHurtFlashClientConfig(event.getConfig())) return;
        rebuildCachedGlowColor();
    }

    /* ---------------- Public accessors for cached color ---------------- */

    /** Cached red channel (0-255) for the configured glow color. */
    public static int glowRed()   { return cachedR; }

    /** Cached green channel (0-255) for the configured glow color. */
    public static int glowGreen() { return cachedG; }

    /** Cached blue channel (0-255) for the configured glow color. */
    public static int glowBlue()  { return cachedB; }
}
