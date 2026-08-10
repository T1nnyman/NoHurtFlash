package net.tinnyman.nohurtflash;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.tinnyman.nohurtflash.Util.GlowColorUtil;

/** Client-side configuration for NoHurtFlash.
 * This class defines the configuration values and keeps a cached
 * RGB representation of the configured glow color for fast access during rendering. */
@Mod.EventBusSubscriber(modid = NoHurtFlash.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Config {
    public static final Builder BUILDER = new Builder();
    public static final ForgeConfigSpec SPEC;

    /* Config Values */
    public static final BooleanValue GLOW_ENABLED;
    public static final BooleanValue OLD_HURT_EFFECT_ENABLED;
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

        OLD_HURT_EFFECT_ENABLED = BUILDER
                .comment("Enable or disable Minecraft's original red hurt effect")
                .define("oldHurtEffect", false);

        GLOW_COLOR_HEX_STRING = BUILDER
                .comment("Glow color as hex. Examples: \\\"#FF0000\\\" or \\\"00FFAA\\\"")
                .define("glowColorHex", "#FF0000");

        RAINBOW_MODE_ENABLED = BUILDER
                .comment("If true, glow rapidly cycles through rainbow colors")
                .define("rgbMode", false);

        RAINBOW_CYCLES_PER_SECOND = BUILDER
                .comment("Rainbow speed (full hue cycles per second). Example: 2.0 = 2 rainbows per second")
                        .defineInRange("rgbCyclesPerSecond", 0.5, 0.1, 2.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /** Re-parses the configured hex color string and updates the cached RGB channels. */
    public static void rebuildCachedGlowColor() {
        int rgb24 = GlowColorUtil.parseRgb24FromHex(GLOW_COLOR_HEX_STRING.get(), 0xFF000);
        cachedR = (rgb24 >> 16) & 0xFF;
        cachedG = (rgb24 >> 8) & 0xFF;
        cachedB = rgb24 & 0xFF;

        NoHurtFlash.LOGGER.info("[NoHurtFlash] Config loaded: enableGlow={}, disableHurtFlash={}, rgbMode={}, " +
                        "rgbCyclesPerSecond={}, glowColorHex={} -> ({},{},{})",
                GLOW_ENABLED.get(),
                OLD_HURT_EFFECT_ENABLED.get(),
                RAINBOW_MODE_ENABLED.get(),
                RAINBOW_CYCLES_PER_SECOND.get(),
                GLOW_COLOR_HEX_STRING.get(),
                cachedR,
                cachedG,
                cachedB
        );
    }

    /** Applies all config-related settings. */
    public static void applyConfigSettings(boolean glowEnabled, boolean oldHurtEffectEnabled, boolean rgbMode, double rgbCps, String hex) {
        GLOW_ENABLED.set(glowEnabled);
        OLD_HURT_EFFECT_ENABLED.set(oldHurtEffectEnabled);
        RAINBOW_MODE_ENABLED.set(rgbMode);
        RAINBOW_CYCLES_PER_SECOND.set(rgbCps);
        GLOW_COLOR_HEX_STRING.set(hex);

        // Update the cached RGB representation
        rebuildCachedGlowColor();
        // Persist the new values to the client config file.
        SPEC.save();
    }

    /** Responds when this mod's client configuration is loaded. */
    @SubscribeEvent
    public static void onConfigLoading(final ModConfigEvent.Loading event) {
        if (!isNoHurtFlashClientConfig(event.getConfig())) return;
        rebuildCachedGlowColor();
    }

    /** Responds when this mod's client configuration is reloaded. */
    @SubscribeEvent
    public static void onConfigReloading(final ModConfigEvent.Reloading event) {
        if (!isNoHurtFlashClientConfig(event.getConfig())) return;
        rebuildCachedGlowColor();
    }

    /** Makes sure the event belongs to NoHurtFlash's client configuration. */
    private static boolean isNoHurtFlashClientConfig(ModConfig config) {
        return config != null && NoHurtFlash.MODID.equals(config.getModId()) && config.getType() == ModConfig.Type.CLIENT;
    }

    /* ---------------- Public accessors for cached color ---------------- */

    /** Cached red channel (0-255). */
    public static int glowRed()   { return cachedR; }

    /** Cached green channel (0-255). */
    public static int glowGreen() { return cachedG; }

    /** Cached blue channel (0-255). */
    public static int glowBlue()  { return cachedB; }
}
