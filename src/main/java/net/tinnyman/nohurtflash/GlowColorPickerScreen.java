package net.tinnyman.nohurtflash;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.tinnyman.nohurtflash.Util.GlowColorUtil;

import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * Client-side GUI for configuring the hurt-glow color.
 *
 * This screen provides:
 *  - Static RGB color selection with live preview
 *  - RGB (rainbow) mode toggle
 *  - RGB cycle speed control
 *
 *  All changes are previewed instantly and can be saved directly to the
 *  client config without requiring a game restart.
 */
public class GlowColorPickerScreen extends Screen {

    /** Parent screen to return to when closing this menu. */
    private final Screen parent;

    /* -------- Static color state -------- */

    /** Static red, green, and blue channel values (0–255). */
    private int r, g, b;

    /* -------- RGB mode state -------- */

    /** Whether rainbow (RGB) mode is enabled. */
    private boolean rgbMode;

    /** Rainbow cycle speed (full hue cycles per second). */
    private double rgbCps;

    /* -------- UI widgets -------- */

    private IntSlider rSlider;
    private IntSlider gSlider;
    private IntSlider bSlider;

    private DoubleSlider cpsSlider;

    /**
     * Creates the glow color picker screen.
     *
     * Initial values are pulled from the current client config so the
     * UI reflects the active settings when opened.
     */
    public GlowColorPickerScreen(Screen parent) {
        super(Component.literal("NoHurtFlash Glow Settings"));
        this.parent = parent;

        // Initialize static color from cached config values
        this.r = ModConfig.glowRed();
        this.g = ModConfig.glowGreen();
        this.b = ModConfig.glowBlue();

        // Initialize RGB mode state from config
        this.rgbMode = ModConfig.RAINBOW_MODE_ENABLED.get();
        this.rgbCps = ModConfig.RAINBOW_CYCLES_PER_SECOND.get();
    }


    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 4;

        /* -------- RGB mode toggle -------- */

        Button rgbToggleBtn = addRenderableWidget(Button.builder(rgbToggleLabel(), btn -> {
            rgbMode = !rgbMode;
            btn.setMessage(rgbToggleLabel());
            updateEnabledStates();
            applyLivePreview();
        }).bounds(cx - 120, y, 240, 20).build());
        y += 26;

        /* -------- RGB speed slider -------- */

        cpsSlider = addRenderableWidget(new DoubleSlider(cx - 120, y, 240, 20,
                "RGB Speed (cycles/sec)", rgbCps, 0.1, 50.0, v -> {
            rgbCps = v;
            applyLivePreview();
        }));
        y += 28;

        /* -------- Static RGB sliders -------- */

        rSlider = addRenderableWidget(new IntSlider(cx - 120, y, 240, 20, "R", r, v -> {
            r = v;
            applyLivePreview();
        }));
        y += 24;

        gSlider = addRenderableWidget(new IntSlider(cx - 120, y, 240, 20, "G", g, v -> {
            g = v;
            applyLivePreview();
        }));
        y += 24;

        bSlider = addRenderableWidget(new IntSlider(cx - 120, y, 240, 20, "B", b, v -> {
            b = v;
            applyLivePreview();
        }));
        y += 34;

        /* -------- Save / Cancel buttons -------- */

        addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
            // Persist settings to config (both in-memory and on disk)
            String hex = String.format("#%02X%02X%02X", r, g, b);
            ModConfig.applyPickerSettings(rgbMode, rgbCps, hex);

            // Clear runtime overrides so the config drives behavior
            GlowColorManager.setOverrideRgbMode(null);
            GlowColorManager.setOverrideColorRgb24(null);

            this.minecraft.setScreen(parent);
        }).bounds(cx - 120, y, 116, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            // Revert any live preview overrides
            GlowColorManager.setOverrideRgbMode(null);
            GlowColorManager.setOverrideColorRgb24(null);
            this.minecraft.setScreen(parent);
        }).bounds(cx + 4, y, 116, 20).build());

        updateEnabledStates();
        applyLivePreview();
    }

    /** @return Label text for the RGB mode toggle button. */
    private Component rgbToggleLabel() {
        return Component.literal("RGB Mode: " + (rgbMode ? "ON" : "OFF"));
    }

    /**
     * Enables or disables UI controls based on the current RGB mode state.
     *
     * When RGB mode is enabled:
     *  - Static RGB sliders are disabled
     *  - RGB speed slider is enabled
     */
    private void updateEnabledStates() {
        // If RGB mode is ON, static sliders still can be adjusted for later,
        // but you can choose to disable them if you prefer.
        boolean staticEnabled = !rgbMode;

        rSlider.active = staticEnabled;
        gSlider.active = staticEnabled;
        bSlider.active = staticEnabled;

        cpsSlider.active = rgbMode;
    }

    /**
     * Applies a live preview of the current UI state.
     *
     * This updates GlowColorManager overrides immediately without
     * saving anything to disk.
     */
    private void applyLivePreview() {
        GlowColorManager.setOverrideRgbMode(rgbMode);

        if (!rgbMode) {
            int rgb24 = (r << 16) | (g << 8) | b;
            GlowColorManager.setOverrideColorRgb24(rgb24);
        } else {
            GlowColorManager.setOverrideColorRgb24(null);

            // Update RGB speed in-memory so the rainbow animation
            // reflects the slider immediately
            ModConfig.RAINBOW_CYCLES_PER_SECOND.set(rgbCps);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int boxY = this.height / 4 - 40;

        int previewRgb24;
        String label;

        if (rgbMode) {
            // Smooth rainbow preview using real-time seconds
            double seconds = Util.getMillis() / 1000.0;
            float hue01 = (float) ((seconds * rgbCps) % 1.0);

            previewRgb24 = GlowColorUtil.hsvToRgb24(hue01, 1.0f, 1.0f);
            label = "RGB Preview";
        } else {
            // Static color preview
            previewRgb24 = (r << 16) | (g << 8) | b;
            label = String.format("Static: #%02X%02X%02X", r, g, b);
        }

        int previewArgb = 0xFF000000 | previewRgb24;
        gfx.fill(cx - 40, boxY, cx + 40, boxY + 20, previewArgb);

        gfx.drawCenteredString(this.font, label, cx, boxY + 26, 0xFFFFFF);
    }


    /* --------------------------------------------------------------------- */
    /* Slider helper implementations                                         */
    /* --------------------------------------------------------------------- */


    /**
     * Integer slider for 0–255 channel values.
     */
    private static final class IntSlider extends AbstractSliderButton {
        private final String label;
        private final IntConsumer onChange;

        IntSlider(int x, int y, int w, int h, String label, int initial, IntConsumer onChange) {
            super(x, y, w, h, Component.empty(), initial / 255.0);
            this.label = label;
            this.onChange = onChange;
            updateMessage();
        }

        @Override protected void updateMessage() {
            int v = (int) Math.round(this.value * 255.0);
            this.setMessage(Component.literal(label + ": " + v));
        }

        @Override protected void applyValue() {
            int v = (int) Math.round(this.value * 255.0);
            onChange.accept(v);
        }
    }

    /**
     * Double slider with a configurable range.
     *
     * Used for RGB cycle speed (cycles per second).
     */
    private static final class DoubleSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final DoubleConsumer onChange;

        DoubleSlider(int x, int y, int w, int h, String label, double initial, double min, double max, DoubleConsumer onChange) {
            super(x, y, w, h, Component.empty(), (initial - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        double getValueDouble() {
            return min + (max - min) * this.value;
        }

        @Override protected void updateMessage() {
            double v = getValueDouble();
            this.setMessage(Component.literal(label + ": " + String.format("%.2f", v)));
        }

        @Override protected void applyValue() {
            onChange.accept(getValueDouble());
        }
    }
}