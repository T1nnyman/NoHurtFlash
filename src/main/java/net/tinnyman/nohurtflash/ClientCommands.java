package net.tinnyman.nohurtflash;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import static net.minecraft.commands.Commands.literal;

/**
 * Client-side command registration for NoHurtFlash.
 *
 * These commands exist purely for client interaction (UI / visual settings)
 * and do not affect server state in any way.
 */
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
                .executes(ctx -> {
                    Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new ConfigScreen(Minecraft.getInstance().screen)));
                    return 1;
                })
        );
    }
}
