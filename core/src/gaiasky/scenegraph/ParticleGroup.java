/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.data.group.DatasetOptions;
import gaiasky.data.group.IParticleGroupDataProvider;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.I3DTextRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.scenegraph.component.RotationComponent;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.*;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.camera.Proximity;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.filter.attrib.IAttribute;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.*;
import gaiasky.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This class represents a group of non-focusable particles, all with the same
 * luminosity. The contents of this group will be sent once to GPU memory and
 * stay there, so all particles get rendered directly in the GPU from the GPU
 * with no CPU intervention. This allows for much faster rendering. Use this for
 * large groups of particles.
 */
public class ParticleGroup extends FadeNode implements I3DTextRenderable, IFocus, IObserver {

    // Sequence id
    private static long idSeq = 0;

    /**
     * List that contains the point data. It contains only [x y z]
     */
    protected List<IParticleRecord> pointData;

    /**
     * Fully qualified name of data provider class
     */
    protected String provider;

    /**
     * Path of data file
     */
    protected String datafile;

    // Parameters for the data provider
    protected Map<String, Object> providerParams;

    /**
     * Profile decay of the particles in the shader
     */
    public float profileDecay = 0.2f;

    /**
     * Noise factor for the color in [0,1]
     */
    public float colorNoise = 0;

    /**
     * Particle size limits
     */
    public double[] particleSizeLimitsPoint = new double[] { 2d, 50d };
    /**
     * Particle size limits for the GL_TRIANGLES renderer. This will be multiplied by
     * the distance to the particle in the shader, so that <code>size = tan(angle) * dist</code>
     */
    public double[] particleSizeLimits = new double[] { Math.tan(Math.toRadians(0.1)), Math.tan(Math.toRadians(6.0)) };

    /**
     * This flag indicates whether the mean position is already given by the
     * JSON injector
     */
    protected boolean fixedMeanPosition = false;

    /**
     * Factor to apply to the data points, usually to normalise distances
     */
    protected Double factor = null;

    /**
     * Index of the particle acting as focus. Negative if we have no focus here.
     */
    int focusIndex;

    /**
     * Candidate to focus. Will be used in {@link #makeFocus()}
     */
    int candidateFocusIndex;

    /**
     * Position of the current focus
     */
    Vector3d focusPosition;

    /**
     * Position in equatorial coordinates of the current focus in radians
     */
    Vector2d focusPositionSph;

    /**
     * FOCUS_MODE attributes
     */
    double focusDistToCamera, focusViewAngle, focusViewAngleApparent, focusSize;

    /**
     * Mapping colors
     */
    protected float[] ccMin = null, ccMax = null;

    /**
     * Stores the time when the last sort operation finished, in ms
     */
    protected long lastSortTime;

    /**
     * The mean distance from the origin of all points in this group.
     * Gives a sense of the scale.
     */
    protected double meanDistance;
    protected double maxDistance, minDistance;

    /**
     * Geometric centre at epoch, for render sorting
     */
    private static Vector3d geomCentre;

    /**
     * Reference to the current focus
     */
    protected IParticleRecord focus;

    /**
     * Proximity particles
     */
    protected Proximity proximity;

    // Has been disposed
    public boolean disposed = false;

    // Name index
    protected Map<String, Integer> index;

    // Minimum amount of time [ms] between two update calls
    protected static final double UPDATE_INTERVAL_MS = 1500;

    // Metadata, for sorting - holds distances from each particle to the camera, squared
    protected double[] metadata;

    // Comparator
    private final Comparator<Integer> comp;

    // Indices list buffer 1
    protected Integer[] indices1;
    // Indices list buffer 2
    protected Integer[] indices2;
    // Active indices list
    protected Integer[] active;
    // Background indices list (the one we sort)
    protected Integer[] background;

    // Visibility array with 1 (visible) or 0 (hidden) for each particle
    protected byte[] visibilityArray;

    // Is it updating?
    protected volatile boolean updating = false;

    // Updater task
    protected UpdaterTask updaterTask;

    // Camera dx threshold
    protected static final double CAM_DX_TH = 100 * Constants.PC_TO_U;
    // Last sort position
    protected Vector3d lastSortCameraPos, cPosD;

    /**
     * User order in metadata arrays to compare indices
     */
    private class ParticleGroupComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer i1, Integer i2) {
            return Double.compare(metadata[i1], metadata[i2]);
        }
    }

    // Updates the group
    public static class UpdaterTask implements Runnable {

        private final ParticleGroup pg;

        public UpdaterTask(ParticleGroup pg) {
            this.pg = pg;
        }

        @Override
        public void run() {
            pg.updateSorter(GaiaSky.instance.time, GaiaSky.instance.getICamera());
        }

    }

    public ParticleGroup() {
        super();
        id = idSeq++;
        focusIndex = -1;
        proximity = new Proximity(Constants.N_DIR_LIGHTS);
        focusPosition = new Vector3d();
        focusPositionSph = new Vector2d();
        cPosD = new Vector3d();
        lastSortCameraPos = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        this.comp = new ParticleGroupComparator();
        EventManager.instance.subscribe(this, Event.FOCUS_CHANGED, Event.CAMERA_MOTION_UPDATE);
    }

    public void initialize() {
        initialize(true, true);
    }

    public void initialize(boolean dataLoad, boolean createCatalogInfo) {
        // Load data
        try {
            if (factor == null)
                factor = 1d;

            lastSortTime = -1;

            if (dataLoad) {
                Class<?> clazz = Class.forName(provider);
                IParticleGroupDataProvider provider = (IParticleGroupDataProvider) clazz.getConstructor().newInstance();
                provider.setProviderParams(providerParams);

                setData(provider.loadData(datafile, factor));
            }

            computeMinMeanMaxDistances();
            computeMeanPosition();
            setLabelPosition();

            if (createCatalogInfo) {
                // Create catalog info and broadcast
                CatalogInfo ci = new CatalogInfo(names[0], names[0], null, CatalogInfoSource.INTERNAL, 1f, this);
                ci.nParticles = pointData != null ? pointData.size() : -1;
                Path df = Path.of(Settings.settings.data.dataFile(datafile));
                ci.sizeBytes = Files.exists(df) && Files.isRegularFile(df) ? df.toFile().length() : -1;

                // Insert
                EventManager.publish(Event.CATALOG_ADD, this, ci, false);
            }

        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
            pointData = null;
        }
    }

    public void computeMinMeanMaxDistances() {
        meanDistance = 0;
        maxDistance = Double.MIN_VALUE;
        minDistance = Double.MAX_VALUE;
        List<Double> distances = new ArrayList<>();
        for (IParticleRecord point : pointData) {
            // Add sample to mean distance
            double dist = len(point.x(), point.y(), point.z());
            if (Double.isFinite(dist)) {
                distances.add(dist);
                maxDistance = Math.max(maxDistance, dist);
                minDistance = Math.min(minDistance, dist);
            }
        }
        // Mean is computed as half of the 90th percentile to avoid outliers
        distances.sort(Double::compare);
        int idx = (int) Math.ceil((90d / 100d) * (double) distances.size());
        meanDistance = distances.get(idx - 1) / 2d;
    }

    public void computeMeanPosition() {
        if (!fixedMeanPosition) {
            // Mean position
            for (IParticleRecord point : data()) {
                pos.add(point.x(), point.y(), point.z());
            }
            pos.scl(1d / pointData.size());
        }
    }

    public void setLabelPosition() {
        // Label position
        if (labelPosition == null)
            labelPosition = new Vector3b(pos);
    }

    private double len(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
    }

    protected void initSortingData() {
        // Metadata
        metadata = new double[pointData.size()];

        // Initialise indices list with natural order
        indices1 = new Integer[pointData.size()];
        indices2 = new Integer[pointData.size()];
        for (int i = 0; i < pointData.size(); i++) {
            indices1[i] = i;
            indices2[i] = i;
        }
        active = indices1;
        background = indices2;

        // Initialize updater task
        updaterTask = new UpdaterTask(this);
    }

    /**
     * Returns the data list
     *
     * @return The data list
     */
    public List<IParticleRecord> data() {
        return pointData;
    }

    public void setData(List<IParticleRecord> pointData) {
        setData(pointData, true);
    }

    public void setData(List<IParticleRecord> pointData, boolean regenerateIndex) {
        this.pointData = pointData;

        // Regenerate index
        if (regenerateIndex)
            regenerateIndex();
        // Initialize visibility - all visible
        this.visibilityArray = new byte[pointData.size()];
        for (int i = 0; i < pointData.size(); i++) {
            this.visibilityArray[i] = (byte) 1;
        }
    }

    /**
     * Checks whether the particle with the given index is visible
     *
     * @param index The index of the particle
     *
     * @return The visibility of the particle
     */
    public boolean isVisible(int index) {
        return this.visibilityArray != null && this.visibilityArray[index] != (byte) 0;
    }

    public boolean isVisible() {
        if (focusIndex < 0)
            return false;

        return isVisible(focusIndex);
    }

    /**
     * Overrides the isVisible() method, and uses the current focus index, if any
     *
     * @param attributeValue Whether to use the visibility attribute directly
     *
     * @return The visibility
     */
    public boolean isVisible(boolean attributeValue) {
        if (focusIndex < 0)
            return false;

        return this.isVisible();
    }

    /**
     * Sets the visibility of the particle with the given index. If the visibility
     * has changed, it marks the particle group for update.
     *
     * @param index   The index of the particle
     * @param visible Visibility flag
     */
    public void setVisible(int index, boolean visible) {
        boolean previousVisibility = this.visibilityArray[index] != 0;
        this.visibilityArray[index] = (byte) (visible ? 1 : 0);
        if (previousVisibility != visible) {
            this.disposeGpuMesh();
        }
    }

    public void setVisible(boolean visible, String name) {
        if (index.containsKey(name)) {
            this.setVisible(index.get(name), visible);
        }
    }

    public boolean isVisible(String name) {
        if (index.containsKey(name)) {
            return this.isVisible(index.get(name));
        }
        return false;
    }

    /**
     * Overrides the setVisible() method and uses the current focus index, if any.
     *
     * @param visible The visibility state
     */
    public void setVisible(boolean visible) {
        if (focusIndex >= 0) {
            setVisible(focusIndex, visible);
        }
    }

    public boolean isVisibleGroup() {
        return super.isVisible();
    }

    public boolean isVisibleGroup(boolean attributeValue) {
        return super.isVisible(attributeValue);
    }

    public void setVisibleGroup(boolean visibility) {
        super.setVisible(visibility);
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
    public Map<String, Integer> generateIndex(List<IParticleRecord> pointData) {
        Map<String, Integer> index = new HashMap<>((int) (pointData.size() * 1.25));
        int n = pointData.size();
        for (int i = 0; i < n; i++) {
            IParticleRecord pb = pointData.get(i);
            if (pb.names() != null) {
                final int idx = i;
                Arrays.stream(pb.names()).forEach(name -> index.put(name.toLowerCase(), idx));
            }
        }
        return index;
    }

    @Override
    protected void addToIndex(Map<String, SceneGraphNode> map) {
        if (index != null) {
            Set<String> keys = index.keySet();
            for (String key : keys) {
                map.put(key, this);
            }
        }
    }

    @Override
    protected void removeFromIndex(Map<String, SceneGraphNode> map) {
        if (index != null) {
            Set<String> keys = index.keySet();
            for (String key : keys) {
                map.remove(key);
            }
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
    public String getRandomParticleName() {
        if (pointData != null)
            for (IParticleRecord pb : pointData) {
                if (pb.names() != null && pb.names().length > 0)
                    return pb.names()[0];
            }
        return null;
    }

    /**
     * Computes the geometric centre of this data cloud
     */
    public Vector3d computeGeomCentre() {
        return computeGeomCentre(false);
    }

    /**
     * Computes the geometric centre of this data cloud
     *
     * @param forceRecompute Recomputes the geometric centre even if it has been already computed
     */
    public Vector3d computeGeomCentre(boolean forceRecompute) {
        if (pointData != null && (forceRecompute || geomCentre == null)) {
            geomCentre = new Vector3d(0, 0, 0);
            int n = pointData.size();
            for (IParticleRecord pb : pointData) {
                geomCentre.add(pb.x(), pb.y(), pb.z());
            }
            geomCentre.scl(1d / (double) n);
        }
        return geomCentre;
    }

    /**
     * Number of objects of this group
     *
     * @return The number of objects
     */
    public int size() {
        return pointData.size();
    }

    public void update(ITimeFrameProvider time, final Vector3b parentTransform, ICamera camera, float opacity) {
        if (pointData != null) {
            cPosD.set(camera.getPos());
            this.opacity = opacity;
            super.update(time, parentTransform, camera, opacity);

            if (focusIndex >= 0) {
                updateFocus(camera);
            }
        }
    }

    @Override
    public void update(ITimeFrameProvider time, Vector3b parentTransform, ICamera camera) {
        update(time, parentTransform, camera, 1f);
    }

    /**
     * Updates the parameters of the focus, if the focus is active in this group
     *
     * @param camera The current camera
     */
    public void updateFocus(ICamera camera) {
        Vector3d aux = aux3d1.get().set(this.focusPosition);
        this.focusDistToCamera = aux.sub(camera.getPos()).len();
        this.focusSize = getFocusSize();
        this.focusViewAngle = (float) ((getRadius() / this.focusDistToCamera) / camera.getFovFactor());
        this.focusViewAngleApparent = this.focusViewAngle * Settings.settings.scene.star.brightness;
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender()) {
            addToRender(this, RenderGroup.PARTICLE_GROUP);

            if (renderText()) {
                addToRender(this, RenderGroup.FONT_LABEL);
            }
        }
    }

    /**
     * Label rendering
     */
    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        // Dataset label
        Vector3d pos = aux3d1.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thOverFactor", 1f);
        shader.setUniformf("u_thOverFactorScl", 1f);
        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, pos.len(), textScale() * 2f * camera.getFovFactor(), textSize() * camera.getFovFactor(), this.forceLabel);

        // Particle labels
        if (active != null) {
            float thOverFactor = 1e-15f;
            for (int i = 0; i < Math.min(50, pointData.size()); i++) {
                IParticleRecord pb = pointData.get(active[i]);
                if (pb.names() != null) {
                    Vector3d lpos = fetchPosition(pb, cPosD, aux3d1.get(), 0);
                    float distToCamera = (float) lpos.len();
                    float viewAngle = 1e-4f / camera.getFovFactor();

                    textPosition(camera, lpos.put(aux3d1.get()), distToCamera, 0);

                    shader.setUniformf("u_viewAngle", viewAngle);
                    shader.setUniformf("u_viewAnglePow", 1f);
                    shader.setUniformf("u_thOverFactor", thOverFactor);
                    shader.setUniformf("u_thOverFactorScl", camera.getFovFactor());
                    float textSize = (float) FastMath.tanh(viewAngle) * distToCamera * 1e5f;
                    float alpha = Math.min((float) FastMath.atan(textSize / distToCamera), 1.e-3f);
                    textSize = (float) FastMath.tan(alpha) * distToCamera * 0.5f;
                    render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, pb.names()[0], lpos.put(aux3d1.get()), distToCamera, textScale() * camera.getFovFactor(), textSize * camera.getFovFactor(), this.forceLabel);
                }
            }
        }
    }

    public void textPosition(ICamera cam, Vector3d out, double len, double rad) {
        out.clamp(0, len - rad);

        Vector3d aux = aux3d2.get();
        aux.set(cam.getUp());

        aux.crs(out).nor();

        float dist = -0.02f * cam.getFovFactor() * (float) out.len();

        aux.add(cam.getUp()).nor().scl(dist);

        out.add(aux);

        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    @Override
    public float textScale() {
        return .5f / Settings.settings.scene.label.size;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider.replace("gaia.cu9.ari.gaiaorbit", "gaiasky");
    }

    public void setDatafile(String datafile) {
        this.datafile = datafile;
    }

    public void setProviderparams(Map<String, Object> params) {
        this.providerParams = params;
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
    }

    @Override
    public boolean renderText() {
        return GaiaSky.instance.isOn(ComponentType.Labels);
    }

    @Override
    public float[] textColour() {
        return labelcolor;
    }

    @Override
    public float textSize() {
        return (float) distToCamera * 1e-3f;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        out.set(labelPosition).add(cam.getInversePos());
    }

    @Override
    public String text() {
        return names[0];
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    @Override
    public boolean isLabel() {
        return true;
    }

    public void setFactor(Double factor) {
        this.factor = factor * Constants.DISTANCE_SCALE_FACTOR;
    }

    public void setProfiledecay(Double profiledecay) {
        this.profileDecay = profiledecay.floatValue();
    }

    public void setColornoise(Double colorNoise) {
        this.colorNoise = colorNoise.floatValue();
    }

    public void setParticlesizelimits(double[] sizeLimits) {
        if (sizeLimits[0] > sizeLimits[1])
            sizeLimits[0] = sizeLimits[1];
        this.particleSizeLimits = sizeLimits;
    }

    /**
     * Default size if not in data, 1e5 km
     *
     * @return The size
     */
    public double getFocusSize() {
        return 1e5 * Constants.KM_TO_U;
    }

    /**
     * Returns the id
     */
    public long getId() {
        return 123L;
    }

    @Override
    public String getClosestName() {
        return proximity.updating[0].name;
    }

    @Override
    public double getClosestDistToCamera() {
        return getDistToCamera();
    }

    @Override
    public Vector3b getClosestAbsolutePos(Vector3b out) {
        return getAbsolutePosition(out);
    }

    @Override
    public int getStarCount() {
        return pointData.size();
    }

    public boolean isActive() {
        return focusIndex >= 0;
    }

    /**
     * Returns position of focus
     */
    public void setPosition(double[] pos) {
        super.setPosition(pos);
        this.fixedMeanPosition = true;
    }

    /**
     * Adds all the children that are focusable objects to the list.
     *
     * @param list The list of focusable objects to add.
     */
    public void addFocusableObjects(Array<IFocus> list) {
        list.add(this);
        super.addFocusableObjects(list);
    }

    // Myself!
    public SceneGraphNode getComputedAncestor() {
        return this;
    }

    // Myself?
    public SceneGraphNode getFirstStarAncestor() {
        return this;
    }

    // The focus position
    public Vector3b getAbsolutePosition(Vector3b out) {
        return out.set(focusPosition);
    }

    public Vector3b getAbsolutePosition(String name, Vector3b out) {
        if (index.containsKey(name)) {
            int idx = index.get(name);
            IParticleRecord pb = pointData.get(idx);
            out.set(pb.x(), pb.y(), pb.z());
            return out;
        } else {
            return null;
        }
    }

    // Same position
    public Vector3b getPredictedPosition(Vector3b aux, ITimeFrameProvider time, ICamera camera, boolean force) {
        return getAbsolutePosition(aux);
    }

    // Spherical position for focus info, will be computed
    public Vector2d getPosSph() {
        return focusPositionSph;
    }

    // FOCUS_MODE dist to camera
    public double getDistToCamera() {
        return focusDistToCamera;
    }

    // FOCUS_MODE view angle
    public double getViewAngle() {
        return focusViewAngle;
    }

    // FOCUS_MODE apparent view angle
    public double getViewAngleApparent() {
        return focusViewAngleApparent;
    }

    // FOCUS_MODE size
    public double getSize() {
        return focusSize;
    }

    public float getAppmag() {
        return 0;
    }

    public float getAbsmag() {
        return 0;
    }

    public String getName() {
        if (focus != null && focus.names() != null)
            return focus.names()[0];
        else
            return super.getName();
    }

    public String[] getNames() {
        if (focus != null && focus.names() != null)
            return focus.names();
        else
            return super.getNames();
    }

    public boolean hasName(String candidate) {
        return hasName(candidate, false);
    }

    public boolean hasName(String candidate, boolean matchCase) {
        String[] names = getNames();
        if (names == null) {
            return false;
        } else {

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

    /**
     * Returns the size of the particle at index i
     *
     * @param i The index
     *
     * @return The size
     */
    public double getSize(int i) {
        return getFocusSize();
    }

    public double getRadius(int i) {
        // All particles have the same radius
        return getRadius();
    }

    // Half the size
    public double getRadius() {
        return getSize() / 2d;
    }

    @Override
    public RotationComponent getRotationComponent() {
        return null;
    }

    @Override
    public Quaterniond getOrientationQuaternion() {
        return null;
    }

    public float[] getColor() {
        return highlighted ? hlc : cc;

    }

    public float highlightedSizeFactor() {
        return (highlighted && catalogInfo != null) ? catalogInfo.hlSizeFactor : getPointscaling();
    }

    public void addHit(int screenX, int screenY, int w, int h, int pxdist, NaturalCamera camera, Array<IFocus> hits) {
        int n = pointData.size();
        if (GaiaSky.instance.isOn(ct) && this.opacity > 0) {
            Array<Pair<Integer, Double>> temporalHits = new Array<>();
            for (int i = 0; i < n; i++) {
                if (filter(i)) {
                    IParticleRecord pb = pointData.get(i);
                    Vector3 pos = aux3f1.get();
                    Vector3d posd = fetchPosition(pb, cPosD, aux3d1.get(), getDeltaYears());
                    pos.set(posd.valuesf());

                    if (camera.direction.dot(posd) > 0) {
                        // The particle is in front of us
                        // Diminish the size of the star
                        // when we are close by
                        double dist = posd.len();
                        double angle = getRadius(i) / dist / camera.getFovFactor();

                        PerspectiveCamera perspectiveCamera;
                        if (Settings.settings.program.modeStereo.active) {
                            if (screenX < Gdx.graphics.getWidth() / 2f) {
                                perspectiveCamera = camera.getCameraStereoLeft();
                            } else {
                                perspectiveCamera = camera.getCameraStereoRight();
                            }
                            perspectiveCamera.update();
                        } else {
                            perspectiveCamera = camera.camera;
                        }

                        angle = (float) Math.toDegrees(angle * camera.fovFactor) * (40f / perspectiveCamera.fieldOfView);
                        double pixelSize = Math.max(pxdist, ((angle * perspectiveCamera.viewportHeight) / perspectiveCamera.fieldOfView) / 2);
                        perspectiveCamera.project(pos);
                        pos.y = perspectiveCamera.viewportHeight - pos.y;
                        if (Settings.settings.program.modeStereo.active) {
                            pos.x /= 2;
                        }

                        // Check click distance
                        if (pos.dst(screenX % perspectiveCamera.viewportWidth, screenY, pos.z) <= pixelSize) {
                            //Hit
                            temporalHits.add(new Pair<>(i, angle));
                        }
                    }
                }
            }

            Pair<Integer, Double> best = null;
            for (Pair<Integer, Double> hit : temporalHits) {
                if (best == null)
                    best = hit;
                else if (hit.getSecond() > best.getSecond()) {
                    best = hit;
                }
            }
            if (best != null) {
                // We found the best hit
                candidateFocusIndex = best.getFirst();
                updateFocusDataPos();
                hits.add(this);
                return;
            }

        }
        candidateFocusIndex = -1;
        updateFocusDataPos();
    }

    public void addHit(Vector3d p0, Vector3d p1, NaturalCamera camera, Array<IFocus> hits) {
        int n = pointData.size();
        if (GaiaSky.instance.isOn(ct) && this.opacity > 0) {
            Vector3d beamDir = new Vector3d();
            Array<Pair<Integer, Double>> temporalHits = new Array<>();
            for (int i = 0; i < n; i++) {
                if (filter(i)) {
                    IParticleRecord pb = pointData.get(i);
                    Vector3d posd = fetchPosition(pb, cPosD, aux3d1.get(), getDeltaYears());
                    beamDir.set(p1).sub(p0);
                    if (camera.direction.dot(posd) > 0) {
                        // The star is in front of us
                        // Diminish the size of the star
                        // when we are close by
                        double dist = posd.len();
                        double angle = getRadius(i) / dist / camera.getFovFactor();
                        double distToLine = Intersectord.distanceLinePoint(p0, p1, posd.put(aux3d1.get()));
                        double value = distToLine / dist;

                        if (value < 0.01) {
                            temporalHits.add(new Pair<>(i, angle));
                        }
                    }
                }
            }

            Pair<Integer, Double> best = null;
            for (Pair<Integer, Double> hit : temporalHits) {
                if (best == null)
                    best = hit;
                else if (hit.getSecond() > best.getSecond()) {
                    best = hit;
                }
            }
            if (best != null) {
                // We found the best hit
                candidateFocusIndex = best.getFirst();
                updateFocusDataPos();
                hits.add(this);
                return;
            }

        }
        candidateFocusIndex = -1;
        updateFocusDataPos();
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case FOCUS_CHANGED:
            if (data[0] instanceof String) {
                focusIndex = data[0].equals(this.getName()) ? focusIndex : -1;
            } else {
                focusIndex = data[0] == this ? focusIndex : -1;
            }
            updateFocusDataPos();
            break;
        case CAMERA_MOTION_UPDATE:
            // Check that the particles have names
            if (updaterTask != null && pointData.size() > 0 && pointData.get(0).names() != null) {
                final Vector3b currentCameraPos = (Vector3b) data[0];
                long t = TimeUtils.millis() - lastSortTime;
                if (!updating && this.opacity > 0 && (t > UPDATE_INTERVAL_MS * 2 || (lastSortCameraPos.dst(currentCameraPos) > CAM_DX_TH && t > UPDATE_INTERVAL_MS))) {
                    updating = GaiaSky.instance.getExecutorService().execute(updaterTask);
                }
            }
            break;
        default:
            break;
        }

    }

    private void updateFocusDataPos() {
        if (focusIndex < 0) {
            focus = null;
        } else {
            focus = pointData.get(focusIndex);
            focusPosition.set(focus.x(), focus.y(), focus.z());
            Vector3d posSph = Coordinates.cartesianToSpherical(focusPosition, aux3d1.get());
            focusPositionSph.set((float) (MathUtilsd.radDeg * posSph.x), (float) (MathUtilsd.radDeg * posSph.y));
            updateFocus(GaiaSky.instance.getICamera());
        }
    }

    public void setFocusIndex(int index) {
        if (index >= 0 && index < pointData.size()) {
            candidateFocusIndex = index;
            makeFocus();
        }
    }

    @Override
    public void makeFocus() {
        focusIndex = candidateFocusIndex;
        updateFocusDataPos();

    }

    public int getCandidateIndex() {
        return candidateFocusIndex;
    }

    @Override
    public long getCandidateId() {
        return getId();
    }

    @Override
    public String getCandidateName() {
        return pointData.get(candidateFocusIndex).names() != null ? pointData.get(candidateFocusIndex).names()[0] : getName();
    }

    @Override
    public double getCandidateViewAngleApparent() {
        if (candidateFocusIndex >= 0) {
            IParticleRecord candidate = pointData.get(candidateFocusIndex);
            Vector3d aux = candidate.pos(aux3d1.get());
            ICamera camera = GaiaSky.instance.getICamera();
            return (float) ((size * .5e2f / aux.sub(camera.getPos()).len()) / camera.getFovFactor());
        } else {
            return -1;
        }
    }

    @Override
    public IFocus getFocus(String name) {
        candidateFocusIndex = index.getOrDefault(name, -1);
        return this;
    }

    public IParticleRecord getCandidateBean() {
        if (candidateFocusIndex >= 0)
            return pointData.get(candidateFocusIndex);
        else
            return null;
    }

    @Override
    public double getAlpha() {
        return focusPositionSph.x;
    }

    @Override
    public double getDelta() {
        return focusPositionSph.y;
    }

    @Override
    protected float getBaseOpacity() {
        return this.opacity;
    }

    /**
     * Fetches the real position of the particle. It will apply the necessary
     * integrations (i.e. proper motion).
     *
     * @param pb          The particle bean
     * @param campos      The position of the camera. If null, the camera position is
     *                    not subtracted so that the coordinates are given in the global
     *                    reference system instead of the camera reference system.
     * @param destination The destination factor
     * @param deltaYears  The delta years
     *
     * @return The vector for chaining
     */
    protected Vector3d fetchPosition(IParticleRecord pb, Vector3d campos, Vector3d destination, double deltaYears) {
        if (campos != null)
            return destination.set(pb.x(), pb.y(), pb.z()).sub(campos);
        else
            return destination.set(pb.x(), pb.y(), pb.z());
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
     * Returns the delta years to integrate the proper motion.
     *
     * @return The current delta years.
     */
    protected double getDeltaYears() {
        return 0;
    }

    @Override
    public boolean isCoordinatesTimeOverflow() {
        return false;
    }

    public boolean canSelect() {
        return candidateFocusIndex < 0 || candidateFocusIndex >= size() || filter(candidateFocusIndex);
    }

    public boolean mustAddToIndex() {
        return false;
    }

    public void disposeGpuMesh(){
       EventManager.publish(Event.DISPOSE_PARTICLE_GROUP_GPU_MESH, this);
    }

    @Override
    public void dispose() {
        this.disposed = true;
        GaiaSky.instance.sceneGraph.remove(this, true);
        // Unsubscribe from all events
        EventManager.instance.removeAllSubscriptions(this);
        // Data to be gc'd
        this.pointData = null;
        // Remove focus if needed
        CameraManager cam = GaiaSky.instance.getCameraManager();
        if (cam != null && cam.getFocus() != null && cam.getFocus() == this) {
            this.setFocusIndex(-1);
            EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);
        }
    }

    @Override
    public float getTextOpacity() {
        return getOpacity();
    }

    @Override
    public void highlight(boolean hl, float[] color, boolean allVisible) {
        disposeGpuMesh();
        super.highlight(hl, color, allVisible);
    }

    @Override
    public void highlight(boolean hl, int cmi, IAttribute cma, double cmmin, double cmmax, boolean allVisible) {
        disposeGpuMesh();
        super.highlight(hl, cmi, cma, cmmin, cmmax, allVisible);
    }

    public void setColorMin(double[] colorMin) {
        this.ccMin = GlobalResources.toFloatArray(colorMin);
    }

    public void setColorMin(float[] colorMin) {
        this.ccMin = colorMin;
    }

    public void setColorMax(double[] colorMax) {
        this.ccMax = GlobalResources.toFloatArray(colorMax);
    }

    public void setColorMax(float[] colorMax) {
        this.ccMax = colorMax;
    }

    public float[] getColorMin() {
        return ccMin;
    }

    public float[] getColorMax() {
        return ccMax;
    }

    /**
     * Overridden because the {@link SceneGraphNode#visible} is not used in particle groups.
     * {@link ParticleGroup#visibilityArray} is used instead.
     *
     * @return Whether the particle group should be sent to render
     */
    protected boolean shouldRender() {
        return GaiaSky.instance.isOn(ct) && opacity > 0;
    }

    /**
     * Evaluates the filter of this dataset (if any) for the given particle index
     *
     * @param index The index to filter
     *
     * @return The result of the filter evaluation, true if the particle passed the filtering, false otherwise
     */
    public boolean filter(int index) {
        if (catalogInfo != null && catalogInfo.filter != null) {
            return catalogInfo.filter.evaluate(get(index));
        }
        return true;
    }

    /**
     * Creates a default particle group with some parameters, given the name and data.
     *
     * @param name           The name of the particle group. Any occurrence of '%%PGID%%' will be replaced with the id of the particle group.
     * @param data           The data of the particle group.
     * @param datasetOptions The dataset options.
     *
     * @return A new particle group with the given parameters.
     */
    public static ParticleGroup getParticleGroup(String name, List<IParticleRecord> data, DatasetOptions datasetOptions) {
        double[] fadeIn = datasetOptions == null || datasetOptions.fadeIn == null ? null : datasetOptions.fadeIn;
        double[] fadeOut = datasetOptions == null || datasetOptions.fadeOut == null ? null : datasetOptions.fadeOut;
        double[] particleColor = datasetOptions == null || datasetOptions.particleColor == null ? new double[] { 1.0, 1.0, 1.0, 1.0 } : datasetOptions.particleColor;
        double colorNoise = datasetOptions == null ? 0 : datasetOptions.particleColorNoise;
        double[] labelColor = datasetOptions == null || datasetOptions.labelColor == null ? new double[] { 1.0, 1.0, 1.0, 1.0 } : datasetOptions.labelColor;
        double particleSize = datasetOptions == null ? 0 : datasetOptions.particleSize;
        double[] particleSizeLimits = datasetOptions == null ? new double[] { Math.tan(Math.toRadians(0.1)), Math.tan(Math.toRadians(6.0)) } : datasetOptions.particleSizeLimits;
        double profileDecay = datasetOptions == null ? 1 : datasetOptions.profileDecay;
        String ct = datasetOptions == null || datasetOptions.ct == null ? ComponentType.Galaxies.toString() : datasetOptions.ct.toString();

        ParticleGroup pg = new ParticleGroup();
        pg.setName(name.replace("%%PGID%%", Long.toString(pg.id)));
        pg.setParent("Universe");
        pg.setFadein(fadeIn);
        pg.setFadeout(fadeOut);
        pg.setProfiledecay(profileDecay);
        pg.setColor(particleColor);
        pg.setColornoise(colorNoise);
        pg.setLabelcolor(labelColor);
        pg.setSize(particleSize);
        pg.setParticlesizelimits(particleSizeLimits);
        pg.setCt(ct);
        pg.setData(data);
        pg.initialize(false, false);
        pg.doneLoading(GaiaSky.instance.assetManager);
        return pg;
    }

    /**
     * Updates the metadata information, to use for sorting. For particles, only the position (distance
     * from camera) is important.
     *
     * @param time   The time frame provider.
     * @param camera The camera.
     */
    public void updateMetadata(ITimeFrameProvider time, ICamera camera) {
        Vector3b camPos = camera.getPos();
        int n = pointData.size();
        for (int i = 0; i < n; i++) {
            IParticleRecord d = pointData.get(i);
            // Pos
            Vector3d x = aux3d1.get().set(d.x(), d.y(), d.z());
            metadata[i] = filter(i) ? camPos.dst2d(x) : Double.MAX_VALUE;
        }
    }

    public void updateSorter(ITimeFrameProvider time, ICamera camera) {
        // Prepare metadata to sort
        updateMetadata(time, camera);

        // Sort background list of indices
        Arrays.sort(background, comp);

        // Synchronously with the render thread, update indices, lastSortTime and updating state
        GaiaSky.postRunnable(() -> {
            swapBuffers();
            lastSortTime = TimeUtils.millis();
            updating = false;
        });
    }

    protected void swapBuffers() {
        if (active == indices1) {
            active = indices2;
            background = indices1;
        } else {
            active = indices1;
            background = indices2;
        }
    }

}
