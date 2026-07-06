/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.shader;

import gaiasky.util.Bits;
import gaiasky.render.gdx.IntRenderable;
import gaiasky.render.gdx.model.IntMeshPart;
import gaiasky.render.gdx.shader.attribute.AtmosphereAttribute;
import gaiasky.render.gdx.shader.attribute.Attributes;
import gaiasky.render.gdx.shader.attribute.FloatAttribute;
import gaiasky.render.gdx.shader.attribute.IntAttribute;
import gaiasky.render.gdx.shader.attribute.Vector3Attribute;
import gaiasky.render.gdx.shader.provider.ShaderProgramProvider;
import gaiasky.render.gdx.shader.provider.TessellationShaderProvider;
import org.lwjgl.opengl.GL41;

import java.util.Objects;

public class TessellationShader extends GroundShader {
    public final int u_noiseType;

    // Noise uniforms for the evaluation shader (procedural planets)
    public final int u_elevationSeed;
    public final int u_elevationAmplitude;
    public final int u_elevationPersistence;
    public final int u_elevationFrequency;
    public final int u_elevationLacunarity;
    public final int u_elevationScale;
    public final int u_elevationPower;
    public final int u_elevationOctaves;
    public final int u_elevationTurbulence;
    public final int u_elevationRidge;

    public final int u_moistureSeed;
    public final int u_moistureAmplitude;
    public final int u_moisturePersistence;
    public final int u_moistureFrequency;
    public final int u_moistureLacunarity;
    public final int u_moistureScale;
    public final int u_moisturePower;
    public final int u_moistureOctaves;
    public final int u_moistureTurbulence;
    public final int u_moistureRidge;

    public final int u_waterLevel;

    public TessellationShader(IntRenderable renderable, TessellationShaderProvider.Config config, String prefix, String vertexShader, String controlShader, String evaluationShader, String fragmentShader) {
        this(renderable, config, new TessellationShaderProgram(ShaderProgramProvider.getShaderCode(prefix, vertexShader), ShaderProgramProvider.getShaderCode(prefix, controlShader), ShaderProgramProvider.getShaderCode(prefix, evaluationShader), ShaderProgramProvider.getShaderCode(prefix, fragmentShader)));
    }

    public TessellationShader(IntRenderable renderable, TessellationShaderProvider.Config config, TessellationShaderProgram shaderProgram) {
        super(renderable, config, shaderProgram);

        u_noiseType = register(Inputs.noiseType, Setters.noiseType);

        // Elevation noise
        u_elevationSeed = register(Inputs.elevationSeed, Setters.elevationSeed);
        u_elevationAmplitude = register(Inputs.elevationAmplitude, Setters.elevationAmplitude);
        u_elevationPersistence = register(Inputs.elevationPersistence, Setters.elevationPersistence);
        u_elevationFrequency = register(Inputs.elevationFrequency, Setters.elevationFrequency);
        u_elevationLacunarity = register(Inputs.elevationLacunarity, Setters.elevationLacunarity);
        u_elevationScale = register(Inputs.elevationScale, Setters.elevationScale);
        u_elevationPower = register(Inputs.elevationPower, Setters.elevationPower);
        u_elevationOctaves = register(Inputs.elevationOctaves, Setters.elevationOctaves);
        u_elevationTurbulence = register(Inputs.elevationTurbulence, Setters.elevationTurbulence);
        u_elevationRidge = register(Inputs.elevationRidge, Setters.elevationRidge);

        // Moisture noise
        u_moistureSeed = register(Inputs.moistureSeed, Setters.moistureSeed);
        u_moistureAmplitude = register(Inputs.moistureAmplitude, Setters.moistureAmplitude);
        u_moisturePersistence = register(Inputs.moisturePersistence, Setters.moisturePersistence);
        u_moistureFrequency = register(Inputs.moistureFrequency, Setters.moistureFrequency);
        u_moistureLacunarity = register(Inputs.moistureLacunarity, Setters.moistureLacunarity);
        u_moistureScale = register(Inputs.moistureScale, Setters.moistureScale);
        u_moisturePower = register(Inputs.moisturePower, Setters.moisturePower);
        u_moistureOctaves = register(Inputs.moistureOctaves, Setters.moistureOctaves);
        u_moistureTurbulence = register(Inputs.moistureTurbulence, Setters.moistureTurbulence);
        u_moistureRidge = register(Inputs.moistureRidge, Setters.moistureRidge);

        u_waterLevel = register(Inputs.waterLevel, Setters.waterLevel);
    }

    public TessellationShader(IntRenderable renderable, TessellationShaderProvider.Config config) {
        this(renderable, config, createPrefix(renderable, config));
    }

    public TessellationShader(IntRenderable renderable, TessellationShaderProvider.Config config, String prefix) {
        this(renderable, config, prefix, config.vertexShaderCode, config.controlShader, config.evaluationShader, config.fragmentShaderCode);
    }

    public static String createPrefix(IntRenderable renderable, TessellationShaderProvider.Config config) {
        String prefix = RelativisticShader.createPrefix(renderable, config);
        Bits mask = renderable.material.getMask();
        // Atmosphere ground only if camera height is set
        if (mask.has(AtmosphereAttribute.CameraHeight))
            prefix += "#define atmosphereGround\n";
        return prefix;
    }

    @Override
    public boolean canRender(IntRenderable renderable) {
        return super.canRender(renderable) && this.shadowMap == (renderable.environment.shadowMap != null);
    }

    public void renderMesh(ExtShaderProgram program, IntMeshPart meshPart) {
        // Override primitive
        meshPart.mesh.render(program, GL41.GL_PATCHES, meshPart.offset, meshPart.size, false);
    }

    public static class Inputs extends GroundShader.Inputs {
        public final static Uniform noiseType = new Uniform("u_noiseType", IntAttribute.NoiseType);

        // Elevation noise
        public final static Uniform elevationSeed = new Uniform("u_elevationSeed", FloatAttribute.ElevationSeed);
        public final static Uniform elevationAmplitude = new Uniform("u_elevationAmplitude", FloatAttribute.ElevationAmplitude);
        public final static Uniform elevationPersistence = new Uniform("u_elevationPersistence", FloatAttribute.ElevationPersistence);
        public final static Uniform elevationFrequency = new Uniform("u_elevationFrequency", FloatAttribute.ElevationFrequency);
        public final static Uniform elevationLacunarity = new Uniform("u_elevationLacunarity", FloatAttribute.ElevationLacunarity);
        public final static Uniform elevationScale = new Uniform("u_elevationScale", Vector3Attribute.ElevationScale);
        public final static Uniform elevationPower = new Uniform("u_elevationPower", FloatAttribute.ElevationPower);
        public final static Uniform elevationOctaves = new Uniform("u_elevationOctaves", IntAttribute.ElevationOctaves);
        public final static Uniform elevationTurbulence = new Uniform("u_elevationTurbulence", IntAttribute.ElevationTurbulence);
        public final static Uniform elevationRidge = new Uniform("u_elevationRidge", IntAttribute.ElevationRidge);

        // Moisture noise
        public final static Uniform moistureSeed = new Uniform("u_moistureSeed", FloatAttribute.MoistureSeed);
        public final static Uniform moistureAmplitude = new Uniform("u_moistureAmplitude", FloatAttribute.MoistureAmplitude);
        public final static Uniform moisturePersistence = new Uniform("u_moisturePersistence", FloatAttribute.MoisturePersistence);
        public final static Uniform moistureFrequency = new Uniform("u_moistureFrequency", FloatAttribute.MoistureFrequency);
        public final static Uniform moistureLacunarity = new Uniform("u_moistureLacunarity", FloatAttribute.MoistureLacunarity);
        public final static Uniform moistureScale = new Uniform("u_moistureScale", Vector3Attribute.MoistureScale);
        public final static Uniform moisturePower = new Uniform("u_moisturePower", FloatAttribute.MoisturePower);
        public final static Uniform moistureOctaves = new Uniform("u_moistureOctaves", IntAttribute.MoistureOctaves);
        public final static Uniform moistureTurbulence = new Uniform("u_moistureTurbulence", IntAttribute.MoistureTurbulence);
        public final static Uniform moistureRidge = new Uniform("u_moistureRidge", IntAttribute.MoistureRidge);

        public final static Uniform waterLevel = new Uniform("u_waterLevel", FloatAttribute.WaterLevel);
    }

    public static class Setters extends GroundShader.Setters {
        public final static Setter noiseType = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(IntAttribute.NoiseType))
                    shader.set(inputID, ((IntAttribute) Objects.requireNonNull(combinedAttributes.get(IntAttribute.NoiseType))).value);
            }
        };
        // Elevation noise
        public final static Setter elevationSeed = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.ElevationSeed))
                    shader.set(inputID, ((FloatAttribute) Objects.requireNonNull(combinedAttributes.get(FloatAttribute.ElevationSeed))).value);
            }
        };
        public final static Setter elevationAmplitude = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.ElevationAmplitude))
                    shader.set(inputID, ((FloatAttribute) Objects.requireNonNull(combinedAttributes.get(FloatAttribute.ElevationAmplitude))).value);
            }
        };
        public final static Setter elevationPersistence = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.ElevationPersistence))
                    shader.set(inputID, ((FloatAttribute) Objects.requireNonNull(combinedAttributes.get(FloatAttribute.ElevationPersistence))).value);
            }
        };
        public final static Setter elevationFrequency = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.ElevationFrequency))
                    shader.set(inputID, ((FloatAttribute) Objects.requireNonNull(combinedAttributes.get(FloatAttribute.ElevationFrequency))).value);
            }
        };
        public final static Setter elevationLacunarity = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.ElevationLacunarity))
                    shader.set(inputID, ((FloatAttribute) Objects.requireNonNull(combinedAttributes.get(FloatAttribute.ElevationLacunarity))).value);
            }
        };
        public final static Setter elevationScale = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(Vector3Attribute.ElevationScale))
                    shader.set(inputID, ((Vector3Attribute) Objects.requireNonNull(combinedAttributes.get(Vector3Attribute.ElevationScale))).value);
            }
        };
        public final static Setter elevationPower = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.ElevationPower))
                    shader.set(inputID, ((FloatAttribute) Objects.requireNonNull(combinedAttributes.get(FloatAttribute.ElevationPower))).value);
            }
        };
        public final static Setter elevationOctaves = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(IntAttribute.ElevationOctaves))
                    shader.set(inputID, ((IntAttribute) Objects.requireNonNull(combinedAttributes.get(IntAttribute.ElevationOctaves))).value);
            }
        };
        public final static Setter elevationTurbulence = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(IntAttribute.ElevationTurbulence))
                    shader.set(inputID, ((IntAttribute) Objects.requireNonNull(combinedAttributes.get(IntAttribute.ElevationTurbulence))).value);
            }
        };
        public final static Setter elevationRidge = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(IntAttribute.ElevationRidge))
                    shader.set(inputID, ((IntAttribute) Objects.requireNonNull(combinedAttributes.get(IntAttribute.ElevationRidge))).value);
            }
        };

        // Moisture noise
        public final static Setter moistureSeed = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.MoistureSeed))
                    shader.set(inputID, ((FloatAttribute) Objects.requireNonNull(combinedAttributes.get(FloatAttribute.MoistureSeed))).value);
            }
        };
        public final static Setter moistureAmplitude = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.MoistureAmplitude))
                    shader.set(inputID, ((FloatAttribute) Objects.requireNonNull(combinedAttributes.get(FloatAttribute.MoistureAmplitude))).value);
            }
        };
        public final static Setter moisturePersistence = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.MoisturePersistence))
                    shader.set(inputID, ((FloatAttribute) Objects.requireNonNull(combinedAttributes.get(FloatAttribute.MoisturePersistence))).value);
            }
        };
        public final static Setter moistureFrequency = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.MoistureFrequency))
                    shader.set(inputID, ((FloatAttribute) Objects.requireNonNull(combinedAttributes.get(FloatAttribute.MoistureFrequency))).value);
            }
        };
        public final static Setter moistureLacunarity = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.MoistureLacunarity))
                    shader.set(inputID, ((FloatAttribute) Objects.requireNonNull(combinedAttributes.get(FloatAttribute.MoistureLacunarity))).value);
            }
        };
        public final static Setter moistureScale = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(Vector3Attribute.MoistureScale))
                    shader.set(inputID, ((Vector3Attribute) Objects.requireNonNull(combinedAttributes.get(Vector3Attribute.MoistureScale))).value);
            }
        };
        public final static Setter moisturePower = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.MoisturePower))
                    shader.set(inputID, ((FloatAttribute) Objects.requireNonNull(combinedAttributes.get(FloatAttribute.MoisturePower))).value);
            }
        };
        public final static Setter moistureOctaves = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(IntAttribute.MoistureOctaves))
                    shader.set(inputID, ((IntAttribute) Objects.requireNonNull(combinedAttributes.get(IntAttribute.MoistureOctaves))).value);
            }
        };
        public final static Setter moistureTurbulence = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(IntAttribute.MoistureTurbulence))
                    shader.set(inputID, ((IntAttribute) Objects.requireNonNull(combinedAttributes.get(IntAttribute.MoistureTurbulence))).value);
            }
        };
        public final static Setter moistureRidge = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(IntAttribute.MoistureRidge))
                    shader.set(inputID, ((IntAttribute) Objects.requireNonNull(combinedAttributes.get(IntAttribute.MoistureRidge))).value);
            }
        };

        public final static Setter waterLevel = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.WaterLevel))
                    shader.set(inputID, ((FloatAttribute) Objects.requireNonNull(combinedAttributes.get(FloatAttribute.WaterLevel))).value);
            }
        };
    }
}
