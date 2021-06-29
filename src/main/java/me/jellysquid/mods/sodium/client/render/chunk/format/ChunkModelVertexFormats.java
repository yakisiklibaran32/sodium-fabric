package me.jellysquid.mods.sodium.client.render.chunk.format;

import me.jellysquid.mods.sodium.client.render.chunk.format.sfp.ModelVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.format.xhfp.XHFPModelVertexType;

public class ChunkModelVertexFormats {
    // Iris: deprecated, use the extended vertex format.
    @Deprecated
    public static final ModelVertexType DEFAULT = null;
    public static final XHFPModelVertexType EXTENDED = new XHFPModelVertexType();
}
