package net.tinnyman.nohurtflash;

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

public final class VisibilityUtil {
    private VisibilityUtil() {}

    // Cache: entityId -> cached result for current gameTime
    private static final Map<Integer, CacheEntry> CACHE = new HashMap<>();

    public static double MAX_RANGE_BLOCKS = 32.0;
    public static double VIEW_CONE_DEGREES = 110.0;
    public static final double BBOX_INSET = 0.15;

    private static final class CacheEntry {
        long gameTime;
        boolean visible;
        CacheEntry(long gameTime, boolean visible) {
            this.gameTime = gameTime;
            this.visible = visible;
        }
    }

    public static boolean canPlayerSeeEntityNow(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        long time = mc.level.getGameTime();
        int id = entity.getId();

        CacheEntry cached = CACHE.get(id);
        if (cached != null && cached.gameTime == time) {
            return cached.visible;
        }

        boolean visible = computeVisibility(mc, entity);
        CACHE.put(entity.getId(), new CacheEntry(time, visible));
        return visible;
    }

    private static boolean computeVisibility(Minecraft mc, Entity entity) {
        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null) return false;

        Vec3 from = camera.getPosition();

        AABB bb = entity.getBoundingBox();
        Vec3 center = bb.getCenter();

        /* ---------------- Range gate ---------------- */

        if (from.distanceToSqr(center) > (MAX_RANGE_BLOCKS * MAX_RANGE_BLOCKS)) {
            return false;
        }

        /* ---------------- View cone gate ---------------- */

        if (VIEW_CONE_DEGREES < 179.0) {
            Vec3 look = new Vec3(camera.getLookVector().x(), camera.getLookVector().y(), camera.getLookVector().z()).normalize();
            Vec3 toDir = center.subtract(from).normalize();

            double cos = look.dot(toDir);
            double cosThreshold = Math.cos(Math.toRadians(VIEW_CONE_DEGREES * 0.5));

            if (cos < cosThreshold) return false;
        }

        /* ---------------- Multi-ray LOS ---------------- */

        Vec3 min = new Vec3(
                bb.minX + BBOX_INSET,
                bb.minY + BBOX_INSET,
                bb.minZ + BBOX_INSET
        );
        Vec3 max = new Vec3(
                bb.maxX - BBOX_INSET,
                bb.maxY - BBOX_INSET,
                bb.maxZ - BBOX_INSET
        );

        Vec3[] targets = new Vec3[] {
                center,
                new Vec3(min.x, (min.y + max.y) * 0.5, min.z),
                new Vec3(max.x, (min.y + max.y) * 0.5, min.z),
                new Vec3(min.x, (min.y + max.y) * 0.5, max.z),
                new Vec3(max.x, (min.y + max.y) * 0.5, max.z),
                new Vec3(center.x, Mth.lerp(0.8, bb.minY, bb.maxY), center.z)
        };

        Entity clipEntity = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        if (clipEntity == null) return false;

        for (Vec3 target : targets) {
            if (hasClearRay(mc, clipEntity, from, target)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasClearRay(Minecraft mc, Entity clipEntity, Vec3 from, Vec3 to) {
        ClipContext ctx = new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                clipEntity
        );

        HitResult hit = mc.level.clip(ctx);
        return hit.getType() == HitResult.Type.MISS;
    }
}
