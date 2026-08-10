package net.tinnyman.nohurtflash.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.tinnyman.nohurtflash.Config;
import net.tinnyman.nohurtflash.Util.RenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mixin responsibilities:
 *  1) Track which LivingEntity is currently being rendered (ThreadLocal), so other hooks can know "who" is being rendered without changing method signatures.
 *  2) Disable the vanilla red damage overlay for non-player entities by forcing OverlayTexture.v(false) when computing overlay coords.
 * Note: We intentionally keep the overlay for players to avoid changing the player's HUD/feedback. */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    /** Push the currently rendering entity into RenderContext for this render call.
     * We also clear any stale value first as a safety net in the rare case that a previous render call exited abnormally (exception) and did not run the RETURN injector. */
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            require = 1)
    private void pushCurrentEntity(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci)  {
        RenderContext.CURRENT_RENDERING_ENTITY.remove(); // failsafe against stale state
        RenderContext.CURRENT_RENDERING_ENTITY.set(entity);
    }

    /** Clear the currently rendering entity after the render call completes normally. */
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"),
            require = 1)
    private void popCurrentEntity(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        RenderContext.CURRENT_RENDERING_ENTITY.remove();
    }

    /** Vanilla decides whether to apply the hurt overlay via OverlayTexture.v(boolean).
     * This hook forces that boolean to false for non-player entities so the red flash is suppressed, allowing our custom glow effect to be the primary feedback.
     * If we cannot identify the current entity (ThreadLocal missing), we fall back to vanilla behavior to avoid unintended side effects. */
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
        // User wants the original Minecraft hurt effect.
        if (Config.OLD_HURT_EFFECT_ENABLED.get()) return original;
        LivingEntity current = RenderContext.CURRENT_RENDERING_ENTITY.get();
        if (current == null) return original; // fallback
        return false;
    }
}
