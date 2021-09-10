package me.jellysquid.mods.sodium.render.chunk.compile;

import me.jellysquid.mods.sodium.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.render.chunk.passes.BlockRenderPass;

import java.util.Collections;
import java.util.Map;

/**
 * The result of a chunk rebuild task which contains any and all data that needs to be processed or uploaded on
 * the main thread. If a task is cancelled after finishing its work and not before the result is processed, the result
 * will instead be discarded.
 */
public class ChunkBuildResult {
    public final RenderSection render;
    public final ChunkRenderData data;
    public final Map<BlockRenderPass, ChunkMeshData> meshes;
    public final int buildTime;
    public final int detailLevel;

    public ChunkBuildResult(RenderSection render, ChunkRenderData data, Map<BlockRenderPass, ChunkMeshData> meshes, int buildTime, int detailLevel) {
        this.render = render;
        this.data = data;
        this.meshes = Collections.unmodifiableMap(meshes);
        this.buildTime = buildTime;
        this.detailLevel = detailLevel;
    }

    public void delete() {
        for (ChunkMeshData data : this.meshes.values()) {
            if (data != null) {
                data.getVertexData()
                        .delete();
            }
        }
    }

    public Iterable<Map.Entry<BlockRenderPass, ChunkMeshData>> getMeshes() {
        return this.meshes.entrySet();
    }
}
