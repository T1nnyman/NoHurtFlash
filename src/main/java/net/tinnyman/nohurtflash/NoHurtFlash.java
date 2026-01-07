package net.tinnyman.nohurtflash;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(NoHurtFlash.MODID)
public class NoHurtFlash {
    public static final String MODID = "nohurtflash";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NoHurtFlash(FMLJavaModLoadingContext context) {
        // Client Config
        context.registerConfig(ModConfig.Type.CLIENT, net.tinnyman.nohurtflash.ModConfig.SPEC);

        if (!ModList.get().isLoaded("geckolib")) {
            NoHurtFlash.LOGGER.warn(
                    "[NoHurtFlash] GeckoLib not detected – GeckoLib entity support will be disabled."
            );
        }
    }
}
