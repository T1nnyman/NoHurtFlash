package net.tinnyman.nohurtflash.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tinnyman.nohurtflash.GlowColorManager;
import net.tinnyman.nohurtflash.RenderContext;
import net.tinnyman.nohurtflash.VisibilityUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(LevelRenderer.class)
public class LevelRendererTeamColorRedirectMixin {
    @Redirect(method = "renderLevel",
              at = @At(
                      value = "INVOKE",
                      target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"
              ),
              require = 0
    )
    private int overrideTeamColor(Entity entity) {
        if (!(entity instanceof LivingEntity le)) {
            return entity.getTeamColor();
        }

        if (le instanceof Player) {
            return entity.getTeamColor();
        }

        if (le.hurtTime > 0 && VisibilityUtil.canPlayerSeeEntityNow(le)) {
            int r = GlowColorManager.getR();
            int g = GlowColorManager.getG();
            int b = GlowColorManager.getB();
            return FastColor.ABGR32.color(255, r, g, b);
        }

        return entity.getTeamColor();
    }
}
