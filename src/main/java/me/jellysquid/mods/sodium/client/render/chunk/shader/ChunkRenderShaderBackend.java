package me.jellysquid.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.*;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.compat.LegacyFogHelper;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;

import net.coderbot.iris.gl.program.ProgramUniforms;
import net.coderbot.iris.pipeline.SodiumTerrainPipeline;
import net.coderbot.iris.shadows.ShadowRenderingState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Optional;

public abstract class ChunkRenderShaderBackend<T extends ChunkGraphicsState>
        implements ChunkRenderBackend<T> {
    private final EnumMap<ChunkFogMode, EnumMap<BlockRenderPass, ChunkProgram>> programs = new EnumMap<>(ChunkFogMode.class);
    private final EnumMap<ChunkFogMode, EnumMap<BlockRenderPass, ChunkProgram>> shadowPrograms = new EnumMap<>(ChunkFogMode.class);

    @Nullable
    private final SodiumTerrainPipeline pipeline = SodiumTerrainPipeline.create().orElse(null);

    protected final ChunkVertexType vertexType;
    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected ChunkProgram activeProgram;

    public ChunkRenderShaderBackend(ChunkVertexType vertexType) {
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getCustomVertexFormat();
    }

    private GlShader createVertexShader(RenderDevice device, ChunkFogMode fogMode, BlockRenderPass pass, boolean shadow) {
        if (pipeline != null) {
            Optional<String> irisVertexShader;

            if (shadow) {
                irisVertexShader = pipeline.getShadowVertexShaderSource();
            } else {
                irisVertexShader = pass.isTranslucent() ? pipeline.getTranslucentVertexShaderSource() : pipeline.getTerrainVertexShaderSource();
            }

            if (irisVertexShader.isPresent()) {
                return new GlShader(device, ShaderType.VERTEX, new Identifier("iris", "sodium-terrain.vsh"),
                        irisVertexShader.get(), ShaderConstants.builder().build());
            }
        }

        return ShaderLoader.loadShader(device, ShaderType.VERTEX,
                new Identifier("sodium", "chunk_gl20.v.glsl"), fogMode.getDefines());
    }

    private GlShader createFragmentShader(RenderDevice device, ChunkFogMode fogMode, BlockRenderPass pass, boolean shadow) {
        if (pipeline != null) {
            Optional<String> irisFragmentShader;

            if (shadow) {
                irisFragmentShader = pipeline.getShadowFragmentShaderSource();
            } else {
                irisFragmentShader = pass.isTranslucent() ? pipeline.getTranslucentFragmentShaderSource() : pipeline.getTerrainFragmentShaderSource();
            }

            if (irisFragmentShader.isPresent()) {
                return new GlShader(device, ShaderType.FRAGMENT, new Identifier("iris", "sodium-terrain.fsh"),
                        irisFragmentShader.get(), ShaderConstants.builder().build());
            }
        }

        return ShaderLoader.loadShader(device, ShaderType.FRAGMENT,
                new Identifier("sodium", "chunk_gl20.f.glsl"), fogMode.getDefines());
    }

    private ChunkProgram createShader(RenderDevice device, ChunkFogMode fogMode, BlockRenderPass pass,
                                      GlVertexFormat<ChunkMeshAttribute> vertexFormat, boolean shadow) {
        GlShader vertShader = createVertexShader(device, fogMode, pass, shadow);
        GlShader fragShader = createFragmentShader(device, fogMode, pass, shadow);

        try {
            return GlProgram.builder(new Identifier("sodium", "chunk_shader_for_" + pass.toString().toLowerCase(Locale.ROOT) + (shadow ? "_gbuffer" : "_shadow")))
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
                    .build((program, name) -> {
                        ProgramUniforms uniforms = null;

                        if (pipeline != null) {
                            uniforms = pipeline.initUniforms(name);
                        }

                        return new ChunkProgram(device, program, name, fogMode.getFactory(), uniforms);
                    });
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

    private EnumMap<BlockRenderPass, ChunkProgram> createShadersForFogMode(RenderDevice device, ChunkFogMode mode, boolean shadow) {
        EnumMap<BlockRenderPass, ChunkProgram> shaders = new EnumMap<>(BlockRenderPass.class);

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            shaders.put(pass, this.createShader(device, mode, pass, this.vertexFormat, shadow));
        }

        return shaders;
    }

    @Override
    public void begin(MatrixStack matrixStack, BlockRenderPass pass) {
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
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
