package me.jellysquid.mods.sodium.client.util.math;

import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionVisibility;

public interface FrustumExtended {
    // Allows Iris to reject som
    default boolean preAabbTest(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return true;
    }

    boolean fastAabbTest(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);

    RenderRegionVisibility aabbTest(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);
}
