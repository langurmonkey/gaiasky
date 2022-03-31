/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import gaiasky.util.Bits;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.model.IntMeshPart;
import gaiasky.util.gdx.shader.attribute.AtmosphereAttribute;
import gaiasky.util.gdx.shader.provider.ShaderProgramProvider;
import gaiasky.util.gdx.shader.provider.TessellationShaderProvider;
import org.lwjgl.opengl.GL41;

public class TessellationShader extends GroundShader {

    public TessellationShader(final IntRenderable renderable, final TessellationShaderProvider.Config config, final String prefix, final String vertexShader, final String controlShader, final String evaluationShader, final String fragmentShader) {
        this(renderable, config, new TessellationShaderProgram(ShaderProgramProvider.getShaderCode(prefix, vertexShader), ShaderProgramProvider.getShaderCode(prefix, controlShader), ShaderProgramProvider.getShaderCode(prefix, evaluationShader), ShaderProgramProvider.getShaderCode(prefix, fragmentShader)));
    }

    public TessellationShader(final IntRenderable renderable, final TessellationShaderProvider.Config config, final TessellationShaderProgram shaderProgram) {
        super(renderable, config, shaderProgram);
    }

    public TessellationShader(final IntRenderable renderable, final TessellationShaderProvider.Config config) {
        this(renderable, config, createPrefix(renderable, config));
    }

    public TessellationShader(final IntRenderable renderable, final TessellationShaderProvider.Config config, final String prefix) {
        this(renderable, config, prefix, config.vertexShaderCode, config.controlShader, config.evaluationShader, config.fragmentShaderCode);
    }

    @Override
    public boolean canRender(final IntRenderable renderable) {
        return super.canRender(renderable) && this.shadowMap == (renderable.environment.shadowMap != null);
    }

    public static String createPrefix(final IntRenderable renderable, final TessellationShaderProvider.Config config) {
        String prefix = RelativisticShader.createPrefix(renderable, config);
        final Bits mask = renderable.material.getMask();
        // Atmosphere ground only if camera height is set
        if (mask.has(AtmosphereAttribute.CameraHeight))
            prefix += "#define atmosphereGround\n";
        return prefix;
    }

    public void renderMesh(ExtShaderProgram program, IntMeshPart meshPart){
        // Override primitive
        meshPart.mesh.render(program, GL41.GL_PATCHES, meshPart.offset, meshPart.size, false);
        //Gdx.gl20.glDrawArrays(GL41.GL_PATCHES, meshPart.offset, meshPart.size);
    }
}
