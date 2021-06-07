package me.jellysquid.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import me.jellysquid.mods.sodium.client.gl.compat.LegacyFogHelper;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;

import net.coderbot.iris.shadows.ShadowRenderingStatus;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.EnumMap;

public abstract class ChunkRenderShaderBackend<T extends ChunkGraphicsState>
        implements ChunkRenderBackend<T> {
    private final EnumMap<ChunkFogMode, EnumMap<BlockRenderPass, ChunkProgram>> programs = new EnumMap<>(ChunkFogMode.class);
    private final EnumMap<ChunkFogMode, EnumMap<BlockRenderPass, ChunkProgram>> shadowPrograms = new EnumMap<>(ChunkFogMode.class);

    protected final ChunkVertexType vertexType;
    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected ChunkProgram activeProgram;

    public ChunkRenderShaderBackend(ChunkVertexType vertexType) {
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getCustomVertexFormat();
    }

    private ChunkProgram createShader(RenderDevice device, ChunkFogMode fogMode, GlVertexFormat<ChunkMeshAttribute> vertexFormat) {
        GlShader vertShader = ShaderLoader.loadShader(device, ShaderType.VERTEX,
                new Identifier("sodium", "chunk_gl20.v.glsl"), fogMode.getDefines());

        GlShader fragShader = ShaderLoader.loadShader(device, ShaderType.FRAGMENT,
                new Identifier("sodium", "chunk_gl20.f.glsl"), fogMode.getDefines());

        try {
            return GlProgram.builder(new Identifier("sodium", "chunk_shader_for_" + pass.toString().toLowerCase() + (shadow ? "_gbuffer" : "_shadow")))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindAttribute("a_Pos", ChunkShaderBindingPoints.POSITION)
                    .bindAttribute("a_Color", ChunkShaderBindingPoints.COLOR)
                    .bindAttribute("a_TexCoord", ChunkShaderBindingPoints.TEX_COORD)
                    .bindAttribute("a_LightCoord", ChunkShaderBindingPoints.LIGHT_COORD)
                    .bindAttribute("mc_Entity", ChunkShaderBindingPoints.BLOCK_ID)
                    .bindAttribute("mc_midTexCoord", ChunkShaderBindingPoints.MID_TEX_COORD)
                    .bindAttribute("at_tangent", ChunkShaderBindingPoints.TANGENT)
                    .bindAttribute("a_Normal", ChunkShaderBindingPoints.NORMAL)
                    .bindAttribute("d_ModelOffset", ChunkShaderBindingPoints.MODEL_OFFSET)
                    .build((program, name) -> new ChunkProgram(device, program, name, fogMode.getFactory()));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    @Override
    public final void createShaders(RenderDevice device) {
        for (ChunkFogMode fogMode : ChunkFogMode.values()) {
            this.programs.put(fogMode, createShadersForFogMode(device, fogMode, false));
            this.shadowPrograms.put(fogMode, createShadersForFogMode(device, fogMode, true));
        }
    }

    private EnumMap<BlockRenderPass, P> createShadersForFogMode(RenderDevice device, ChunkFogMode mode, boolean shadow) {
        EnumMap<BlockRenderPass, P> shaders = new EnumMap<>(BlockRenderPass.class);

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            shaders.put(pass, this.createShader(device, mode, pass, this.vertexFormat, shadow));
        }

        return shaders;
    }

    @Override
    public void begin(MatrixStack matrixStack, BlockRenderPass pass) {
        if (ShadowRenderingStatus.areShadowsCurrentlyBeingRendered()) {
            this.activeProgram = this.shadowPrograms.get(LegacyFogHelper.getFogMode()).get(pass);

            // No back face culling during the shadow pass
            // TODO: Hopefully this won't be necessary in the future...
            RenderSystem.disableCull();
        } else {
            this.activeProgram = this.programs.get(LegacyFogHelper.getFogMode()).get(pass);
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
        for (EnumMap<BlockRenderPass, ChunkProgram> shaders: this.programs.values()) {
            for (ChunkProgram shader : shaders.values()) {
                shader.delete();
            }
        }

        for (EnumMap<BlockRenderPass, ChunkProgram> shaders: this.shadowPrograms.values()) {
            for (ChunkProgram shader : shaders.values()) {
                shader.delete();
            }
        }
    }

    @Override
    public ChunkVertexType getVertexType() {
        return this.vertexType;
    }
}
