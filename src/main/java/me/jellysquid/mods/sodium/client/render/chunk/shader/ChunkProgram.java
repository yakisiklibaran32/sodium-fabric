package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.GameRendererContext;
import net.coderbot.iris.gl.program.ProgramSamplers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL20C;
import net.minecraft.util.math.Matrix4f;

import net.coderbot.iris.gl.program.ProgramUniforms;
import net.coderbot.iris.texunits.TextureUnit;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.function.Function;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkProgram extends GlProgram {
    // Uniform variable binding indexes
    private final int uModelViewProjectionMatrix;
    private final int uModelScale;
    private final int uTextureScale;
    private final int uBlockTex;
    private final int uLightTex;
    private final int modelViewMatrixOffset;
    private final int normalMatrixOffset;

    @Nullable
    private final ProgramUniforms irisProgramUniforms;

    @Nullable
    private final ProgramSamplers irisProgramSamplers;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    protected ChunkProgram(RenderDevice owner, Identifier name, int handle, Function<ChunkProgram, ChunkShaderFogComponent> fogShaderFunction,
                           @Nullable ProgramUniforms irisProgramUniforms, @Nullable ProgramSamplers irisProgramSamplers) {
        super(owner, name, handle);

        this.uModelViewProjectionMatrix = this.getUniformLocation("u_ModelViewProjectionMatrix");

        this.uBlockTex = this.getUniformLocation("u_BlockTex");
        this.uLightTex = this.getUniformLocation("u_LightTex");
        this.uModelScale = this.getUniformLocation("u_ModelScale");
        this.uTextureScale = this.getUniformLocation("u_TextureScale");

        this.modelViewMatrixOffset = this.getUniformLocation("u_ModelViewMatrix");
        this.normalMatrixOffset = this.getUniformLocation("u_NormalMatrix");
        this.irisProgramUniforms = irisProgramUniforms;
        this.irisProgramSamplers = irisProgramSamplers;

        this.fogShader = fogShaderFunction.apply(this);
    }

    public void setup(MatrixStack matrixStack, float modelScale, float textureScale) {
        GL20C.glUniform1i(this.uBlockTex, TextureUnit.TERRAIN.getSamplerId());
        GL20C.glUniform1i(this.uLightTex, TextureUnit.LIGHTMAP.getSamplerId());

        GL20C.glUniform3f(this.uModelScale, modelScale, modelScale, modelScale);
        GL20C.glUniform2f(this.uTextureScale, textureScale, textureScale);

        this.fogShader.setup();

        if (this.uModelViewProjectionMatrix != -1) {
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                GL20C.glUniformMatrix4fv(this.uModelViewProjectionMatrix, false,
                        GameRendererContext.getModelViewProjectionMatrix(matrixStack.peek(), memoryStack));
            }
        }

        if (irisProgramUniforms != null) {
            irisProgramUniforms.update();
        }

        if (irisProgramSamplers != null) {
            irisProgramSamplers.update();
        }

        Matrix4f modelViewMatrix = matrixStack.peek().getModel();
        Matrix4f normalMatrix = matrixStack.peek().getModel().copy();
        normalMatrix.invert();
        normalMatrix.transpose();

        uniformMatrix(modelViewMatrixOffset, modelViewMatrix);
        uniformMatrix(normalMatrixOffset, normalMatrix);
    }

    @Override
    public int getUniformLocation(String name) {
        try {
            return super.getUniformLocation(name);
        } catch (NullPointerException e) {
            // Suppress getUniformLocation
            // TODO: Better way to cancel accesses to these uniforms?
            return -1;
        }
    }

    private void uniformMatrix(int location, Matrix4f matrix) {
        if (location == -1) {
            return;
        }

        // TODO: Don't use BufferUtils here...
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        matrix.writeToBuffer(buffer);
        buffer.rewind();

        GL20C.glUniformMatrix4fv(location, false, buffer);
    }
}
