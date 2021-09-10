package me.jellysquid.mods.sodium.render.chunk.format;

import net.minecraft.client.model.Model;

import java.nio.ByteBuffer;

public class QuadView {
    ByteBuffer buffer;
    int writeOffset;
    private static final int STRIDE = XHFPModelVertexType.STRIDE;

    public float x(int index) {
        return ModelVertexCompression.decodePosition(buffer.getShort(writeOffset + 0 - STRIDE * (3 - index)));
    }

    public float y(int index) {
        return ModelVertexCompression.decodePosition(buffer.getShort(writeOffset + 2 - STRIDE * (3 - index)));
    }

    public float z(int index) {
        return ModelVertexCompression.decodePosition(buffer.getShort(writeOffset + 4 - STRIDE * (3 - index)));
    }
}