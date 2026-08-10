package net.tinnyman.nohurtflash;

import net.minecraft.client.Minecraft;
import net.tinnyman.nohurtflash.util.GlowColorUtil;

/**
 * Provides the glow color that should be used *right now*.
 *
 * This supports two modes:
 *  1) Static color from config (hex string)
 *  2) Rainbow mode (HSV hue cycles over time)
 *
 * The result is stored as a packed RGB24 int (0xRRGGBB) for fast consumption by rendering hooks/shader uniform updates.
 */
public final class GlowColorManager {
    private GlowColorManager() {}

    /**
     * Current glow color as packed RGB24 (0xRRGGBB).
     *
     * Volatile so rendering code reading this value sees updates immediately without needing additional synchronization.
     */
    private static volatile int currentGlowRgb24 = 0xFF0000;

    private static volatile boolean overrideRgbModeActive = false;
    private static volatile boolean overrideRgbModeValue = false;
    private static volatile boolean overrideColorActive = false;
    private static volatile int overrideColorRgb24 = 0xFF0000;

    public static void setOverrideRgbMode(Boolean valueOrNull) {
        if (valueOrNull == null) {
            overrideRgbModeActive = false;
        } else {
            overrideRgbModeActive = true;
            overrideRgbModeValue = valueOrNull;
        }
    }

    public static void setOverrideColorRgb24(Integer rgb24OrNull) {
        if (rgb24OrNull == null) {
            overrideColorActive = false;
        } else {
            overrideColorActive = true;
            overrideColorRgb24 = rgb24OrNull;
        }
    }

    /**
     * Called once per client tick to update the current glow color.
     *
     * This should remain cheap and deterministic. Rendering code should only read the latest computed color
     * rather than recompute it every frame.
     */
    public static void tickClient() {
        if (!Config.GLOW_ENABLED.get()) return;

        boolean rgbMode = overrideRgbModeActive ? overrideRgbModeValue : Config.RAINBOW_MODE_ENABLED.get();

        if (!rgbMode) {
            if (overrideColorActive) {
                currentGlowRgb24 = overrideColorRgb24;
            } else {
                currentGlowRgb24 = GlowColorUtil.parseRgb24FromHex(Config.GLOW_COLOR_HEX_STRING.get(), 0xFF0000);
            }
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        double cps = Config.RAINBOW_CYCLES_PER_SECOND.get();
        float tSeconds = mc.level.getGameTime() / 20f;

        float hue = (float) (tSeconds * cps);
        currentGlowRgb24 = GlowColorUtil.hsvToRgb24(hue, 1.0f, 1.0f);
    }

    /** @return current glow color as packed RGB24 (0xRRGGBB). */
    public static int getCurrentGlowRgb24() { return currentGlowRgb24; }

    /** @return red channel (0-255) from the current glow color. */
    public static int getR() { return (currentGlowRgb24 >> 16) & 0xFF; }

    /** @return green channel (0-255) from the current glow color. */
    public static int getG() { return (currentGlowRgb24 >> 8) & 0xFF; }

    /** @return blue channel (0-255) from the current glow color. */
    public static int getB() { return currentGlowRgb24 & 0xFF; }
}
