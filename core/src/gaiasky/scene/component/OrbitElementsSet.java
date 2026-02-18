/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.TextureArray;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.record.ParticleKepler;
import gaiasky.util.FastObjectIntMap;
import gaiasky.util.Logger;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.coord.KeplerianElements;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector2D;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import net.jafama.FastMath;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Aggregates children entities (typically orbits) so that they are treated as one,
 * especially in terms of GPU draw calls.
 */
public class OrbitElementsSet implements Component {
    // Auxiliary vectors.
    protected final Vector3Q B31 = new Vector3Q();
    protected final Vector3D D31 = new Vector3D();
    protected final Vector3D D32 = new Vector3D();

    public Array<Entity> alwaysUpdate;
    public boolean initialUpdate = true;

    /**
     * Particle data.
     */
    public List<IParticleRecord> pointData;

    /**
     * Reference to the current focus.
     */
    public IParticleRecord focus;

    /**
     * Index of the particle acting as focus. Negative if we have no focus here.
     */
    public int focusIndex = -1;

    /**
     * Candidate to focus.
     */
    public int candidateFocusIndex = -1;

    /**
     * Position of the current focus.
     */
    public Vector3Q focusPosition;

    /**
     * Position in equatorial coordinates of the current focus in radians.
     */
    public Vector2D focusPositionSph;

    /**
     * FOCUS_MODE attributes.
     */
    public double focusDistToCamera, focusSolidAngle, focusSolidAngleApparent, focusSize;

    /** Name to array index. **/
    public FastObjectIntMap<String> index;
    private final Object indexSync = new Object();
    /**
     * Profile decay of the particles in the shader, when using quads and plain {@link ParticleSet.ShadingType}.
     */
    public float profileDecay = 6.5f;

    /**
     * Noise factor for the color in [0,1].
     */
    public float colorNoise = 0;

    /**
     * Fully qualified name of data provider class.
     */
    public String provider;

    /**
     * Pointer to the data file.
     */
    public String datafile;

    /**
     * The noise in the size determination for particles. This only has effect if particles themselves do not provide a size.
     * In that case, the final size is computed as <code>clamp(body.size + rand.gaussian() * body.size * sizeNoise)</code>.
     */
    public double sizeNoise = 1.0;

    /**
     * Particle size limits for the quad renderer (using quads as GL_TRIANGLES). This will be multiplied by
     * the distance to the particle in the shader, so that <code>size = tan(angle) * dist</code>.
     */
    public double[] particleSizeLimits = new double[]{Math.tan(Math.toRadians(0.02)), FastMath.tan(Math.toRadians(20.0))};

    /**
     * The texture attribute is an attribute in the original table that points to the texture to use. Ideally, it should
     * be an integer value from 1 to n, where n is the number of textures.
     */
    public String textureAttribute;

    /**
     * Texture files to use for rendering the particles, at random. Applies only to quads.
     **/
    public String[] textureFiles = null;

    /**
     * Reference to the texture array containing the textures for this set, if any. Applies only to quads.
     **/
    public TextureArray textureArray;

    /** The shading type, for lighting the particles. **/
    public ParticleSet.ShadingType shadingType = ParticleSet.ShadingType.PLAIN;
    /** Controls how sharply the sphere edges darken (higher values = more pronounced sphere appearance with darker edges). **/
    public float sphericalPower = 3f;

    public void markForUpdate(Render render) {
        EventManager.publish(Event.GPU_DISPOSE_ORBITAL_ELEMENTS, render);
    }

    /**
     * Returns the list of particles.
     */
    public List<IParticleRecord> data() {
        return pointData;
    }

    public void setDataFile(String dataFile) {
        this.datafile = dataFile;
    }

    public void setData(List<IParticleRecord> pointData,
                        boolean regenerateIndex) {
        this.pointData = pointData;

        // Regenerate index
        if (regenerateIndex)
            regenerateIndex();
    }

    public void setPointData(List<IParticleRecord> pointData) {
        setData(pointData, true);
    }

    public void setProfileDecay(Double profileDecay) {
        this.profileDecay = profileDecay.floatValue();
    }

    /**
     * @deprecated Use {@link #setProfileDecay(Double)}.
     */
    @Deprecated
    public void setProfiledecay(Double profiledecay) {
        setProfileDecay(profiledecay);
    }

    public void setColorNoise(Double colorNoise) {
        this.colorNoise = colorNoise.floatValue();
    }

    public void setColornoise(Double colorNoise) {
        this.setColorNoise(colorNoise);
    }

    /**
     * Set the size noise.
     */
    public void setSizeNoise(Double sizeNoise) {
        this.sizeNoise = sizeNoise;
    }

    public void setParticleSizeLimitsDeg(double[] sizeLimits) {
        sizeLimits[0] = MathUtilsDouble.clamp(sizeLimits[0], 0, 90.0);
        sizeLimits[1] = MathUtilsDouble.clamp(sizeLimits[1], 0, 90.0);
        if (sizeLimits[0] > sizeLimits[1])
            sizeLimits[0] = sizeLimits[1];

        sizeLimits[0] = FastMath.toRadians(sizeLimits[0]);
        sizeLimits[1] = FastMath.toRadians(sizeLimits[1]);
        this.particleSizeLimits = sizeLimits;
    }

    public void setParticleSizeLimits(double[] sizeLimits) {
        sizeLimits[0] = MathUtilsDouble.clamp(sizeLimits[0], 0, 1.57);
        sizeLimits[1] = MathUtilsDouble.clamp(sizeLimits[1], 0, 1.57);
        if (sizeLimits[0] > sizeLimits[1])
            sizeLimits[0] = sizeLimits[1];
        this.particleSizeLimits = sizeLimits;
    }

    public void setParticlesizelimits(double[] sizeLimits) {
        setParticleSizeLimits(sizeLimits);
    }

    public void setTextureAttribute(String textureAttribute) {
        this.textureAttribute = textureAttribute;
    }

    public void setTexture(String texture) {
        this.textureFiles = new String[]{texture};
    }

    public void setTextures(String[] textures) {
        this.textureFiles = textures;
    }

    public void setShadingType(int type) {
        type = MathUtils.clamp(type, 0, ParticleSet.ShadingType.values().length - 1);
        this.shadingType = ParticleSet.ShadingType.values()[type];
    }

    public void setShadingType(String type) {
        try {
            this.shadingType = ParticleSet.ShadingType.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Logger.getLogger(this.getClass()
                                     .getSimpleName())
                    .error("Error setting shading type: " + type, e);
        }
    }

    public void setSphericalPower(Double power) {
        this.sphericalPower = (float) MathUtilsDouble.clamp(power, 0.1, 20.0);
    }


    /**
     * Regenerates the name index
     */
    public void regenerateIndex() {
        index = generateIndex(data());
    }

    /**
     * Generates the index (maps name to array index)
     * and computes the geometric center of this group
     *
     * @param pointData The data
     *
     * @return An map{string,int} mapping names to indices
     */
    public FastObjectIntMap<String> generateIndex(List<IParticleRecord> pointData) {
        FastObjectIntMap<String> index = new FastObjectIntMap<>((int) (pointData.size() * 1.25f), String.class);
        synchronized (indexSync) {
            int n = pointData.size();
            for (int i = 0; i < n; i++) {
                ParticleKepler pb = (ParticleKepler) pointData.get(i);
                if (pb.name() != null) {
                    final int idx = i;
                    var name = pb.name();
                    var nlc = name.toLowerCase(Locale.ROOT);
                    index.put(nlc, idx);
                    if (I18n.hasLocalizedVersion(nlc)) {
                        var loc = I18n.localize(nlc);
                        if (!loc.equals(nlc)) {
                            // We still use root locale here, by design.
                            index.put(I18n.localize(nlc).toLowerCase(Locale.ROOT), idx);
                        }
                    }
                }
            }
        }
        return index;
    }

    public void setFocusIndex(String name) {
        synchronized (indexSync) {
            if (index != null) {
                candidateFocusIndex = index.get(name.toLowerCase(Locale.ROOT)
                                                        .trim());
            }
        }
    }
    public long getId() {
        if (focus != null)
            return focus.id();
        else
            return -1;
    }

    public String getName() {
        if (focus != null && focus.names() != null)
            return focus.names()[0];
        return null;
    }

    public String[] getNames() {
        if (focus != null && focus.names() != null)
            return focus.names();
        return null;
    }

    public boolean hasName(String candidate) {
        return hasName(candidate, false);
    }

    public boolean hasName(String candidate, boolean matchCase) {
        if (focus == null || focus.names() == null) {
            return false;
        } else {
            var names = focus.names();
            for (String name : names) {
                if (matchCase) {
                    if (name.equals(candidate))
                        return true;
                } else {
                    if (name.equalsIgnoreCase(candidate))
                        return true;
                }
            }
        }
        return false;
    }

    public String getLocalizedName() {
        return I18n.localize(getName());
    }

    public long getCandidateId() {
        return candidateFocusIndex >= 0 ? pointData.get(candidateFocusIndex).id() : -1L;
    }

    public String getCandidateName() {
        if (candidateFocusIndex >= 0) {
            return pointData.get(candidateFocusIndex)
                    .names() != null ?
                    pointData.get(candidateFocusIndex)
                            .names()[0] : getName();
        } else {
            return null;
        }
    }

    public Vector2D getPosSph() {
        return focusPositionSph;
    }

    public double getAlpha() {
        return focusPositionSph.x;
    }

    public double getDelta() {
        return focusPositionSph.y;
    }

    public double getDistToCamera() {
        return focusDistToCamera;
    }

    public double getSolidAngle() {
        return focusSolidAngle;
    }

    // FOCUS_MODE apparent view angle
    public double getSolidAngleApparent() {
        return focusSolidAngleApparent;
    }

    public Vector3D fetchPositionDouble(IParticleRecord p, Vector3Q camPos, Vector3D out, Instant t) {
        if (p instanceof ParticleKepler k) {
            var dtDays = AstroUtils.getDaysSince(t, k.epoch());
            KeplerianElements.keplerianToCartesianTime(out,
                                                       dtDays,
                                                       k.period(),
                                                       k.inclination(),
                                                       k.eccentricity(),
                                                       k.ascendingNode(),
                                                       k.argOfPericenter(),
                                                       k.semiMajorAxis(),
                                                       k.meanAnomaly());
            if (camPos != null && !camPos.hasNaN()) {
                out.sub(camPos);
            }
        }
        return out;

    }

    /**
     * Returns the current focus position at the given date, if any, in the out vector.
     *
     * @param date The date at which to get the position. If null, the position is given at the current simulation date.
     * @param out  The out vector.
     **/
    public Vector3Q getAbsolutePosition(Instant date,
                                        Vector3Q out) {
        IParticleRecord focus = pointData.get(focusIndex);
        return getAbsolutePosition(focus, date, out);
    }

    /**
     * Returns the current focus position, if any, in the out vector.
     **/
    public Vector3Q getAbsolutePosition(Vector3Q out) {
        if (focusIndex >= 0 && focusIndex < pointData.size()) {
            IParticleRecord focus = pointData.get(focusIndex);
            return out.set(fetchPositionDouble(focus, null, D31, GaiaSky.instance.time.getTime()));
        } else {
            return out.set(focusPosition);
        }
    }

    /**
     * Returns the position of the object with the given name at the given date, if any, in the out vector.
     *
     * @param date The date at which to get the position. If null, the position is given at the current simulation date.
     * @param out  The out vector.
     **/
    public Vector3Q getAbsolutePosition(String name,
                                        Instant date,
                                        Vector3Q out) {
        name = name.toLowerCase(Locale.ROOT)
                .trim();
        synchronized (indexSync) {
            if (index != null && index.containsKey(name)) {
                int idx = index.get(name);
                if (idx >= 0 && idx < pointData.size()) {
                    IParticleRecord pb = pointData.get(idx);
                    return getAbsolutePosition(pb, date, out);
                }
            }
        }
        return null;
    }

    /**
     * Returns the position of the given object at the given date, if any, in the out vector.
     *
     * @param object The particle record object.
     * @param date   The date at which to get the position. If null, the position is given at the current simulation
     *               date.
     * @param out    The out vector.
     **/
    public Vector3Q getAbsolutePosition(IParticleRecord object,
                                        Instant date,
                                        Vector3Q out) {
            Vector3D aux = this.fetchPositionDouble(object, null, D31, date);
            return out.set(aux);
    }

    // FOCUS_MODE size
    public double getSize() {
        return focusSize;
    }

    /**
     * Returns the size of the particle at index i
     *
     * @param i The index
     *
     * @return The size
     */
    public double getSize(int i) {
        return pointData.get(i)
                .size();
    }

    // Radius in stars is different!
    public double getRadius() {
        return getSize();
    }

    public double getRadius(int i) {
        return getSize(i);
    }

    /**
     * Default size if not in data, 1e5 km
     *
     * @return The size
     */
    public double getFocusSize() {
        return focusSize;
    }

    /**
     * Updates the parameters of the focus, if the focus is active in this group
     *
     * @param camera The current camera
     */
    public void updateFocus(ICamera camera) {
        IParticleRecord focus = pointData.get(focusIndex);
        Vector3D aux = this.fetchPositionDouble(focus, camera.getPos(), D31, GaiaSky.instance.time.getTime());
        this.focusPosition.set(aux)
                .add(camera.getPos());
        this.focusSize = getSize(focusIndex);
        this.focusDistToCamera = aux.len() - this.focusSize;
        this.focusSolidAngle = (float) ((getRadius(focusIndex) / this.focusDistToCamera) / camera.getFovFactor());
        this.focusSolidAngleApparent = this.focusSolidAngle;
    }

    public void updateFocusDataPos() {
        if (focusIndex < 0) {
            focus = null;
        } else {
            focus = pointData.get(focusIndex);
            updateFocus(GaiaSky.instance.getICamera());
            Vector3D posSph = Coordinates.cartesianToSpherical(focusPosition, D31);
            focusPositionSph.set((float) (MathUtilsDouble.radDeg * posSph.x), (float) (MathUtilsDouble.radDeg * posSph.y));
        }
    }

    public void setFocusIndex(int index) {
        if (index >= 0 && index < pointData.size()) {
            candidateFocusIndex = index;
            makeFocus();
        }
    }

    public void makeFocus() {
        focusIndex = candidateFocusIndex;
        updateFocusDataPos();
    }
}
