/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.GaiaSky;
import gaiasky.data.AssetBean;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;

import java.util.HashMap;
import java.util.Map;

public class ModelComponent implements Disposable, IObserver {
    private static final Log logger = Logger.getLogger(ModelComponent.class);

    public boolean forceInit = false;
    private static ColorAttribute ambient;
    /**
     * Light never changes; set fixed ambient light for this model
     */
    private Boolean staticLight = false;
    /** Ambient light level for static light objects **/
    private float staticLightLevel = 0.6f;
    /** Flag **/
    private boolean updateStaticLight = false;

    static {
        ambient = new ColorAttribute(ColorAttribute.AmbientLight, (float) GlobalConf.scene.AMBIENT_LIGHT, (float) GlobalConf.scene.AMBIENT_LIGHT, (float) GlobalConf.scene.AMBIENT_LIGHT, 1f);
    }

    public static void toggleAmbientLight(boolean on) {
        if (on) {
            ambient.color.set(.7f, .7f, .7f, 1f);
        } else {
            ambient.color.set((float) GlobalConf.scene.AMBIENT_LIGHT, (float) GlobalConf.scene.AMBIENT_LIGHT, (float) GlobalConf.scene.AMBIENT_LIGHT, 1f);
        }
    }

    /**
     * Sets the ambient light
     *
     * @param level Ambient light level between 0 and 1
     */
    public static void setAmbientLight(float level) {
        ambient.color.set(level, level, level, 1f);
    }

    public IntModelInstance instance;
    public Environment env;
    /** Directional light **/
    public DirectionalLight dLight;

    public Map<String, Object> params;

    public String type, modelFile;

    public double scale = 1d;
    public boolean culling = true;
    private boolean modelInitialised, modelLoading;
    private boolean useColor = true;

    private AssetManager manager;
    private float[] cc;

    /**
     * COMPONENTS
     */
    // Texture
    public MaterialComponent mtc;
    // Relativistic effects
    public RelativisticEffectsComponent rec;
    // Velocity buffer
    public VelocityBufferComponent vbc;

    public ModelComponent() {
        this(true);
    }

    public ModelComponent(Boolean initEnvironment) {
        if (initEnvironment) {
            env = new Environment();
            env.set(ambient);
            // Direction from Sun to Earth
            dLight = new DirectionalLight();
            dLight.color.set(1f, 0f, 0f, 1f);
            env.add(dLight);
        }
    }

    public void initialize() {
        this.initialize(false);
    }

    public void initialize(boolean mesh) {
        FileHandle model = modelFile != null ? GlobalConf.data.dataFileHandle(modelFile) : null;
        if (mesh) {
            if (!GlobalConf.scene.LAZY_MESH_INIT && modelFile != null && model.exists()) {
                AssetBean.addAsset(GlobalConf.data.dataFile(modelFile), IntModel.class);
            }
        } else {
            if (modelFile != null && model.exists()) {
                AssetBean.addAsset(GlobalConf.data.dataFile(modelFile), IntModel.class);
            }
        }

        if ((forceInit || !GlobalConf.scene.LAZY_TEXTURE_INIT) && mtc != null) {
            mtc.initialize();
            mtc.texLoading = true;
        }

        rec = new RelativisticEffectsComponent();
        vbc = new VelocityBufferComponent();

        SkyboxComponent.initSkybox();
    }

    public void doneLoading(AssetManager manager, Matrix4 localTransform, float[] cc) {
        doneLoading(manager, localTransform, cc, false);
    }

    public void doneLoading(AssetManager manager, Matrix4 localTransform, float[] cc, boolean mesh) {
        this.manager = manager;
        this.cc = cc;

        IntModel model = null;
        Map<String, Material> materials = null;

        if (staticLight) {
            // Remove dir and global ambient. Add ambient
            //env.remove(dLight);
            // Ambient

            // If lazy texture init, we turn off the lights until the texture is loaded
            float level = GlobalConf.scene.LAZY_TEXTURE_INIT ? 0f : staticLightLevel;
            ColorAttribute alight = new ColorAttribute(ColorAttribute.AmbientLight, level, level, level, 1f);
            env.set(alight);
            updateStaticLight = GlobalConf.scene.LAZY_TEXTURE_INIT;
        }

        if (!mesh || !GlobalConf.scene.LAZY_MESH_INIT) {
            Pair<IntModel, Map<String, Material>> modmat = initModelFile();
            model = modmat.getFirst();
        }

        // CREATE MAIN MODEL INSTANCE
        if (!mesh || !GlobalConf.scene.LAZY_MESH_INIT) {
            instance = new IntModelInstance(model, localTransform);
        }

        // INITIALIZE MATERIAL
        if ((forceInit || !GlobalConf.scene.LAZY_TEXTURE_INIT) && mtc != null) {
            mtc.initMaterial(manager, instance, cc, culling);
            mtc.texLoading = false;
            mtc.texInitialised = true;
        }

        // COLOR IF NO TEXTURE
        if (mtc == null && instance != null) {
            addColorToMat();
        }
        // Subscribe to new graphics quality setting event
        EventManager.instance.subscribe(this, Events.GRAPHICS_QUALITY_UPDATED);

        modelInitialised = !GlobalConf.scene.LAZY_MESH_INIT;
        modelLoading = false;
    }

    private Pair<IntModel, Map<String, Material>> initModelFile() {
        IntModel model = null;
        Map<String, Material> materials = null;
        if (modelFile != null && manager.isLoaded(GlobalConf.data.dataFile(modelFile))) {
            // Model comes from file (probably .obj or .g3db)
            model = manager.get(GlobalConf.data.dataFile(modelFile), IntModel.class);
            materials = new HashMap<>();
            if (model.materials.size == 0) {
                Material material = new Material();
                model.materials.add(material);
                materials.put("base", material);
            } else {
                if (model.materials.size > 1)
                    for (int i = 0; i < model.materials.size; i++) {
                        materials.put("base" + i, model.materials.get(i));
                    }
                else
                    materials.put("base", model.materials.first());
            }
            // Add skybox to materials if reflection present
            for (Material mat : model.materials) {
                if (mat.has(ColorAttribute.Reflection) && !ColorUtils.isZero(((ColorAttribute) mat.get(ColorAttribute.Reflection)).color)) {
                    SkyboxComponent.prepareSkybox();
                    mat.set(new CubemapAttribute(CubemapAttribute.EnvironmentMap, SkyboxComponent.skybox));
                }
            }

        } else if (type != null) {
            // We create the model
            Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel(type, params, Usage.Position | Usage.Normal | Usage.Tangent | Usage.BiNormal | Usage.TextureCoordinates);
            model = pair.getFirst();
            materials = pair.getSecond();
        } else {
            // Data error!
            logger.error(new RuntimeException("The 'model' element must contain either a 'type' or a 'model' attribute"));
        }
        // Clear base material
        if (materials.containsKey("base"))
            materials.get("base").clear();

        return new Pair<>(model, materials);
    }

    public void update(Matrix4 localTransform, float alpha, int blendSrc, int blendDst){
        touch(localTransform);
        if(instance != null) {
            setTransparency(alpha, blendSrc, blendDst);
            updateRelativisticEffects(GaiaSky.instance.getICamera());
            updateVelocityBufferUniforms(GaiaSky.instance.getICamera());
        }
    }

    public void update(Matrix4 localTransform, float alpha){
        update(localTransform, alpha, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void update(float alpha){
        update(null, alpha);
    }

    public void touch() {
        touch(null);
    }

    /**
     * Initialises the model or texture if LAZY_X_INIT is on
     */
    public void touch(Matrix4 localTransform) {
        if (GlobalConf.scene.LAZY_TEXTURE_INIT && mtc != null && !mtc.texInitialised) {
            if (mtc != null) {
                if (!mtc.texLoading) {
                    mtc.initialize(manager);
                    mtc.texLoading = true;
                } else if (mtc.isFinishedLoading(manager)) {
                    GaiaSky.postRunnable(() -> {
                        mtc.initMaterial(manager, instance, cc, culling);
                        // Set to initialised
                        updateStaticLightImmediate();
                    });
                    mtc.texLoading = false;
                    mtc.texInitialised = true;
                }
            } else if (localTransform == null) {
                // Use color if necessary
                addColorToMat();
                // Set to initialised
                updateStaticLightImmediate();
            }

        }

        if (localTransform != null && GlobalConf.scene.LAZY_MESH_INIT && !modelInitialised) {
            if (!modelLoading) {
                String mf = GlobalConf.data.dataFile(modelFile);
                logger.info(I18n.txt("notif.loading", mf));
                AssetBean.addAsset(mf, IntModel.class);
                modelLoading = true;
            } else if (manager.isLoaded(GlobalConf.data.dataFile(modelFile))) {
                IntModel model;
                Pair<IntModel, Map<String, Material>> modMat = initModelFile();
                model = modMat.getFirst();
                instance = new IntModelInstance(model, localTransform);

                updateStaticLightImmediate();

                // COLOR IF NO TEXTURE
                if (mtc == null && instance != null) {
                    addColorToMat();
                }

                modelInitialised = true;
                modelLoading = false;
            }
        }
    }


    private void updateStaticLight() {
        GaiaSky.postRunnable(() -> {
            updateStaticLightImmediate();
        });
    }

    private void updateStaticLightImmediate() {
        // Update static
        if (updateStaticLight) {
            ColorAttribute ambient = (ColorAttribute) env.get(ColorAttribute.AmbientLight);
            if (ambient != null)
                ambient.color.set(staticLightLevel, staticLightLevel, staticLightLevel, 1.0f);
            updateStaticLight = false;
        }
    }

    public void addColorToMat() {
        if (cc != null && useColor) {
            // Regular mesh, we use the color
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                Material material = instance.materials.get(i);
                if (material.get(TextureAttribute.Ambient) == null && material.get(TextureAttribute.Diffuse) == null) {
                    material.set(new ColorAttribute(ColorAttribute.Diffuse, cc[0], cc[1], cc[2], cc[3]));
                    material.set(new ColorAttribute(ColorAttribute.Ambient, cc[0], cc[1], cc[2], cc[3]));
                    if (!culling)
                        material.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
                }
            }
        }
    }

    public void addDirectionalLight(float r, float g, float b, float x, float y, float z) {
        DirectionalLight dl = new DirectionalLight();
        dl.set(r, g, b, x, y, z);
        env.add(dl);
    }

    public void dispose() {
        if (instance != null && instance.model != null)
            instance.model.dispose();
    }

    public void setTransparency(float alpha, int src, int dst) {
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                Material mat = instance.materials.get(i);
                BlendingAttribute ba;
                if (mat.has(BlendingAttribute.Type)) {
                    ba = (BlendingAttribute) mat.get(BlendingAttribute.Type);
                    ba.destFunction = dst;
                    ba.sourceFunction = src;
                } else {
                    ba = new BlendingAttribute(src, dst);
                    mat.set(ba);
                }
                ba.opacity = alpha;
            }
    }

    public void setDepthTest(int func, boolean mask) {
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
                dta.depthFunc = func;
                dta.depthMask = mask;
            }
        }
    }

    public void setTransparency(float alpha) {
        setTransparency(alpha, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void setTransparencyColor(float alpha) {
        if (instance != null) {
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                ((ColorAttribute) instance.materials.get(i).get(ColorAttribute.Diffuse)).color.a = alpha;
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
     * @param model
     */
    public void setModel(String model) {
        this.modelFile = model;
    }

    public void setStaticlight(String staticLight) {
        setStaticlight(Boolean.valueOf(staticLight));
    }

    public void setStaticlight(Boolean staticLight) {
        this.staticLight = staticLight;
    }

    public void setStaticlight(Double lightLevel) {
        this.staticLight = true;
        this.staticLightLevel = lightLevel.floatValue();
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
        } catch (Exception e) {
        }
    }

    public void setCulling(Boolean culling) {
        this.culling = culling;
    }

    public void setUsecolor(String usecolor) {
        try {
            this.useColor = Boolean.parseBoolean(usecolor);
        } catch (Exception e) {
        }
    }

    public void setUsecolor(Boolean usecolor) {
        try {
            this.useColor = usecolor;
        } catch (Exception e) {
        }
    }

    public void updateVelocityBufferUniforms(ICamera camera) {
        for (Material mat : instance.materials) {
            updateVelocityBufferUniforms(mat, camera);
        }
    }

    public void updateVelocityBufferUniforms(Material mat, ICamera camera) {
        if (GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR) {
            vbc.updateVelocityBufferMaterial(mat, camera);
        } else if (vbc.hasVelocityBuffer(mat)) {
            vbc.removeVelocityBufferMaterial(mat);
        }
    }

    public void updateRelativisticEffects(ICamera camera) {
        updateRelativisticEffects(camera, -1);
    }

    public void updateRelativisticEffects(ICamera camera, float vc) {
        for (Material mat : instance.materials) {
            updateRelativisticEffects(mat, camera, vc);
        }
    }

    public void updateRelativisticEffects(Material mat, ICamera camera) {
        updateRelativisticEffects(mat, camera, -1);
    }

    public void updateRelativisticEffects(Material mat, ICamera camera, float vc) {
        if (GlobalConf.runtime.RELATIVISTIC_ABERRATION) {
            rec.updateRelativisticEffectsMaterial(mat, camera, vc);
        } else if (rec.hasRelativisticEffects(mat)) {
            rec.removeRelativisticEffectsMaterial(mat);
        }

        if (GlobalConf.runtime.GRAVITATIONAL_WAVES) {
            rec.updateGravitationalWavesMaterial(mat);
        } else if (rec.hasGravitationalWaves(mat)) {
            rec.removeGravitationalWavesMaterial(mat);
        }
    }

    public boolean hasHeight() {
        return mtc != null && mtc.hasHeight();
    }

    @Override
    public void notify(final Events event, final Object... data) {
        switch (event) {
        case GRAPHICS_QUALITY_UPDATED:
            GaiaSky.postRunnable(() -> {
                if (mtc != null && mtc.texInitialised) {
                    // Remove current textures
                    if (mtc != null)
                        mtc.disposeTextures(this.manager);
                }
            });
            break;
        default:
            break;
        }
    }

    public String toString(){
        String desc;
        if(modelFile != null)
            desc = modelFile;
        else
            desc = "{" + type + ", params: " + params.toString() + "}";
        return desc;
    }

}
