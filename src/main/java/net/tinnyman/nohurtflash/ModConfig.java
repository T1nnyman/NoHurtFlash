package net.tinnyman.nohurtflash;

import net.minecraft.ChatFormatting;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;

public class ModConfig {
    public static final Builder BUILDER = new Builder();
    public static final BooleanValue ENABLE_GLOW;
    public static final ConfigValue<String> GLOW_COLOR;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("General Settings");

        ENABLE_GLOW = BUILDER
                .comment("Enable or disable the hurt-time glow effect")
                .define("enableGlow", true);

        GLOW_COLOR = BUILDER
                .comment("Glow color (RED, BLUE, GREEN, etc.) - must be a valid ChatFormatting color")
                .define("glowColor", "RED");

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static ChatFormatting getGlowColor() {
        try {
            return ChatFormatting.valueOf(GLOW_COLOR.get().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ChatFormatting.RED;
        }
    }
}
