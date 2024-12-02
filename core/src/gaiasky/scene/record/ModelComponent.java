/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.GaiaSky;
import gaiasky.data.AssetBean;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.event.Observer;
import gaiasky.gui.beans.PrimitiveComboBoxBean.Primitive;
import gaiasky.render.BlendMode;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IUpdatable;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.model.gltf.scene3d.model.ModelInstanceHack;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.*;
import gaiasky.util.i18n.I18n;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class ModelComponent extends NamedComponent implements Disposable, IObserver, IUpdatable<ModelComponent> {
    private static final Log logger = Logger.getLogger(ModelComponent.class);
    private static final ColorAttribute globalAmbient;

    static {
        globalAmbient = new ColorAttribute(ColorAttribute.AmbientLight, (float) Settings.settings.scene.renderer.ambient,
                (float) Settings.settings.scene.renderer.ambient, (float) Settings.settings.scene.renderer.ambient, 1f);
        // Ambient light watcher.
        var observer = new Observer() {
            @Override
            public void notify(Event event,
                               Object source,
                               Object... data) {
                if (event == Event.AMBIENT_LIGHT_CMD) {
                    ModelComponent.setGlobalAmbientLight((float) data[0]);
                }
            }
        };
        EventManager.instance.subscribe(observer, Event.AMBIENT_LIGHT_CMD);
    }

    public boolean forceInit = false;
    public IntModelInstance instance;
    public Environment env;
    public Map<String, Object> params;
    public String type, modelFile;
    public double scale = 1d;
    public boolean culling = true;
    public boolean tessellated = false;

    /**
     * COMPONENTS
     */
    // Material with textures, colors, and other properties.
    public MaterialComponent mtc;
    // Relativistic effects.
    public RelativisticEffectsComponent rec;
    /**
     * Light never changes; set fixed ambient light for this model
     */
    private Boolean staticLight = false;
    /**
     * Ambient light level for static light objects
     **/
    private final float[] ambientColor = new float[]{-1f, -1f, -1f, -1f};
    private boolean modelInitialised, modelLoading;
    private boolean useColor = false;
    /**
     * The blend mode
     **/
    private BlendMode blendMode = BlendMode.ALPHA;
    private AssetManager manager;
    private float[] cc;
    private int primitiveType = GL20.GL_TRIANGLES;
    private final Vector3 aux = new Vector3();

    public ModelComponent() {
        this(true);
    }

    public ModelComponent(Boolean initEnvironment) {
    }

    /**
     * Sets the ambient light
     *
     * @param level Ambient light level between 0 and 1
     */
    public static void setGlobalAmbientLight(float level) {
        globalAmbient.color.set(level, level, level, 1f);
    }

    /**
     * Returns the given point light.
     *
     * @param i The index of the light (must be less than {@link Constants#N_POINT_LIGHTS}).
     * @return The point light with index i.
     */
    public PointLight pointLight(int i) {
        var attribute = env.get(PointLightsAttribute.Type);
        if (attribute != null) {
            var pointLightsAttribute = (PointLightsAttribute) attribute;
            if (pointLightsAttribute.lights.size > i) {
                return pointLightsAttribute.lights.get(i);
            }
        }
        return null;
    }

    public boolean hasPointLight(int i) {
        var point = pointLight(i);
        return point != null && !ColorUtils.isZero(point.color);
    }

    /**
     * Turns off all point lights.
     */
    public void clearPoints() {
        var attribute = env.get(PointLightsAttribute.Type);
        if (attribute != null) {
            var pointLightsAttribute = (PointLightsAttribute) attribute;
            for (PointLight light : pointLightsAttribute.lights) {
                light.color.set(0f, 0f, 0f, 1f);
            }
        }
    }

    /**
     * Returns the given directional light.
     *
     * @param i The index of the light (must be less than {@link Constants#N_DIR_LIGHTS}).
     * @return The directional light with index i.
     */
    public DirectionalLight dirLight(int i) {
        var attribute = env.get(DirectionalLightsAttribute.Type);
        if (attribute != null) {
            var directionalLightAttribute = (DirectionalLightsAttribute) attribute;
            if (directionalLightAttribute.lights.size > i) {
                return directionalLightAttribute.lights.get(i);
            }
        }
        return null;
    }

    public boolean hasDirLight(int i) {
        var directional = dirLight(i);
        return directional != null && !ColorUtils.isZero(directional.color);
    }

    /**
     * Turns off all directional lights.
     */
    public void clearDirectionals() {
        var attribute = env.get(DirectionalLightsAttribute.Type);
        if (attribute != null) {
            var directionalLightAttribute = (DirectionalLightsAttribute) attribute;
            for (DirectionalLight light : directionalLightAttribute.lights) {
                light.color.set(0f, 0f, 0f, 1f);
            }
        }
    }

    public void initialize(String name) {
        super.initialize(name);
        this.initialize(false);
    }

    public void initialize(boolean mesh) {
        FileHandle model = modelFile != null ? Settings.settings.data.dataFileHandle(modelFile) : null;
        if (mesh) {
            if (!Settings.settings.scene.initialization.lazyMesh && modelFile != null && model.exists()) {
                AssetBean.addAsset(Settings.settings.data.dataFile(modelFile), IntModel.class);
            }
        } else {
            if (modelFile != null && model.exists()) {
                AssetBean.addAsset(Settings.settings.data.dataFile(modelFile), IntModel.class);
            }
        }

        if ((forceInit || !Settings.settings.scene.initialization.lazyTexture) && mtc != null) {
            mtc.initialize(name);
            mtc.texLoading = true;
        }

        initializeEnvironment();

        rec = new RelativisticEffectsComponent();

        MaterialComponent.reflectionCubemap.initialize();
    }

    public void initializeEnvironment() {
        env = new Environment();
        if (staticLight || (ambientColor[0] >= 0 && ambientColor[1] >= 0 && ambientColor[2] >= 0 && ambientColor[3] >= 0)) {
            // If lazy texture init, we turn off the lights until the texture is loaded.
            ColorAttribute staticAmbientLight = new ColorAttribute(ColorAttribute.AmbientLight, ambientColor[0], ambientColor[1], ambientColor[2], ambientColor[3]);
            env.set(staticAmbientLight);
        } else {
            // Use global ambient.
            env.set(globalAmbient);
        }

        if (!staticLight) {
            // Directional lights.
            for (int i = 0; i < Constants.N_DIR_LIGHTS; i++) {
                DirectionalLight dLight = new DirectionalLight();
                dLight.color.set(0f, 0f, 0f, 1f);
                env.add(dLight);
            }
            // Point lights.
            for (int i = 0; i < Constants.N_POINT_LIGHTS; i++) {
                PointLight pointLight = new PointLight();
                pointLight.color.set(0f, 0f, 0f, 1f);
                env.add(pointLight);
            }

        }
    }

    public void doneLoading(AssetManager manager,
                            Matrix4 localTransform,
                            float[] cc) {
        doneLoading(manager, localTransform, cc, false);
    }

    public void doneLoading(AssetManager manager,
                            Matrix4 localTransform,
                            float[] cc,
                            boolean mesh) {
        this.manager = manager;
        this.cc = cc;
        IntModel model;

        // CREATE MAIN MODEL INSTANCE
        if (!mesh || !Settings.settings.scene.initialization.lazyMesh) {
            Pair<IntModel, Map<String, Material>> modelMaterial = initModelFile();
            if (modelMaterial != null) {
                model = modelMaterial.getFirst();
                if (modelFile != null && (modelFile.endsWith("gltf") || modelFile.endsWith("glb"))) {
                    instance = new ModelInstanceHack(model, localTransform);
                } else {
                    instance = new IntModelInstance(model, localTransform);
                }
            }
            this.modelInitialised = true;
        }

        // INITIALIZE MATERIAL
        if ((forceInit || !Settings.settings.scene.initialization.lazyTexture) && mtc != null) {
            mtc.initMaterial(manager, instance, cc, culling);
            mtc.texLoading = false;
            mtc.texInitialised = true;
        }

        // COLOR IF NO TEXTURE
        if (mtc == null && instance != null) {
            addColorToMaterial();
        }
        // Subscribe to new graphics quality setting event
        EventManager.instance.subscribe(this, Event.ECLIPSES_CMD);

        this.modelInitialised = this.modelInitialised || !Settings.settings.scene.initialization.lazyMesh;
        this.modelLoading = false;
    }

    private Pair<IntModel, Map<String, Material>> initModelFile() {
        IntModel model = null;
        Map<String, Material> materials = null;
        if (modelFile != null && manager.isLoaded(Settings.settings.data.dataFile(modelFile))) {
            // Model comes from file (probably .obj or .g3db)
            model = manager.get(Settings.settings.data.dataFile(modelFile), IntModel.class);
            materials = new HashMap<>();
            if (model.materials.size == 0) {
                Material material = new Material();
                model.materials.add(material);
                materials.put("base", material);
            } else if (modelFile == null) {
                if (model.materials.size > 1) {
                    for (int i = 0; i < model.materials.size; i++) {
                        var mat = model.materials.get(i);
                        if (mat.id != null && !mat.id.isEmpty()) {
                            materials.put(mat.id, mat);
                        } else {
                            materials.put("base" + i, model.materials.get(i));
                        }
                    }
                } else {
                    var mat = model.materials.first();
                    if (mat.id != null && !mat.id.isEmpty()) {
                        materials.put(mat.id, mat);
                    } else {
                        materials.put("base", mat);
                    }
                }
            }
            // Add skybox to materials.
            addReflectionCubemapAttribute(model.materials);
        } else if (type != null) {
            // We actually need to create the model.
            Bits attributes = Bits.indices(Usage.Position, Usage.Normal, Usage.Tangent, Usage.BiNormal, Usage.TextureCoordinates);
            if (params.containsKey("attributes")) {
                attributes = Bits.indices(((Long) params.get("attributes")).intValue());
            }
            Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel(type, params, attributes, primitiveType);
            model = pair.getFirst();
            materials = pair.getSecond();
        } else {
            // Data error!
            if (modelFile != null) {
                logger.error(new RuntimeException("Error loading model: " + modelFile));
            } else {
                logger.error(new RuntimeException("Error loading model type: " + type + " (" + params.toString() + ")"));
            }
        }
        // Clear base material
        if (materials != null) {
            if (materials.containsKey("base") && materials.get("base").size() < 2)
                materials.get("base").clear();

            return new Pair<>(model, materials);
        } else {
            return null;
        }
    }

    public void load(Matrix4 localTransform) {
        if (manager == null) {
            manager = AssetBean.manager();
        }
        if (manager.isLoaded(Settings.settings.data.dataFile(modelFile))) {
            this.doneLoading(manager, localTransform, null);
            this.modelLoading = false;
            this.modelInitialised = true;
        }
    }

    public void update(boolean relativistic,
                       Matrix4 localTransform,
                       float alpha,
                       int blendSrc,
                       int blendDst,
                       boolean blendEnabled) {
        touch(localTransform);
        if (instance != null) {
            ICamera cam = GaiaSky.instance.getICamera();
            setVROffset(GaiaSky.instance.getCameraManager().naturalCamera);
            setTransparency(alpha, blendSrc, blendDst, blendEnabled);
            if (relativistic) {
                updateRelativisticEffects(cam);
            } else {
                updateRelativisticEffects(cam);
            }
        }
    }

    public void update(Matrix4 localTransform,
                       float alpha,
                       int blendSrc,
                       int blendDst,
                       boolean blendEnabled) {
        update(true, localTransform, alpha, blendSrc, blendDst, blendEnabled);
    }

    public void update(boolean relativistic,
                       Matrix4 localTransform,
                       float alpha) {
        switch (blendMode) {
            case ALPHA ->
                    update(relativistic, localTransform, alpha, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, true);
            case COLOR -> update(relativistic, localTransform, alpha, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR, true);
            case ADDITIVE -> update(relativistic, localTransform, alpha, GL20.GL_ONE, GL20.GL_ONE, true);
            case NONE -> update(relativistic, localTransform, alpha, GL20.GL_ONE, GL20.GL_ONE, false);
        }

    }

    public void update(Matrix4 localTransform,
                       float alpha) {
        update(true, localTransform, alpha);
    }

    public void update(float alpha,
                       boolean relativistic) {
        update(relativistic, null, alpha);
    }

    public void update(float alpha) {
        update(null, alpha);
    }

    public void touch() {
        touch(null);
    }

    /**
     * Initialises the model or texture if LAZY_X_INIT is on
     */
    public void touch(Matrix4 localTransform) {
        if (Settings.settings.scene.initialization.lazyTexture && mtc != null && !mtc.texInitialised) {
            if (!mtc.texLoading) {
                mtc.initialize(name, manager);
                mtc.texLoading = true;
            } else if (mtc.isFinishedLoading(manager)) {
                GaiaSky.postRunnable(() -> {
                    mtc.initMaterial(manager, instance, cc, culling);
                });
                mtc.texLoading = false;
                mtc.texInitialised = true;
            }
        }

        if (localTransform != null && Settings.settings.scene.initialization.lazyMesh && !modelInitialised) {
            if (!modelLoading) {
                String mf = Settings.settings.data.dataFile(modelFile);
                logger.info(I18n.msg("notif.loading", mf));
                AssetBean.addAsset(mf, IntModel.class);
                modelLoading = true;
            } else if (manager.isLoaded(Settings.settings.data.dataFile(modelFile))) {
                IntModel model;
                Pair<IntModel, Map<String, Material>> modMat = initModelFile();
                assert modMat != null;
                model = modMat.getFirst();
                if (modelFile != null && (modelFile.endsWith("gltf") || modelFile.endsWith("glb"))) {
                    instance = new ModelInstanceHack(model, localTransform);
                } else {
                    instance = new IntModelInstance(model, localTransform);
                }

                // COLOR IF NO TEXTURE
                if (mtc == null) {
                    addColorToMaterial();
                }

                this.modelInitialised = true;
                this.modelLoading = false;
            }
        }
    }

    public void setModelInitialized(boolean initialized) {
        this.modelInitialised = initialized;
    }

    public void addColorToMaterial() {
        if (cc != null) {
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                Material material = instance.materials.get(i);
                // Only set color if useColor is true or the material does not have it yet.
                if (useColor || !material.has(ColorAttribute.Diffuse)) {
                    material.set(new ColorAttribute(ColorAttribute.Diffuse, cc[0], cc[1], cc[2], cc[3]));
                    if (!culling) {
                        material.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
                    }
                }
            }
        }
    }

    public void dispose() {
        if (instance != null && instance.model != null)
            instance.model.dispose();
    }

    public void setVROffset(NaturalCamera cam) {
        if (Settings.settings.runtime.openXr && cam.vrOffset != null) {
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                Material mat = instance.materials.get(i);
                if (mat.has(Vector3Attribute.VrOffset)) {
                    cam.vrOffset.put(((Vector3Attribute) Objects.requireNonNull(mat.get(Vector3Attribute.VrOffset))).value);
                    ((Vector3Attribute) Objects.requireNonNull(mat.get(Vector3Attribute.VrOffset))).value.scl(5e4f);
                } else {
                    Vector3Attribute v3a = new Vector3Attribute(Vector3Attribute.VrOffset, cam.vrOffset.toVector3());
                    mat.set(v3a);
                }
            }
        }
    }

    public void setTransparency(float alpha,
                                int blendSrc,
                                int blendDest,
                                boolean blendEnabled) {
        int n = instance.materials.size;
        for (int i = 0; i < n; i++) {
            Material mat = instance.materials.get(i);
            BlendingAttribute ba;
            if (mat.has(BlendingAttribute.Type)) {
                ba = (BlendingAttribute) mat.get(BlendingAttribute.Type);
                assert ba != null;
                ba.destFunction = blendDest;
                ba.sourceFunction = blendSrc;
            } else {
                ba = new BlendingAttribute(blendSrc, blendDest);
                mat.set(ba);
            }
            ba.blended = blendEnabled;
            ba.opacity = alpha;
        }
    }

    private boolean sizeSet = false;

    public void setSize(double sizeInternal) {
        if (!sizeSet && instance != null) {
            float sizeKm = (float) (sizeInternal * Constants.U_TO_KM);
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                Material mat = instance.materials.get(i);
                mat.set(new FloatAttribute(FloatAttribute.BodySize, sizeKm));
            }
            sizeSet = true;
        }
    }

    public void disableBlending() {
        int n = instance.materials.size;
        for (int i = 0; i < n; i++) {
            Material mat = instance.materials.get(i);
            BlendingAttribute ba;
            if (mat.has(BlendingAttribute.Type)) {
                ba = (BlendingAttribute) mat.get(BlendingAttribute.Type);
            } else {
                ba = new BlendingAttribute(false, 1);
                mat.set(ba);
            }
            assert ba != null;
            ba.blended = false;
        }

    }

    /**
     * Sets the depth test of this model using the information in the {@link #blendMode}
     * attribute like so:
     * <ul>
     *     <li>blendMode = alpha -> depth reads and writes</li>
     *     <li>blendMode = additive -> depth reads but no writes</li>
     * </ul>
     */
    public void updateDepthTest() {
        switch (blendMode) {
            // Read-write depth test.
            case ALPHA, COLOR, NONE -> depthTestReadWrite();
            // Read-only depth test.
            case ADDITIVE -> depthTestReadOnly();
        }
    }

    /**
     * Sets the depth test of this model to read only.
     */
    public void depthTestReadOnly() {
        setDepthTest(GL20.GL_LEQUAL, false);
    }

    /**
     * Sets the depth test of this model to read and write.
     */
    public void depthTestReadWrite() {
        setDepthTest(GL20.GL_LEQUAL, true);
    }

    /**
     * Completely disables the depth test.
     */
    public void depthTestDisable() {
        setDepthTest(GL20.GL_NONE, false);
    }

    public void setDepthTest(int func,
                             boolean mask) {
        if (instance != null) {
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                Material mat = instance.materials.get(i);
                DepthTestAttribute dta;
                if (mat.has(DepthTestAttribute.Type)) {
                    dta = (DepthTestAttribute) mat.get(DepthTestAttribute.Type);
                } else {
                    dta = new DepthTestAttribute();
                    mat.set(dta);
                }
                assert dta != null;
                dta.depthFunc = func;
                dta.depthMask = mask;
            }
        }
    }

    public void setTransparency(float alpha) {
        switch (blendMode) {
            case ALPHA -> setTransparency(alpha, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, true);
            case COLOR -> setTransparency(alpha, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR, true);
            case ADDITIVE -> setTransparency(alpha, GL20.GL_ONE, GL20.GL_ONE, true);
            case NONE -> setTransparency(alpha, GL20.GL_ONE, GL20.GL_ONE, false);
        }
    }

    public void setTransparencyColor(float alpha) {
        if (instance != null) {
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                ((ColorAttribute) Objects.requireNonNull(instance.materials.get(i).get(ColorAttribute.Diffuse))).color.a = alpha;
            }
        }
    }

    public void setFloatExtAttribute(int attrib,
                                     float value) {
        if (instance != null) {
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                Material mat = instance.materials.get(i);
                setFloatAttribute(mat, attrib, value);
            }
        }
    }

    public void setIntAttribute(Material mat,
                                int attribute,
                                int value) {
        if (!mat.has(attribute)) {
            mat.set(new IntAttribute(attribute, value));
        } else {
            ((IntAttribute) Objects.requireNonNull(mat.get(attribute))).value = value;
        }
    }

    public void setFloatAttribute(Material mat,
                                  int attribute,
                                  float value) {
        if (!mat.has(attribute)) {
            mat.set(new FloatAttribute(attribute, value));
        } else {
            ((FloatAttribute) Objects.requireNonNull(mat.get(attribute))).value = value;
        }
    }

    public void setVector3Attribute(Material mat,
                                    int attribute,
                                    Vector3 value) {
        if (!mat.has(attribute)) {
            mat.set(new Vector3Attribute(attribute, value));
        } else {
            ((Vector3Attribute) Objects.requireNonNull(mat.get(attribute))).value.set(value);
        }
    }

    public void setColorAttribute(int attrib,
                                  float[] rgba) {
        if (instance != null) {
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                Material mat = instance.materials.get(i);
                if (!mat.has(attrib)) {
                    mat.set(new ColorAttribute(attrib, new Color(rgba[0], rgba[1], rgba[2], rgba[3])));
                } else {
                    ((ColorAttribute) Objects.requireNonNull(mat.get(attrib))).color.set(rgba[0], rgba[1], rgba[2], rgba[3]);
                }
            }
        }
    }

    /**
     * Sets the type of the model to construct.
     *
     * @param type The type. Currently supported types are
     *             sphere|cylinder|ring|disc.
     */
    public void setType(String type) {
        this.type = type;
    }

    public void setMaterial(MaterialComponent mtc) {
        this.mtc = mtc;
    }

    /**
     * Sets the model file path (this must be a .g3db, .g3dj or .obj).
     *
     * @param model The model file name.
     */
    public void setModel(String model) {
        this.modelFile = model;
    }

    public void setStaticlight(String staticLight) {
        setStaticLight(staticLight);
    }

    public void setStaticlight(Boolean staticLight) {
        setStaticLight(staticLight);
    }

    public void setStaticlight(Double lightLevel) {
        setStaticLight(lightLevel);
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public void setScale(Double scale) {
        this.scale = scale;
    }

    public void setScale(Long scale) {
        this.scale = scale;
    }

    public void setCulling(String culling) {
        try {
            this.culling = Boolean.parseBoolean(culling);
        } catch (Exception ignored) {
        }
    }

    public void setCulling(Boolean culling) {
        this.culling = culling;
    }

    public void setUsecolor(String usecolor) {
        setUseColor(usecolor);
    }

    public void setUseColor(String useColor) {
        try {
            this.useColor = Boolean.parseBoolean(useColor);
        } catch (Exception ignored) {
        }
    }

    public void setUsecolor(Boolean usecolor) {
        setUseColor(usecolor);
    }

    public void setUseColor(Boolean useColor) {
        try {
            this.useColor = useColor;
        } catch (Exception ignored) {
        }
    }

    public BlendMode getBlendMode() {
        return blendMode;
    }

    public void setBlendMode(BlendMode blendMode) {
        if (blendMode != null) {
            this.blendMode = blendMode;
        }
    }

    public void setBlendMode(String blendModeString) {
        if (blendModeString != null && !blendModeString.isBlank()) {
            blendMode = BlendMode.valueOf(blendModeString.trim().toUpperCase());
        }
    }

    public void setBlending(String blendModeString) {
        setBlendMode(blendModeString);
    }

    public void updateEclipsingBodyUniforms(Entity entity) {
        if (!Settings.settings.scene.renderer.eclipses.active) {
            return;
        }
        var graph = Mapper.graph.get(entity);
        if (graph == null) {
            return;
        }
        Entity eclipsingBody = null;
        // Look down at moons.
        if (graph.children != null && !graph.children.isEmpty()) {
            for (var child : graph.children) {
                var base = Mapper.base.get(child);
                if (base.hasCt(ComponentType.Moons)) {
                    eclipsingBody = child;
                    break;
                }
            }
        }

        // Look up at planets.
        if (eclipsingBody == null) {
            var base = Mapper.base.get(entity);
            if (base.hasCt(ComponentType.Moons) && graph.parent != null) {
                var parentBase = Mapper.base.get(graph.parent);
                if (parentBase.hasCt(ComponentType.Planets)) {
                    // Found!
                    eclipsingBody = graph.parent;
                }

            }
        }

        // Add.
        if (eclipsingBody != null) {
            for (Material mat : instance.materials) {
                updateEclipsingBodyUniforms(mat, eclipsingBody);
            }
        }
    }

    private void updateEclipsingBodyUniforms(Material mat,
                                             Entity eclipsingBody) {
        var body = Mapper.body.get(eclipsingBody);
        var graph = Mapper.graph.get(eclipsingBody);

        setVector3Attribute(mat, Vector3Attribute.EclipsingBodyPos, graph.translation.put(aux));
        setFloatAttribute(mat, FloatAttribute.EclipsingBodyRadius, (float) (body.size * 0.5));
        if (Settings.settings.scene.renderer.eclipses.outlines) {
            setIntAttribute(mat, IntAttribute.EclipseOutlines, 1);
        } else {
            mat.remove(IntAttribute.EclipseOutlines);
        }
    }

    private void removeEclipsingBodyUniforms() {
        if (instance == null) {
            return;
        }
        for (Material mat : instance.materials) {
            mat.remove(Vector3Attribute.EclipsingBodyPos);
            mat.remove(FloatAttribute.EclipsingBodyRadius);
            mat.remove(IntAttribute.EclipseOutlines);
        }

    }

    public void updateRelativisticEffects(ICamera camera) {
        updateRelativisticEffects(camera, -1);
    }

    public void updateRelativisticEffects(ICamera camera,
                                          float vc) {
        for (Material mat : instance.materials) {
            updateRelativisticEffects(mat, camera, vc);
        }
    }

    public void updateRelativisticEffects(Material mat,
                                          ICamera camera) {
        updateRelativisticEffects(mat, camera, -1);
    }

    public void updateRelativisticEffects(Material mat,
                                          ICamera camera,
                                          float vc) {
        if (Settings.settings.runtime.relativisticAberration) {
            rec.updateRelativisticEffectsMaterial(mat, camera, vc);
        } else if (rec.hasRelativisticEffects(mat)) {
            rec.removeRelativisticEffectsMaterial(mat);
        }

        if (Settings.settings.runtime.gravitationalWaves) {
            rec.updateGravitationalWavesMaterial(mat);
        } else if (rec.hasGravitationalWaves(mat)) {
            rec.removeGravitationalWavesMaterial(mat);
        }
    }

    public void addReflectionCubemapAttribute(Array<Material> materials) {
        for (Material mat : materials) {
            if (mat.has(ColorAttribute.Metallic) || mat.has(TextureAttribute.Metallic)) {
                MaterialComponent.reflectionCubemap.prepareCubemap(manager);
                mat.set(new CubemapAttribute(CubemapAttribute.ReflectionCubemap, MaterialComponent.reflectionCubemap.cubemap));
            }
        }
    }

    public void removeReflectionCubemapAttribute(Array<Material> materials) {
        for (Material mat : materials) {
            if (mat.has(ColorAttribute.Metallic) || mat.has(TextureAttribute.Metallic)) {
                mat.remove(CubemapAttribute.ReflectionCubemap);
            }
        }
    }

    public boolean hasHeight() {
        return mtc != null && mtc.hasHeight();
    }

    /**
     * Returns whether this model is tessellated or not.
     *
     * @return Whether the model is tessellated.
     */
    public boolean isTessellated() {
        return tessellated && hasHeight();
    }

    @Override
    public void notify(final Event event,
                       Object source,
                       final Object... data) {
        if (event == Event.ECLIPSES_CMD) {
            boolean active = (Boolean) data[0];
            if (!active) {
                removeEclipsingBodyUniforms();
            }
        }
    }

    public boolean isModelInitialised() {
        return modelInitialised;
    }

    /**
     * @return Whether the model is loading or it has finished.
     */
    public boolean isModelLoading() {
        return modelLoading;
    }

    public boolean isStaticLight() {
        return staticLight;
    }

    public void setStaticLight(String staticLight) {
        setStaticlight(Boolean.valueOf(staticLight));
    }

    public void setStaticLight(Boolean staticLight) {
        this.staticLight = staticLight;
        if (staticLight) {
            // Default static light value.
            setStaticLight(0.6);
        }
    }

    public void setStaticLight(Double lightLevel) {
        this.staticLight = true;
        this.ambientColor[0] = lightLevel.floatValue();
        this.ambientColor[1] = lightLevel.floatValue();
        this.ambientColor[2] = lightLevel.floatValue();
        this.ambientColor[3] = 1f;
    }

    public void setAmbientColor(double[] color) {
        this.ambientColor[0] = (float) color[0];
        this.ambientColor[1] = (float) color[1];
        this.ambientColor[2] = (float) color[2];
        if (color.length > 3) {
            this.ambientColor[3] = (float) color[3];
        } else {
            this.ambientColor[3] = 1;
        }
    }

    public void setAmbientLevel(Double level) {
        setAmbientColor(level);
    }

    public void setAmbientColor(Double level) {
        if (level >= 0) {
            this.ambientColor[0] = level.floatValue();
            this.ambientColor[1] = level.floatValue();
            this.ambientColor[2] = level.floatValue();
            this.ambientColor[3] = 1f;
        }
    }

    public void setPrimitiveType(int primitiveType) {
        this.primitiveType = primitiveType;
    }

    public void setPrimitiveType(String primitiveType) {
        this.primitiveType = Primitive.valueOf(primitiveType.toUpperCase()).equals(Primitive.LINES) ? GL20.GL_LINES : GL20.GL_TRIANGLES;
    }

    public String toString() {
        return Objects.requireNonNullElseGet(modelFile, () -> "{" + type + ", params: " + params.toString() + "}");
    }

    /**
     * Creates a random model component using the given seed. Creates
     * random elevation data, as well as diffuse, normal and specular textures.
     *
     * @param seed The seed to use.
     * @param size The size of the base body in internal units.
     */
    public void randomizeAll(long seed,
                             double size) {
        // Type
        setType("sphere");
        // Parameters
        setParams(createModelParameters(1000L, 1.0, false));
        // Material
        MaterialComponent mtc = new MaterialComponent();
        // Randomize material.
        Random rnd = new Random(seed);
        switch(rnd.nextInt(5)) {
            case 0 -> mtc.randomizeAll(seed);
            case 1 -> mtc.randomizeEarthLike(seed);
            case 2 -> mtc.randomizeColdPlanet(seed);
            case 3 -> mtc.randomizeRockyPlanet(seed);
            case 4 -> mtc.randomizeGasGiant(seed);
        }

        // Set to model
        setMaterial(mtc);
    }

    public void print(Log log) {
        if (mtc != null)
            mtc.print(log);
    }

    @Override
    public void updateWith(ModelComponent object) {
        if (object.mtc != null) {
            if (this.mtc == null) {
                setMaterial(object.mtc);
            } else {
                this.mtc.updateWith(object.mtc);
            }
        }
    }

    public void updateTime(double t) {
        int n = instance.materials.size;
        for (int i = 0; i < n; i++) {
            Material mat = instance.materials.get(i);
            if(mat.has(FloatAttribute.Time)) {
                // Update.
                ((FloatAttribute)mat.get(FloatAttribute.Time)).value = (float) t;

            } else {
                // Add attribute.
                mat.set(new FloatAttribute(FloatAttribute.Time, (float) t));
            }
        }
    }
}
