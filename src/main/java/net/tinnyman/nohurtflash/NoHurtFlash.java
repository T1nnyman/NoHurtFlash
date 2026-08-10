package net.tinnyman.nohurtflash;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(NoHurtFlash.MODID)
public class NoHurtFlash {
    public static final String MODID = "nohurtflash_neoforge";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NoHurtFlash(IEventBus modEventBus, ModContainer modContainer) {
        // Client Config
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        // Register configuration event handlers.
        modEventBus.register(Config.class);

        // Register client/game event handlers.
        NeoForge.EVENT_BUS.register(GlowEffectHandler.class);
        NeoForge.EVENT_BUS.register(ClientCommands.class);

        // Geckolib dependency check
        if (ModList.get().isLoaded("geckolib")) {
            LOGGER.info("[NoHurtFlash] GeckoLib detected – GeckoLib entity support is enabled.");
        } else {
            LOGGER.warn("[NoHurtFlash] GeckoLib not detected – GeckoLib entity support will be disabled.");
        }
    }
}
