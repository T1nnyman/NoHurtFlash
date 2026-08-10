package net.tinnyman.nohurtflash.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.tinnyman.nohurtflash.Config;
import net.tinnyman.nohurtflash.util.RenderContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** GeckoLib integration.
 * GeckoLib entities are rendered through GeoEntityRenderer rather than vanilla LivingEntityRenderer.
 * We mirror our vanilla behavior by:
 *  - setting RenderContext.CURRENT_RENDERING_ENTITY while a GeckoLib living entity is rendering
 *  - disabling the vanilla hurt overlay during overlay coordinate calculation (non-players only)
 * Notes:
 *  - remap = false because GeoEntityRenderer is not part of Mojang-mapped Minecraft classes.
 *  - require = 0 so the mod can run without GeckoLib present (optional dependency). */
@Mixin(value = GeoEntityRenderer.class)
public class GeoEntityRendererMixin {
    @Inject(method = "actuallyRender(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/Entity;Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIII)V",
            at = @At("HEAD"),
            require = 0,
            remap = false
    )
    private void pushCurrentEntity(PoseStack poseStack, Entity animatable, BakedGeoModel model, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                                   boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour, CallbackInfo ci) {
        RenderContext.CURRENT_RENDERING_ENTITY.remove();

        if (animatable instanceof LivingEntity living) {
            RenderContext.CURRENT_RENDERING_ENTITY.set(living);
        }
    }

    @Inject(method = "actuallyRender(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/Entity;Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIII)V",
            at = @At("RETURN"),
            require = 0,
            remap = false
    )
    private void popCurrentEntity(PoseStack poseStack, Entity animatable, BakedGeoModel model, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour, CallbackInfo ci) {
        RenderContext.CURRENT_RENDERING_ENTITY.remove();
    }

    /** GeckoLib calls getPackedOverlay which ultimately computes OverlayTexture.v(boolean).
     * We force the boolean to false for entities to disable the red hurt flash. */
    @ModifyArg(
            method = "getPackedOverlay(Lnet/minecraft/world/entity/Entity;FF)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/OverlayTexture;v(Z)I"),
            index = 0,
            require = 0,
            remap = false
    )
    private boolean disableGeckoHurtFlash(boolean original) {
        // User wants the original Minecraft hurt effect.
        if (Config.OLD_HURT_EFFECT_ENABLED.get()) return original;

        return false;
    }
}