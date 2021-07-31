package me.jellysquid.mods.sodium.client.render.chunk.format.xhfp;

import sun.misc.Unsafe;

import java.nio.ByteBuffer;

public class QuadView {
	ByteBuffer buffer;
	int writeOffset;
	long writePointer;
	Unsafe UNSAFE;
	private static final int STRIDE = 48;

	float x(int index, boolean unsafe) {
        if (unsafe) {
            return normalizeVertexPositionShortAsFloat(UNSAFE.getShort(writePointer - STRIDE * (3 - index)));
        }
		return normalizeVertexPositionShortAsFloat(buffer.getShort(writeOffset - STRIDE * (3 - index)));
	}

	float y(int index, boolean unsafe) {
        if (unsafe) {
            return normalizeVertexPositionShortAsFloat(UNSAFE.getShort(writePointer + 2 - STRIDE * (3 - index)));
        }
		return normalizeVertexPositionShortAsFloat(buffer.getShort(writeOffset + 2 - STRIDE * (3 - index)));
	}

	float z(int index, boolean unsafe) {
	    if (unsafe) {
	        return normalizeVertexPositionShortAsFloat(UNSAFE.getShort(writePointer + 4 - STRIDE * (3 - index)));
        }
		return normalizeVertexPositionShortAsFloat(buffer.getShort(writeOffset + 4 - STRIDE * (3 - index)));
	}

	// TODO: Verify that this works with the new changes to the CVF
	private static float normalizeVertexPositionShortAsFloat(short value) {
		return (value & 0xFFFF) * (1.0f / 65535.0f);
	}
}
