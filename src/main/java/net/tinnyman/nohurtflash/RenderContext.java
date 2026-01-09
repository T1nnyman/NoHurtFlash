package net.tinnyman.nohurtflash;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Holds per-render-thread state and used during entity rendering.
 *
 * This is primarily used to expose the "currently rendering entity" to rendering layers, mixins, or helper utilities that
 * do not receive the entity directly through method parameters.
 *
 * The value is stored in a ThreadLocal because entity rendering can occur on different render threads depending on the
 * pipeline, and this state must not leak across entities or threads.
 *
 * Expected lifecycle:
 *  - Set immediately before an entity begins rendering
 *  - Cleared immediately after rending completes
 */
public final class RenderContext {
    /**
     * The entity currently being rendered on this render thread.
     *
     * This is intentionally nullable:
     *  - null means "no entity is currently rendering"
     *
     *  Callers are responsible for setting and clearing this value within a try/finally block to ensure proper cleanup.
     */
    public static final ThreadLocal<LivingEntity> CURRENT_RENDERING_ENTITY = new ThreadLocal<>();

    private RenderContext() {}
}
