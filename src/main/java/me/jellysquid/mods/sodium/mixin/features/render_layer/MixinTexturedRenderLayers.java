package me.jellysquid.mods.sodium.mixin.features.render_layer;

import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.SignType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(TexturedRenderLayers.class)
public class MixinTexturedRenderLayers {
    @Shadow
    @Final
    public static Map<SignType, SpriteIdentifier> WOOD_TYPE_TEXTURES;

}
