/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.data.group.DatasetOptions;
import gaiasky.data.group.IStarGroupDataProvider;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.*;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.FovCamera;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.scenegraph.particle.VariableRecord;
import gaiasky.util.*;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.BlendingAttribute;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.gdx.shader.attribute.FloatAttribute;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * A particle group which additionally to the xyz position, supports color and
 * magnitude. id x y z pmx pmy pmz appmag absmag col size additional
 */
public class StarGroup extends ParticleGroup implements ILineRenderable, IStarFocus, IQuadRenderable, IModelRenderable, IObserver {

    /** Model used to represent the star **/
    private ModelComponent mc;

    /**
     * Epoch for positions/proper motions in julian days
     **/
    private double epochJd;

    /**
     * Epoch for the times in the light curves in julian days
     */
    private double variabilityEpochJd;
    /**
     * Current computed epoch time
     **/
    private double currDeltaYears = 0;

    private double modelDist;

    /** Does this contain variable stars? **/
    private boolean variableStars = false;

    /** Stars for which forceLabel is enabled **/
    private Set<Integer> forceLabelStars;
    /** Stars with special label colors **/
    private Map<Integer, float[]> labelColors;

    public StarGroup() {
        super();
        this.lastSortTime = -1;
        // Default epochs
        this.epochJd = AstroUtils.JD_J2015_5;
        this.variabilityEpochJd = AstroUtils.JD_J2010;
        this.forceLabelStars = new HashSet<>();
        this.labelColors = new HashMap<>();
    }

    public void initialize() {
        initialize(true);
    }
    public void initialize(boolean createCatalogInfo) {
        // Load data
        try {
            Class<?> clazz = Class.forName(provider);
            IStarGroupDataProvider provider = (IStarGroupDataProvider) clazz.getConstructor().newInstance();
            provider.setProviderParams(providerParams);

            if (factor == null)
                factor = 1d;

            // Set data, generate index
            List<IParticleRecord> l = provider.loadData(datafile, factor);
            this.setData(l);

        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
            pointData = null;
        }

        computeMeanPosition();
        setLabelPosition();

        initializeCatalogInfo(createCatalogInfo, names[0], names[0], pointData != null ? pointData.size() : -1, datafile);
    }

    @Override
    public void doneLoading(final AssetManager manager) {
        super.doneLoading(manager);
        // Is it variable?
        variableStars = this.pointData.size() > 0 && this.pointData.get(0) instanceof VariableRecord;
        initSortingData();
        // Load model in main thread
        GaiaSky.postRunnable(() -> initModel(manager));
    }

    private void initModel(final AssetManager manager) {
        Texture tex = manager.get(Settings.settings.data.dataFile("tex/base/star.jpg"), Texture.class);
        Texture lut = manager.get(Settings.settings.data.dataFile("tex/base/lut.jpg"), Texture.class);
        tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        Map<String, Object> params = new TreeMap<>();
        params.put("quality", 120L);
        params.put("diameter", 1d);
        params.put("flip", false);

        Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Bits.indexes(Usage.Position, Usage.Normal, Usage.Tangent, Usage.BiNormal, Usage.TextureCoordinates), GL20.GL_TRIANGLES);
        IntModel model = pair.getFirst();
        Material mat = pair.getSecond().get("base");
        mat.clear();
        mat.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
        mat.set(new TextureAttribute(TextureAttribute.Normal, lut));
        // Only to activate view vector (camera position)
        mat.set(new ColorAttribute(ColorAttribute.Specular));
        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
        Matrix4 modelTransform = new Matrix4();
        mc = new ModelComponent(false);
        mc.initialize(null);
        mc.env = new Environment();
        mc.env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
        mc.env.set(new FloatAttribute(FloatAttribute.Time, 0f));
        mc.instance = new IntModelInstance(model, modelTransform);
        // Relativistic effects
        if (Settings.settings.runtime.relativisticAberration)
            mc.rec.setUpRelativisticEffectsMaterial(mc.instance.materials);
        mc.setModelInitialized(true);
    }

    /**
     * Returns the data list
     *
     * @return The data list
     */
    public List<IParticleRecord> data() {
        return pointData;
    }

    public void setData(List<IParticleRecord> pointData, boolean regenerateIndex) {
        super.setData(pointData, regenerateIndex);
    }

    /**
     * Generates the index (maps star name and id to array index)
     *
     * @param pointData The star data
     *
     * @return An map{string,int} mapping names/ids to indexes
     */
    public Map<String, Integer> generateIndex(Array<IParticleRecord> pointData) {
        Map<String, Integer> index = new HashMap<>();
        int n = pointData.size;
        for (int i = 0; i < n; i++) {
            IParticleRecord sb = pointData.get(i);
            if (sb.names() != null) {
                for (String lowerCaseName : sb.names()) {
                    lowerCaseName = lowerCaseName.toLowerCase();
                    index.put(lowerCaseName, i);
                    String lcid = Long.toString(sb.id()).toLowerCase();
                    if (sb.id() > 0 && !lcid.equals(lowerCaseName)) {
                        index.put(lcid, i);
                    }
                    if (sb.hip() > 0) {
                        String lowerCaseHip = "hip " + sb.hip();
                        if (!lowerCaseHip.equals(lowerCaseName))
                            index.put(lowerCaseHip, i);
                    }
                }
            }
        }
        return index;
    }

    public void update(ITimeFrameProvider time, final Vector3b parentTransform, ICamera camera, float opacity) {
        // Fade node visibility
        if (active.length > 0) {
            cPosD.set(camera.getPos());
            // Delta years
            currDeltaYears = AstroUtils.getMsSince(time.getTime(), epochJd) * Nature.MS_TO_Y;

            super.update(time, parentTransform, camera, opacity);

            // Update close stars
            for (int i = 0; i < Math.min(proximity.updating.length, pointData.size()); i++) {
                if (filter(active[i]) && isVisible(active[i])) {
                    IParticleRecord closeStar = pointData.get(active[i]);
                    proximity.set(i, active[i], closeStar, camera, currDeltaYears);
                    camera.checkClosestParticle(proximity.updating[i]);

                    // Model distance
                    if (i == 0) {
                        modelDist = 172.4643429 * closeStar.radius();
                    }
                }
            }
        }

    }

    /**
     * Updates the parameters of the focus, if the focus is active in this group
     *
     * @param camera The current camera
     */
    public void updateFocus(ICamera camera) {
        IParticleRecord focus = pointData.get(focusIndex);
        Vector3d aux = this.fetchPosition(focus, cPosD, D31.get(), currDeltaYears);

        this.focusPosition.set(aux).add(camera.getPos());
        this.focusDistToCamera = aux.len();
        this.focusSize = getFocusSize();
        this.focusViewAngle = (float) ((getRadius() / this.focusDistToCamera) / camera.getFovFactor());
        this.focusViewAngleApparent = this.focusViewAngle * Settings.settings.scene.star.brightness;
    }

    /**
     * Overrides {@link ParticleGroup}'s implementation by actually integrating
     * the position using the proper motion and the given time.
     */
    public Vector3b getPredictedPosition(Vector3b aux, ITimeFrameProvider time, ICamera camera, boolean force) {
        if (time.getHdiff() == 0 && !force) {
            return getAbsolutePosition(aux);
        } else {
            double deltaYears = AstroUtils.getMsSince(time.getTime(), epochJd) * Nature.MS_TO_Y;
            if (pointData != null) {
                return aux.set(this.fetchPosition(pointData.get(focusIndex), null, D31.get(), deltaYears));
            }
            return aux;
        }
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender()) {
            if (variableStars) {
                addToRender(this, RenderGroup.VARIABLE_GROUP);
            } else {
                addToRender(this, RenderGroup.STAR_GROUP);
            }
            addToRender(this, RenderGroup.MODEL_VERT_STAR);
            if (Settings.settings.scene.star.group.billboard) {
                addToRender(this, RenderGroup.BILLBOARD_STAR);
            }
            if (GaiaSky.instance.sgr.isOn(ComponentTypes.ComponentType.VelocityVectors)) {
                //addToRender(this, RenderGroup.LINE);
                addToRender(this, RenderGroup.LINE);
            }
            if (renderText()) {
                addToRender(this, RenderGroup.FONT_LABEL);
            }
        }
    }

    /**
     * Billboard rendering
     */
    @Override
    public void render(ExtShaderProgram shader, float alpha, IntMesh mesh, ICamera camera) {
        double thPointTimesFovFactor = Settings.settings.scene.star.threshold.point * camera.getFovFactor();
        double thUpOverFovFactor = Constants.THRESHOLD_UP / camera.getFovFactor();
        double thDownOverFovFactor = Constants.THRESHOLD_DOWN / camera.getFovFactor();
        double innerRad = 0.006 + Settings.settings.scene.star.pointSize * 0.008;
        alpha = alpha * this.opacity;
        float fovFactor = camera.getFovFactor();

        // GENERAL UNIFORMS
        shader.setUniformf("u_thpoint", (float) thPointTimesFovFactor);
        // Light glow always disabled with star groups
        shader.setUniformi("u_lightScattering", 0);
        shader.setUniformf("u_inner_rad", (float) innerRad);

        // RENDER ACTUAL STARS
        boolean focusRendered = false;
        int n = Math.min(Settings.settings.scene.star.group.numBillboard, pointData.size());
        for (int i = 0; i < n; i++) {
            renderCloseupStar(active[i], fovFactor, cPosD, shader, mesh, thPointTimesFovFactor, thUpOverFovFactor, thDownOverFovFactor, alpha);
            focusRendered = focusRendered || active[i] == focusIndex;
        }
        if (focus != null && !focusRendered) {
            renderCloseupStar(focusIndex, fovFactor, cPosD, shader, mesh, thPointTimesFovFactor, thUpOverFovFactor, thDownOverFovFactor, alpha);
        }
    }

    Color c = new Color();

    private void renderCloseupStar(int idx, float fovFactor, Vector3d cPosD, ExtShaderProgram shader, IntMesh mesh, double thPointTimesFovFactor, double thUpOverFovFactor, double thDownOverFovFactor, float alpha) {
        if (filter(idx) && isVisible(idx)) {
            IParticleRecord star = pointData.get(idx);
            double varScl = getVariableSizeScaling(idx);

            double sizeOriginal = getSize(idx);
            double size = sizeOriginal * varScl;
            double radius = size * Constants.STAR_SIZE_FACTOR;
            Vector3d starPos = fetchPosition(star, cPosD, D31.get(), currDeltaYears);
            double distToCamera = starPos.len();
            double viewAngle = (sizeOriginal * Constants.STAR_SIZE_FACTOR / distToCamera) / fovFactor;

            Color.abgr8888ToColor(c, getColor(idx));
            if (viewAngle >= thPointTimesFovFactor) {
                double fuzzySize = getFuzzyRenderSize(sizeOriginal, radius, distToCamera, viewAngle, thDownOverFovFactor, thUpOverFovFactor);

                Vector3 pos = starPos.put(F33.get());
                shader.setUniformf("u_pos", pos);
                shader.setUniformf("u_size", (float) fuzzySize);

                shader.setUniformf("u_color", c.r, c.g, c.b, alpha);
                shader.setUniformf("u_distance", (float) distToCamera);
                shader.setUniformf("u_apparent_angle", (float) (viewAngle * Settings.settings.scene.star.pointSize));
                shader.setUniformf("u_radius", (float) radius);

                // Sprite.render
                mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);

            }
        }
    }

    public double getFuzzyRenderSize(double size, double radius, double distToCamera, double viewAngle, double thDown, double thUp) {
        double computedSize = size;
        if (viewAngle > thDown) {
            double dist = distToCamera;
            if (viewAngle > thUp) {
                dist = radius / Constants.THRESHOLD_UP;
            }
            computedSize = (size * (dist / radius) * Constants.THRESHOLD_DOWN);
        }
        // Change the factor at the end here to control the stray light of stars
        computedSize *= Settings.settings.scene.star.pointSize * 0.4;

        return computedSize;
    }

    /**
     * Model rendering
     */
    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc, RenderGroup group) {
        if (mc != null && mc.isModelInitialised()) {
            mc.touch();
            float opacity = (float) MathUtilsd.lint(proximity.updating[0].distToCamera, modelDist / 50f, modelDist, 1f, 0f);
            if (alpha * opacity > 0) {
                mc.setTransparency(alpha * opacity);
                float[] col = proximity.updating[0].col;
                ((ColorAttribute) mc.env.get(ColorAttribute.AmbientLight)).color.set(col[0], col[1], col[2], 1f);
                ((FloatAttribute) mc.env.get(FloatAttribute.Time)).value = (float) t;
                // Local transform
                double variableScaling = getVariableSizeScaling(proximity.updating[0].index);
                mc.instance.transform.idt().translate((float) proximity.updating[0].pos.x, (float) proximity.updating[0].pos.y, (float) proximity.updating[0].pos.z).scl((float) (getRadius(active[0]) * 2d * variableScaling));
                mc.updateRelativisticEffects(GaiaSky.instance.getICamera());
                mc.updateVelocityBufferUniforms(GaiaSky.instance.getICamera());
                modelBatch.render(mc.instance, mc.env);
            }
        }
    }

    private long getMaxProperMotionLines() {
        return Math.min(pointData.size(), Settings.settings.scene.star.group.numVelocityVector);
    }

    private final float[] rgba = new float[4];

    /**
     * Proper motion rendering
     */
    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        alpha *= GaiaSky.instance.sgr.alphas[ComponentTypes.ComponentType.VelocityVectors.ordinal()];
        float thPointTimesFovFactor = (float) Settings.settings.scene.star.threshold.point * camera.getFovFactor();
        int n = (int) getMaxProperMotionLines();
        for (int i = n - 1; i >= 0; i--) {
            IParticleRecord star = pointData.get(active[i]);
            float radius = (float) (getSize(active[i]) * Constants.STAR_SIZE_FACTOR);
            // Position
            Vector3d lPos = fetchPosition(star, cPosD, D31.get(), currDeltaYears);
            // Proper motion
            Vector3d pm = D32.get().set(star.pmx(), star.pmy(), star.pmz()).scl(currDeltaYears);
            // Rest of attributes
            float distToCamera = (float) lPos.len();
            float viewAngle = ((radius / distToCamera) / camera.getFovFactor()) * Settings.settings.scene.star.brightness;
            if (viewAngle >= thPointTimesFovFactor / Settings.settings.scene.properMotion.number && (star.pmx() != 0 || star.pmy() != 0 || star.pmz() != 0)) {
                Vector3d p1 = D31.get().set(star.x() + pm.x, star.y() + pm.y, star.z() + pm.z).sub(camera.getPos());
                Vector3d ppm = D32.get().set(star.pmx(), star.pmy(), star.pmz()).scl(Settings.settings.scene.properMotion.length);
                double p1p2len = ppm.len();
                Vector3d p2 = D33.get().set(ppm).add(p1);

                // Maximum speed in km/s, to normalize
                float maxSpeedKms = 100;
                float r, g, b;
                switch (Settings.settings.scene.properMotion.colorMode) {
                case 0:
                default:
                    // DIRECTION
                    // Normalize, each component is in [-1:1], map to [0:1] and to a color channel
                    ppm.nor();
                    r = (float) (ppm.x + 1d) / 2f;
                    g = (float) (ppm.y + 1d) / 2f;
                    b = (float) (ppm.z + 1d) / 2f;
                    break;
                case 1:
                    // LENGTH
                    ppm.set(star.pmx(), star.pmy(), star.pmz());
                    // Units/year to Km/s
                    ppm.scl(Constants.U_TO_KM / Nature.Y_TO_S);
                    double len = MathUtilsd.clamp(ppm.len(), 0d, maxSpeedKms) / maxSpeedKms;
                    ColorUtils.colormap_long_rainbow((float) (1 - len), rgba);
                    r = rgba[0];
                    g = rgba[1];
                    b = rgba[2];
                    break;
                case 2:
                    // HAS RADIAL VELOCITY - blue: stars with RV, red: stars without RV
                    if (star.radvel() != 0) {
                        r = ColorUtils.gBlue[0] + 0.2f;
                        g = ColorUtils.gBlue[1] + 0.4f;
                        b = ColorUtils.gBlue[2] + 0.4f;
                    } else {
                        r = ColorUtils.gRed[0] + 0.4f;
                        g = ColorUtils.gRed[1] + 0.2f;
                        b = ColorUtils.gRed[2] + 0.2f;
                    }
                    break;
                case 3:
                    // REDSHIFT from Sun - blue: -100 Km/s, red: 100 Km/s
                    float rav = star.radvel();
                    if (rav != 0) {
                        // rv in [0:1]
                        float rv = ((MathUtilsd.clamp(rav, -maxSpeedKms, maxSpeedKms) / maxSpeedKms) + 1) / 2;
                        ColorUtils.colormap_blue_white_red(rv, rgba);
                        r = rgba[0];
                        g = rgba[1];
                        b = rgba[2];
                    } else {
                        r = g = b = 1;
                    }
                    break;
                case 4:
                    // REDSHIFT from Camera - blue: -100 Km/s, red: 100 Km/s
                    if (ppm.len2() != 0) {
                        ppm.set(star.pmx(), star.pmy(), star.pmz());
                        // Units/year to Km/s
                        ppm.scl(Constants.U_TO_KM / Nature.Y_TO_S);
                        Vector3d camStar = D34.get().set(p1);
                        double pr = ppm.dot(camStar.nor());
                        double projection = ((MathUtilsd.clamp(pr, -(double) maxSpeedKms, maxSpeedKms) / (double) maxSpeedKms) + 1) / 2;
                        ColorUtils.colormap_blue_white_red((float) projection, rgba);
                        r = rgba[0];
                        g = rgba[1];
                        b = rgba[2];
                    } else {
                        r = g = b = 1;
                    }
                    break;
                case 5:
                    // SINGLE COLOR
                    r = ColorUtils.gBlue[0] + 0.2f;
                    g = ColorUtils.gBlue[1] + 0.4f;
                    b = ColorUtils.gBlue[2] + 0.4f;
                    break;
                }

                // Clamp
                r = MathUtilsd.clamp(r, 0, 1);
                g = MathUtilsd.clamp(g, 0, 1);
                b = MathUtilsd.clamp(b, 0, 1);

                renderer.addLine(this, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, r, g, b, alpha * this.opacity);
                if (Settings.settings.scene.properMotion.arrowHeads) {
                    // Add Arrow cap
                    Vector3d p3 = D32.get().set(ppm).nor().scl(p1p2len * .86).add(p1);
                    p3.rotate(p2, 30);
                    renderer.addLine(this, p3.x, p3.y, p3.z, p2.x, p2.y, p2.z, r, g, b, alpha * this.opacity);
                    p3.rotate(p2, -60);
                    renderer.addLine(this, p3.x, p3.y, p3.z, p2.x, p2.y, p2.z, r, g, b, alpha * this.opacity);
                }

            }
        }
    }

    @Override
    public float getLineWidth() {
        return 0.6f;
    }

    @Override
    public int getGlPrimitive() {
        return GL20.GL_LINES;
    }

    /**
     * Label rendering
     */
    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        float thOverFactor = (float) (Settings.settings.scene.star.threshold.point / Settings.settings.scene.label.number / camera.getFovFactor());

        Vector3d starPosition = D31.get();
        int n = Math.min(pointData.size(), Settings.settings.scene.star.group.numLabel);
        if (camera.getCurrent() instanceof FovCamera) {
            for (int i = 0; i < n; i++) {
                IParticleRecord star = pointData.get(active[i]);
                starPosition = fetchPosition(star, cPosD, starPosition, currDeltaYears);
                double distToCamera = starPosition.len();
                float radius = (float) getRadius(active[i]);
                float viewAngle = (float) (((radius / distToCamera) / camera.getFovFactor()) * Settings.settings.scene.star.brightness * 6f);

                if (camera.isVisible(viewAngle, starPosition, distToCamera)) {
                    render2DLabel(batch, shader, rc, sys.font2d, camera, star.names()[0], starPosition);
                }
            }
        } else {
            for (int i = 0; i < n; i++) {
                int idx = active[i];
                renderStarLabel(idx, starPosition, thOverFactor, batch, shader, sys, rc, camera);
            }
            for (Integer i : forceLabelStars) {
                renderStarLabel(i, starPosition, thOverFactor, batch, shader, sys, rc, camera);
            }
        }
    }

    private void renderStarLabel(int idx, Vector3d starPosition, float thOverFactor, ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        boolean forceLabel = forceLabelStars.contains(idx);
        IParticleRecord star = pointData.get(idx);
        starPosition = fetchPosition(star, cPosD, starPosition, currDeltaYears);

        double distToCamera = starPosition.len();
        float radius = (float) getRadius(idx);
        if (forceLabel) {
            radius = Math.max(radius, 1e4f);
        }
        float viewAngle = (float) (((radius / distToCamera) / camera.getFovFactor()) * Settings.settings.scene.star.brightness * 1.5f);

        if (forceLabel || viewAngle >= thOverFactor && camera.isVisible(viewAngle, starPosition, distToCamera) && distToCamera > radius * 100) {
            textPosition(camera, starPosition, distToCamera, radius);

            shader.setUniformf("u_viewAngle", viewAngle);
            shader.setUniformf("u_viewAnglePow", 1f);
            shader.setUniformf("u_thOverFactor", thOverFactor);
            shader.setUniformf("u_thOverFactorScl", camera.getFovFactor());
            // Override object color
            shader.setUniform4fv("u_color", textColour(star.names()[0]), 0, 4);
            double textSize = FastMath.tanh(viewAngle) * distToCamera * 1e5d;
            float alpha = Math.min((float) FastMath.atan(textSize / distToCamera), 1.e-3f);
            textSize = (float) FastMath.tan(alpha) * distToCamera * 0.5f;
            render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, star.names()[0], starPosition, distToCamera, textScale() * camera.getFovFactor(), textSize * camera.getFovFactor(), forceLabel);
        }
    }

    private double getVariableSizeScaling(final int idx) {
        IParticleRecord ipr = pointData.get(idx);
        if (ipr instanceof VariableRecord) {
            VariableRecord vr = (VariableRecord) ipr;
            double[] times = vr.variTimes;
            float[] sizes = vr.variMags;
            int n = vr.nVari;

            // Days since epoch
            double t = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), this.getVariabilityepoch());
            double t0 = times[0];
            double t1 = times[n - 1];
            double period = t1 - t0;
            t = t % period;
            for (int i = 0; i < n - 1; i++) {
                double x0 = times[i] - t0;
                double x1 = times[i + 1] - t0;
                if (t >= x0 && t <= x1) {
                    return MathUtilsd.lint(t, x0, x1, sizes[i], sizes[i + 1]) / vr.size();
                }
            }
        }
        return 1;
    }

    public double getFocusSize() {
        return focus.size();
    }

    // Radius in stars is different!
    public double getRadius() {
        return getSize() * Constants.STAR_SIZE_FACTOR;
    }

    // Radius in stars is different!
    public double getRadius(int i) {
        return getSize(i) * Constants.STAR_SIZE_FACTOR;
    }

    public float getAppmag() {
        return focus.appmag();
    }

    public float getAbsmag() {
        return focus.absmag();
    }

    public long getId() {
        if (focus != null)
            return focus.id();
        else
            return -1;
    }

    @Override
    public double getMuAlpha() {
        if (focus != null)
            return focus.mualpha();
        else
            return 0;
    }

    @Override
    public double getMuDelta() {
        if (focus != null)
            return focus.mudelta();
        else
            return 0;
    }

    @Override
    public double getRadialVelocity() {
        if (focus != null)
            return focus.radvel();
        else
            return 0;
    }

    /**
     * Returns the size of the particle at index i
     *
     * @param i The index
     *
     * @return The size
     */
    public double getSize(int i) {
        return pointData.get(i).size();
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        // Super handles FOCUS_CHANGED and CAMERA_MOTION_UPDATED event
        super.notify(event, source, data);
    }

    @Override
    public int getCatalogSource() {
        return 1;
    }

    @Override
    public int getHip() {
        if (focus != null && focus.hip() > 0)
            return focus.hip();
        return -1;
    }

    @Override
    public long getCandidateId() {
        return pointData.get(candidateFocusIndex).id();
    }

    @Override
    public String getCandidateName() {
        return pointData.get(candidateFocusIndex).names()[0];
    }

    @Override
    public double getCandidateViewAngleApparent() {
        if (candidateFocusIndex >= 0) {
            IParticleRecord candidate = pointData.get(candidateFocusIndex);
            Vector3d aux = candidate.pos(D31.get());
            ICamera camera = GaiaSky.instance.getICamera();
            double va = (float) ((candidate.radius() / aux.sub(camera.getPos()).len()) / camera.getFovFactor());
            return va * Settings.settings.scene.star.brightness;
        } else {
            return -1;
        }
    }

    @Override
    public double getClosestDistToCamera() {
        return this.proximity.updating[0].distToCamera;
    }

    @Override
    public String getClosestName() {
        return this.proximity.updating[0].name;
    }

    @Override
    public double getClosestSize() {
        return this.proximity.updating[0].size;
    }

    @Override
    public Vector3d getClosestPos(Vector3d out) {
        return out.set(this.proximity.updating[0].pos);
    }

    @Override
    public Vector3b getClosestAbsolutePos(Vector3b out) {
        return out.set(this.proximity.updating[0].absolutePos);
    }

    @Override
    public float[] getClosestCol() {
        return this.proximity.updating[0].col;
    }

    @Override
    public boolean hasAtmosphere() {
        return false;
    }

    @Override
    public IFocus getFocus(String name) {
        candidateFocusIndex = index.getOrDefault(name, -1);
        return this;
    }

    @Override
    public Vector3b getAbsolutePosition(String name, Vector3b aux) {
        Vector3d vec = getAbsolutePosition(name, D31.get());
        aux.set(vec);
        return aux;
    }

    public Vector3d getAbsolutePosition(String name, Vector3d aux) {
        if (index.containsKey(name)) {
            int idx = index.get(name);
            IParticleRecord sb = pointData.get(idx);
            fetchPosition(sb, null, aux, currDeltaYears);
            return aux;
        } else {
            return null;
        }
    }

    @Override
    protected Vector3d fetchPosition(IParticleRecord pb, Vector3d campos, Vector3d out, double deltaYears) {
        Vector3d pm = D32.get().set(pb.pmx(), pb.pmy(), pb.pmz()).scl(deltaYears);
        Vector3d dest = D33.get().set(pb.x(), pb.y(), pb.z());
        if (campos != null && !campos.hasNaN())
            dest.sub(campos).add(pm);
        else
            dest.add(pm);

        return out.set(dest);
    }

    @Override
    protected double getDeltaYears() {
        return currDeltaYears;
    }

    /**
     * Sets the epoch to use for the stars in this group
     *
     * @param epochJd The epoch in julian days (days since January 1, 4713 BCE)
     */
    public void setEpoch(Double epochJd) {
        this.epochJd = epochJd;
    }

    /**
     * Returns the epoch in Julian Days used for the stars in this group
     *
     * @return The epoch in julian days
     */
    public Double getEpoch() {
        return this.epochJd;
    }

    /**
     * Sets the light curve epoch to use for the stars in this group
     *
     * @param epochJd The light curve epoch in julian days (days since January 1, 4713 BCE)
     */
    public void setVariabilityepoch(Double epochJd) {
        this.variabilityEpochJd = epochJd;
    }

    /**
     * Returns the light curve epoch in Julian Days used for the stars in this group
     *
     * @return The light curve epoch in julian days
     */
    public Double getVariabilityepoch() {
        return this.variabilityEpochJd;
    }

    @Override
    public void dispose() {
        this.disposed = true;
        if(GaiaSky.instance.sceneGraph != null) {
            GaiaSky.instance.sceneGraph.remove(this, true);
        }
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

    public float getColor(int index) {
        return highlighted ? Color.toFloatBits(hlc[0], hlc[1], hlc[2], hlc[3]) : (float) pointData.get(index).col();
    }

    /**
     * Creates a default star group with some parameters, given the name and data
     *
     * @param name           The name of the star group. Any occurrence of '%%SGID%%' will be replaced with the id of the star group
     * @param data           The data of the star group
     * @param datasetOptions The dataset options
     *
     * @return A new star group with the given parameters
     */
    public static StarGroup getStarGroup(String name, List<IParticleRecord> data, DatasetOptions datasetOptions) {
        double[] fadeIn = datasetOptions == null || datasetOptions.fadeIn == null ? null : datasetOptions.fadeIn;
        double[] fadeOut = datasetOptions == null || datasetOptions.fadeOut == null ? null : datasetOptions.fadeOut;
        double[] labelColor = datasetOptions == null || datasetOptions.labelColor == null ? new double[] { 1.0, 1.0, 1.0, 1.0 } : datasetOptions.labelColor;

        StarGroup sg = new StarGroup();
        sg.setName(name.replace("%%SGID%%", Long.toString(sg.id)));
        sg.setParent("Universe");
        sg.setFadein(fadeIn);
        sg.setFadeout(fadeOut);
        sg.setLabelcolor(labelColor);
        sg.setColor(new double[] { 1.0, 1.0, 1.0, 0.25 });
        sg.setSize(6.0);
        sg.setLabelposition(new double[] { 0.0, -5.0e7, -4e8 });
        sg.setCt("Stars");
        sg.setData(data);
        sg.doneLoading(GaiaSky.instance.assetManager);
        return sg;
    }

    /**
     * Creates a default star group with some sane parameters, given the name and the data
     *
     * @param name The name of the star group. Any occurrence of '%%SGID%%' in name will be replaced with the id of the star group
     * @param data The data of the star group
     *
     * @return A new star group with sane parameters
     */
    public static StarGroup getDefaultStarGroup(String name, List<IParticleRecord> data) {
        return getDefaultStarGroup(name, data, true);
    }

    /**
     * Creates a default star group with some sane parameters, given the name and the data
     *
     * @param name     The name of the star group. Any occurrence of '%%SGID%%' in name will be replaced with the id of the star group
     * @param data     The data of the star group
     * @param fullInit Initializes the group right away
     *
     * @return A new star group with sane parameters
     */
    public static StarGroup getDefaultStarGroup(String name, List<IParticleRecord> data, boolean fullInit) {
        StarGroup sg = new StarGroup();
        sg.setName(name.replace("%%SGID%%", Long.toString(sg.id)));
        sg.setParent("Universe");
        sg.setLabelcolor(new double[] { 1.0, 1.0, 1.0, 1.0 });
        sg.setColor(new double[] { 1.0, 1.0, 1.0, 0.25 });
        sg.setSize(6.0);
        sg.setLabelposition(new double[] { 0.0, -5.0e7, -4e8 });
        sg.setCt("Stars");
        sg.setData(data);
        if (fullInit) {
            sg.doneLoading(GaiaSky.instance.assetManager);
        }
        return sg;
    }

    /**
     * Updates the additional information array, to use for sorting.
     * In stars, we need to take into account the proper motion and the brightness.
     *
     * @param time   The current time frame provider
     * @param camera The camera
     */
    public void updateMetadata(ITimeFrameProvider time, ICamera camera) {
        Vector3d camPos = camera.getPos().tov3d(D34.get());
        double deltaYears = AstroUtils.getMsSince(time.getTime(), epochJd) * Nature.MS_TO_Y;
        if (pointData != null) {
            int n = pointData.size();
            for (int i = 0; i < n; i++) {
                IParticleRecord d = pointData.get(i);

                // Pm
                Vector3d dx = D32.get().set(d.pmx(), d.pmy(), d.pmz()).scl(deltaYears);
                // Pos
                Vector3d x = D31.get().set(d.x(), d.y(), d.z()).add(dx);

                metadata[i] = filter(i) ? (-(((d.size() * Constants.STAR_SIZE_FACTOR) / camPos.dst(x)) / camera.getFovFactor()) * Settings.settings.scene.star.brightness) : Double.MAX_VALUE;
            }
        }
    }

    @Override
    public void setLabelcolor(float[] color, String name) {
        if (index.containsKey(name)) {
            int idx = index.get(name);
            labelColors.put(idx, color);
        }
    }

    @Override
    public void setForceLabel(Boolean forceLabel, String name) {
        if (index.containsKey(name)) {
            int idx = index.get(name);
            if (forceLabelStars.contains(idx)) {
                if (!forceLabel) {
                    // Remove from forceLabelStars
                    forceLabelStars.remove(idx);
                }
            } else if (forceLabel) {
                // Add to forceLabelStars
                forceLabelStars.add(idx);
            }
        }
    }

    public boolean isForceLabel(String name) {
        if (index.containsKey(name)) {
            int idx = index.get(name);
            return forceLabelStars.contains(idx);
        }
        return false;
    }

    public float[] textColour(String name) {
        name = name.toLowerCase(Locale.ROOT).trim();
        if (index.containsKey(name)) {
            int idx = index.get(name);
            if (labelColors.containsKey(idx)) {
                return labelColors.get(idx);
            }
        }
        return labelcolor;
    }

    public void markForUpdate() {
        if (variableStars) {
            EventManager.publish(Event.GPU_DISPOSE_VARIABLE_GROUP, this);
        } else {
            EventManager.publish(Event.GPU_DISPOSE_STAR_GROUP, this);
        }
    }
}
