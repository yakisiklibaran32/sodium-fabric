package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;

import net.coderbot.iris.shadows.ShadowRenderingStatus;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.EnumMap;

public abstract class ChunkRenderShaderBackend<T extends ChunkGraphicsState, P extends ChunkProgram>
        implements ChunkRenderBackend<T> {
    private final EnumMap<ChunkFogMode, EnumMap<BlockRenderPass, P>> programs = new EnumMap<>(ChunkFogMode.class);
    private final EnumMap<ChunkFogMode, EnumMap<BlockRenderPass, P>> shadowPrograms = new EnumMap<>(ChunkFogMode.class);

    protected final ChunkVertexType vertexType;
    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected P activeProgram;

    public ChunkRenderShaderBackend(ChunkVertexType vertexType) {
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getCustomVertexFormat();
    }

    @Override
    public final void createShaders() {
        for (ChunkFogMode fogMode : ChunkFogMode.values()) {
            this.programs.put(fogMode, createShadersForFogMode(fogMode, false));
            this.shadowPrograms.put(fogMode, createShadersForFogMode(fogMode, true));
        }
    }

    private EnumMap<BlockRenderPass, P> createShadersForFogMode(ChunkFogMode mode, boolean shadow) {
        EnumMap<BlockRenderPass, P> shaders = new EnumMap<>(BlockRenderPass.class);

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            shaders.put(pass, this.createShader(mode, pass, this.vertexFormat, shadow));
        }

        return shaders;
    }

    private P createShader(ChunkFogMode fogMode, BlockRenderPass pass, GlVertexFormat<ChunkMeshAttribute> format, boolean shadow) {
        GlShader vertShader = this.createVertexShader(fogMode, pass, shadow);
        GlShader fragShader = this.createFragmentShader(fogMode, pass, shadow);

        try {
            return GlProgram.builder(new Identifier("sodium", "chunk_shader_for_" + pass.toString().toLowerCase() + (shadow ? "_gbuffer" : "_shadow")))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindAttribute("a_Pos", format.getAttribute(ChunkMeshAttribute.POSITION))
                    .bindAttribute("a_Color", format.getAttribute(ChunkMeshAttribute.COLOR))
                    .bindAttribute("a_TexCoord", format.getAttribute(ChunkMeshAttribute.TEXTURE))
                    .bindAttribute("a_LightCoord", format.getAttribute(ChunkMeshAttribute.LIGHT))
                    .bindAttribute("mc_Entity", format.getAttribute(ChunkMeshAttribute.BLOCK_ID))
                    .bindAttribute("mc_midTexCoord", format.getAttribute(ChunkMeshAttribute.MID_TEX_COORD))
                    .bindAttribute("at_tangent", format.getAttribute(ChunkMeshAttribute.TANGENT))
                    .bindAttribute("a_Normal", format.getAttribute(ChunkMeshAttribute.NORMAL))
                    // TODO: This is hardcoded, we can't assume that index 8 will be okay
                    .bindAttribute("d_ModelOffset", 8)
                    .build((program, name) -> this.createShaderProgram(program, name, fogMode, pass));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    protected abstract GlShader createFragmentShader(ChunkFogMode fogMode, BlockRenderPass pass, boolean shadow);

    protected abstract GlShader createVertexShader(ChunkFogMode fogMode, BlockRenderPass pass, boolean shadow);

    protected abstract P createShaderProgram(Identifier name, int handle, ChunkFogMode fogMode, BlockRenderPass pass);

    @Override
    public void begin(MatrixStack matrixStack, BlockRenderPass pass) {
        if (ShadowRenderingStatus.areShadowsCurrentlyBeingRendered()) {
            this.activeProgram = this.shadowPrograms.get(ChunkFogMode.getActiveMode()).get(pass);
        } else {
            this.activeProgram = this.programs.get(ChunkFogMode.getActiveMode()).get(pass);
        }

        this.activeProgram.bind();
        this.activeProgram.setup(matrixStack, this.vertexType.getModelScale(), this.vertexType.getTextureScale());
    }

    @Override
    public void end(MatrixStack matrixStack) {
        this.activeProgram.unbind();
        this.activeProgram = null;
    }

    @Override
    public void delete() {
        for (EnumMap<BlockRenderPass, P> shaders: this.programs.values()) {
            for (P shader : shaders.values()) {
                shader.delete();
            }
        }

        for (EnumMap<BlockRenderPass, P> shaders: this.shadowPrograms.values()) {
            for (P shader : shaders.values()) {
                shader.delete();
            }
        }
    }

    @Override
    public ChunkVertexType getVertexType() {
        return this.vertexType;
    }
}
