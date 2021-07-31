package me.jellysquid.mods.sodium.client.render.chunk.format.xhfp;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class QuadView {
	ByteBuffer buffer;
	int writeOffset;
	long writePointer;
	private static final int STRIDE = 48;

	float x(int index, boolean unsafe) {
        if (unsafe) {
            return normalizeVertexPositionShortAsFloat(MemoryUtil.memGetShort(writePointer - STRIDE * (3 - index)));
        }
		return normalizeVertexPositionShortAsFloat(buffer.getShort(writeOffset - STRIDE * (3 - index)));
	}

	float y(int index, boolean unsafe) {
        if (unsafe) {
            return normalizeVertexPositionShortAsFloat(MemoryUtil.memGetShort(writePointer + 2 - STRIDE * (3 - index)));
        }
		return normalizeVertexPositionShortAsFloat(buffer.getShort(writeOffset + 2 - STRIDE * (3 - index)));
	}

	float z(int index, boolean unsafe) {
	    if (unsafe) {
	        return normalizeVertexPositionShortAsFloat(MemoryUtil.memGetShort(writePointer + 4 - STRIDE * (3 - index)));
        }
		return normalizeVertexPositionShortAsFloat(buffer.getShort(writeOffset + 4 - STRIDE * (3 - index)));
	}

	// TODO: Verify that this works with the new changes to the CVF
	private static float normalizeVertexPositionShortAsFloat(short value) {
		return (value & 0xFFFF) * (1.0f / 65535.0f);
	}
}
