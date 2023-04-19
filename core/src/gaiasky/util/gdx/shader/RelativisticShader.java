/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import gaiasky.util.Bits;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.shader.attribute.*;
import gaiasky.util.gdx.shader.provider.ShaderProgramProvider;

public class RelativisticShader extends DefaultIntShader {

    // Special relativity
    public final int u_vc;
    public final int u_velDir;
    // Gravitational waves
    public final int u_hterms;
    public final int u_gw;
    public final int u_gwmat3;
    public final int u_ts;
    public final int u_omgw;
    public RelativisticShader(final IntRenderable renderable) {
        this(renderable, new Config());
    }
    public RelativisticShader(final IntRenderable renderable, final Config config) {
        this(renderable, config, createPrefix(renderable, config));
    }

    public RelativisticShader(final IntRenderable renderable, final Config config, final String prefix) {
        this(renderable, config, prefix, config.vertexShaderCode != null ? config.vertexShaderCode : getDefaultVertexShader(), config.fragmentShaderCode != null ? config.fragmentShaderCode : getDefaultFragmentShader());
    }

    public RelativisticShader(final IntRenderable renderable, final Config config, final String prefix, final String vertexShader, final String fragmentShader) {
        this(renderable, config, new ExtShaderProgram(config.vertexShaderFile, config.fragmentShaderFile, ShaderProgramProvider.getShaderCode(prefix, vertexShader), ShaderProgramProvider.getShaderCode(prefix, fragmentShader)));
    }

    public RelativisticShader(final IntRenderable renderable, final Config config, final ExtShaderProgram shaderProgram) {
        super(renderable, config, shaderProgram);

        u_vc = register(Inputs.vc, Setters.vc);
        u_velDir = register(Inputs.velDir, Setters.velDir);

        u_hterms = register(Inputs.hterms, Setters.hTerms);
        u_gw = register(Inputs.gw, Setters.gw);
        u_gwmat3 = register(Inputs.gwmat3, Setters.gwmat3);
        u_ts = register(Inputs.ts, Setters.ts);
        u_omgw = register(Inputs.omgw, Setters.omgw);

    }

    public static String createPrefix(final IntRenderable renderable, final Config config) {
        String prefix = DefaultIntShader.createPrefix(renderable, config);
        final Bits mask = renderable.material.getMask();
        // Special relativity
        if (mask.has(FloatAttribute.Vc))
            prefix += "#define relativisticEffects\n";
        // Gravitational waves
        if (mask.has(FloatAttribute.Omgw))
            prefix += "#define gravitationalWaves\n";
        return prefix;
    }

    public static class Inputs extends DefaultIntShader.Inputs {
        // Special relativity
        public final static Uniform vc = new Uniform("u_vc");
        public final static Uniform velDir = new Uniform("u_velDir");

        // Gravitational waves
        public final static Uniform hterms = new Uniform("u_hterms");
        public final static Uniform gw = new Uniform("u_gw");
        public final static Uniform gwmat3 = new Uniform("u_gwmat3");
        public final static Uniform ts = new Uniform("u_ts");
        public final static Uniform omgw = new Uniform("u_omgw");
    }

    public static class Setters extends DefaultIntShader.Setters {
        public final static Setter vc = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.Vc))
                    shader.set(inputID, ((FloatAttribute) (combinedAttributes.get(FloatAttribute.Vc))).value);
            }
        };

        public final static Setter velDir = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(Vector3Attribute.VelDir))
                    shader.set(inputID, ((Vector3Attribute) (combinedAttributes.get(Vector3Attribute.VelDir))).value);
            }
        };

        public final static Setter hTerms = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(Vector4Attribute.Hterms)) {
                    float[] val = ((Vector4Attribute) (combinedAttributes.get(Vector4Attribute.Hterms))).value;
                    shader.set(inputID, val[0], val[1], val[2], val[3]);
                }
            }
        };

        public final static Setter gw = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(Vector3Attribute.Gw))
                    shader.set(inputID, ((Vector3Attribute) (combinedAttributes.get(Vector3Attribute.Gw))).value);
            }
        };

        public final static Setter gwmat3 = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(Matrix3Attribute.Gwmat3))
                    shader.set(inputID, ((Matrix3Attribute) (combinedAttributes.get(Matrix3Attribute.Gwmat3))).value);
            }
        };

        public final static Setter ts = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.Ts))
                    shader.set(inputID, ((FloatAttribute) (combinedAttributes.get(FloatAttribute.Ts))).value);
            }
        };

        public final static Setter omgw = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.Omgw))
                    shader.set(inputID, ((FloatAttribute) (combinedAttributes.get(FloatAttribute.Omgw))).value);
            }
        };

    }

}
