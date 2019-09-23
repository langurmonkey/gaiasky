/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.desktop.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.desktop.util.SysUtils;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor;
import gaia.cu9.ari.gaiaorbit.scenegraph.BackgroundModel;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.MaterialComponent;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ModelComponent;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.PostprocessConf.Antialias;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ProgramConf.StereoProfile;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.SceneConf.GraphicsQuality;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.coord.StaticCoordinates;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.PostProcessor;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.effects.*;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters.Glow;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils.ShaderLoader;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.util.HashMap;
import java.util.Map;

public class DesktopPostProcessor implements IPostProcessor, IObserver {
    private Logger.Log logger = Logger.getLogger(this.getClass().getSimpleName());
    private AssetManager manager;
    private PostProcessBean[] pps;

    float bloomFboScale = 0.5f;

    // Intensity of flare
    float flareIntensity = 0.15f;
    // Number of flares
    int nGhosts = 8;
    // Number of samples for the light glow
    int lightGlowNSamples = 1;

    BackgroundModel blurObject;

    Vector3d auxd, prevCampos;
    Vector3 auxf;
    Matrix4 prevViewProj;
    Matrix4 invView, invProj;

    private String starTextureName, lensDirtName, lensColorName, lensStarburstName;

    public DesktopPostProcessor() {
        ShaderLoader.BasePath = "shader/postprocess/";

        auxd = new Vector3d();
        auxf = new Vector3();
        prevCampos = new Vector3d();
        prevViewProj = new Matrix4();
        invView = new Matrix4();
        invProj = new Matrix4();
    }

    public void initialize(AssetManager manager) {
        this.manager = manager;
        starTextureName = GlobalConf.data.dataFile(GlobalResources.unpackTexName("data/tex/base/star-tex-02*.png"));
        lensDirtName = GlobalConf.data.dataFile(GlobalResources.unpackTexName("data/tex/base/lensdirt*.jpg"));
        lensColorName = GlobalConf.data.dataFile("data/tex/base/lenscolor.png");
        lensStarburstName = GlobalConf.data.dataFile("data/tex/base/lensstarburst.jpg");
        manager.load(starTextureName, Texture.class);
        manager.load(lensDirtName, Texture.class);
        manager.load(lensColorName, Texture.class);
        manager.load(lensStarburstName, Texture.class);
        initializeBlurObject();
    }

    public void doneLoading(AssetManager manager) {
        logger.info("Initializing post-processor");

        pps = new PostProcessBean[RenderType.values().length];

        pps[RenderType.screen.index] = newPostProcessor(RenderType.screen, getWidth(RenderType.screen), getHeight(RenderType.screen), manager);
        if (GlobalConf.screenshot.isRedrawMode())
            pps[RenderType.screenshot.index] = newPostProcessor(RenderType.screenshot, getWidth(RenderType.screenshot), getHeight(RenderType.screenshot), manager);
        if (GlobalConf.frame.isRedrawMode())
            pps[RenderType.frame.index] = newPostProcessor(RenderType.frame, getWidth(RenderType.frame), getHeight(RenderType.frame), manager);

        EventManager.instance.subscribe(this, Events.SCREENSHOT_SIZE_UDPATE, Events.FRAME_SIZE_UDPATE, Events.BLOOM_CMD, Events.LENS_FLARE_CMD, Events.MOTION_BLUR_CMD, Events.LIGHT_POS_2D_UPDATED, Events.LIGHT_SCATTERING_CMD, Events.FISHEYE_CMD, Events.CUBEMAP360_CMD, Events.ANTIALIASING_CMD, Events.BRIGHTNESS_CMD, Events.CONTRAST_CMD, Events.HUE_CMD, Events.SATURATION_CMD, Events.GAMMA_CMD, Events.TONEMAPPING_TYPE_CMD, Events.EXPOSURE_CMD, Events.STEREO_PROFILE_CMD, Events.STEREOSCOPIC_CMD, Events.FPS_INFO, Events.FOV_CHANGE_NOTIFICATION, Events.STAR_BRIGHTNESS_CMD, Events.STAR_POINT_SIZE_CMD, Events.CAMERA_MOTION_UPDATED);
    }

    private int getWidth(RenderType type) {
        switch (type) {
        case screen:
            return Gdx.graphics.getWidth();
        case screenshot:
            return GlobalConf.screenshot.SCREENSHOT_WIDTH;
        case frame:
            return GlobalConf.frame.RENDER_WIDTH;
        }
        return 0;
    }

    private int getHeight(RenderType type) {
        switch (type) {
        case screen:
            return Gdx.graphics.getHeight();
        case screenshot:
            return GlobalConf.screenshot.SCREENSHOT_HEIGHT;
        case frame:
            return GlobalConf.frame.RENDER_HEIGHT;
        }
        return 0;
    }

    private PostProcessBean newPostProcessor(RenderType rt, int width, int height, AssetManager manager) {
        PostProcessBean ppb = new PostProcessBean();

        float ar = (float) width / (float) height;

        ppb.pp = new PostProcessor(rt, width, height, true, false, true);

        // DEPTH BUFFER
        //ppb.depthBuffer = new DepthBuffer();
        //ppb.pp.addEffect(ppb.depthBuffer);

        // CAMERA MOTION BLUR
        initCameraBlur(ppb, width, height);

        // LIGHT GLOW
        int lgw, lgh;
        Texture glow = manager.get(starTextureName);
        // TODO Listen to GRAPHICS_QUALITY_CHANGED and apply new settings on the fly
        if (GlobalConf.scene.GRAPHICS_QUALITY.isAtLeast(GraphicsQuality.HIGH)) {
            lightGlowNSamples = 12;
            lgw = 1280;
            Glow.N = 30;
        } else if (GlobalConf.scene.GRAPHICS_QUALITY.isNormal()) {
            lightGlowNSamples = 8;
            lgw = 1000;
            Glow.N = 20;
        } else {
            lightGlowNSamples = 4;
            lgw = 1000;
            Glow.N = 10;
        }
        lgh = Math.round(lgw / ar);
        glow.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        ppb.lightglow = new LightGlow(lgw, lgh);
        ppb.lightglow.setLightGlowTexture(glow);
        ppb.lightglow.setNSamples(lightGlowNSamples);
        ppb.lightglow.setTextureScale(getGlowTextureScale(GlobalConf.scene.STAR_BRIGHTNESS, GlobalConf.scene.STAR_POINT_SIZE, GaiaSky.instance.cam.getFovFactor()));
        ppb.lightglow.setSpiralScale(getGlowSpiralScale(GlobalConf.scene.STAR_BRIGHTNESS, GlobalConf.scene.STAR_POINT_SIZE, GaiaSky.instance.cam.getFovFactor()));
        ppb.lightglow.setEnabled(SysUtils.isMac() ? false : GlobalConf.postprocess.POSTPROCESS_LIGHT_SCATTERING);
        ppb.pp.addEffect(ppb.lightglow);

        /*
            TODO
            This is a pretty brutal patch for macOS. For some obscure reason,
            the sucker will welcome you with a nice cozy blank screen if
            the activation of the light glow effect is
            not delayed. No time to get to the bottom of this.
         */
        if (SysUtils.isMac() && GlobalConf.postprocess.POSTPROCESS_LIGHT_SCATTERING) {
            Task enableLG = new Task() {
                @Override
                public void run() {
                    logger.info("Enabling light glow effect...");
                    ppb.lightglow.setEnabled(GlobalConf.postprocess.POSTPROCESS_LIGHT_SCATTERING);
                }
            };
            Timer.schedule(enableLG, 5);
        }

        // LENS FLARE
        float lensFboScale = 0.2f;
        Texture lcol = manager.get(lensColorName);
        lcol.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        Texture ldirt = manager.get(lensDirtName);
        ldirt.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        Texture lburst = manager.get(lensStarburstName);
        lburst.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        ppb.lens = new LensFlare2((int) (width * lensFboScale), (int) (height * lensFboScale));
        ppb.lens.setGhosts(nGhosts);
        ppb.lens.setHaloWidth(0.5f);
        ppb.lens.setLensColorTexture(lcol);
        ppb.lens.setLensDirtTexture(ldirt);
        ppb.lens.setLensStarburstTexture(lburst);
        ppb.lens.setFlareIntesity(GlobalConf.postprocess.POSTPROCESS_LENS_FLARE ? flareIntensity : 0f);
        ppb.lens.setFlareSaturation(0.8f);
        ppb.lens.setBaseIntesity(1f);
        ppb.lens.setBias(-0.98f);
        ppb.lens.setBlurPasses(35);
        ppb.pp.addEffect(ppb.lens);

        // BLOOM
        ppb.bloom = new Bloom((int) (width * bloomFboScale), (int) (height * bloomFboScale));
        ppb.bloom.setBloomIntesity(GlobalConf.postprocess.POSTPROCESS_BLOOM_INTENSITY);
        ppb.bloom.setThreshold(0.3f);
        ppb.bloom.setBlurPasses(10);
        ppb.bloom.setBlurAmount(20f);
        ppb.bloom.setEnabled(GlobalConf.postprocess.POSTPROCESS_BLOOM_INTENSITY > 0);
        ppb.pp.addEffect(ppb.bloom);

        // DISTORTION (STEREOSCOPIC MODE)
        ppb.curvature = new Curvature();
        ppb.curvature.setDistortion(1.2f);
        ppb.curvature.setZoom(0.75f);
        ppb.curvature.setEnabled(GlobalConf.program.STEREOSCOPIC_MODE && GlobalConf.program.STEREO_PROFILE == StereoProfile.VR_HEADSET);
        ppb.pp.addEffect(ppb.curvature);

        // FISHEYE DISTORTION (DOME)
        ppb.fisheye = new Fisheye();
        ppb.fisheye.setEnabled(GlobalConf.postprocess.POSTPROCESS_FISHEYE);
        ppb.pp.addEffect(ppb.fisheye);

        // ANTIALIAS
        initAntiAliasing(GlobalConf.postprocess.POSTPROCESS_ANTIALIAS, width, height, ppb);

        // LEVELS - BRIGHTNESS, CONTRAST, HUE, SATURATION, GAMMA CORRECTION and HDR TONE MAPPING
        initLevels(ppb);

        return ppb;
    }

    private void initializeBlurObject() {
        // Create blur object
        BackgroundModel bm = new BackgroundModel();
        bm.setName("BlurObject1199");
        bm.setColor(new float[] { 0, 0, 0, 0 });
        bm.setSize(1e14d);
        bm.setCt("");
        bm.setLabel(false);
        bm.setParent("Universe");
        StaticCoordinates sc = new StaticCoordinates();
        sc.setPosition(new double[] { 0, 0, 0 });
        bm.setCoordinates(sc);
        ModelComponent mc = new ModelComponent(true);
        mc.setType("sphere");
        Map<String, Object> params = new HashMap<>();
        params.put("quality", 90l);
        params.put("diameter", 1.0d);
        params.put("flip", true);
        mc.setParams(params);
        MaterialComponent mtc = new MaterialComponent();
        mc.setMaterial(mtc);
        bm.setModel(mc);
        bm.initialize();
        blurObject = bm;
    }

    private void initCameraBlur(PostProcessBean ppb, int width, int height) {
        ppb.camblur = new CameraMotion(width, height);
        ppb.camblur.setBlurMaxSamples(25);
        ppb.camblur.setBlurScale(1f);
        ppb.camblur.setEnabled(GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR > 0);
        ppb.pp.addEffect(ppb.camblur);

        // Add to scene graph
        if (blurObject != null) {
            blurObject.doneLoading(manager);
            Gdx.app.postRunnable(() -> EventManager.instance.post(Events.SCENE_GRAPH_ADD_OBJECT_CMD, blurObject, false));
        }
    }

    private void initLevels(PostProcessBean ppb) {
        ppb.levels = new Levels();
        ppb.levels.setBrightness(GlobalConf.postprocess.POSTPROCESS_BRIGHTNESS);
        ppb.levels.setContrast(GlobalConf.postprocess.POSTPROCESS_CONTRAST);
        ppb.levels.setHue(GlobalConf.postprocess.POSTPROCESS_HUE);
        ppb.levels.setSaturation(GlobalConf.postprocess.POSTPROCESS_SATURATION);
        ppb.levels.setGamma(GlobalConf.postprocess.POSTPROCESS_GAMMA);

        switch (GlobalConf.postprocess.POSTPROCESS_TONEMAPPING_TYPE) {
        case AUTO:
            ppb.levels.enableToneMappingAuto();
            break;
        case EXPOSURE:
            ppb.levels.enableToneMappingExposure();
            ppb.levels.setExposure(GlobalConf.postprocess.POSTPROCESS_EXPOSURE);
            break;
        case NONE:
            ppb.levels.disableToneMapping();
            break;
        }

        ppb.pp.addEffect(ppb.levels);
    }

    private void initAntiAliasing(Antialias aavalue, int width, int height, PostProcessBean ppb) {
        if (aavalue.equals(Antialias.FXAA)) {
            ppb.antialiasing = new Fxaa(width, height);
            ((Fxaa) ppb.antialiasing).setSpanMax(8f);
            ((Fxaa) ppb.antialiasing).setReduceMin(1f / 128f);
            ((Fxaa) ppb.antialiasing).setReduceMul(1f / 8f);
            Logger.getLogger(this.getClass()).debug(I18n.bundle.format("notif.selected", "FXAA"));
        } else if (aavalue.equals(Antialias.NFAA)) {
            ppb.antialiasing = new Nfaa(width, height);
            Logger.getLogger(this.getClass()).debug(I18n.bundle.format("notif.selected", "NFAA"));
        }
        if (ppb.antialiasing != null) {
            ppb.antialiasing.setEnabled(GlobalConf.postprocess.POSTPROCESS_ANTIALIAS.isPostProcessAntialias());
            ppb.pp.addEffect(ppb.antialiasing);
        }
    }

    @Override
    public PostProcessBean getPostProcessBean(RenderType type) {
        return pps[type.index];
    }

    @Override
    public void resize(final int width, final int height) {
        Gdx.app.postRunnable(() -> replace(RenderType.screen, width, height));
    }

    @Override
    public void resizeImmediate(final int width, final int height) {
        replace(RenderType.screen, width, height);
    }

    @Override
    public void dispose() {
        if (pps != null)
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.dispose();
                }
            }
    }

    private float getGlowTextureScale(double starBrightness, float starSize, float fovFactor) {
        float ts = (float) starBrightness * starSize * 7e-2f / fovFactor;
        return ts;
    }

    private float getGlowSpiralScale(double starBrightness, float starSize, float fovFactor) {
        float ss = (float) starBrightness * starSize * 1e-4f / fovFactor;
        return ss;
    }

    @Override
    public void notify(Events event, final Object... data) {
        switch (event) {
        case STAR_BRIGHTNESS_CMD:
            float brightness = (Float) data[0];
            Gdx.app.postRunnable(() -> {
                for (int i = 0; i < RenderType.values().length; i++) {
                    if (pps[i] != null) {
                        PostProcessBean ppb = pps[i];
                        ppb.lightglow.setTextureScale(getGlowTextureScale(brightness, GlobalConf.scene.STAR_POINT_SIZE, GaiaSky.instance.cam.getFovFactor()));
                        ppb.lightglow.setSpiralScale(getGlowSpiralScale(brightness, GlobalConf.scene.STAR_POINT_SIZE, GaiaSky.instance.cam.getFovFactor()));
                    }
                }
            });
            break;
        case STAR_POINT_SIZE_CMD:
            float size = (Float) data[0];
            Gdx.app.postRunnable(() -> {
                for (int i = 0; i < RenderType.values().length; i++) {
                    if (pps[i] != null) {
                        PostProcessBean ppb = pps[i];
                        ppb.lightglow.setTextureScale(getGlowTextureScale(GlobalConf.scene.STAR_BRIGHTNESS, size, GaiaSky.instance.cam.getFovFactor()));
                        ppb.lightglow.setSpiralScale(getGlowSpiralScale(GlobalConf.scene.STAR_BRIGHTNESS, size, GaiaSky.instance.cam.getFovFactor()));
                    }
                }
            });
            break;
        case LIGHT_POS_2D_UPDATED:
            Integer nLights = (Integer) data[0];
            float[] lightPos = (float[]) data[1];
            float[] angles = (float[]) data[2];
            float[] colors = (float[]) data[3];
            Texture prePass = (Texture) data[4];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.lightglow.setLightPositions(nLights, lightPos);
                    ppb.lightglow.setLightViewAngles(angles);
                    ppb.lightglow.setLightColors(colors);
                    if (prePass != null)
                        ppb.lightglow.setPrePassTexture(prePass);
                }
            }
            break;
        case FOV_CHANGE_NOTIFICATION:
            Gdx.app.postRunnable(() -> {
                for (int i = 0; i < RenderType.values().length; i++) {
                    if (pps[i] != null) {
                        PostProcessBean ppb = pps[i];
                        ppb.lightglow.setTextureScale(getGlowTextureScale(GlobalConf.scene.STAR_BRIGHTNESS, GlobalConf.scene.STAR_POINT_SIZE, GaiaSky.instance.cam.getFovFactor()));
                        ppb.lightglow.setSpiralScale(getGlowSpiralScale(GlobalConf.scene.STAR_BRIGHTNESS, GlobalConf.scene.STAR_POINT_SIZE, GaiaSky.instance.cam.getFovFactor()));
                    }
                }
            });
            break;
        case SCREENSHOT_SIZE_UDPATE:
            if (pps != null && GlobalConf.screenshot.isRedrawMode()) {
                int neww = (Integer) data[0];
                int newh = (Integer) data[1];
                if (pps[RenderType.screenshot.index] != null) {
                    if (changed(pps[RenderType.screenshot.index].pp, neww, newh)) {
                        Gdx.app.postRunnable(() -> replace(RenderType.screenshot, neww, newh));
                    }
                } else {
                    pps[RenderType.screenshot.index] = newPostProcessor(RenderType.screenshot, neww, newh, manager);
                }
            }
            break;
        case FRAME_SIZE_UDPATE:
            if (pps != null && GlobalConf.frame.isRedrawMode()) {
                int neww = (Integer) data[0];
                int newh = (Integer) data[1];
                if (pps[RenderType.frame.index] != null) {
                    if (changed(pps[RenderType.frame.index].pp, neww, newh)) {
                        Gdx.app.postRunnable(() -> {
                            replace(RenderType.frame, neww, newh);
                        });
                    }
                } else {
                    pps[RenderType.frame.index] = newPostProcessor(RenderType.frame, neww, newh, manager);
                }
            }
            break;
        case BLOOM_CMD:
            Gdx.app.postRunnable(() -> {
                float intensity = (float) data[0];
                for (int i = 0; i < RenderType.values().length; i++) {
                    if (pps[i] != null) {
                        PostProcessBean ppb = pps[i];
                        ppb.bloom.setBloomIntesity(intensity);
                        ppb.bloom.setEnabled(intensity > 0);
                    }
                }
            });
            break;
        case LENS_FLARE_CMD:
            boolean active = (Boolean) data[0];
            int nnghosts = active ? nGhosts : 0;
            float intensity = active ? flareIntensity : 0;
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.lens.setGhosts(nnghosts);
                    ppb.lens.setFlareIntesity(intensity);
                }
            }
            break;
        case CAMERA_MOTION_UPDATED:
            PerspectiveCamera cam = (PerspectiveCamera) data[3];
            float cameraOffset = (cam.direction.x + cam.direction.y + cam.direction.z);
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.lens.setStarburstOffset(cameraOffset);
                    ppb.lightglow.setOrientation(cameraOffset * 50f);
                }
            }
            // Update previous projectionView matrix
            prevViewProj = cam.combined;
            break;
        case LIGHT_SCATTERING_CMD:
            active = (Boolean) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.lightglow.setEnabled(active);
                }
            }
            break;
        case FISHEYE_CMD:
            active = (Boolean) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.fisheye.setEnabled(active);
                    ppb.lightglow.setNSamples(active ? 1 : lightGlowNSamples);
                }
            }
            break;
        case MOTION_BLUR_CMD:
            Gdx.app.postRunnable(() -> {
                float opacity = (float) data[0];
                for (int i = 0; i < RenderType.values().length; i++) {
                    if (pps[i] != null) {
                        PostProcessBean ppb = pps[i];
                        ppb.camblur.setEnabled(opacity > 0);
                    }
                }
            });
            break;
        case CUBEMAP360_CMD:
            boolean c360 = (Boolean) data[0];
            boolean enabled = !c360 && GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR > 0;
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.camblur.setEnabled(enabled);
                    ppb.lightglow.setNSamples(enabled ? 1 : lightGlowNSamples);
                }
            }

            break;
        case STEREOSCOPIC_CMD:
            updateStereo((boolean) data[0], GlobalConf.program.STEREO_PROFILE);
            break;
        case STEREO_PROFILE_CMD:
            updateStereo(GlobalConf.program.STEREOSCOPIC_MODE, StereoProfile.values()[(Integer) data[0]]);
            break;
        case ANTIALIASING_CMD:
            final Antialias aavalue = (Antialias) data[0];
            Gdx.app.postRunnable(() -> {
                for (int i = 0; i < RenderType.values().length; i++) {
                    if (pps[i] != null) {
                        PostProcessBean ppb = pps[i];
                        if (aavalue.isPostProcessAntialias()) {
                            // clean
                            if (ppb.antialiasing != null) {
                                ppb.antialiasing.setEnabled(false);
                                ppb.pp.removeEffect(ppb.antialiasing);
                                ppb.antialiasing = null;
                            }
                            // update
                            initAntiAliasing(aavalue, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), ppb);
                            // ensure motion blur and levels go after
                            ppb.pp.removeEffect(ppb.levels);
                            initLevels(ppb);
                        } else {
                            // remove
                            if (ppb.antialiasing != null) {
                                ppb.antialiasing.setEnabled(false);
                                ppb.pp.removeEffect(ppb.antialiasing);
                                ppb.antialiasing = null;
                            }
                        }
                    }
                }
            });
            break;
        case BRIGHTNESS_CMD:
            float br = (Float) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.levels.setBrightness(br);
                }
            }
            break;
        case CONTRAST_CMD:
            float cn = (Float) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.levels.setContrast(cn);
                }
            }
            break;
        case HUE_CMD:
            float hue = (Float) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.levels.setHue(hue);
                }
            }
            break;
        case SATURATION_CMD:
            float sat = (Float) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.levels.setSaturation(sat);
                }
            }
            break;
        case GAMMA_CMD:
            float gamma = (Float) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.levels.setGamma(gamma);
                }
            }
            break;
        case TONEMAPPING_TYPE_CMD:
            GlobalConf.PostprocessConf.ToneMapping tm;
            if (data[0] instanceof String) {
                tm = GlobalConf.PostprocessConf.ToneMapping.valueOf((String) data[0]);
            } else {
                tm = (GlobalConf.PostprocessConf.ToneMapping) data[0];
            }
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    switch (tm) {
                    case AUTO:
                        ppb.levels.enableToneMappingAuto();
                        break;
                    case EXPOSURE:
                        ppb.levels.enableToneMappingExposure();
                        break;
                    case ACES:
                        ppb.levels.enableToneMappingACES();
                        break;
                    case UNCHARTED:
                        ppb.levels.enableToneMappingUncharted();
                        break;
                    case FILMIC:
                        ppb.levels.enableToneMappingFilmic();
                        break;
                    case NONE:
                        ppb.levels.disableToneMapping();
                        break;
                    }
                }
            }
            break;
        case EXPOSURE_CMD:
            float exposure = (Float) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.levels.setExposure(exposure);
                }
            }
            break;
        case FPS_INFO:
            Float fps = (Float) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.camblur.setVelocityScale(fps / 60f);
                }
            }
            break;
        default:
            break;
        }

    }

    /**
     * Reloads the postprocessor at the given index with the given width and
     * height.new Runnable() {
     *
     * @param rt
     * @param width
     * @param height
     * @Override public void run()
     */
    private void replace(RenderType rt, final int width, final int height) {
        // Dispose of old post processor
        pps[rt.index].dispose(false);
        // Create new
        pps[rt.index] = newPostProcessor(rt, width, height, manager);
    }

    private boolean changed(PostProcessor postProcess, int width, int height) {
        return (postProcess.getCombinedBuffer().width != width || postProcess.getCombinedBuffer().height != height);
    }

    @Override
    public boolean isLightScatterEnabled() {
        return pps[RenderType.screen.index].lightglow.isEnabled();
    }

    private void updateStereo(boolean stereo, StereoProfile profile) {
        boolean curvatureEnabled = stereo && profile == StereoProfile.VR_HEADSET;
        boolean viewportHalved = stereo && profile != StereoProfile.ANAGLYPHIC && profile != StereoProfile.HD_3DTV_HORIZONTAL;

        for (int i = 0; i < RenderType.values().length; i++) {
            if (pps[i] != null) {
                PostProcessBean ppb = pps[i];
                ppb.curvature.setEnabled(curvatureEnabled);

                RenderType currentRenderType = RenderType.values()[i];
                ppb.lightglow.setViewportSize(getWidth(currentRenderType) / (viewportHalved ? 2 : 1), getHeight(currentRenderType));
            }
        }
    }

}
