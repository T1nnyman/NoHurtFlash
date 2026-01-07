package net.tinnyman.nohurtflash.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tinnyman.nohurtflash.RenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            require = 1)
    private void storeRenderingEntity(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci)  {
        RenderContext.CURRENT_RENDERING_ENTITY.set(entity);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"),
            require = 1)
    private void clearRenderingEntity(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        RenderContext.CURRENT_RENDERING_ENTITY.remove();
    }

    @ModifyArg(
            method = "getOverlayCoords(Lnet/minecraft/world/entity/LivingEntity;F)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/OverlayTexture;v(Z)I"
            ),
            index = 0,
            require = 1
    )
    private static boolean disableHurtOverlay(boolean original) {
        LivingEntity entity = RenderContext.CURRENT_RENDERING_ENTITY.get();
        if (entity instanceof Player) {
            return original; // Keep hurt overlay for players.
        }
        return false;
    }
}
