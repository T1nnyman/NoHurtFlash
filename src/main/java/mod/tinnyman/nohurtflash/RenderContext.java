package mod.tinnyman.nohurtflash;

import net.minecraft.world.entity.LivingEntity;

public class RenderContext {
    public static final ThreadLocal<LivingEntity> CURRENT_RENDERING_ENTITY = new ThreadLocal<>();
}
