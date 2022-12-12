/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.shader.attribute.Attributes;
import gaiasky.util.gdx.shader.attribute.BlendingAttribute;
import gaiasky.util.gdx.shader.attribute.FloatAttribute;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;

public class DepthIntShader extends DefaultIntShader {
    private final static Attributes tmpAttributes = new Attributes();
    private static String defaultVertexShader = null;
    private static String defaultFragmentShader = null;
    public final int numBones;
    public final int weights;
    private final FloatAttribute alphaTestAttribute;

    public DepthIntShader(final IntRenderable renderable) {
        this(renderable, new Config());
    }
    public DepthIntShader(final IntRenderable renderable, final Config config) {
        this(renderable, config, createPrefix(renderable, config));
    }
    public DepthIntShader(final IntRenderable renderable, final Config config, final String prefix) {
        this(renderable, config, prefix, config.vertexShaderCode != null ? config.vertexShaderCode : getDefaultVertexShader(),
                config.fragmentShaderCode != null ? config.fragmentShaderCode : getDefaultFragmentShader());
    }

    public DepthIntShader(final IntRenderable renderable, final Config config, final String prefix, final String vertexShader,
            final String fragmentShader) {
        this(renderable, config, new ExtShaderProgram(prefix + vertexShader, prefix + fragmentShader));
    }

    public DepthIntShader(final IntRenderable renderable, final Config config, final ExtShaderProgram shaderProgram) {
        super(renderable, config, shaderProgram);
        final Attributes attributes = combineAttributes(renderable);
        this.numBones = renderable.bones == null ? 0 : config.numBones;
        int w = 0;
        final int n = renderable.meshPart.mesh.getVertexAttributes().size();
        for (int i = 0; i < n; i++) {
            final VertexAttribute attr = renderable.meshPart.mesh.getVertexAttributes().get(i);
            if (attr.usage == Usage.BoneWeight)
                w |= (1 << attr.unit);
        }
        weights = w;
        alphaTestAttribute = new FloatAttribute(FloatAttribute.AlphaTest, config.defaultAlphaTest);
    }

    public final static String getDefaultVertexShader() {
        if (defaultVertexShader == null)
            defaultVertexShader = Gdx.files.classpath("com/badlogic/gdx/graphics/g3d/shaders/depth.vertex.glsl").readString();
        return defaultVertexShader;
    }

    public final static String getDefaultFragmentShader() {
        if (defaultFragmentShader == null)
            defaultFragmentShader = Gdx.files.classpath("com/badlogic/gdx/graphics/g3d/shaders/depth.fragment.glsl").readString();
        return defaultFragmentShader;
    }

    public static String createPrefix(final IntRenderable renderable, final Config config) {
        String prefix = DefaultIntShader.createPrefix(renderable, config);
        if (!config.depthBufferOnly)
            prefix += "#define PackedDepthFlag\n";
        return prefix;
    }

    // TODO: Move responsibility for combining attributes to IntRenderableProvider
    private static final Attributes combineAttributes(final IntRenderable renderable) {
        tmpAttributes.clear();
        if (renderable.environment != null)
            tmpAttributes.set(renderable.environment);
        if (renderable.material != null)
            tmpAttributes.set(renderable.material);
        return tmpAttributes;
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        super.begin(camera, context);
        // Gdx.gl20.glEnable(GL20.GL_POLYGON_OFFSET_FILL);
        // Gdx.gl20.glPolygonOffset(2.f, 100.f);
    }

    @Override
    public void end() {
        super.end();
        // Gdx.gl20.glDisable(GL20.GL_POLYGON_OFFSET_FILL);
    }

    @Override
    public boolean canRender(IntRenderable renderable) {
        final Attributes attributes = combineAttributes(renderable);
        if (attributes.has(BlendingAttribute.Type)) {
            if (!attributes.has(TextureAttribute.Diffuse))
                return false;
        }
        final boolean skinned = ((renderable.meshPart.mesh.getVertexAttributes().getMask() & Usage.BoneWeight) == Usage.BoneWeight);
        if (skinned != (numBones > 0))
            return false;
        if (!skinned)
            return true;
        int w = 0;
        final int n = renderable.meshPart.mesh.getVertexAttributes().size();
        for (int i = 0; i < n; i++) {
            final VertexAttribute attr = renderable.meshPart.mesh.getVertexAttributes().get(i);
            if (attr.usage == Usage.BoneWeight)
                w |= (1 << attr.unit);
        }
        return w == weights;
    }

    @Override
    public void render(IntRenderable renderable, Attributes combinedAttributes) {
        if (combinedAttributes.has(BlendingAttribute.Type)) {
            final BlendingAttribute blending = (BlendingAttribute) combinedAttributes.get(BlendingAttribute.Type);
            combinedAttributes.remove(BlendingAttribute.Type);
            final boolean hasAlphaTest = combinedAttributes.has(FloatAttribute.AlphaTest);
            if (!hasAlphaTest)
                combinedAttributes.set(alphaTestAttribute);
            if (blending.opacity >= ((FloatAttribute) combinedAttributes.get(FloatAttribute.AlphaTest)).value)
                super.render(renderable, combinedAttributes);
            if (!hasAlphaTest)
                combinedAttributes.remove(FloatAttribute.AlphaTest);
            combinedAttributes.set(blending);
        } else
            super.render(renderable, combinedAttributes);
    }

    public static class Config extends DefaultIntShader.Config {
        public boolean depthBufferOnly = false;
        public float defaultAlphaTest = 0.5f;

        public Config() {
            super();
            defaultCullFace = GL20.GL_FRONT;
        }

        public Config(String vertexShader, String fragmentShader) {
            super(vertexShader, fragmentShader);
        }
    }
}
