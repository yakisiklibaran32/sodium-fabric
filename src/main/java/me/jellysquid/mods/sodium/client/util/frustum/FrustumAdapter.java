package me.jellysquid.mods.sodium.client.util.frustum;

public interface FrustumAdapter {
    Frustum sodium$createFrustum();

    static Frustum adapt(net.minecraft.client.renderer.culling.Frustum frustum) {
        return ((FrustumAdapter) frustum).sodium$createFrustum();
    }
}
