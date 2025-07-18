package mod.tinnyman.nohurtflash.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @ModifyArg(method = "getOverlayCoords", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/OverlayTexture;v(Z)I"))
    private static boolean notHurtOverlay(boolean original) {
        return false;
    }
}
