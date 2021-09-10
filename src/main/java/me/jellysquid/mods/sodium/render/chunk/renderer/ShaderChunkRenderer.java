package me.jellysquid.mods.sodium.render.chunk.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.render.chunk.passes.DefaultBlockRenderPasses;
import me.jellysquid.mods.thingl.attribute.GlVertexFormat;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.shader.*;
import me.jellysquid.mods.thingl.texture.GlSampler;
import me.jellysquid.mods.thingl.texture.GlTexture;
import me.jellysquid.mods.thingl.texture.TextureData;
import me.jellysquid.mods.sodium.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.interop.vanilla.lightmap.LightmapTextureManagerAccessor;
import me.jellysquid.mods.sodium.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.render.chunk.shader.*;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gl.framebuffer.GlFramebuffer;
import net.coderbot.iris.gl.program.ProgramSamplers;
import net.coderbot.iris.gl.program.ProgramUniforms;
import net.coderbot.iris.pipeline.SodiumTerrainPipeline;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.shadows.ShadowRenderingState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private final Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> gbufferPrograms = new Object2ObjectOpenHashMap<>();
    private final Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> shadowPrograms = new Object2ObjectOpenHashMap<>();

    protected final ChunkVertexType vertexType;
    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected final RenderDevice device;

    protected GlProgram<ChunkShaderInterface> activeProgram;

    private final Map<ChunkShaderTextureUnit, GlSampler> samplers = new EnumMap<>(ChunkShaderTextureUnit.class);
    private final GlTexture stippleTexture;
    private final float detailDistance;

    public ShaderChunkRenderer(RenderDevice device, ChunkVertexType vertexType, float detailDistance) {
        this.device = device;
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getCustomVertexFormat();
        this.detailDistance = detailDistance;

        try (TextureData data = TextureData.loadInternal("/assets/sodium/textures/shader/stipple.png")) {
            this.stippleTexture = new GlTexture();
            this.stippleTexture.setTextureData(data);
        }

        for (ChunkShaderTextureUnit unit : ChunkShaderTextureUnit.values()) {
            this.samplers.put(unit, new GlSampler());
        }

        var blockTexSampler = this.samplers.get(ChunkShaderTextureUnit.BLOCK_TEXTURE);
        blockTexSampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST_MIPMAP_LINEAR);
        blockTexSampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_NEAREST);
        blockTexSampler.setParameter(GL33C.GL_TEXTURE_LOD_BIAS, 0.0F);

        var lightTexSampler = this.samplers.get(ChunkShaderTextureUnit.LIGHT_TEXTURE);
        lightTexSampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_LINEAR);
        lightTexSampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR);
        lightTexSampler.setParameter(GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_CLAMP_TO_EDGE);
        lightTexSampler.setParameter(GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_CLAMP_TO_EDGE);

        var stippleSampler = this.samplers.get(ChunkShaderTextureUnit.STIPPLE_TEXTURE);
        stippleSampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST);
        stippleSampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_NEAREST);
        stippleSampler.setParameter(GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_REPEAT);
        stippleSampler.setParameter(GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_REPEAT);
    }

    protected GlProgram<ChunkShaderInterface> compileProgram(boolean isShadowPass, ChunkShaderOptions options, SodiumTerrainPipeline pipeline) {
        Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> programMap =
                isShadowPass ? shadowPrograms : gbufferPrograms;

        GlProgram<ChunkShaderInterface> program = programMap.get(options);

        if (program == null) {
            programMap.put(options, program = this.createShader("blocks/block_layer_opaque", isShadowPass, pipeline, options));
        }

        return program;
    }

    private GlProgram<ChunkShaderInterface> createShader(String path, boolean isShadowPass, SodiumTerrainPipeline pipeline, ChunkShaderOptions options) {
        ShaderConstants constants = options.constants();

        GlShader vertShader = createVertexShader(device, isShadowPass, options.pass(), pipeline, constants);
        GlShader geomShader = createGeometryShader(device, isShadowPass, options.pass(), pipeline, constants);
        GlShader fragShader = createFragmentShader(device, isShadowPass, options.pass(), pipeline, constants);
        try {
            return GlProgram.builder(new Identifier("sodium", "chunk_shader"))
                    .attachShader(vertShader)
                    .attachShader(geomShader)
                    .attachShader(fragShader)
                    .bindAttribute("a_Pos", ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID)
                    .bindAttribute("a_Color", ChunkShaderBindingPoints.ATTRIBUTE_COLOR)
                    .bindAttribute("a_TexCoord", ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE)
                    .bindAttribute("a_LightCoord", ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE)
                    .bindAttribute("a_Options", ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_FLAGS)
                    .bindAttribute("mc_Entity", ChunkShaderBindingPoints.BLOCK_ID)
                    .bindAttribute("mc_midTexCoord", ChunkShaderBindingPoints.MID_TEX_COORD)
                    .bindAttribute("at_tangent", ChunkShaderBindingPoints.TANGENT)
                    .bindAttribute("a_Normal", ChunkShaderBindingPoints.NORMAL)
                    .bindFragmentData("fragColor", ChunkShaderBindingPoints.FRAG_COLOR)
                     .bindFragmentData("iris_FragData", ChunkShaderBindingPoints.FRAG_COLOR)
                    .link((shader) -> {
                        ProgramUniforms uniforms = null;
                        ProgramSamplers samplers = null;

                        if (pipeline != null) {
                            uniforms = pipeline.initUniforms(shader.handle());

                            if (isShadowPass) {
                                samplers = pipeline.initShadowSamplers(shader.handle());
                            } else {
                                samplers = pipeline.initTerrainSamplers(shader.handle());
                            }
                        }

                        return new ChunkShaderInterface(shader, options, uniforms, samplers);
                    });
        } finally {
            vertShader.delete();
            if (geomShader != null) {
                geomShader.delete();
            }
            fragShader.delete();
        }
    }

    private GlShader createVertexShader(RenderDevice device, boolean isShadowPass, BlockRenderPass pass,
                                        SodiumTerrainPipeline pipeline, ShaderConstants constants) {
        if (pipeline != null) {
            Optional<String> irisVertexShader;
            String name;

            if (isShadowPass) {
                irisVertexShader = pipeline.getShadowVertexShaderSource();
                name = "shadow_terrain";
            } else {
                irisVertexShader = pass.isTranslucent() ? pipeline.getTranslucentVertexShaderSource() : pipeline.getTerrainVertexShaderSource();
                name = pass.isTranslucent() ? "terrain_translucent" : "terrain";
            }

            String fullName = "patched_" + name + "_for_sodium.vsh";

            if (irisVertexShader.isPresent()) {
                return new GlShader(ShaderType.VERTEX, new Identifier("iris", fullName),
                        irisVertexShader.get());
            }
        }

        return ShaderLoader.loadShader(ShaderType.VERTEX,
                new Identifier("sodium", "blocks/block_layer_opaque" + ".vsh"), constants);
    }

    private GlShader createGeometryShader(RenderDevice device, boolean isShadowPass, BlockRenderPass pass,
                                          SodiumTerrainPipeline pipeline, ShaderConstants constants) {
        if (pipeline != null) {
            Optional<String> irisGeometryShader;
            String name;

            if (isShadowPass) {
                irisGeometryShader = pipeline.getShadowGeometryShaderSource();
                name = "shadow_terrain";
            } else {
                irisGeometryShader = pass.isTranslucent() ? pipeline.getTranslucentGeometryShaderSource() : pipeline.getTerrainGeometryShaderSource();
                name = pass.isTranslucent() ? "terrain_translucent" : "terrain";
            }

            String fullName = "patched_" + name + "_for_sodium.gsh";

            if (irisGeometryShader.isPresent()) {
                return new GlShader(ShaderType.GEOMETRY, new Identifier("iris", fullName),
                        irisGeometryShader.get());
            }
        }

        return null;
    }

    private GlShader createFragmentShader(RenderDevice device, boolean isShadowPass, BlockRenderPass pass,
                                          SodiumTerrainPipeline pipeline, ShaderConstants constants) {
        if (pipeline != null) {
            Optional<String> irisFragmentShader;
            String name;

            if (isShadowPass) {
                if (pass == DefaultBlockRenderPasses.OPAQUE || pass == DefaultBlockRenderPasses.DETAIL) {
                    irisFragmentShader = pipeline.getShadowCutoutFragmentShaderSource();
                    name = "shadow_terrain_cutout";
                } else {
                    irisFragmentShader = pipeline.getShadowFragmentShaderSource();
                    name = "shadow_terrain";
                }
            } else if (pass == DefaultBlockRenderPasses.OPAQUE || pass == DefaultBlockRenderPasses.DETAIL) {
                irisFragmentShader = pipeline.getTerrainCutoutFragmentShaderSource();
                name = "terrain_cutout";
            } else if (pass.isTranslucent()) {
                irisFragmentShader = pipeline.getTranslucentFragmentShaderSource();
                name = "terrain_translucent";
            } else {
                irisFragmentShader = pipeline.getTerrainFragmentShaderSource();
                name = "terrain";
            }

            String fullName = "patched_" + name + "_for_sodium.fsh";

            if (irisFragmentShader.isPresent()) {
                return new GlShader(ShaderType.FRAGMENT, new Identifier("iris", fullName),
                        irisFragmentShader.get());
            }
        }

        return ShaderLoader.loadShader(ShaderType.FRAGMENT,
                new Identifier("sodium", "blocks/block_layer_opaque" + ".fsh"), constants);
    }

    protected void begin(BlockRenderPass pass) {
        if (Iris.getPipelineManager().isSodiumShaderReloadNeeded()) {
            deleteAllPrograms();
            Iris.getPipelineManager().clearSodiumShaderReloadNeeded();
        }

        WorldRenderingPipeline worldRenderingPipeline = Iris.getPipelineManager().getPipeline();
        SodiumTerrainPipeline sodiumTerrainPipeline = null;

        if (worldRenderingPipeline != null) {
            sodiumTerrainPipeline = worldRenderingPipeline.getSodiumTerrainPipeline();
        }

        boolean isShadowPass = ShadowRenderingState.areShadowsCurrentlyBeingRendered();

        if (isShadowPass) {
            // No back face culling during the shadow pass
            // TODO: Hopefully this won't be necessary in the future...
            RenderSystem.disableCull();
        }

        ChunkShaderOptions options = new ChunkShaderOptions(ChunkFogMode.SMOOTH, pass);

        this.activeProgram = this.compileProgram(isShadowPass, options, sodiumTerrainPipeline);
        this.activeProgram.bind();

        if (sodiumTerrainPipeline != null) {
            GlFramebuffer framebuffer = null;

            if (isShadowPass) {
                framebuffer = sodiumTerrainPipeline.getShadowFramebuffer();
            } else if (pass.isTranslucent()) {
                framebuffer = sodiumTerrainPipeline.getTranslucentFramebuffer();
            } else {
                framebuffer = sodiumTerrainPipeline.getTerrainFramebuffer();
            }

            if (framebuffer != null) {
                framebuffer.bind();
            }
        }


        var shader = this.activeProgram.getInterface();
        shader.setup(this.vertexType);
        shader.setDetailParameters(this.detailDistance);

        MinecraftClient client = MinecraftClient.getInstance();
        TextureManager textureManager = client.getTextureManager();

        LightmapTextureManagerAccessor lightmapTextureManager =
                ((LightmapTextureManagerAccessor) client.gameRenderer.getLightmapTextureManager());

        AbstractTexture blockAtlasTex = textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        AbstractTexture lightTex = lightmapTextureManager.getTexture();

        this.bindTexture(ChunkShaderTextureUnit.BLOCK_TEXTURE, blockAtlasTex.getGlId());
        this.bindTexture(ChunkShaderTextureUnit.LIGHT_TEXTURE, lightTex.getGlId());
        this.bindTexture(ChunkShaderTextureUnit.STIPPLE_TEXTURE, this.stippleTexture.handle());
    }

    private void bindTexture(ChunkShaderTextureUnit unit, int texture) {
        RenderSystem.activeTexture(GL32C.GL_TEXTURE0 + unit.id());
        RenderSystem.bindTexture(texture);

        GlSampler sampler = this.samplers.get(unit);
        sampler.bindTextureUnit(unit.id());
    }

    protected void end() {
        this.activeProgram.unbind();
        this.activeProgram = null;
        ProgramUniforms.clearActiveUniforms();

        // TODO: Bind the framebuffer to whatever fallback is specified by SodiumTerrainPipeline.
        MinecraftClient.getInstance().getFramebuffer().beginWrite(false);

        for (Map.Entry<ChunkShaderTextureUnit, GlSampler> entry : this.samplers.entrySet()) {
            entry.getValue().unbindTextureUnit(entry.getKey().id());
        }
    }

    private void deletePrograms(Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> programs) {
        programs.values()
                .stream().forEach(GlProgram::delete);

        programs.clear();
    }

    private void deleteAllPrograms() {
        deletePrograms(shadowPrograms);
        deletePrograms(gbufferPrograms);
    }

    @Override
    public void delete() {
        deleteAllPrograms();

        this.stippleTexture.delete();

        for (GlSampler sampler : this.samplers.values()) {
            sampler.delete();
        }
    }

    @Override
    public ChunkVertexType getVertexType() {
        return this.vertexType;
    }
}
