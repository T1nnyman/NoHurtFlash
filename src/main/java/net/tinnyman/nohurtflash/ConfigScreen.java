package net.tinnyman.nohurtflash;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.tinnyman.nohurtflash.util.GlowColorUtil;

import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/** Client-side GUI for configuring the hurt-glow color.
 *
 * This screen provides:
 *  - Static RGB color selection with live preview
 *  - RGB (rainbow) mode toggle
 *  - RGB cycle speed control
 *  - Enable/disable custom hurt glow
 *  - Enable/disable vanilla Minecraft hurt effect
 *
 *  All changes are previewed instantly and can be saved directly to the
 *  client config without requiring a game restart. */
public class ConfigScreen extends Screen {

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

    /* -------- Effect state -------- */
    /** Whether the NoHurtFlash custom glow is enabled. */
    private boolean glowEnabled;

    /** Whether Minecraft's original red hurt overlay is enabled.
    This is intentionally independent of glowEnabled so users can choose either effect, both, or neither. */
    private boolean oldHurtEffectEnabled;

    /* -------- UI widgets -------- */

    private IntSlider rSlider;
    private IntSlider gSlider;
    private IntSlider bSlider;

    private DoubleSlider cpsSlider;

    /** Creates the glow color picker screen.
     * Initial values are pulled from the current client config so the
     * UI reflects the active settings when opened. */
    public ConfigScreen(Screen parent) {
        super(Component.literal("NoHurtFlash Settings"));
        this.parent = parent;

        // Initialize effect enabled state from config
        this.glowEnabled = Config.GLOW_ENABLED.get();
        this.oldHurtEffectEnabled = Config.OLD_HURT_EFFECT_ENABLED.get();

        // Initialize static color from cached config values
        this.r = Config.glowRed();
        this.g = Config.glowGreen();
        this.b = Config.glowBlue();

        // Initialize RGB mode state from config
        this.rgbMode = Config.RAINBOW_MODE_ENABLED.get();
        this.rgbCps = Config.RAINBOW_CYCLES_PER_SECOND.get();
    }


    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 4;
        int controlWidth = 240;
        int controlHeight = 20;
        int buttonWidth = 116;
        int buttonGap = 8;
        int verticalGap = 6;

        /* ================================================================ */
        /* Glow Effect + Old Hurt Effect                                    */
        /* ================================================================ */

        Button glowToggleBtn = addRenderableWidget(Button.builder(glowToggleLabel(), btn -> {
            glowEnabled = !glowEnabled;
            btn.setMessage(glowToggleLabel());
            updateEnabledStates();
            applyLivePreview();
        }).bounds(cx - buttonWidth - (buttonGap / 2), y, buttonWidth, controlHeight).build());

        Button oldHurtToggleBtn = addRenderableWidget(Button.builder(oldHurtEffectLabel(), btn -> {
            oldHurtEffectEnabled = !oldHurtEffectEnabled;
            btn.setMessage(oldHurtEffectLabel());
            applyLivePreview();
        }).bounds(cx + (buttonGap / 2), y, buttonWidth, controlHeight).build());

        // Move to the next row.
        y += controlHeight + verticalGap;


        /* ================================================================ */
        /* RGB Mode                                                         */
        /* ================================================================ */

        Button rgbToggleBtn = addRenderableWidget(Button.builder(rgbToggleLabel(), btn -> {
            rgbMode = !rgbMode;
            btn.setMessage(rgbToggleLabel());
            updateEnabledStates();
            applyLivePreview();
        }).bounds(cx - controlWidth / 2, y, controlWidth, controlHeight).build());

        y += controlHeight + verticalGap;

        /* ================================================================ */
        /* RGB Speed                                                        */
        /* ================================================================ */

        cpsSlider = addRenderableWidget(new DoubleSlider(cx - controlWidth / 2, y, controlWidth, controlHeight, "RGB Speed (cycles/sec)",
                rgbCps, 0.1, 2.0, v -> {
                    rgbCps = v;
                    applyLivePreview();
                }));

        y += controlHeight + verticalGap;

        /* ================================================================ */
        /* Red                                                               */
        /* ================================================================ */

        rSlider = addRenderableWidget(new IntSlider(cx - controlWidth / 2, y, controlWidth, controlHeight, "R", r, v -> {
            r = v;
            applyLivePreview();
        }));

        y += controlHeight + verticalGap;


        /* ================================================================ */
        /* Green                                                             */
        /* ================================================================ */

        gSlider = addRenderableWidget(new IntSlider(cx - controlWidth / 2, y, controlWidth, controlHeight, "G", g, v -> {
            g = v;
            applyLivePreview();
        }));

        y += controlHeight + verticalGap;


        /* ================================================================ */
        /* Blue                                                              */
        /* ================================================================ */

        bSlider = addRenderableWidget(new IntSlider(cx - controlWidth / 2, y, controlWidth, controlHeight, "B", b, v -> {
            b = v;
            applyLivePreview();
        }));

        y += controlHeight + verticalGap;

        /* ================================================================ */
        /* Save / Cancel                                                     */
        /* ================================================================ */

        addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
            String hex = String.format("#%02X%02X%02X", r, g, b);

            // Persist all settings to the config.
            Config.applyConfigSettings(glowEnabled, oldHurtEffectEnabled, rgbMode, rgbCps, hex);

            // Clear runtime overrides so the saved config
            // becomes the source of truth.
            GlowColorManager.setOverrideRgbMode(null);
            GlowColorManager.setOverrideColorRgb24(null);

            this.minecraft.setScreen(parent);
        }).bounds(cx - buttonWidth - (buttonGap / 2), y, buttonWidth, controlHeight).build());


        addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            // Remove live preview overrides.
            GlowColorManager.setOverrideRgbMode(null);
            GlowColorManager.setOverrideColorRgb24(null);

            this.minecraft.setScreen(parent);
        }).bounds(cx + (buttonGap / 2), y, buttonWidth, controlHeight).build());

        updateEnabledStates();
        applyLivePreview();
    }

    /** @return Label text for the RGB mode toggle button. */
    private Component rgbToggleLabel() {
        return Component.literal("RGB Mode: " + (rgbMode ? "ON" : "OFF"));
    }

    /** @return Label for the custom glow toggle. */
    private Component glowToggleLabel() { return Component.literal( "Glow Effect: " + (glowEnabled ? "ON" : "OFF") ); }

    /** @return Label for the vanilla hurt effect toggle. */
    private Component oldHurtEffectLabel() { return Component.literal( "Old Hurt Effect: " + (oldHurtEffectEnabled ? "ON" : "OFF") ); }

    /** Enables or disables UI controls based on the current RGB mode state.
     * When RGB mode is enabled:
     *  - Static RGB sliders are disabled
     *  - RGB speed slider is enabled */
    private void updateEnabledStates() {
        // If RGB mode is ON, static sliders still can be adjusted for later,
        // but you can choose to disable them if you prefer.
        boolean staticEnabled = !rgbMode;

        rSlider.active = staticEnabled;
        gSlider.active = staticEnabled;
        bSlider.active = staticEnabled;

        cpsSlider.active = rgbMode;
    }

    /** Applies a live preview of the current UI state.
     * This updates GlowColorManager overrides immediately without saving anything to disk. */
    private void applyLivePreview() {
        GlowColorManager.setOverrideRgbMode(rgbMode);

        if (!rgbMode) {
            int rgb24 = (r << 16) | (g << 8) | b;
            GlowColorManager.setOverrideColorRgb24(rgb24);
        } else {
            GlowColorManager.setOverrideColorRgb24(null);

            // Update RGB speed in-memory so the rainbow animation
            // reflects the slider immediately
            Config.RAINBOW_CYCLES_PER_SECOND.set(rgbCps);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
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


    /** Integer slider for 0–255 channel values. */
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

    /** Double slider with a configurable range.
     * Used for RGB cycle speed (cycles per second). */
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