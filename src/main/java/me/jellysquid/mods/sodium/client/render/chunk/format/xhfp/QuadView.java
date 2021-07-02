package me.jellysquid.mods.sodium.client.render.chunk.format.xhfp;

import sun.misc.Unsafe;
import java.nio.ByteBuffer;

public class QuadView {
	ByteBuffer buffer;
	int writeOffset;
    Unsafe unsafe;
	long writePointer;
	private static final int STRIDE = XHFPModelVertexType.STRIDE;

    float x(int index) {
        return XHFPModelVertexType.decodePosition(buffer.getShort(writeOffset + 4 - STRIDE * (3 - index)));
    }

    float y(int index) {
        return XHFPModelVertexType.decodePosition(buffer.getShort(writeOffset + 6 - STRIDE * (3 - index)));
    }

    float z(int index) {
        return XHFPModelVertexType.decodePosition(buffer.getShort(writeOffset + 8 - STRIDE * (3 - index)));
    }

    float x(long index) {
        return XHFPModelVertexType.decodePosition(unsafe.getShort(writePointer + 4 - STRIDE * (3 - index)));
    }

    float y(long index) {
        return XHFPModelVertexType.decodePosition(unsafe.getShort(writePointer + 6 - STRIDE * (3 - index)));
    }

    float z(long index) {
        return XHFPModelVertexType.decodePosition(unsafe.getShort(writePointer + 8 - STRIDE * (3 - index)));
    }
}
