package net.tinnyman.nohurtflash;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;

import static net.tinnyman.nohurtflash.util.GlowColorUtil.hsvToRgb24;
import static net.tinnyman.nohurtflash.util.GlowColorUtil.rgbToHsv;

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
    private DoubleSlider cpsSlider;
    private ColorPickerWidget colorPicker;
    private ColorPreviewWidget colorPreview;

    /* -------- Color Picker State -------- */
    private float hue;
    private float saturation;
    private float value;

    /* -------- Layout constants -------- */
    private static final int CONTROL_WIDTH = 240;
    private static final int CONTROL_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 116;
    private static final int BUTTON_GAP = 8;
    private static final int VERTICAL_GAP = 6;
    private static final int COLOR_PICKER_WIDTH = CONTROL_WIDTH;
    private static final int COLOR_PICKER_HEIGHT = 90;
    private static final int HUE_HEIGHT = 14;
    private static final int HUE_GAP = 6;
    private static final int PREVIEW_WIDTH = CONTROL_WIDTH;
    private static final int PREVIEW_HEIGHT = CONTROL_HEIGHT;

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

        float[] hsv = rgbToHsv(r, g, b);

        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];

        // Initialize RGB mode state from config
        this.rgbMode = Config.RAINBOW_MODE_ENABLED.get();
        this.rgbCps = Config.RAINBOW_CYCLES_PER_SECOND.get();
    }


    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 20;

        /* ================================================================ */
        /* Color Preview                                                     */
        /* ================================================================ */

        colorPreview = addRenderableWidget(new ColorPreviewWidget(cx - PREVIEW_WIDTH / 2, y, PREVIEW_WIDTH, PREVIEW_HEIGHT, this));
        y+= PREVIEW_HEIGHT + VERTICAL_GAP;

        /* ================================================================ */
        /* Glow Effect + Old Hurt Effect                                    */
        /* ================================================================ */

        Button glowToggleBtn = addRenderableWidget(Button.builder(glowToggleLabel(), btn -> {
            glowEnabled = !glowEnabled;
            btn.setMessage(glowToggleLabel());
            updateEnabledStates();
            applyLivePreview();
        }).bounds(cx - BUTTON_WIDTH - (BUTTON_GAP / 2), y, BUTTON_WIDTH, CONTROL_HEIGHT).build());

        Button oldHurtToggleBtn = addRenderableWidget(Button.builder(oldHurtEffectLabel(), btn -> {
            oldHurtEffectEnabled = !oldHurtEffectEnabled;
            btn.setMessage(oldHurtEffectLabel());
            applyLivePreview();
        }).bounds(cx + (BUTTON_GAP / 2), y, BUTTON_WIDTH, CONTROL_HEIGHT).build());

        // Move to the next row.
        y += CONTROL_HEIGHT + VERTICAL_GAP;

        /* ================================================================ */
        /* RGB Mode                                                         */
        /* ================================================================ */

        Button rgbToggleBtn = addRenderableWidget(Button.builder(rgbToggleLabel(), btn -> {
            rgbMode = !rgbMode;
            btn.setMessage(rgbToggleLabel());
            updateEnabledStates();
            applyLivePreview();
        }).bounds(cx - CONTROL_WIDTH / 2, y, CONTROL_WIDTH, CONTROL_HEIGHT).build());

        y += CONTROL_HEIGHT + VERTICAL_GAP;

        /* ================================================================ */
        /* RGB Speed                                                        */
        /* ================================================================ */

        cpsSlider = addRenderableWidget(new DoubleSlider(cx - CONTROL_WIDTH / 2, y, CONTROL_WIDTH, CONTROL_HEIGHT, "RGB Speed (cycles/sec)",
                rgbCps, 0.1, 2.0, v -> {
            rgbCps = v;
            applyLivePreview();
        }));

        y += CONTROL_HEIGHT + VERTICAL_GAP;

        /* ================================================================ */
        /* Color Picker                                                     */
        /* ================================================================ */

        colorPicker = addRenderableWidget(new ColorPickerWidget(cx - COLOR_PICKER_WIDTH / 2, y, COLOR_PICKER_WIDTH, COLOR_PICKER_HEIGHT, hue, saturation, value, this::onColorChanged));
        y+= COLOR_PICKER_HEIGHT + VERTICAL_GAP;

        /* ================================================================ */
        /* Save / Cancel                                                    */
        /* ================================================================ */

        addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
            String hex = String.format("#%02X%02X%02X", r, g, b);

            // Persist all settings to the config.
            Config.applyConfigSettings(glowEnabled, oldHurtEffectEnabled, rgbMode, rgbCps, hex);

            // Clear runtime overrides so the saved config
            // becomes the source of truth.
            GlowColorManager.setOverrideRgbMode(null);
            GlowColorManager.setOverrideColorRgb24(null);

            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(cx - BUTTON_WIDTH - (BUTTON_GAP / 2), y, BUTTON_WIDTH, CONTROL_HEIGHT).build());


        addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            // Remove live preview overrides.
            GlowColorManager.setOverrideRgbMode(null);
            GlowColorManager.setOverrideColorRgb24(null);

            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(cx + (BUTTON_GAP / 2), y, BUTTON_WIDTH, CONTROL_HEIGHT).build());

        updateEnabledStates();
        applyLivePreview();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
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

        if (colorPicker != null) colorPicker.active = staticEnabled;

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
            Config.RAINBOW_CYCLES_PER_SECOND.set(rgbCps);
        }
    }

    private void applyHexColor(String hex) {
        String normalized = hex.trim();

        if (normalized.startsWith("#")) normalized = normalized.substring(1);
        if (!normalized.matches("[0-9a-fA-F]{6}")) return;

        int rgb24;

        try {
            rgb24 = Integer.parseInt(normalized, 16);
        } catch (NumberFormatException e) {
            return;
        }

        this.r = (rgb24 >> 16) & 0xFF;
        this.g = (rgb24 >> 8) & 0xFF;
        this.b = rgb24 & 0xFF;

        float[] hsv = rgbToHsv(r, g, b);

        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];

        if (colorPicker != null) colorPicker.setColor(hue, saturation, value);

        applyLivePreview();
    }

    private void onColorChanged(float hue, float saturation, float value) {
        this.hue = hue;
        this.saturation = saturation;
        this.value = value;

        int rgb24 = hsvToRgb24(hue, saturation, value);

        this.r = (rgb24 >> 16) & 0xFF;
        this.g = (rgb24 >> 8) & 0xFF;
        this.b = rgb24 & 0xFF;

        applyLivePreview();
    }

    /* --------------------------------------------------------------------- */
    /* RGB Speed Slider                                                      */
    /* --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- */
    /* Color Picker                                                          */
    /* --------------------------------------------------------------------- */
    private static final class ColorPickerWidget extends AbstractWidget {
        private final int pickerWidth;
        private final int pickerHeight;

        private float hue;
        private float saturation;
        private float value;

        private final ColorChangeListener listener;

        private boolean draggingPicker;
        private boolean draggingHue;

        ColorPickerWidget(int x, int y, int width, int height, float hue, float saturation, float value, ColorChangeListener listener) {
            super(x, y, width, height, Component.empty());

            this.pickerWidth = width;
            this.pickerHeight = height;
            this.hue = hue;
            this.saturation = saturation;
            this.value = value;
            this.listener = listener;
        }

        @Override
        protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int squareHeight = pickerHeight - HUE_HEIGHT - HUE_GAP;

            /* ============================================================= */
            /* Saturation / Value square                                     */
            /* ============================================================= */

            for (int px = 0; px < pickerWidth; px++) {
                float columnSaturation = px / (float) (pickerWidth - 1);
                int topRgb = hsvToRgb24(hue, columnSaturation, 1.0f);
                int topArgb = 0xFF000000 | topRgb;
                int bottomArgb = 0xFF000000;

                gfx.fillGradient(x + px, y, x + px + 1, y + squareHeight, topArgb, bottomArgb);
            }

            /* ============================================================= */
            /* Selection cursor                                              */
            /* ============================================================= */

            int cursorX = x + Math.round(saturation * (pickerWidth - 1));
            int cursorY = y + Math.round((1.0f - value) * (squareHeight - 1));

            gfx.renderOutline(cursorX - 4, cursorY - 4, 8, 8, 0xFF000000);
            gfx.renderOutline(cursorX - 3, cursorY - 3, 6, 6, 0xFFFFFFFF);

            /* ============================================================= */
            /* Hue bar                                                        */
            /* ============================================================= */

            int hueY = y + squareHeight + HUE_GAP;
            int segments = 180;

            for (int i = 0; i < segments; i++) {
                float h = i / (float) segments;
                int color = hsvToRgb24(h, 1.0f, 1.0f);
                int segmentX1 = x + (i * pickerWidth / segments);
                int segmentX2 = x + ((i + 1) * pickerWidth / segments);

                gfx.fill(segmentX1, hueY, segmentX2 + 1, hueY + HUE_HEIGHT, 0xFF000000 | color);
            }

            /* ============================================================= */
            /* Hue cursor                                                     */
            /* ============================================================= */

            int hueCursorX = x + Math.round(hue * (pickerWidth - 1));

            gfx.renderOutline(hueCursorX - 3, hueY - 2, 6, HUE_HEIGHT + 4, 0xFFFFFFFF);
            gfx.renderOutline(hueCursorX - 4, hueY -3, 8, HUE_HEIGHT + 6, 0xFF000000);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.literal("Color Picker"));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || button != 0) return false;

            int x = getX();
            int y = getY();
            int squareHeight = pickerHeight - HUE_HEIGHT - HUE_GAP;

            /* ------------------------------------------------------------- */
            /* Saturation / Value square                                     */
            /* ------------------------------------------------------------- */

            if (mouseX >= x && mouseX < x + pickerWidth && mouseY >= y && mouseY < y + squareHeight) {
                draggingPicker = true;
                updatePickerPosition(mouseX, mouseY);
                return true;
            }

            /* ------------------------------------------------------------- */
            /* Hue bar                                                        */
            /* ------------------------------------------------------------- */

            int hueY = y + squareHeight + HUE_GAP;

            if (mouseX >= x && mouseX < x + pickerWidth && mouseY >= hueY && mouseY < hueY + HUE_HEIGHT) {
                draggingHue = true;
                updateHuePosition(mouseX);
                return true;
            }

            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (!active || button != 0) return false;

            if (draggingPicker) {
                updatePickerPosition(mouseX, mouseY);
                return true;
            }

            if (draggingHue) {
                updateHuePosition(mouseX);
                return true;
            }

            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0) {
                draggingPicker = false;
                draggingHue = false;
            }

            return super.mouseReleased(mouseX, mouseY, button);
        }

        private void setColor(float hue, float saturation, float value) {
            this.hue = clamp01(hue);
            this.saturation = clamp01(saturation);
            this.value = clamp01(value);
        }

        private void updatePickerPosition(double mouseX, double mouseY) {
            int hueHeight = 14;
            int hueGap = 6;
            int squareHeight = pickerHeight - hueHeight - hueGap;
            float newSaturation = (float) ((mouseX - getX()) / (double) (pickerWidth - 1));
            float newValue = 1.0f - (float) ((mouseY - getY()) / (double) (squareHeight - 1));

            saturation = clamp01(newSaturation);
            value = clamp01(newValue);

            listener.onColorChanged(hue, saturation, value);
        }

        private void updateHuePosition(double mouseX) {
            float newHue = (float) ((mouseX - getX()) / (double) (pickerWidth - 1));

            hue = clamp01(newHue);

            listener.onColorChanged(hue, saturation, value);
        }

        private static float clamp01(float value) {
            return Math.max(0.0f, Math.min(1.0f, value));
        }

        @FunctionalInterface
        interface ColorChangeListener {
            void onColorChanged(float hue, float saturation, float value);
        }
    }

    /* --------------------------------------------------------------------- */
    /* Color Picker Preview                                                  */
    /* --------------------------------------------------------------------- */
    private static final class ColorPreviewWidget extends AbstractWidget {
        private final ConfigScreen screen;

        ColorPreviewWidget(int x, int y, int width, int height, ConfigScreen screen) {
            super(x, y, width, height, Component.empty());

            this.screen = screen;
        }

        @Override
        protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            int rgb24;
            String label;

            if (screen.rgbMode) {
                double seconds = Util.getMillis() / 1000.0;
                float hue01 = (float) ((seconds * screen.rgbCps) % 1.0);
                rgb24 = hsvToRgb24(hue01, 1.0f, 1.0f);
                label = "RGB Preview";
            } else {
                rgb24 = (screen.r << 16) | (screen.g << 8) | screen.b;
                label = String.format("#%02X%02X%02X", screen.r, screen.g, screen.b);
            }

            gfx.renderOutline(getX(), getY(), width, height, 0xFF000000);
            gfx.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFF000000 | rgb24);

            int textColor = getPreviewTextColor(rgb24);
            boolean showEditIndicator = !screen.rgbMode && isHovered;
            String displayLabel = showEditIndicator ? label + "✎" : label;
            int textWidth = screen.font.width(label);
            int textX = getX() + (width - textWidth) / 2;
            int textY = getY() + (height - screen.font.lineHeight) / 2;

            gfx.drawString(screen.font, displayLabel, textX, textY, textColor, false);

            if (showEditIndicator) {
                int hexWidth = screen.font.width(label);
                int underlineY = textY + screen.font.lineHeight;

                gfx.fill(textX, underlineY, textX + hexWidth, underlineY + 1, textColor);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.literal("Current Glow Color"));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || button != 0) return false;
            if (!isMouseOver(mouseX, mouseY)) return false;
            if (screen.rgbMode) return false;

            String currentHex = String.format("#%02X%02X%02X", screen.r, screen.g, screen.b);

            if (screen.minecraft != null) screen.minecraft.setScreen(new HexColorInputScreen(screen, currentHex, screen::applyHexColor));

            return true;
        }

        private int getPreviewTextColor(int rgb) {
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;

            double luminance = (0.299 * red) + (0.587 * green) + (0.114 * blue);

            return luminance > 160 ? 0xFF000000 : 0xFFFFFFFF;
        }
    }
}