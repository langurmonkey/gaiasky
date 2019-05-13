/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.shader;

import gaia.cu9.ari.gaiaorbit.util.gdx.IntRenderable;

public class TessellationShader extends GroundShader {

    public TessellationShader(final IntRenderable renderable, final Config config, final String prefix, final String vertexShader, final String controlShader, final String evaluationShader, final String fragmentShader) {
        this(renderable, config, new TessellationShaderProgram(ShaderProgramProvider.getShaderCode(prefix, vertexShader), ShaderProgramProvider.getShaderCode(prefix, controlShader), ShaderProgramProvider.getShaderCode(prefix, evaluationShader), ShaderProgramProvider.getShaderCode(prefix, fragmentShader)));
    }

    public TessellationShader(final IntRenderable renderable, final Config config, final TessellationShaderProgram shaderProgram) {
        super(renderable, config, shaderProgram);
    }

    @Override
    public boolean canRender(final IntRenderable renderable) {
        return super.canRender(renderable) && this.shadowMap == (renderable.environment.shadowMap != null);
    }

    public static String createPrefix(final IntRenderable renderable, final Config config) {
        String prefix = RelativisticShader.createPrefix(renderable, config);
        final long mask = renderable.material.getMask();
        // Atmosphere ground only if camera height is set
        if ((mask & AtmosphereAttribute.CameraHeight) == AtmosphereAttribute.CameraHeight)
            prefix += "#define atmosphereGround\n";
        return prefix;
    }
}
