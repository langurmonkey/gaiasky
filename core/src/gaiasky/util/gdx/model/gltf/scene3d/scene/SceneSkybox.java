/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.scene;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.IntRenderableProvider;
import gaiasky.util.gdx.OwnCubemap;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.gltf.scene3d.shaders.PBRShader;
import gaiasky.util.gdx.model.gltf.scene3d.shaders.PBRShaderConfig;
import gaiasky.util.gdx.model.gltf.scene3d.shaders.PBRShaderConfig.SRGB;
import gaiasky.util.gdx.shader.DefaultIntShader;
import gaiasky.util.gdx.shader.DefaultIntShader.Config;
import gaiasky.util.gdx.shader.IntShader;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.gdx.shader.attribute.CubemapAttribute;
import gaiasky.util.gdx.shader.attribute.Matrix4Attribute;
import gaiasky.util.gdx.shader.provider.DefaultIntShaderProvider;
import gaiasky.util.gdx.shader.provider.IntShaderProvider;
import net.jafama.FastMath;

public class SceneSkybox implements IntRenderableProvider, Updatable, Disposable {

    private IntShaderProvider shaderProvider;
    private boolean ownShaderProvider;
    private IntModel boxModel;
    private IntRenderable box;

    /**
     * Create a sky box with a default shader.
     */
    public SceneSkybox(OwnCubemap cubemap) {
        this(cubemap, null);
    }

    /**
     * Create a sky box with color space conversion settings.
     *
     * @param manualSRGB      see {@link PBRShaderConfig#manualSRGB}
     * @param gammaCorrection when null, gamma correction is disabled.
     *                        see {@link PBRShaderConfig#manualGammaCorrection}
     */
    public SceneSkybox(OwnCubemap cubemap, SRGB manualSRGB, Float gammaCorrection) {
        createShaderProvider(manualSRGB, gammaCorrection);
    }

    /**
     * Create a sky box with color space conversion settings.
     *
     * @param manualSRGB      see {@link PBRShaderConfig#manualSRGB}
     * @param gammaCorrection when true, {@link PBRShaderConfig#DEFAULT_GAMMA} is used.
     */
    public SceneSkybox(OwnCubemap cubemap, SRGB manualSRGB, boolean gammaCorrection) {
        createShaderProvider(manualSRGB, gammaCorrection ? PBRShaderConfig.DEFAULT_GAMMA : null);
    }

    /**
     * Create a sky box with an optional custom shader.
     *
     * @param shaderProvider when null, a default shader provider is used (without manual SRGB nor gamma correction).
     *                       when not null, caller is responsible to dispose it.
     */
    public SceneSkybox(OwnCubemap cubemap, IntShaderProvider shaderProvider) {
        if (shaderProvider == null) {
            createShaderProvider(SRGB.NONE, null);
        } else {
            this.shaderProvider = shaderProvider;
        }
    }

    private void createShaderProvider(SRGB manualSRGB, Float gammaCorrection) {
    }


    private static class SkyboxShader extends DefaultIntShader {

        public SkyboxShader(IntRenderable renderable, Config config) {
            super(renderable, config, createPrefix(renderable, config) + createSkyBoxPrefix(renderable));
            register(PBRShader.envRotationUniform, PBRShader.envRotationSetter);
        }

        private static String createSkyBoxPrefix(IntRenderable renderable) {
            String prefix = "";
            if (renderable.environment.has(Matrix4Attribute.EnvRotation)) {
                prefix += "#define ENV_ROTATION\n";
            }
            return prefix;
        }
    }

    private static class SkyboxShaderProvider extends DefaultIntShaderProvider {
        public SkyboxShaderProvider(Config config) {
            super(config);
        }

        @Override
        protected IntShader createShader(final IntRenderable renderable) {
            return new SkyboxShader(renderable, config);
        }
    }

    public SceneSkybox set(OwnCubemap cubemap) {
        box.environment.set(new CubemapAttribute(CubemapAttribute.ReflectionCubemap, cubemap));
        return this;
    }

    /**
     * @return skybox material color to be modified (default is white)
     */
    public Color getColor() {
        return box.material.get(ColorAttribute.class, ColorAttribute.Diffuse).color;
    }

    @Override
    public void update(Camera camera, float delta) {
        // scale skybox to camera range.
        float s = camera.far * (float) FastMath.sqrt(2.0);
        box.worldTransform.setToScaling(s, s, s);
        box.worldTransform.setTranslation(camera.position);
    }

    @Override
    public void getRenderables(Array<IntRenderable> renderables, Pool<IntRenderable> pool) {
        // assign shader
        box.shader = shaderProvider.getShader(box);
        renderables.add(box);
    }

    @Override
    public void dispose() {
        if (shaderProvider != null && ownShaderProvider) shaderProvider.dispose();
        boxModel.dispose();
    }

    public void setRotation(float azymuthAngleDegree) {
        Matrix4Attribute attribute = box.environment.get(Matrix4Attribute.class, Matrix4Attribute.EnvRotation);
        if (attribute != null) {
            attribute.set(azymuthAngleDegree);
        } else {
            box.environment.set(Matrix4Attribute.createEnvRotation(azymuthAngleDegree));
        }
    }

    public void setRotation(Matrix4 envRotation) {
        Matrix4Attribute attribute = box.environment.get(Matrix4Attribute.class, Matrix4Attribute.EnvRotation);
        if (envRotation != null) {
            if (attribute != null) {
                attribute.value.set(envRotation);
            } else {
                box.environment.set(Matrix4Attribute.createEnvRotation(envRotation));
            }
        } else if (attribute != null) {
            box.environment.remove(Matrix4Attribute.EnvRotation);
        }
    }
}
