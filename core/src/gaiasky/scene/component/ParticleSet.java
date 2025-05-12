/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.TextureArray;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntSet;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.task.ParticleSetUpdaterTask;
import gaiasky.scene.view.FilterView;
import gaiasky.util.*;
import gaiasky.util.camera.Proximity;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.*;
import net.jafama.FastMath;
import uk.ac.starlink.table.ColumnInfo;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ParticleSet implements Component, IDisposable {

    private static long idSeq = 0;

    public static synchronized long getNextSequence() {
        return idSeq++;
    }

    // Auxiliary vectors.
    protected final Vector3b B31 = new Vector3b();
    protected final Vector3b B32 = new Vector3b();
    protected final Vector3D D31 = new Vector3D();
    protected final Vector3D D32 = new Vector3D();

    /**
     * List that contains the point data. It contains only [x y z].
     */
    public List<IParticleRecord> pointData;

    /**
     * List of {@link uk.ac.starlink.table.ColumnInfo} objects for the data in this set.
     */
    public List<ColumnInfo> columnInfoList;

    /**
     * This flag enables muting particle rendering.
     **/
    public boolean renderParticles = true;

    /**
     * Flag indicating whether the particle set holds stars or particles (extended or not).
     **/
    public boolean isStars;

    /**
     * Flag indicating whether the particle set holds extended particles.
     **/
    public boolean isExtended;

    /**
     * Whether to render the global set label or not.
     **/
    public boolean renderSetLabel = true;

    /**
     * Whether to render particle labels at all for this set.
     **/
    public boolean renderParticleLabels = true;

    /**
     * Number of labels to render for this group.
     **/
    public int numLabels = -1;

    /**
     * Fully qualified name of data provider class.
     */
    public String provider;

    /** Parameters for the data provider. **/
    public Map<String, Object> providerParams;

    /**
     * Path of data file.
     */
    public String datafile;

    /**
     * Model file to use (obj, g3db, g3dj, gltf, glb). If present, modelType and modelParams are ignored.
     * The model should have only positions (vector-3), normals (vector-3) and texture coordinates (vector-2) as vertex
     * attributes.
     * Only the first mesh of the model is used. Textures, lighting and material are ignored.
     */
    public String modelFile;

    /**
     * The loaded model pointed by modelFile.
     */
    public IntModel model;

    /**
     * Default model type to use for the particles of this set.
     * Typically, this should be set to quad, but allows for other model types.
     **/
    public String modelType = "quad";

    /**
     * Parameters for the model, in case 'modelType' is used.
     */
    public Map<String, Object> modelParams;

    /**
     * Render primitive. Triangles by default.
     */
    public int modelPrimitive = GL30.GL_TRIANGLES;

    /**
     * Proximity loading location, the location of the directory that contains descriptor JSON files with the
     * name of objects in the dataset. These get loaded whenever the camera gets close to a particle with the
     * given name.
     */
    public String proximityDescriptorsLocation;
    /** The path for proximity loading. **/
    public Path proximityDescriptorsPath;
    /** Threshold solid angle when loading happens. About 4 degrees (0.069 rad). **/
    public double proximityThreshold = 0.064;
    /** Flag that indicates whether this particle set has proximity loading. **/
    public boolean proximityLoadingFlag = false;
    /** Set that contains the indexes of the particles whose descriptors have already been loaded. **/
    public IntSet proximityLoaded;
    /** Contains the indices of the particles whose descriptors are not present. **/
    public IntSet proximityMissing;

    /**
     * Profile decay of the particles in the shader, when using quads.
     */
    public float profileDecay = 0.2f;

    /**
     * Noise factor for the color in [0,1].
     */
    public float colorNoise = 0;

    /**
     * Assign a different color to each texture. Only used when textures are active.
     */
    public boolean colorFromTexture = false;

    /**
     * Fixed angular size for all particles in this set, in radians. Applies only to quads. Negative to disable.
     */
    public double fixedAngularSize = -1;

    /**
     * Particle size limits. Applies to legacy point render (using GL_POINTS).
     */
    public double[] particleSizeLimitsPoint = new double[]{2d, 50d};

    /**
     * Particle size limits for the quad renderer (using quads as GL_TRIANGLES). This will be multiplied by
     * the distance to the particle in the shader, so that <code>size = tan(angle) * dist</code>.
     */
    public double[] particleSizeLimits = new double[]{Math.tan(Math.toRadians(0.07)), FastMath.tan(Math.toRadians(6.0))};

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

    public enum ShadingStyle {
        /** No special shading, only default color determination. **/
        DEFAULT,
        /** Apply random twinkling of particles in (app) time. **/
        TWINKLE
    }

    /** Shading style to use for this set. **/
    public ShadingStyle shadingStyle = ShadingStyle.DEFAULT;

    /**
     * Temporary storage for the mean position of this particle set, if it is given externally.
     * If this is set, the mean position is not computed from the positions of all the particles automatically.
     **/
    public Vector3D meanPosition;

    /**
     * Factor to apply to the data points, usually to normalise distances.
     */
    public Double factor = null;

    /**
     * Mapping colors.
     */
    public float[] ccMin = null, ccMax = null;

    /**
     * Particles for which forceLabel is enabled.
     **/
    public IntSet forceLabel;

    /**
     * Particles with special label colors.
     **/
    public IntMap<float[]> labelColors;

    /**
     * Stores the time when the last sort operation finished, in seconds.
     */
    public double lastSortTime;

    /**
     * The mean distance from the origin of all points in this group.
     * Gives a sense of the scale.
     */
    public double meanDistance;
    public double maxDistance, minDistance;

    /**
     * Epoch for positions/proper motions in julian days.
     **/
    public double epochJd;

    /**
     * Current computed epoch time.
     **/
    public double currDeltaYears = 0;

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
    public Vector3b focusPosition;

    /**
     * Position in equatorial coordinates of the current focus in radians.
     */
    public Vector2D focusPositionSph;

    /**
     * FOCUS_MODE attributes.
     */
    public double focusDistToCamera, focusSolidAngle, focusSolidAngleApparent, focusSize;

    /**
     * Proximity particles.
     */
    public Proximity proximity;
    // Has been disposed.
    public boolean disposed = false;
    // Name index.
    public FastObjectIntMap<String> index;
    private final Object indexSync = new Object();
    // Metadata, for sorting - holds distances from each particle to the camera, squared.
    public double[] metadata;
    // Indices list buffer.
    public int[] indices;

    /** Indices of stars that are not visible. **/
    public IntSet invisibilityArray;

    // Reference to the entity.
    public Entity entity;

    // Updater task.
    public ParticleSetUpdaterTask updaterTask;

    // Last sort position.
    public Vector3b lastSortCameraPos, cPosD;
    // Auxiliary matrix.
    protected final Matrix4D mat = new Matrix4D();

    public float[] getColorMin() {
        return ccMin;
    }

    public void setColorMin(double[] colorMin) {
        this.ccMin = GlobalResources.toFloatArray(colorMin);
    }

    public void setColorMin(float[] colorMin) {
        this.ccMin = colorMin;
    }

    public float[] getColorMax() {
        return ccMax;
    }

    public void setColorMax(double[] colorMax) {
        this.ccMax = GlobalResources.toFloatArray(colorMax);
    }

    public void setColorMax(float[] colorMax) {
        this.ccMax = colorMax;
    }

    public double getMeanDistance() {
        return meanDistance;
    }

    public double getMinDistance() {
        return minDistance;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    /**
     * Returns the list of particles.
     */
    public List<IParticleRecord> data() {
        return pointData;
    }

    public void setData(List<IParticleRecord> pointData) {
        setData(pointData, true);
    }

    public void setData(List<IParticleRecord> pointData,
                        boolean regenerateIndex) {
        this.pointData = pointData;

        // Regenerate index
        if (regenerateIndex)
            regenerateIndex();
        // Initialize visibility - all visible
        this.invisibilityArray = new IntSet();
    }

    public void setColumnInfoList(List<ColumnInfo> columnInfoList) {
        this.columnInfoList = columnInfoList;
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
                IParticleRecord pb = pointData.get(i);
                if (pb.names() != null) {
                    final int idx = i;
                    Arrays.stream(pb.names())
                            .forEach(name -> index.put(name.toLowerCase(), idx));
                }
            }
        }
        return index;
    }

    public void setMeanPosition(double[] pos) {
        this.meanPosition = new Vector3D(pos[0], pos[1], pos[2]);
    }

    public void setPosition(double[] pos) {
        setMeanPosition(pos);
    }

    public void setPosKm(double[] pos) {
        setPositionKm(pos);
    }

    public void setMeanPositionKm(double[] pos) {
        this.meanPosition = new Vector3D(pos[0] * Constants.KM_TO_U, pos[1] * Constants.KM_TO_U, pos[2] * Constants.KM_TO_U);
    }

    public void setPositionKm(double[] pos) {
        setMeanPositionKm(pos);
    }

    public void setPosPc(double[] pos) {
        setPositionPc(pos);
    }

    public void setMeanPositionPc(double[] pos) {
        this.meanPosition = new Vector3D(pos[0] * Constants.PC_TO_U, pos[1] * Constants.PC_TO_U, pos[2] * Constants.PC_TO_U);
    }

    public void setPositionPc(double[] pos) {
        setMeanPositionPc(pos);
    }

    public void setPosition(int[] pos) {
        setPosition(new double[]{pos[0], pos[1], pos[2]});
    }

    public void setDataFile(String dataFile) {
        this.datafile = dataFile;
    }

    public void setDatafile(String dataFile) {
        setDataFile(dataFile);
    }

    public void setProviderParams(Map<String, Object> params) {
        this.providerParams = params;
    }

    public void setProviderparams(Map<String, Object> params) {
        setProviderParams(params);
    }

    public void setFactor(Double factor) {
        this.factor = factor * Constants.DISTANCE_SCALE_FACTOR;
    }

    /**
     * @deprecated Use {@link #setProfileDecay(Double)}.
     */
    @Deprecated
    public void setProfiledecay(Double profiledecay) {
        setProfileDecay(profiledecay);
    }

    public void setProfileDecay(Double profileDecay) {
        this.profileDecay = profileDecay.floatValue();
    }

    /**
     * @deprecated Use {@link #setColorNoise(Double)}.
     */
    @Deprecated
    public void setColornoise(Double colorNoise) {
        setColorNoise(colorNoise);
    }

    public void setColorNoise(Double colorNoise) {
        this.colorNoise = colorNoise.floatValue();
    }

    public void setColorFromTexture(boolean colorFromTexture) {
        this.colorFromTexture = colorFromTexture;
    }

    /**
     * Set fixed angular size, in radians.
     */
    public void setFixedAngularSize(Double fixedAngularSize) {
        setFixedAngularSizeRad(fixedAngularSize);
    }

    /**
     * Set fixed angular size, in radians.
     */
    public void setFixedAngularSizeRad(Double fixedAngularSizeRad) {
        this.fixedAngularSize = fixedAngularSizeRad;
    }

    /**
     * Set fixed angular size, in degrees.
     */
    public void setFixedAngularSizeDeg(Double fixedAngularSizeDeg) {
        setFixedAngularSizeRad(Math.toRadians(fixedAngularSizeDeg));
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

    public void setShadingStyle(String style) {
        try {
            this.shadingStyle = ShadingStyle.valueOf(style.toUpperCase());
        } catch (IllegalArgumentException e) {
            Logger.getLogger(this.getClass()
                                     .getSimpleName())
                    .error("Error setting shading style: " + style, e);
        }
    }

    public void setRenderParticles(Boolean renderParticles) {
        this.renderParticles = renderParticles;
    }

    public void setRenderSetLabel(Boolean renderSetLabel) {
        this.renderSetLabel = renderSetLabel;
    }

    public void setExtended(Boolean extended) {
        this.isExtended = extended;
    }

    public void setNumLabels(Long numLabels) {
        this.numLabels = FastMath.toIntExact(numLabels);
    }

    public void updateNumLabelsValue(int n, Entity e) {
        int newNumLabels = FastMath.min(n, pointData.size());
        if (newNumLabels != numLabels) {
            setNumLabels((long) newNumLabels);
            // Update indices array with new length.
            indices = new int[newNumLabels];
            // Re-initialize updater task so that the new size is picked up.
            updaterTask = new ParticleSetUpdaterTask(e, this);
        }
    }

    public IParticleRecord get(int index) {
        return pointData.get(index);
    }

    /**
     * Gets the name of a random particle in this group
     *
     * @return The name of a random particle
     */
    public String getFirstParticleName() {
        if (pointData != null)
            for (IParticleRecord pb : pointData) {
                if (pb.names() != null && pb.names().length > 0)
                    return pb.names()[0];
            }
        return null;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public void setModelParams(Map<String, Object> params) {
        this.modelParams = params;
    }

    public void setModelPrimitive(String modelPrimitive) {
        this.modelPrimitive = switch (modelPrimitive) {
            case "GL_TRIANGLE_STRIP", "gl_triangle_strip" -> GL30.GL_TRIANGLE_STRIP;
            case "GL_LINES", "gl_lines" -> GL30.GL_LINES;
            case "GL_LINE_LOOP", "gl_line_loop" -> GL30.GL_LINE_LOOP;
            case "GL_LINE_STRIP", "gl_line_strip" -> GL30.GL_LINE_STRIP;
            default -> GL30.GL_TRIANGLES;
        };
    }

    public void setProximityDescriptorsLocation(String loc) {
        this.proximityDescriptorsLocation = loc;
    }

    public void setProximityDescriptors(String loc) {
        setProximityDescriptorsLocation(loc);
    }

    public void setDescriptorsLocation(String loc) {
        setProximityDescriptorsLocation(loc);
    }

    public void setProximityThreshold(Double thRadians) {
        this.proximityThreshold = thRadians;
    }

    public void setProximityThresholdRad(Double thRadians) {
        setProximityThreshold(thRadians);
    }

    public void setProximityThresholdDeg(Double thDegrees) {
        setProximityThreshold(Math.toRadians(thDegrees));
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

    /**
     * Default size if not in data, 1e5 km
     *
     * @return The size
     */
    public double getFocusSize() {
        return isStars || isExtended ? focus.size() : 1e5 * Constants.KM_TO_U;
    }

    public void setFocusIndex(String name) {
        synchronized (indexSync) {
            candidateFocusIndex = index.get(name.toLowerCase()
                                                             .trim());
        }
    }

    public IParticleRecord getCandidateBean() {
        if (candidateFocusIndex >= 0)
            return pointData.get(candidateFocusIndex);
        else
            return null;
    }

    // Radius in stars is different!
    public double getRadius() {
        return isStars ? getSize() * Constants.STAR_SIZE_FACTOR : getSize();
    }

    // Radius in stars is different!
    public double getRadius(int i) {
        return isStars ? getSize(i) * Constants.STAR_SIZE_FACTOR : isExtended ? getSize(i) : getRadius();
    }

    public double getTEff() {
        return isStars ? focus.tEff() : -1;
    }

    /**
     * Updates the parameters of the focus, if the focus is active in this group
     *
     * @param camera The current camera
     */
    public void updateFocus(ICamera camera) {
        IParticleRecord focus = pointData.get(focusIndex);
        Vector3b aux = this.fetchPosition(focus, cPosD, B31, currDeltaYears);
        this.focusPosition.set(aux)
                .add(camera.getPos());
        this.focusDistToCamera = aux.lenDouble();
        this.focusSize = getFocusSize();
        this.focusSolidAngle = (float) ((getRadius() / this.focusDistToCamera) / camera.getFovFactor());
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

    /**
     * Checks whether the particle with the given index is visible
     *
     * @param index The index of the particle
     *
     * @return The visibility of the particle
     */
    public boolean isVisible(int index) {
        return !invisibilityArray.contains(index);
    }

    public void setVisible(boolean visible,
                           String name,
                           Render render) {
        synchronized (indexSync) {
            if (index.containsKey(name)) {
                this.setVisible(index.get(name), visible, render);
            }
        }
    }

    /**
     * Sets the visibility of the particle with the given index. If the visibility
     * has changed, it marks the particle group for update.
     *
     * @param index   The index of the particle
     * @param visible Visibility flag
     */
    public void setVisible(int index,
                           boolean visible,
                           Render render) {
        if (index >= 0 && index < pointData.size()) {
            boolean changed;
            if (visible) {
                changed = this.invisibilityArray.remove(index);
            } else {
                changed = this.invisibilityArray.add(index);
            }
            if (changed) {
                markForUpdate(render);
            }
        }
    }

    public void markForUpdate(Render render) {
        if (render != null) {
            GaiaSky.postRunnable(() -> EventManager.publish(Event.GPU_DISPOSE_PARTICLE_GROUP, render));
        }
    }

    /**
     * Returns the position of the object with the given name at the given date, if any, in the out vector.
     *
     * @param date The date at which to get the position. If null, the position is given at the current simulation date.
     * @param out  The out vector.
     **/
    public Vector3b getAbsolutePosition(String name,
                                        Instant date,
                                        Vector3b out) {
        name = name.toLowerCase()
                .trim();
        synchronized (indexSync) {
            if (index.containsKey(name)) {
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
    public Vector3b getAbsolutePosition(IParticleRecord object,
                                        Instant date,
                                        Vector3b out) {
        if (object.hasProperMotion()) {
            double deltaYears = AstroUtils.getMsSince(date == null ? GaiaSky.instance.time.getTime() : date, epochJd) * Nature.MS_TO_Y;
            Vector3b aux = this.fetchPosition(object, null, B31, deltaYears);
            return out.set(aux);
        } else {
            return getAbsolutePosition(out);
        }
    }

    /**
     * Returns the current focus position at the given date, if any, in the out vector.
     *
     * @param date The date at which to get the position. If null, the position is given at the current simulation date.
     * @param out  The out vector.
     **/
    public Vector3b getAbsolutePosition(Instant date,
                                        Vector3b out) {
        IParticleRecord focus = pointData.get(focusIndex);
        return getAbsolutePosition(focus, date, out);
    }

    /**
     * Returns the current focus position, if any, in the out vector.
     **/
    public Vector3b getAbsolutePosition(Vector3b out) {
        if (entity != null && Mapper.affine.has(entity) && focusIndex >= 0 && focusIndex < pointData.size()) {
            IParticleRecord focus = pointData.get(focusIndex);
            return fetchPosition(focus, null, out, currDeltaYears);
        } else {
            return out.set(focusPosition);
        }
    }

    /**
     * Returns the absolute position of the particle with the given name.
     *
     * @param name The name.
     * @param out  The out vector.
     *
     * @return The absolute position in the out vector.
     */
    public Vector3b getAbsolutePosition(String name,
                                        Vector3b out) {
        name = name.toLowerCase()
                .trim();
        synchronized (indexSync) {
            if (index.containsKey(name)) {
                int idx = index.get(name);
                IParticleRecord pb = pointData.get(idx);
                fetchPosition(pb, null, out, currDeltaYears);
                return out;
            } else {
                return null;
            }
        }
    }

    /**
     * Returns the absolute position of the particle with the given name.
     *
     * @param name The name.
     * @param out  The out vector.
     *
     * @return The absolute position in the out vector.
     */
    public Vector3D getAbsolutePosition(String name,
                                        Vector3D out) {
        var result = getAbsolutePosition(name, B32);
        if (result != null) {
            out.set(result);
            return out;
        } else {
            return null;
        }

    }

    /**
     * Fetches the real position of the particle. It will apply the necessary
     * integrations (i.e. proper motion). Double-precision version.
     *
     * @param pb         The particle bean
     * @param camPos     The position of the camera. If null, the camera position is
     *                   not subtracted so that the coordinates are given in the global
     *                   reference system instead of the camera reference system.
     * @param out        The output vector
     * @param deltaYears The delta years
     *
     * @return The vector for chaining
     */
    public Vector3D fetchPositionDouble(IParticleRecord pb,
                                        Vector3b camPos,
                                        Vector3D out,
                                        double deltaYears) {
        Vector3D pm = D32.set(0, 0, 0);
        if (pb.hasProperMotion()) {
            pm.set(pb.vx(),
                   pb.vy(),
                   pb.vz())
                    .scl(deltaYears);
        }
        Vector3D destination = out.set(pb.x(), pb.y(), pb.z());
        // Apply affine transformations, if any.
        if (entity != null) {
            var affine = Mapper.affine.get(entity);
            if (affine != null && !affine.isEmpty()) {
                synchronized (mat) {
                    affine.apply(mat.idt());
                    destination.mul(mat);
                }
            }
        }
        if (camPos != null && !camPos.hasNaN())
            destination.sub(camPos)
                    .add(pm);
        else
            destination.add(pm);

        return destination;
    }

    /**
     * Fetches the real position of the particle. It will apply the necessary
     * integrations (i.e. proper motion). Arbitrary-precision version.
     *
     * @param pb         The particle bean.
     * @param camPos     The position of the camera. If null, the camera position is
     *                   not subtracted so that the coordinates are given in the global
     *                   reference system instead of the camera reference system.
     * @param out        The output vector.
     * @param deltaYears The delta years.
     *
     * @return The vector for chaining.
     */
    public Vector3b fetchPosition(IParticleRecord pb,
                                  Vector3b camPos,
                                  Vector3b out,
                                  double deltaYears) {
        Vector3D pm = D32;
        if (pb.hasProperMotion()) {
            pm.set(pb.vx(),
                   pb.vy(),
                   pb.vz())
                    .scl(deltaYears);
        } else {
            pm.set(0, 0, 0);
        }
        out.set(pb.x(), pb.y(), pb.z());
        // Apply affine transformations, if any.
        if (entity != null && Mapper.affine.has(entity)) {
            var affine = Mapper.affine.get(entity);
            if (affine != null && !affine.isEmpty()) {
                synchronized (mat) {
                    affine.apply(mat.idt());
                    out.mul(mat);
                }
            }
        }
        if (camPos != null && !camPos.hasNaN()) {
            out.sub(camPos);
        }
        if (!pm.isZero()) {
            out.add(pm);
        }

        return out;
    }

    /**
     * Sets the epoch to use for the stars in this set.
     *
     * @param epochJd The epoch in julian days (days since January 1, 4713 BCE).
     */
    public void setEpoch(Double epochJd) {
        setEpochJd(epochJd);
    }

    /**
     * Sets the epoch to use for the stars in this set.
     *
     * @param epochJd The epoch in julian days (days since January 1, 4713 BCE).
     */
    public void setEpochJd(Double epochJd) {
        this.epochJd = epochJd;
    }

    /**
     * Returns the delta years to integrate the proper motion.
     *
     * @return The current delta years.
     */
    public double getDeltaYears() {
        return currDeltaYears;
    }

    public long getId() {
        return 123L;
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
        String name = getName();
        if (name == null) {
            return null;
        }
        String base = name.toLowerCase(Locale.ROOT)
                .replace(' ', '_');
        if (I18n.hasObject(base)) {
            return I18n.obj(base);
        } else {
            return name;
        }
    }

    public long getCandidateId() {
        return pointData.get(candidateFocusIndex)
                .id();
    }

    public String getCandidateName() {
        return pointData.get(candidateFocusIndex)
                .names() != null ?
                pointData.get(candidateFocusIndex)
                        .names()[0] : getName();
    }

    public double getCandidateSolidAngleApparent() {
        return getSolidAngleApparent(candidateFocusIndex);
    }

    public double getSolidAngleApparent(int idx) {
        if (idx >= 0 && idx < pointData.size()) {
            IParticleRecord candidate = pointData.get(idx);
            Vector3D aux = candidate.pos(D31);
            ICamera camera = GaiaSky.instance.getICamera();
            float size = candidate.hasSize() ? candidate.size() : 0.5e2f;
            return (float) ((size / aux.sub(camera.getPos())
                    .len()) / camera.getFovFactor());
        } else {
            return -1;
        }
    }

    public String getClosestName() {
        return this.proximity.updating[0].name;
    }

    public boolean canSelect(FilterView view) {
        return candidateFocusIndex < 0 || candidateFocusIndex >= pointData.size() || view.filter(candidateFocusIndex);
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

    public boolean isWireframe() {
        return modelPrimitive >= GL30.GL_LINES && modelPrimitive <= GL30.GL_LINE_STRIP;
    }

    public boolean isBillboard() {
        return model == null && modelType.equalsIgnoreCase("quad");
    }

    public void setForceLabel(Boolean forceLabel,
                              String name) {
        name = name.toLowerCase()
                .trim();
        synchronized (indexSync) {
            if (index.containsKey(name)) {
                int idx = index.get(name);
                if (this.forceLabel.contains(idx)) {
                    if (!forceLabel) {
                        // Remove from forceLabelStars
                        this.forceLabel.remove(idx);
                    }
                } else if (forceLabel) {
                    // Add to forceLabelStars
                    this.forceLabel.add(idx);
                }
            }
        }
    }

    public boolean isForceLabel(String name) {
        name = name.toLowerCase()
                .trim();
        synchronized (indexSync) {
            if (index.containsKey(name)) {
                int idx = index.get(name);
                return forceLabel.contains(idx);
            }
        }
        return false;
    }

    public void setLabelColor(float[] color,
                              String name) {
        name = name.toLowerCase()
                .trim();
        synchronized (indexSync) {
            if (index.containsKey(name)) {
                int idx = index.get(name);
                labelColors.put(idx, color);
            }
        }
    }

    @Override
    public void dispose(Entity entity) {
        if (updaterTask != null) {
            updaterTask.dispose();
        }

        this.disposed = true;
        markForUpdate(Mapper.render.get(entity));
        // Data -> null, to be garbage collected.
        this.pointData = null;
    }
}
