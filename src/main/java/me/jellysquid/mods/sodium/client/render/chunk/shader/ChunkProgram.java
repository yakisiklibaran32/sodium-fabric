package me.jellysquid.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL20C;
import net.coderbot.iris.gl.program.ProgramSamplers;
import net.coderbot.iris.gl.program.ProgramUniforms;
import net.coderbot.iris.texunits.TextureUnit;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkProgram extends GlProgram {
    // Uniform variable binding indexes
    private final int uModelScale;
    private final int uModelOffset;
    private final int uTextureScale;
    private final int uBlockTex;
    private final int uLightTex;
    public final int uModelViewMatrix;
    public final int uProjectionMatrix;
    public final int uModelViewProjectionMatrix;
    public final int uNormalMatrix;

    @Nullable
    private final ProgramUniforms irisProgramUniforms;

    @Nullable
    private final ProgramSamplers irisProgramSamplers;

    public final int uboDrawParametersIndex;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    public ChunkProgram(RenderDevice owner, int handle, ChunkShaderOptions options,
                           @Nullable ProgramUniforms irisProgramUniforms, @Nullable ProgramSamplers irisProgramSamplers) {
        super(handle);

        this.uModelViewMatrix = this.getUniformLocation("u_ModelViewMatrix");
        this.uProjectionMatrix = this.getUniformLocation("u_ProjectionMatrix");
        this.uModelViewProjectionMatrix = this.getUniformLocation("u_ModelViewProjectionMatrix");

        this.uBlockTex = this.getUniformLocation("u_BlockTex");
        this.uLightTex = this.getUniformLocation("u_LightTex");
        this.uModelScale = this.getUniformLocation("u_ModelScale");
        this.uModelOffset = this.getUniformLocation("u_ModelOffset");
        this.uTextureScale = this.getUniformLocation("u_TextureScale");

        this.uboDrawParametersIndex = this.getUniformBlockIndex("ubo_DrawParameters");

        this.fogShader = options.fog().getFactory().apply(this);

        this.uNormalMatrix = this.getUniformLocation("u_NormalMatrix");
        this.irisProgramUniforms = irisProgramUniforms;
        this.irisProgramSamplers = irisProgramSamplers;
    }

    public void setup(ChunkVertexType vertexType) {
        RenderSystem.activeTexture(TextureUnit.TERRAIN.getUnitId());
        RenderSystem.bindTexture(RenderSystem.getShaderTexture(0));

        RenderSystem.activeTexture(TextureUnit.LIGHTMAP.getUnitId());
        RenderSystem.bindTexture(RenderSystem.getShaderTexture(2));

        GL20C.glUniform1i(this.uBlockTex, TextureUnit.TERRAIN.getSamplerId());
        GL20C.glUniform1i(this.uLightTex, TextureUnit.LIGHTMAP.getSamplerId());

        GL20C.glUniform1f(this.uModelScale, vertexType.getModelScale());
        GL20C.glUniform1f(this.uModelOffset, vertexType.getModelOffset());
        GL20C.glUniform1f(this.uTextureScale, vertexType.getTextureScale());

        this.fogShader.setup();

        if (irisProgramUniforms != null) {
            irisProgramUniforms.update();
        }

        if (irisProgramSamplers != null) {
            irisProgramSamplers.update();
        }
    }

    @Override
    public int getUniformLocation(String name) {
        try {
            return super.getUniformLocation(name);
        } catch (NullPointerException e) {
            System.err.println(e.getMessage());
            return -1;
        }
    }
}
