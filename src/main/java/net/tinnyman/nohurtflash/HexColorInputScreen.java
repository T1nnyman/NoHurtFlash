package net.tinnyman.nohurtflash;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class HexColorInputScreen extends Screen {
    private final Screen parent;
    private final String initialHex;
    private final Consumer<String> onApply;

    private EditBox hexInput;
    private boolean valid;

    public HexColorInputScreen(Screen parent, String initialHex, Consumer<String> onApply) {
        super(Component.literal("Enter Hex Color"));

        this.parent = parent;
        this.initialHex = initialHex;
        this.onApply = onApply;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int inputWidth = 160;

        hexInput = new EditBox(this.font, cx - inputWidth / 2, cy - 20, inputWidth, 20, Component.literal("Hex Color"));
        hexInput.setMaxLength(7);
        hexInput.setValue(initialHex);

        hexInput.setFilter(value -> {
            if (value.length() > 7) return false;
            if (value.isEmpty()) return true;

            String normalized = value.startsWith("#") ? value.substring(1) : value;

            return normalized.length() <= 6 && normalized.matches("[0-9a-fA-F]*");
        });

        hexInput.setResponder(value -> {
            valid = isValidHex(value);
        });

        valid = isValidHex(initialHex);

        addRenderableWidget(hexInput);

        addRenderableWidget(Button.builder(Component.literal("Apply"), btn -> {
            if (!valid) return;

            String normalized = normalizeHex(hexInput.getValue());

            onApply.accept(normalized);

            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(cx - 82, cy + 15, 78, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(cx + 4, cy + 15, 78, 20).build());

        setInitialFocus(hexInput);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int cy = this.height / 2;

        gfx.drawCenteredString(this.font, "Enter HEX Color", cx, cy - 50, 0xFFFFFF);
        gfx.drawCenteredString(this.font, valid ? "Valid" : "Use #RRGGBB", cx, cy + 45, valid ? 0x55FF55 : 0xFF5555);
    }

    private static boolean isValidHex(String value) {
        if (value == null) return false;

        String normalized = value.startsWith("#") ? value.substring(1) : value;

        return normalized.matches("[0-9a-fA-F]{6}");
    }

    private static String normalizeHex(String value) {
        String normalized = value.startsWith("#") ? value.substring(1) : value;

        return "#" + normalized.toUpperCase();
    }
}
