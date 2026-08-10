package net.tinnyman.nohurtflash;

import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tinnyman.nohurtflash.Util.VisibilityUtil;
import net.tinnyman.nohurtflash.mixin.EntityAccessor;

/** Applies a temporary "glowing" flag to hurt entities on the client.
 *
 * Implementation details:
 * Vanilla stores the glowing state in the entity shared flags byte (bit 6 / 0x40).
 * We toggle that bit through EntityData to trigger the existing outline pipeline. */
@Mod.EventBusSubscriber(modid = NoHurtFlash.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlowEffectHandler {
    /** Mask for the "glowing" bit inside the entity shared flags byte.
     * Vanilla uses bit 6 / 0x40 for glowing. */
    private static final int GLOWING_FLAG_MASK = 0x40; // bit 6

    /** Tracks which entities currently have their glow forced by this mod.
     * Key: entity ID
     * Value: always true */
    private static final Int2BooleanMap FORCED_GLOW_ENTITY_IDS = new Int2BooleanOpenHashMap();

    /** Remembers whether an entity already had the glowing flag set before we started forcing it.
     * This allows us to safely restore the original state and avoid disabling glow effects from other sources. */
    private static final Int2BooleanMap ENTITY_HAD_GLOW_BEFORE_FORCING = new Int2BooleanOpenHashMap();

    /** End-of-tick update:
     *  - Decide which entities should glow this tick
     *  - Apply or remove forced glowing flag as needed
     *  - Cleanup tracking maps for despawned entities */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!Config.GLOW_ENABLED.get()) {
            clearForcedGlowTracking();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;

        if (level == null || mc.isPaused()) return;

        // Advances any per-tick color logic used by the glow pipeline.
        GlowColorManager.tickClient();

        // Accessor for the entity's "shared flags" tracked data entry.
        EntityDataAccessor<Byte> sharedFlagsAccessor = EntityAccessor.getSharedFlagsId();

        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living)) continue;

            int entityId = living.getId();

            // We only force glow during hurt time, and only if the player can actually see the entity.
            boolean shouldForceGlowThisTick = living.hurtTime > 0 && VisibilityUtil.canPlayerSeeEntityNow(living);
            byte flags = living.getEntityData().get(sharedFlagsAccessor);
            boolean isGlowingFlagSet = (flags & GLOWING_FLAG_MASK) != 0;
            boolean isGlowForcedByThisMod = FORCED_GLOW_ENTITY_IDS.get(entityId);

            if (shouldForceGlowThisTick) {
                // Start forcing glow for this entity (store original state once.)
                if (!isGlowForcedByThisMod) {
                    FORCED_GLOW_ENTITY_IDS.put(entityId, true);
                    ENTITY_HAD_GLOW_BEFORE_FORCING.put(entityId, isGlowingFlagSet);
                }

                // Ensure the glow flag is enabled while we want it.
                if (!isGlowingFlagSet) {
                    living.getEntityData().set(sharedFlagsAccessor, (byte) (flags | GLOWING_FLAG_MASK));
                }
            } else {
                // Stop forcing glow for this entity and restore original glow state if needed.
                if (isGlowForcedByThisMod) {
                    boolean hadGlowOriginally = ENTITY_HAD_GLOW_BEFORE_FORCING.get(entityId);

                    // Only remove the glow flag if we were the one that introduced it.
                    if (!hadGlowOriginally && isGlowingFlagSet) {
                        living.getEntityData().set(sharedFlagsAccessor, (byte) (flags & ~GLOWING_FLAG_MASK));
                    }

                    FORCED_GLOW_ENTITY_IDS.remove(entityId);
                    ENTITY_HAD_GLOW_BEFORE_FORCING.remove(entityId);
                }
            }
        }

        removeEntriesForMissingEntities(level);
    }

    /** Clears internal tracking state.
     * This does not actively scan the world and revert flags; it only forgets what we were forcing. */
    private static void clearForcedGlowTracking() {
        FORCED_GLOW_ENTITY_IDS.clear();
        ENTITY_HAD_GLOW_BEFORE_FORCING.clear();
    }

    /** Removes entries for entities that no longer exist in the level (despawn, unloaded, etc.) */
    private static void removeEntriesForMissingEntities(ClientLevel level) {
        FORCED_GLOW_ENTITY_IDS.keySet().removeIf(id -> level.getEntity(id) == null);
        ENTITY_HAD_GLOW_BEFORE_FORCING.keySet().removeIf(id -> level.getEntity(id) == null);
    }
}
