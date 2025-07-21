package mod.tinnyman.nohurtflash;

import net.minecraft.ChatFormatting;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraftforge.common.ForgeConfig.Client;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

public class ModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLE_GLOW;
    public static final ForgeConfigSpec.ConfigValue<String> GLOW_COLOR;

    static {
        BUILDER.push("General Settings");

        ENABLE_GLOW = BUILDER
                .comment("Enable or disable the hurt-time glow effect")
                .define("enableGlow", true);

        GLOW_COLOR = BUILDER
                .comment("Glow color (RED, BLUE, GREEN, etc.) - must be a valid ChatFormatting color")
                .define("glowColor", "RED");

        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static ChatFormatting getGlowColor() {
        try {
            return ChatFormatting.valueOf(GLOW_COLOR.get().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ChatFormatting.RED;
        }
    }
}
