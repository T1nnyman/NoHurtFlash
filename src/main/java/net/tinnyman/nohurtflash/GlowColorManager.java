package net.tinnyman.nohurtflash;

import net.minecraft.client.Minecraft;

public final class GlowColorManager {
    private GlowColorManager() {}

    private static volatile int currentRgb = 0xFF0000;

    public static void clientTick() {
        if (!ModConfig.ENABLE_GLOW.get()) return;

        if (!ModConfig.RGB_MODE.get()) {
            currentRgb = GlowColorUtil.parseHexRgb(ModConfig.GLOW_COLOR_HEX.get(), 0xFF0000);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        double cps = ModConfig.RGB_CYCLES_PER_SECOND.get();
        long gameTime = mc.level.getGameTime();
        float tSeconds = gameTime / 20f;

        float hue = (float) (tSeconds * cps);
        currentRgb = GlowColorUtil.hsvToRgb(hue, 1.0f, 1.0f);
    }

    public static int getCurrentRgb() {
        return currentRgb;
    }

    public static int getR() { return (currentRgb >> 16) & 0xFF; }
    public static int getG() { return (currentRgb >> 8) & 0xFF; }
    public static int getB() { return currentRgb & 0xFF; }
}
