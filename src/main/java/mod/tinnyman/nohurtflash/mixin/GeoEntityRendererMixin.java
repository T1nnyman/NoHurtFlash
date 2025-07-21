package mod.tinnyman.nohurtflash.mixin;

import mod.tinnyman.nohurtflash.ModCompat;
import mod.tinnyman.nohurtflash.ModConfig;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Pseudo
@Mixin(GeoEntityRenderer.class)
public abstract class GeoEntityRendererMixin<T extends Entity & GeoAnimatable> {
    @ModifyArg(
            method = "getPackedOverlay(Lnet/minecraft/world/entity/Entity;F)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/OverlayTexture;v(Z)I"
            )
    )
    private boolean noGeckoHurtOverlay(boolean original) {
        return false;
    }
}