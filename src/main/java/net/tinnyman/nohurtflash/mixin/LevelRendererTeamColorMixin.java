package net.tinnyman.nohurtflash.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tinnyman.nohurtflash.GlowColorManager;
import net.tinnyman.nohurtflash.Util.VisibilityUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Redirects LevelRenderer's call to Entity#getTeamColor() during world rendering.
 *
 * Why this exists:
 *  - Vanilla uses "team color" to tint the glowing outline.
 *  - We temporarily force entities to "glow" while hurt (via shared flags).
 *  - This mixin supplies a custom outline color for that forced glow so the
 *    effect isn't always the default team/white color.
 *
 * Safety rules:
 *  - Do not affect players (keep vanilla behavior).
 *  - Only override color when we are actively applying the hurt-glow effect.
 *  - Only override color when the entity is actually visible (no x-ray).
 *
 * Important:
 *  - This changes what color the outline renders with, not whether the entity glows.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererTeamColorMixin {
    @WrapOperation(method = "renderLevel",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(IIII)V"
                ),
                require = 0
    )
    private void overrideOutlineColor(OutlineBufferSource outline, int r, int g, int b, int a, Operation<Void> original, @Local Entity entity) {
        int rr = r, gg = g, bb = b, aa = a;

        if (entity instanceof LivingEntity living && !(living instanceof Player)) {
            if (living.hurtTime > 0 && VisibilityUtil.canPlayerSeeEntityNow(living)) {
                rr = GlowColorManager.getR();
                gg = GlowColorManager.getG();
                bb = GlowColorManager.getB();
                aa = 255;
            }
        }

        original.call(outline, rr, gg, bb, aa);
    }
}
