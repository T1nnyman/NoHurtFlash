package net.tinnyman.nohurtflash.mixin;

import com.cobblemon.mod.common.client.render.models.blockbench.PosableEntityModel;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.tinnyman.nohurtflash.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cobblemon integration.
 * Cobblemon's PosableEntityModel overrides the normal Minecraft overlay
 * calculation and manually creates the hurt overlay using:
 * OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0)
 * Because of this, disabling the vanilla hurt effect through
 * LivingEntityRenderer is not enough for Cobblemon Pokemon.
 * We intercept PosableEntityModel#getOverlayTexture and return
 * OverlayTexture.NO_OVERLAY for Pokemon entities when the old hurt
 * effect is disabled.
 * This allows the NoHurtFlash glow effect to replace Cobblemon's
 * normal red hurt flash. */
@Mixin(PosableEntityModel.class)
public class CobblemonPosableEntityModelMixin {
    @Inject(
            method = "getOverlayTexture",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void disableCobblemonHurtFlash(Entity entity, CallbackInfoReturnable<Integer> cir) {
        if (!(entity instanceof PokemonEntity)) return;

        // If the user wants the original Minecraft hurt effect, allow cobblemons normal behavior.
        if (Config.OLD_HURT_EFFECT_ENABLED.get()) return;

        // remove the hurt overlay.
        cir.setReturnValue(OverlayTexture.NO_OVERLAY);
    }
}
