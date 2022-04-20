/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.scenegraph.Planet;
import gaiasky.scenegraph.SceneGraphNode;
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
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

public class AtmosphereComponent extends NamedComponent {

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
    public float fogDensity = 1.0f;
    public Vector3 fogColor;
    public float m_eSun = 10f;

    // Model parameters
    public Map<String, Object> params;

    Vector3 aux;
    Vector3d aux3, aux1;

    public AtmosphereComponent() {
        localTransform = new Matrix4();
        mc = new ModelComponent(false);
        mc.initialize(null);
        aux = new Vector3();
        aux3 = new Vector3d();
        aux1 = new Vector3d();
        fogColor = new Vector3();
    }

    public void doneLoading(Material planetMat, float planetSize) {
        this.planetSize = planetSize;
        setUpAtmosphericScatteringMaterial(planetMat);

        Material atmMat;
        if(mc.instance == null) {
            Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Bits.indexes(Usage.Position, Usage.Normal), GL20.GL_TRIANGLES);
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

    public void update(Vector3b transform) {
        transform.getMatrix(localTransform).scl(size);
    }

    /**
     * Sets up the atmospheric scattering parameters to the given material
     *
     * @param mat The material to set up.
     */
    public void setUpAtmosphericScatteringMaterial(Material mat) {
        float camHeight = 1f;
        float m_Kr4PI = m_Kr * 4.0f * (float) Math.PI;
        float m_Km4PI = m_Km * 4.0f * (float) Math.PI;
        float m_ESun = m_eSun; // Sun brightness (almost) constant
        float m_g = 0.97f; // The Mie phase asymmetry factor
        m_fInnerRadius = planetSize / 2f;
        m_fOuterRadius = this.size;
        m_fAtmosphereHeight = m_fOuterRadius - m_fInnerRadius;
        float m_fScaleDepth = .20f;
        float m_fScale = 1.0f / (m_fAtmosphereHeight);
        float m_fScaleOverScaleDepth = m_fScale / m_fScaleDepth;
        int m_nSamples = 11;

        double[] m_fWavelength = wavelengths;
        float[] m_fWavelength4 = new float[3];
        m_fWavelength4[0] = (float) Math.pow(m_fWavelength[0], 4.0);
        m_fWavelength4[1] = (float) Math.pow(m_fWavelength[1], 4.0);
        m_fWavelength4[2] = (float) Math.pow(m_fWavelength[2], 4.0);

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

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.nSamples, m_nSamples));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.FogDensity, fogDensity));
        mat.set(new Vector3Attribute(Vector3Attribute.FogColor, fogColor));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.G, m_g));

        mat.set(new Vector3Attribute(Vector3Attribute.PlanetPos, new Vector3()));
        mat.set(new Vector3Attribute(Vector3Attribute.CameraPos, new Vector3()));
        mat.set(new Vector3Attribute(Vector3Attribute.LightPos, new Vector3()));
        mat.set(new Vector3Attribute(Vector3Attribute.InvWavelength, new Vector3(1.0f / m_fWavelength4[0], 1.0f / m_fWavelength4[1], 1.0f / m_fWavelength4[2])));
    }

    public void removeAtmosphericScattering(Material mat) {
        mat.remove(AtmosphereAttribute.CameraHeight);
    }

    /**
     * Updates the atmospheric scattering shader parameters
     *
     * @param mat    The material to update.
     * @param alpha  The opacity value.
     * @param ground Whether it is the ground shader or the atmosphere.
     * @param planet The planet itself, holder of this atmosphere
     */
    public void updateAtmosphericScatteringParams(Material mat, float alpha, boolean ground, Planet planet, Vector3d vrOffset) {
        Vector3b transform = planet.translation;
        RotationComponent rc = planet.rc;
        SceneGraphNode sol = planet.parent;
        transform.put(aux3);
        if (vrOffset != null) {
            aux1.set(vrOffset).scl(1 / Constants.M_TO_U);
            aux3.sub(aux1);
        }

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
            ((AtmosphereAttribute) mat.get(AtmosphereAttribute.KrESun)).value = m_Kr * m_ESun;
        else
            mat.set(new AtmosphereAttribute(AtmosphereAttribute.KrESun, m_Kr * m_ESun));

        if (mat.has(AtmosphereAttribute.KmESun))
            ((AtmosphereAttribute) mat.get(AtmosphereAttribute.KmESun)).value = m_Km * m_ESun;
        else
            mat.set(new AtmosphereAttribute(AtmosphereAttribute.KmESun, m_Km * m_ESun));

        // Camera height
        if (mat.has(AtmosphereAttribute.CameraHeight))
            ((AtmosphereAttribute) mat.get(AtmosphereAttribute.CameraHeight)).value = camHeight;
        else
            mat.set(new AtmosphereAttribute(AtmosphereAttribute.CameraHeight, camHeight));

        // Planet position
        if (ground) {
            // Camera position must be corrected using the rotation angle of the planet
            aux3.mul(Coordinates.getTransformD(planet.inverseRefPlaneTransform)).rotate(rc.ascendingNode, 0, 1, 0).rotate(-rc.inclination - rc.axialTilt, 0, 0, 1).rotate(-rc.angle, 0, 1, 0);
        }
        ((Vector3Attribute) mat.get(Vector3Attribute.PlanetPos)).value.set(aux3.put(aux));
        // CameraPos = -PlanetPos
        aux3.scl(-1f);

        ((Vector3Attribute) mat.get(Vector3Attribute.CameraPos)).value.set(aux3.put(aux));

        // Light position respect the earth: LightPos = SunPos - EarthPos
        aux3.add(sol.translation).nor();
        if (ground) {
            // Camera position must be corrected using the rotation angle of the planet
            aux3.mul(Coordinates.getTransformD(planet.inverseRefPlaneTransform)).rotate(rc.ascendingNode, 0, 1, 0).rotate(-rc.inclination - rc.axialTilt, 0, 0, 1).rotate(-rc.angle, 0, 1, 0);
        }
        ((Vector3Attribute) mat.get(Vector3Attribute.LightPos)).value.set(aux3.put(aux));

        // Alpha value
        ((AtmosphereAttribute) mat.get(AtmosphereAttribute.Alpha)).value = alpha;
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

    public void setWavelengths(double[] wavelengths) {
        this.wavelengths = wavelengths;
    }

    public void setFogcolor(double[] fogColor) {
        this.fogColor.set((float) fogColor[0], (float) fogColor[1], (float) fogColor[2]);
    }

    public void setFogdensity(Double fogDensity) {
        this.fogDensity = fogDensity.floatValue();
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

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    /**
     * Creates a random atmosphere component using the given seed and the base
     * body size.
     *
     * @param seed The seed to use.
     * @param size The body size in internal units.
     */
    public void randomizeAll(long seed, double size) {
        Random rand = new Random(seed);
        // Size
        double sizeKm = size * Constants.U_TO_KM;
        setSize(sizeKm + gaussian(rand, 120.0, 10.0, 100.0, 150.0));
        // Wavelengths
        setWavelengths(new double[] { gaussian(rand, 0.6, 0.1), gaussian(rand, 0.54, 0.1), gaussian(rand, 0.45, 0.1) });
        // Kr
        setM_Kr(0.0025);
        // Km
        setM_Km(0.0015);
        // eSun
        setM_eSun(gaussian(rand, 5.0, 4.0));
        // Fog density
        setFogdensity(gaussian(rand, 4.0, 1.0, 0.5));
        // Fog color
        setFogcolor(new double[] { 0.5 + rand.nextDouble() * 0.5, 0.5 + rand.nextDouble() * 0.5, 0.5 + rand.nextDouble() * 0.5 });
        // Params
        setParams(createModelParameters(600L, 1.0, true));
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
    }

    public void print(Log log){
        log.debug("Size: " + size);
        log.debug("Wavelengths: " + Arrays.toString(wavelengths));
        log.debug("eSun: " + m_eSun);
        log.debug("Fog density: " + fogDensity);
        log.debug("Fog color: " + fogColor);
    }

    @Override
    public void dispose() {
    }
}

