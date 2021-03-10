package me.jellysquid.mods.sodium.mixin.core.frustum;

import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import net.coderbot.iris.Iris;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Vector4f;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Frustum.class)
public class MixinFrustum implements FrustumExtended {
    private float xF, yF, zF;

    private float nxX, nxY, nxZ, nxW;
    private float pxX, pxY, pxZ, pxW;
    private float nyX, nyY, nyZ, nyW;
    private float pyX, pyY, pyZ, pyW;
    private float nzX, nzY, nzZ, nzW;
    private float pzX, pzY, pzZ, pzW;

    @Shadow
    private boolean isAnyCornerVisible(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return false;
    }

    @Inject(method = "setPosition", at = @At("HEAD"))
    private void prePositionUpdate(double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        this.xF = (float) cameraX;
        this.yF = (float) cameraY;
        this.zF = (float) cameraZ;
    }



    @Override
    public boolean fastAabbTest(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {

            return this.isAnyCornerVisible(minX - this.xF, minY - this.yF, minZ - this.zF,
                    maxX - this.xF, maxY - this.yF, maxZ - this.zF);

    }

    /**
     * @author JellySquid
     * @reason Optimize away object allocations and for-loop
     */

}
