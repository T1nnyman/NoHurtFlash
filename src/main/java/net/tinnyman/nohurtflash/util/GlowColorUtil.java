package net.tinnyman.nohurtflash.util;

/** Color helper methods used by the glow rendering pipeline.
 * Conventions used in this class:
 *  - "RGB24" means a packed 24-bit integer in the form 0xRRGGBB (no alpha).
 *  - HSV inputs are normalized floats (0..1) unless stated otherwise. */
public final class GlowColorUtil {
    /** Parses a hex color string into a packed RGB24 value (0xRRGGBB).
     * Accepted formats:
     *  - "#RRGGBB"
     *  - "RRGGBB"
     *  - "#RGB" (shorthand, e.g. "#f0a" -> "#ff00aa")
     *  - "RGB"  (shorthand)
     * If the input is null or invalid, the provided fallback is returned.
     *
     * @param s        user-provided hex string
     * @param fallbackRgb packed RGB24 fallback color
     * @return packed RGB24 color (0xRRGGBB) */
    public static int parseRgb24FromHex(String s, int fallbackRgb) {
        if (s == null) return fallbackRgb;

        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);

        // 3-digit shorthand (#f0a -> #ff00aa)
        if (s.length() == 3) {
            char r = s.charAt(0), g = s.charAt(1), b = s.charAt(2);
            s = "" + r + r + g + g + b + b;
        }

        if (s.length() != 6) return fallbackRgb;

        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallbackRgb;
        }
    }

    /** Converts HSV -> packed RGB24 (0xRRGGBB).
     * Input ranges:
     *  - h (hue): any float, wraps around using fractional part (0..1 repeats)
     *  - s (saturation): expected 0..1
     *  - v (value/brightness): expected 0..1
     * This is used for rainbow cycling ("RGB mode") where hue changes over time. */
    public static int hsvToRgb24(float h, float s, float v) {
        h = h - (float)Math.floor(h);
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6f) % 2f - 1));
        float m = v - c;

        float r1, g1, b1;
        float hp = h * 6f;

        if (hp < 1) { r1 = c; g1 = x; b1 = 0; }
        else if (hp < 2) { r1 = x; g1 = c; b1 = 0; }
        else if (hp < 3) { r1 = 0; g1 = c; b1 = x; }
        else if (hp < 4) { r1 = 0; g1 = x; b1 = c; }
        else if (hp < 5) { r1 = x; g1 = 0; b1 = c; }
        else { r1 = c; g1 = 0; b1 = x; }

        int r = clamp255((int)((r1 + m) * 255f));
        int g = clamp255((int)((g1 + m) * 255f));
        int b = clamp255((int)((b1 + m) * 255f));

        return (r << 16) | (g << 8) | b;
    }

    public static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;
        float hue = 0.0f;

        if (delta != 0.0f) {
            if (max == rf) {
                hue = ((gf - bf) / delta) % 6.0f;
            } else if (max == gf) {
                hue = ((bf - rf) / delta) + 2.0f;
            } else {
                hue = ((rf - gf) / delta) + 4.0f;
            }

            hue /= 6.0f;

            if (hue < 0.0f) hue += 1.0f;
        }

        float saturation = max == 0.0f ? 0.0f : delta / max;
        float value = max;

        return new float[]{hue, saturation, value};
    }

    /** Clamps an integer channel to the valid 8-bit color range (0..255). */
    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
