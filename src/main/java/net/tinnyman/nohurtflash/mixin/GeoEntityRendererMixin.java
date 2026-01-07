package net.tinnyman.nohurtflash.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Mixin(GeoEntityRenderer.class)
public class GeoEntityRendererMixin {
    @ModifyArg(
            method = "getPackedOverlay(Lnet/minecraft/world/entity/Entity;F)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/OverlayTexture;v(Z)I"),
            index = 0,
            require = 0
    )
    private boolean disableGeckoHurtFlash(boolean original) {
        return false;
    }
}