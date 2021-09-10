package me.jellysquid.mods.sodium.render.chunk.renderer;

import com.google.common.collect.Lists;
import me.jellysquid.mods.sodium.SodiumClient;
import me.jellysquid.mods.thingl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.thingl.buffer.GlBufferUsage;
import me.jellysquid.mods.thingl.buffer.GlMutableBuffer;
import me.jellysquid.mods.thingl.device.CommandList;
import me.jellysquid.mods.thingl.device.DrawCommandList;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.tessellation.GlIndexType;
import me.jellysquid.mods.thingl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.thingl.tessellation.GlTessellation;
import me.jellysquid.mods.thingl.tessellation.TessellationBinding;
import me.jellysquid.mods.thingl.util.ElementRange;
import me.jellysquid.mods.sodium.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.render.chunk.*;
import me.jellysquid.mods.sodium.render.chunk.context.ChunkCameraContext;
import me.jellysquid.mods.sodium.render.chunk.ChunkRenderList;
import me.jellysquid.mods.sodium.render.chunk.context.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderInterface;
import net.coderbot.iris.shadows.ShadowRenderingState;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class RegionChunkRenderer extends ShaderChunkRenderer {
    private final MultiDrawBatch[] batches;
    private final GlVertexAttributeBinding[] vertexAttributeBindings;

    private final GlMutableBuffer chunkInfoBuffer;
    private final boolean isBlockFaceCullingEnabled = SodiumClient.options().advanced.useBlockFaceCulling;

    public RegionChunkRenderer(RenderDevice device, ChunkVertexType vertexType, float detailDistance) {
        super(device, vertexType, detailDistance);

        this.vertexAttributeBindings = new GlVertexAttributeBinding[] {
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.POSITION_ID)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_COLOR,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.COLOR)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.BLOCK_TEXTURE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.LIGHT_TEXTURE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_FLAGS,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.BLOCK_FLAGS)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.BLOCK_ID,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.BLOCK_ID)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.MID_TEX_COORD,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.MID_TEX_COORD)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.TANGENT,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.TANGENT)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.NORMAL,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.NORMAL),true)
        };

        try (CommandList commandList = device.createCommandList()) {
            this.chunkInfoBuffer = commandList.createMutableBuffer();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                commandList.uploadData(this.chunkInfoBuffer, createChunkInfoBuffer(stack), GlBufferUsage.STATIC_DRAW);
            }
        }

        this.batches = new MultiDrawBatch[GlIndexType.VALUES.length];

        for (int i = 0; i < this.batches.length; i++) {
            this.batches[i] = MultiDrawBatch.create(ModelQuadFacing.COUNT * RenderRegion.REGION_SIZE);
        }
    }

    private static ByteBuffer createChunkInfoBuffer(MemoryStack stack) {
        int stride = 4 * 4;
        ByteBuffer data = stack.malloc(RenderRegion.REGION_SIZE * stride);

        for (int x = 0; x < RenderRegion.REGION_WIDTH; x++) {
            for (int y = 0; y < RenderRegion.REGION_HEIGHT; y++) {
                for (int z = 0; z < RenderRegion.REGION_LENGTH; z++) {
                    int i = RenderRegion.getChunkIndex(x, y, z) * stride;

                    data.putFloat(i + 0, x * 16.0f);
                    data.putFloat(i + 4, y * 16.0f);
                    data.putFloat(i + 8, z * 16.0f);
                }
            }
        }

        return data;
    }

    @Override
    public void render(ChunkRenderMatrices matrices, CommandList commandList,
                       ChunkRenderList list, BlockRenderPass pass,
                       ChunkCameraContext camera) {
        super.begin(pass);

        ChunkShaderInterface shader = this.activeProgram.getInterface();
        shader.setProjectionMatrix(matrices.projection());
        shader.setDrawUniforms(this.chunkInfoBuffer);

        for (Map.Entry<RenderRegion, List<RenderSection>> entry : sortedRegions(list, pass.isTranslucent())) {
            RenderRegion region = entry.getKey();
            List<RenderSection> regionSections = entry.getValue();

            if (!buildDrawBatches(regionSections, pass, camera)) {
                continue;
            }

            this.setModelMatrixUniforms(shader, matrices, region, camera);

            GlTessellation tessellation = this.createTessellationForRegion(commandList, region.getArenas(), pass);
            executeDrawBatches(commandList, tessellation);
        }
        
        super.end();
    }

    private boolean buildDrawBatches(List<RenderSection> sections, BlockRenderPass pass, ChunkCameraContext camera) {
        for (MultiDrawBatch batch : this.batches) {
            batch.begin();
        }

        for (RenderSection render : sortedChunks(sections, pass.isTranslucent())) {
            ChunkGraphicsState state = render.getGraphicsState(pass);

            if (state == null) {
                continue;
            }

            ChunkRenderBounds bounds = render.getBounds();

            long indexOffset = state.getIndexSegment()
                    .getOffset();

            int baseVertex = state.getVertexSegment()
                    .getOffset() / this.vertexFormat.getStride();

            this.addDrawCall(state.getModelPart(ModelQuadFacing.UNASSIGNED), indexOffset, baseVertex);

            if (this.isBlockFaceCullingEnabled && !ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
                if (camera.posY > bounds.y1) {
                    this.addDrawCall(state.getModelPart(ModelQuadFacing.UP), indexOffset, baseVertex);
                }

                if (camera.posY < bounds.y2) {
                    this.addDrawCall(state.getModelPart(ModelQuadFacing.DOWN), indexOffset, baseVertex);
                }

                if (camera.posX > bounds.x1) {
                    this.addDrawCall(state.getModelPart(ModelQuadFacing.EAST), indexOffset, baseVertex);
                }

                if (camera.posX < bounds.x2) {
                    this.addDrawCall(state.getModelPart(ModelQuadFacing.WEST), indexOffset, baseVertex);
                }

                if (camera.posZ > bounds.z1) {
                    this.addDrawCall(state.getModelPart(ModelQuadFacing.SOUTH), indexOffset, baseVertex);
                }

                if (camera.posZ < bounds.z2) {
                    this.addDrawCall(state.getModelPart(ModelQuadFacing.NORTH), indexOffset, baseVertex);
                }
            } else {
                for (ModelQuadFacing facing : ModelQuadFacing.DIRECTIONS) {
                    this.addDrawCall(state.getModelPart(facing), indexOffset, baseVertex);
                }
            }
        }

        boolean nonEmpty = false;

        for (MultiDrawBatch batch : this.batches) {
            batch.end();

            nonEmpty |= !batch.isEmpty();
        }

        return nonEmpty;
    }

    private GlTessellation createTessellationForRegion(CommandList commandList, RenderRegion.RenderRegionArenas arenas, BlockRenderPass pass) {
        GlTessellation tessellation = arenas.getTessellation(pass);

        if (tessellation == null) {
            arenas.setTessellation(pass, tessellation = this.createRegionTessellation(commandList, arenas));
        }

        return tessellation;
    }

    private void executeDrawBatches(CommandList commandList, GlTessellation tessellation) {
        for (int i = 0; i < this.batches.length; i++) {
            MultiDrawBatch batch = this.batches[i];

            try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
                drawCommandList.multiDrawElementsBaseVertex(batch.getPointerBuffer(), batch.getCountBuffer(), batch.getBaseVertexBuffer(), GlIndexType.VALUES[i]);
            }
        }
    }

    private final Matrix4f cachedModelViewMatrix = new Matrix4f();

    private void setModelMatrixUniforms(ChunkShaderInterface shader, ChunkRenderMatrices matrices, RenderRegion region, ChunkCameraContext camera) {
        float x = getCameraTranslation(region.getOriginX(), camera.blockX, camera.deltaX);
        float y = getCameraTranslation(region.getOriginY(), camera.blockY, camera.deltaY);
        float z = getCameraTranslation(region.getOriginZ(), camera.blockZ, camera.deltaZ);

        Matrix4f matrix = this.cachedModelViewMatrix;
        matrix.set(matrices.modelView());


        Matrix4f modelViewProjectionMatrix = new Matrix4f(matrices.projection());
        modelViewProjectionMatrix.mul(matrix);

        Matrix4f normal = new Matrix4f(matrices.modelView());
        matrix.translate(x, y, z);
        modelViewProjectionMatrix.translate(x, y, z);
        normal.translate(x, y, z);
        normal.invert();
        normal.transpose();

        shader.setModelViewMatrix(matrix);
        shader.setModelViewProjectionMatrix(modelViewProjectionMatrix);
        shader.setNormalMatrix(normal);
    }

    private void addDrawCall(ElementRange part, long baseIndexPointer, int baseVertexIndex) {
        if (part != null) {
            MultiDrawBatch batch = this.batches[part.indexType().ordinal()];
            batch.add(baseIndexPointer + part.elementPointer(), part.elementCount(), baseVertexIndex + part.baseVertex());
        }
    }

    private GlTessellation createRegionTessellation(CommandList commandList, RenderRegion.RenderRegionArenas arenas) {
        return commandList.createTessellation(GlPrimitiveType.TRIANGLES, new TessellationBinding[] {
                TessellationBinding.forVertexBuffer(arenas.vertexBuffers.getBufferObject(), this.vertexAttributeBindings),
                TessellationBinding.forElementBuffer(arenas.indexBuffers.getBufferObject())
        });
    }

    @Override
    public void delete() {
        super.delete();

        for (MultiDrawBatch batch : this.batches) {
            batch.delete();
        }

        RenderDevice.INSTANCE.createCommandList()
                .deleteBuffer(this.chunkInfoBuffer);
    }

    private static Iterable<Map.Entry<RenderRegion, List<RenderSection>>> sortedRegions(ChunkRenderList list, boolean translucent) {
        return list.sorted(translucent);
    }

    private static Iterable<RenderSection> sortedChunks(List<RenderSection> chunks, boolean translucent) {
        return translucent ? Lists.reverse(chunks) : chunks;
    }

    private static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }
}
