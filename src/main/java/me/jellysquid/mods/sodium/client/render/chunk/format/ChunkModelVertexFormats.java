package me.jellysquid.mods.sodium.client.render.chunk.format;

import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.format.sfp.ModelVertexType;

public class ChunkModelVertexFormats {
    public static final ModelVertexType DEFAULT = new ModelVertexType();

    public static ChunkVertexType getDefaultChunkVertexType() {
        return DEFAULT;
    }
}
