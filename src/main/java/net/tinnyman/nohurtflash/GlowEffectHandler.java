package net.tinnyman.nohurtflash;

import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tinnyman.nohurtflash.mixin.EntityAccessor;

@Mod.EventBusSubscriber(modid = NoHurtFlash.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlowEffectHandler {
    private static final int GLOW_FLAG_BIT = 0x40; // bit 6
    private static final Int2BooleanMap FORCED_GLOW = new Int2BooleanOpenHashMap();
    private static final Int2BooleanMap HAD_GLOW_BEFORE_FORCE = new Int2BooleanOpenHashMap();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!ModConfig.ENABLE_GLOW.get()) {
            clearAllForced();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;

        GlowColorManager.clientTick();
        EntityDataAccessor<Byte> sharedFlagsId = EntityAccessor.getSharedFlagsId();

        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living)) continue;
            if (living instanceof Player) continue;

            int id = living.getId();
            boolean wantHurtGlow = living.hurtTime > 0 && VisibilityUtil.canPlayerSeeEntityNow(living);
            byte flags = living.getEntityData().get(sharedFlagsId);
            boolean hasGlowBitNow = (flags & GLOW_FLAG_BIT) != 0;
            boolean weAreForcing = FORCED_GLOW.get(id);

            if (wantHurtGlow) {
                if (!weAreForcing) {
                    FORCED_GLOW.put(id, true);
                    HAD_GLOW_BEFORE_FORCE.put(id, hasGlowBitNow);
                }

                if (!hasGlowBitNow) {
                    living.getEntityData().set(sharedFlagsId, (byte) (flags | GLOW_FLAG_BIT));
                }
            } else {
                if (weAreForcing) {
                    boolean hadGlowOriginally = HAD_GLOW_BEFORE_FORCE.get(id);

                    if (!hadGlowOriginally && hasGlowBitNow) {
                        living.getEntityData().set(sharedFlagsId, (byte) (flags & ~GLOW_FLAG_BIT));
                    }

                    FORCED_GLOW.remove(id);
                    HAD_GLOW_BEFORE_FORCE.remove(id);
                }
            }
        }

        pruneStale(level);
    }

    private static void clearAllForced() {
        FORCED_GLOW.clear();
        HAD_GLOW_BEFORE_FORCE.clear();
    }

    private static void pruneStale(ClientLevel level) {
        FORCED_GLOW.keySet().removeIf(id -> level.getEntity(id) == null);
        HAD_GLOW_BEFORE_FORCE.keySet().removeIf(id -> level.getEntity(id) == null);
    }
}
