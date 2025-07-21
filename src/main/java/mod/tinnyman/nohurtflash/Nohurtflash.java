package mod.tinnyman.nohurtflash;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import static mod.tinnyman.nohurtflash.ModConfig.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Nohurtflash.MODID)
public class Nohurtflash {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "nohurtflash";

    public Nohurtflash() {
        //noinspection removal
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }
}
