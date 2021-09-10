package me.jellysquid.mods.sodium.render.chunk.format;

import me.jellysquid.mods.thingl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.thingl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.model.vertex.type.BlittableVertexType;
import me.jellysquid.mods.sodium.model.vertex.type.ChunkVertexType;
import net.minecraft.client.render.VertexConsumer;

/**
 * Simple vertex format which uses single-precision floating point numbers to represent position and texture
 * coordinates.
 */
public class XHFPModelVertexType implements ChunkVertexType {
    public static final XHFPModelVertexType INSTANCE = new XHFPModelVertexType();

    public static final int STRIDE = 44;

    public static final GlVertexFormat<ChunkMeshAttribute> VERTEX_FORMAT = GlVertexFormat.builder(ChunkMeshAttribute.class, 44)
            .addElement(ChunkMeshAttribute.POSITION_ID, 0, GlVertexAttributeFormat.UNSIGNED_SHORT, 4, false)
            .addElement(ChunkMeshAttribute.COLOR, 8, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true)
            .addElement(ChunkMeshAttribute.BLOCK_TEXTURE, 12, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false)
            .addElement(ChunkMeshAttribute.LIGHT_TEXTURE, 16, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, true)
            .addElement(ChunkMeshAttribute.BLOCK_FLAGS, 20, GlVertexAttributeFormat.INT, 1, false)
            .addElement(ChunkMeshAttribute.MID_TEX_COORD, 24, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, true)
            .addElement(ChunkMeshAttribute.TANGENT, 28, GlVertexAttributeFormat.BYTE, 4, true)
            .addElement(ChunkMeshAttribute.NORMAL, 32, GlVertexAttributeFormat.BYTE, 3, true)
            .addElement(ChunkMeshAttribute.BLOCK_ID, 36, GlVertexAttributeFormat.SHORT, 4, false)
            .build();


    @Override
    public ModelVertexSink createFallbackWriter(VertexConsumer consumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
        return createBufferWriter(buffer, new MaterialIdHolder());
    }

    @Override
    public ModelVertexSink createBufferWriter(VertexBufferView buffer, MaterialIdHolder idHolder) {
        return new XHFPModelVertexBufferWriterNio(buffer, idHolder);
    }

    @Override
    public BlittableVertexType<ModelVertexSink> asBlittable() {
        return this;
    }

    @Override
    public GlVertexFormat<ChunkMeshAttribute> getCustomVertexFormat() {
        return VERTEX_FORMAT;
    }

}
