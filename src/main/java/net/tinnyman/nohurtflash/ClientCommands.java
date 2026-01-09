package net.tinnyman.nohurtflash;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraft.commands.Commands.literal;

/**
 * Client-side command registration for NoHurtFlash.
 *
 * These commands exist purely for client interaction (UI / visual settings)
 * and do not affect server state in any way.
 */
@Mod.EventBusSubscriber(modid = NoHurtFlash.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientCommands {
    /**
     * Registers client-only commands.
     *
     * Currently, provided commands:
     *  - /nohurtflash picker
     *      Opens the glow color picker GUI, allowing live preview and
     *      immediate configuration of glow color and RGB mode.
     */
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(literal("nohurtflash")
                .then(literal("picker")
                        .executes(ctx -> {
                            Minecraft.getInstance().tell(() ->
                                    Minecraft.getInstance().setScreen(new GlowColorPickerScreen(Minecraft.getInstance().screen))
                            );
                            return 1;
                        }))
        );
    }
}
