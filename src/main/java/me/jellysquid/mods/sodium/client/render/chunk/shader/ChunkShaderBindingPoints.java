package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderBindingPoint;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;

import java.util.ArrayList;
import java.util.List;

public class ChunkShaderBindingPoints {
    private static final List<ShaderBindingPoint> activeBindingPointsDefault = new ArrayList<>();

    private static final ShaderBindingPoint ATTRIBUTE_POSITION_ID = new ShaderBindingPoint("a_Pos", ChunkMeshAttribute.POSITION_ID, 1);
    private static final ShaderBindingPoint ATTRIBUTE_COLOR = new ShaderBindingPoint("a_Color",ChunkMeshAttribute.COLOR, 2);

    private static final ShaderBindingPoint ATTRIBUTE_BLOCK_TEXTURE = new ShaderBindingPoint("a_TexCoord", ChunkMeshAttribute.BLOCK_TEXTURE, 3);
    private static final ShaderBindingPoint ATTRIBUTE_LIGHT_TEXTURE = new ShaderBindingPoint("a_LightCoord", ChunkMeshAttribute.LIGHT_TEXTURE,4);

    public static final ShaderBindingPoint FRAG_COLOR = new ShaderBindingPoint("fragColor", null,0);

    static {
        activeBindingPointsDefault.add(ATTRIBUTE_POSITION_ID);
        activeBindingPointsDefault.add(ATTRIBUTE_COLOR);
        activeBindingPointsDefault.add(ATTRIBUTE_BLOCK_TEXTURE);
        activeBindingPointsDefault.add(ATTRIBUTE_LIGHT_TEXTURE);
    }

    public static List<ShaderBindingPoint> getActiveBindingPoints() {
        return activeBindingPointsDefault;
    }
}
