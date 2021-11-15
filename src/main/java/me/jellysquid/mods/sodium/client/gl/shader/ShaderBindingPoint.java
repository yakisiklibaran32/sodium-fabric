package me.jellysquid.mods.sodium.client.gl.shader;

import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;

public record ShaderBindingPoint(String name, ChunkMeshAttribute attribute, int genericAttributeIndex) {
}
