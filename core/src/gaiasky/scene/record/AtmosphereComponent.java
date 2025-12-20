/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IUpdatable;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.ModelScaffolding;
import gaiasky.util.Bits;
import gaiasky.util.Constants;
import gaiasky.util.Logger.Log;
import gaiasky.util.ModelCache;
import gaiasky.util.Pair;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.AtmosphereAttribute;
import gaiasky.util.gdx.shader.attribute.BlendingAttribute;
import gaiasky.util.gdx.shader.attribute.Vector3Attribute;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import net.jafama.FastMath;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public final class AtmosphereComponent extends NamedComponent implements IUpdatable<AtmosphereComponent> {

    public int quality;
    public float size;
    public float planetSize;
    public ModelComponent mc;
    public Matrix4 localTransform;
    public double[] wavelengths;
    public float m_fInnerRadius;
    public float m_fOuterRadius;
    public float m_fAtmosphereHeight;
    public float m_Kr, m_Km;
    public float fogDensity = 0.3f;
    public Vector3 fogColor;
    public float m_eSun = 10f;
    public int samples = 23;

    // Model parameters
    public Map<String, Object> params;

    Vector3 aux;
    Vector3D aux3, aux1;

    public AtmosphereComponent() {
        localTransform = new Matrix4();
        mc = new ModelComponent(false);
        mc.initialize(null);
        aux = new Vector3();
        aux3 = new Vector3D();
        aux1 = new Vector3D();
        fogColor = new Vector3();
    }

    public void doneLoading(Material planetMat,
                            float planetSize) {
        this.planetSize = planetSize;
        setUpAtmosphericScatteringMaterial(planetMat);

        Material atmMat;
        if (mc.instance == null) {
            Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere",
                                                                                   params,
                                                                                   Bits.indices(Usage.Position, Usage.Normal),
                                                                                   GL20.GL_TRIANGLES);
            IntModel atmosphereModel = pair.getFirst();
            atmMat = pair.getSecond().get("base");

            setUpAtmosphericScatteringMaterial(atmMat);
            atmMat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
            // CREATE ATMOSPHERE MODEL
            mc.instance = new IntModelInstance(atmosphereModel, this.localTransform);
        } else {
            atmMat = mc.instance.materials.get(0);
            setUpAtmosphericScatteringMaterial(atmMat);
        }
    }

    public void update(Vector3Q transform) {
        transform.setToTranslation(localTransform).scl(size);
    }

    /**
     * Sets up the atmospheric scattering parameters to the given material
     *
     * @param mat The material to set up.
     */
    public void setUpAtmosphericScatteringMaterial(Material mat) {
        float camHeight = 1f;
        float m_Kr4PI = m_Kr * 4.0f * (float) FastMath.PI;
        float m_Km4PI = m_Km * 4.0f * (float) FastMath.PI;
        float m_ESun = m_eSun; // Sun brightness (almost) constant
        float m_g = 0.98f; // The Mie phase asymmetry factor

        // Normalization factor is inner radius.
        float normFactor = 2f / planetSize;
        m_fInnerRadius = (planetSize / 2f) * normFactor;
        m_fOuterRadius = this.size * normFactor;
        m_fAtmosphereHeight = m_fOuterRadius - m_fInnerRadius;
        float m_fScaleDepth = 0.35f;
        float m_fScale = 1.0f / (m_fAtmosphereHeight);
        float m_fScaleOverScaleDepth = m_fScale / m_fScaleDepth;

        double[] m_fWavelength = wavelengths;
        float[] m_fWavelength4 = new float[3];
        m_fWavelength4[0] = (float) FastMath.pow(m_fWavelength[0], 4.0);
        m_fWavelength4[1] = (float) FastMath.pow(m_fWavelength[1], 4.0);
        m_fWavelength4[2] = (float) FastMath.pow(m_fWavelength[2], 4.0);

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.Alpha, 1f));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.ColorOpacity, 1f));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.CameraHeight, camHeight));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.OuterRadius, m_fOuterRadius));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.InnerRadius, m_fInnerRadius));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.KrESun, m_Kr * m_ESun));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.KmESun, m_Km * m_ESun));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.Kr4PI, m_Kr4PI));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.Km4PI, m_Km4PI));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.Scale, m_fScale));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.ScaleDepth, m_fScaleDepth));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.ScaleOverScaleDepth, m_fScaleOverScaleDepth));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.nSamples, samples));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.FogDensity, fogDensity));
        mat.set(new Vector3Attribute(Vector3Attribute.FogColor, fogColor));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.G, m_g));

        mat.set(new Vector3Attribute(Vector3Attribute.PlanetPos, new Vector3()));
        mat.set(new Vector3Attribute(Vector3Attribute.CameraPos, new Vector3()));
        mat.set(new Vector3Attribute(Vector3Attribute.LightPos, new Vector3()));
        mat.set(new Vector3Attribute(Vector3Attribute.InvWavelength,
                                     new Vector3(1.0f / m_fWavelength4[0], 1.0f / m_fWavelength4[1], 1.0f / m_fWavelength4[2])));
    }

    public void removeAtmosphericScattering(Material mat) {
        mat.remove(AtmosphereAttribute.CameraHeight);
    }

    /**
     * Updates the atmospheric scattering shader parameters.
     *
     * @param mat           The material to update.
     * @param alpha         The opacity value.
     * @param ground        Whether it is the ground shader or the atmosphere.
     * @param graph         The graph node component.
     * @param rigidRotation The rotation component.
     * @param scaffolding   The model scaffolding component.
     * @param vrOffset      The VR offset vector.
     */
    public void updateAtmosphericScatteringParams(Material mat,
                                                  float alpha,
                                                  boolean ground,
                                                  GraphNode graph,
                                                  RotationComponent rigidRotation,
                                                  ModelScaffolding scaffolding,
                                                  Vector3D vrOffset) {
        Vector3Q parentTranslation = null;
        Entity parent = graph.parent;
        if (parent != null) {
            parentTranslation = Mapper.graph.get(parent).translation;
        }
        updateAtmosphericScatteringParams(mat,
                                          alpha,
                                          ground,
                                          graph.translation,
                                          rigidRotation,
                                          scaffolding.inverseRefPlaneTransform,
                                          parentTranslation,
                                          vrOffset);
    }

    /**
     * Updates the atmospheric scattering shader parameters.
     *
     * @param mat               The material to update.
     * @param alpha             The opacity value.
     * @param ground            Whether it is the ground shader or the atmosphere.
     * @param translation       The translation vector.
     * @param rc                The rotation component.
     * @param parentTranslation The parent translation vector.
     * @param vrOffset          The VR offset vector.
     */
    public void updateAtmosphericScatteringParams(Material mat,
                                                  float alpha,
                                                  boolean ground,
                                                  Vector3Q translation,
                                                  RotationComponent rc,
                                                  String inverseRefPlaneTransform,
                                                  Vector3Q parentTranslation,
                                                  Vector3D vrOffset) {


        translation.put(aux3);
        if (vrOffset != null) {
            aux1.set(vrOffset).scl(Constants.U_TO_M);
            aux3.sub(aux1);
        }

        // Normalization factor is inner radius.
        float normFactor = 2f / planetSize;
        // Normalize planet pos
        aux3.scl(normFactor);
        // Distance to planet
        float camHeight = (float) (aux3.len());
        float m_ESun = m_eSun;
        float camHeightGr = camHeight - m_fInnerRadius;
        float atmFactor = (m_fAtmosphereHeight - camHeightGr) / m_fAtmosphereHeight;

        if (!ground && camHeightGr < m_fAtmosphereHeight) {
            // Camera inside atmosphere
            m_ESun += atmFactor * 100f;
        }

        // These are here to get the desired effect inside the atmosphere
        if (mat.has(AtmosphereAttribute.KrESun))
            ((AtmosphereAttribute) Objects.requireNonNull(mat.get(AtmosphereAttribute.KrESun))).value = m_Kr * m_ESun;
        else
            mat.set(new AtmosphereAttribute(AtmosphereAttribute.KrESun, m_Kr * m_ESun));

        if (mat.has(AtmosphereAttribute.KmESun))
            ((AtmosphereAttribute) Objects.requireNonNull(mat.get(AtmosphereAttribute.KmESun))).value = m_Km * m_ESun;
        else
            mat.set(new AtmosphereAttribute(AtmosphereAttribute.KmESun, m_Km * m_ESun));

        // Camera height
        if (mat.has(AtmosphereAttribute.CameraHeight))
            ((AtmosphereAttribute) Objects.requireNonNull(mat.get(AtmosphereAttribute.CameraHeight))).value = camHeight;
        else
            mat.set(new AtmosphereAttribute(AtmosphereAttribute.CameraHeight, camHeight));

        // Planet position
        if (ground && rc != null) {
            // Camera position must be corrected using the rotation angle of the planet
            aux3.rotate(-rc.ascendingNode, 0, 1, 0)
                    .mul(Coordinates.getTransformD(inverseRefPlaneTransform))
                    .rotate(-rc.inclination - rc.axialTilt, 0, 0, 1)
                    .rotate(-rc.angle, 0, 1, 0);
        }
        ((Vector3Attribute) Objects.requireNonNull(mat.get(Vector3Attribute.PlanetPos))).value.set(aux3.put(aux));
        // CameraPos = -PlanetPos
        aux3.scl(-1f);

        ((Vector3Attribute) Objects.requireNonNull(mat.get(Vector3Attribute.CameraPos))).value.set(aux3.put(aux));

        // Light position respect the earth: LightPos = SunPos - EarthPos
        if (parentTranslation != null) {
            var tr = new Vector3Q(parentTranslation).scl(normFactor);
            aux3.add(tr);
        }
        aux3.nor();
        if (ground && rc != null) {
            // Camera position must be corrected using the rotation angle of the planet
            aux3.rotate(-rc.ascendingNode, 0, 1, 0)
                    .mul(Coordinates.getTransformD(inverseRefPlaneTransform))
                    .rotate(-rc.inclination - rc.axialTilt, 0, 0, 1)
                    .rotate(-rc.angle, 0, 1, 0);
        }
        ((Vector3Attribute) Objects.requireNonNull(mat.get(Vector3Attribute.LightPos))).value.set(aux3.put(aux));

        // Alpha value
        ((AtmosphereAttribute) Objects.requireNonNull(mat.get(AtmosphereAttribute.Alpha))).value = alpha;
        // Number of samples
        ((AtmosphereAttribute) Objects.requireNonNull(mat.get(AtmosphereAttribute.nSamples))).value = samples;
    }

    public void setQuality(Long quality) {
        this.quality = quality.intValue();
    }

    public void setSize(Double size) {
        this.size = (float) (size * Constants.KM_TO_U);
    }

    public void setMc(ModelComponent mc) {
        this.mc = mc;
    }

    public void setLocalTransform(Matrix4 localTransform) {
        this.localTransform = localTransform;
    }

    public void setFogColor(double[] fogColor) {
        this.fogColor.set((float) fogColor[0], (float) fogColor[1], (float) fogColor[2]);
    }

    public void setFogcolor(double[] fogColor) {
        setFogColor(fogColor);
    }

    public void setSamples(Long samples) {
        this.samples = samples.intValue();
    }

    public void setNumSamples(Long samples) {
        this.samples = samples.intValue();
    }

    public void setFogDensity(Double fogDensity) {
        this.fogDensity = MathUtils.clamp(fogDensity.floatValue(),
                                          Constants.MIN_ATM_FOG_DENSITY,
                                          Constants.MAX_ATM_FOG_DENSITY);
    }

    public void setFogdensity(Double fogDensity) {
        this.setFogDensity(fogDensity);
    }

    public void setM_Kr(Double m_Kr) {
        this.m_Kr = m_Kr.floatValue();
    }

    public void setM_Km(Double m_Km) {
        this.m_Km = m_Km.floatValue();
    }

    public void setM_eSun(Double m_eSun) {
        this.m_eSun = m_eSun.floatValue();
    }

    public double[] getWavelengths() {
        return wavelengths;
    }

    public void setWavelengths(double[] wavelengths) {
        this.wavelengths = wavelengths;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    /**
     * Creates a random atmosphere component using the given seed and the base
     * body size.
     *
     * @param seed       The seed to use.
     * @param bodyRadius The body size in internal units.
     */
    public void randomizeAll(long seed,
                             double bodyRadius) {
        Random rand = new Random(seed);
        // Size
        double bodyRadiusKm = bodyRadius * Constants.U_TO_KM;
        setSize(bodyRadiusKm + bodyRadiusKm * 0.029);
        // Wavelengths
        setWavelengths(new double[]{gaussian(rand, 0.6, 0.1), gaussian(rand, 0.54, 0.1), gaussian(rand, 0.45, 0.1)});
        // Kr
        setM_Kr(rand.nextDouble(0.002f, 0.0069f));
        // Km
        setM_Km(rand.nextDouble(0.001f, 0.0079f));
        // eSun
        setM_eSun(gaussian(rand, 7.0, 4.0, 3.0));
        // Fog density
        setFogdensity(gaussian(rand, 0.6, 0.3, 0.01));
        // Fog color
        setFogcolor(new double[]{0.5 + rand.nextDouble() * 0.5, 0.5 + rand.nextDouble() * 0.5, 0.5 + rand.nextDouble() * 0.5});
        // Samples
        setSamples((long) rand.nextInt(20, 24));
        // Params
        setParams(createModelParameters(200L, 2.0, true));
    }

    public void copyFrom(AtmosphereComponent other) {
        this.params = other.params;
        this.size = other.size;
        this.wavelengths = Arrays.copyOf(other.wavelengths, other.wavelengths.length);
        this.m_Km = other.m_Km;
        this.m_Kr = other.m_Kr;
        this.m_eSun = other.m_eSun;
        this.fogDensity = other.fogDensity;
        this.fogColor = new Vector3(other.fogColor);
        this.samples = other.samples;
    }

    public void print(Log log) {
        log.debug("Size: " + size);
        log.debug("Wavelengths: " + Arrays.toString(wavelengths));
        log.debug("eSun: " + m_eSun);
        log.debug("Fog density: " + fogDensity);
        log.debug("Fog color: " + fogColor);
        log.debug("Samples: " + samples);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void updateWith(AtmosphereComponent object) {
        copyFrom(object);
    }
}

