package net.tinnyman.nohurtflash.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tinnyman.nohurtflash.RenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib integration.
 *
 * GeckoLib entities are rendered through GeoEntityRenderer rather than vanilla LivingEntityRenderer.
 * We mirror our vanilla behavior by:
 *  - setting RenderContext.CURRENT_RENDERING_ENTITY while a GeckoLib living entity is rendering
 *  - disabling the vanilla hurt overlay during overlay coordinate calculation (non-players only)
 *
 * Notes:
 *  - remap = false because GeoEntityRenderer is not part of Mojang-mapped Minecraft classes.
 *  - require = 0 so the mod can run without GeckoLib present (optional dependency).
 */
@Mixin(value = GeoEntityRenderer.class)
public class GeoEntityRendererMixin {
    @Inject(method = "actuallyRender(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/Entity;Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIIFFFF)V",
            at = @At("HEAD"),
            require = 0,
            remap = false
    )
    private void pushCurrentEntity(PoseStack poseStack, Entity animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                   boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        RenderContext.CURRENT_RENDERING_ENTITY.remove();

        if (animatable instanceof LivingEntity living) {
            RenderContext.CURRENT_RENDERING_ENTITY.set(living);
        }
    }

    @Inject(method = "actuallyRender(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/Entity;Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIIFFFF)V",
            at = @At("RETURN"),
            require = 0,
            remap = false
    )
    private void popCurrentEntity(PoseStack poseStack, Entity animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        RenderContext.CURRENT_RENDERING_ENTITY.remove();
    }

    /**
     * GeckoLib calls getPackedOverlay which ultimately computes OverlayTexture.v(boolean).
     * We force the boolean to false for entities to disable the red hurt flash.
     */
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