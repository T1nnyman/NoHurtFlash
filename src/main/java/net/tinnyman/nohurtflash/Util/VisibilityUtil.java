package net.tinnyman.nohurtflash.Util;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for determining whether the player camera has a clear, direct line of sight to an entity "right now".
 *
 * This is used to gate visual effects (e.g. hurt glow) so that entities are only highlighted when they are actually visible
 * to the player and not through walls or behind the camera.
 *
 * The result is cached per-entity per-tick to avoid repeated ray-casts during the same frame.
 */
public final class VisibilityUtil {
    private VisibilityUtil() {}

    /**
     * Per-tick visibility cache.
     * Key: entity ID
     * Value: visibility result for the current level game time
     */
    private static final Map<Integer, CacheEntry> VISIBILITY_CACHE = new HashMap<>();

    /** Maximum distance (in blocks) at which visibility is considered. */
    public static double MAX_RANGE_BLOCKS = 32.0;

    /**
     * Horizontal view cone in degrees.
     * Entities outside this cone are ignored before ray-casting.
     */
    public static double VIEW_CONE_DEGREES = 110.0;

    /** Amount to inset the entity bounding box when selecting ray target points, reducing edge-clipping false negatives. */
    public static final double BBOX_INSET = 0.15;

    /**
     * Cached visibility state for a single entity for one game tick.
     */
    private static final class CacheEntry {
        long levelGameTime;
        boolean visible;

        CacheEntry(long levelGameTime, boolean visible) {
            this.levelGameTime = levelGameTime;
            this.visible = visible;
        }
    }

    /**
     * Returns whether the player camera can currently see the given entity.
     *
     * Results are cached for the current tick to prevent repeated ray-casts
     * when called multiple times during rendering or event processing.
     */
    public static boolean canPlayerSeeEntityNow(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false; // Not in world

        long gameTime = mc.level.getGameTime();
        int id = entity.getId();

        CacheEntry cached = VISIBILITY_CACHE.get(id);
        if (cached != null && cached.levelGameTime == gameTime) {
            return cached.visible;
        }

        boolean visible = computeVisibility(mc, entity);
        VISIBILITY_CACHE.put(entity.getId(), new CacheEntry(gameTime, visible));
        return visible;
    }

    /**
     * Performs the actual visibility calculation without caching.
     *
     * Visibility is determined using:
     *  - distance gating
     *  - camera view cone gating
     *  - multipoint line-of-sight ray-casting
     */
    private static boolean computeVisibility(Minecraft mc, Entity entity) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        AABB boundingBox = entity.getBoundingBox();
        Vec3 entityCenter = boundingBox.getCenter();

        /* ---------------- Distance Check ---------------- */
        if (cameraPos.distanceToSqr(entityCenter) > (MAX_RANGE_BLOCKS * MAX_RANGE_BLOCKS)) { return false; }

        /* ---------------- View cone Check ---------------- */
        if (VIEW_CONE_DEGREES < 179.0) {
            Vec3 lookDir = new Vec3(camera.getLookVector().x(), camera.getLookVector().y(), camera.getLookVector().z()).normalize();
            Vec3 toEntity = entityCenter.subtract(cameraPos).normalize();
            double cos = lookDir.dot(toEntity);
            double threshold = Math.cos(Math.toRadians(VIEW_CONE_DEGREES * 0.5));

            if (cos < threshold) return false;
        }

        /* ---------------- Line-of-sight ray-casts ---------------- */

        Vec3 min = new Vec3(
                boundingBox.minX + BBOX_INSET,
                boundingBox.minY + BBOX_INSET,
                boundingBox.minZ + BBOX_INSET
        );
        Vec3 max = new Vec3(
                boundingBox.maxX - BBOX_INSET,
                boundingBox.maxY - BBOX_INSET,
                boundingBox.maxZ - BBOX_INSET
        );

        double midY = (min.y + max.y) * 0.5;
        Vec3[] targets = new Vec3[] {
                entityCenter,
                new Vec3(min.x, midY, min.z),
                new Vec3(max.x, midY, min.z),
                new Vec3(min.x, midY, max.z),
                new Vec3(max.x, midY, max.z),
                new Vec3(entityCenter.x, Mth.lerp(0.8, boundingBox.minY, boundingBox.maxY), entityCenter.z)
        };

        Entity rayContextEntity = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        if (rayContextEntity == null) return false;

        for (Vec3 target : targets) {
            if (isLineOfSightClear(mc, rayContextEntity, cameraPos, target)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Performs a block-collider ray-cast between two points.
     *
     * Returns true if no solid block obstructs the ray.
     */
    private static boolean isLineOfSightClear(Minecraft mc, Entity rayContextEntity, Vec3 from, Vec3 to) {
        ClipContext ctx = new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                rayContextEntity
        );

        if (mc.level == null) return false;

        HitResult hit = mc.level.clip(ctx);
        return hit.getType() == HitResult.Type.MISS;
    }
}
