package mod.tinnyman.nohurtflash.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.tinnyman.nohurtflash.RenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.checkerframework.checker.units.qual.A;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "render*", at = @At("HEAD"))
    private void storeRenderingEntity(LivingEntity entity, float f1, float f2, PoseStack stack,
                                      MultiBufferSource source, int packedLight, CallbackInfo ci) {
        RenderContext.CURRENT_RENDERING_ENTITY.set(entity);
    }

    @Inject(method = "render*", at = @At("RETURN"))
    private void clearRenderingEntity(LivingEntity entity, float f3, float f4, PoseStack stack,
                                      MultiBufferSource source, int packedLight, CallbackInfo ci) {
        RenderContext.CURRENT_RENDERING_ENTITY.remove();
    }

    @ModifyArg(
            method = "getOverlayCoords",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/OverlayTexture;v(Z)I"))
    private static boolean noHurtOverlay(boolean original) {
        LivingEntity entity = RenderContext.CURRENT_RENDERING_ENTITY.get();

        if (entity instanceof Player) {
            return original;
        }

        return false;
    }
}
