/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.process;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.api.IPostProcessor.PostProcessBean;
import gaiasky.render.api.IRenderMode;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.gdx.contrib.postprocess.effects.CubmeapProjectionEffect;
import gaiasky.util.gdx.contrib.postprocess.effects.CubmeapProjectionEffect.CubemapProjection;
import gaiasky.util.gdx.contrib.postprocess.effects.WarpingMesh;
import gaiasky.util.gdx.contrib.postprocess.filters.CopyFilter;
import gaiasky.util.gdx.loader.WarpMeshReader;
import gaiasky.util.i18n.I18n;
import gaiasky.util.screenshot.ImageRenderer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class RenderModeCubemapProjections extends RenderModeCubemap implements IRenderMode, IObserver {
    private static final Log logger = Logger.getLogger(RenderModeCubemapProjections.class);

    private final CubmeapProjectionEffect cubemapProjection;
    private WarpingMesh geometryWarp;
    private final CopyFilter copyFilter;

    public RenderModeCubemapProjections() {
        super();

        // Geometry warp, if needed.
        var cubemapSettings = Settings.settings.program.modeCubemap;
        initializeGeometryWarp(cubemapSettings.planetarium.sphericalMirrorWarp);

        // Cubemap projection.
        cubemapProjection = new CubmeapProjectionEffect(0, 0);
        setPlanetariumAngle(Settings.settings.program.modeCubemap.planetarium.angle);
        setPlanetariumAperture(Settings.settings.program.modeCubemap.planetarium.aperture);
        setProjection(Settings.settings.program.modeCubemap.projection);
        setCelestialSphereIndexOfRefraction(Settings.settings.program.modeCubemap.celestialSphereIndexOfRefraction);
        copyFilter = new CopyFilter();

        EventManager.instance.subscribe(this, Event.CUBEMAP_RESOLUTION_CMD, Event.CUBEMAP_PROJECTION_CMD, Event.PLANETARIUM_PROJECTION_CMD,
                Event.CUBEMAP_CMD, Event.PLANETARIUM_APERTURE_CMD, Event.PLANETARIUM_ANGLE_CMD, Event.INDEXOFREFRACTION_CMD,
                Event.PLANETARIUM_GEOMETRYWARP_FILE_CMD, Event.SCREENSHOT_CUBEMAP_CMD);
    }

    private void setProjection(CubemapProjection projection) {
        if (cubemapProjection != null) {
            cubemapProjection.setProjection(projection);
        }
        if (projection.isPlanetarium()) {// In planetarium mode we only render back iff aperture > 180
            xPosFlag = true;
            xNegFlag = true;
            yPosFlag = true;
            yNegFlag = true;
            zPosFlag = true;
            assert cubemapProjection != null;
            zNegFlag = cubemapProjection.getPlanetariumAperture() > 180f;
            setPlanetariumAngle(Settings.settings.program.modeCubemap.planetarium.angle);
        } else {// In 360 mode we always need all sides
            xPosFlag = true;
            xNegFlag = true;
            yPosFlag = true;
            yNegFlag = true;
            zPosFlag = true;
            zNegFlag = true;
            setPlanetariumAngle(0);
        }
    }

    private void setPlanetariumAngle(float planetariumAngle) {
        // We do not use the planetarium angle in the effect because
        // we optimize the rendering of the cubemap sides when
        // using planetarium mode and the aperture is <= 180 by
        // skipping the -Z direction (back). We manipulate
        // the cameras before rendering instead.

        //cubemapEffect.setPlanetariumAngle(planetariumAngle);
        this.angleFromZenith = planetariumAngle;
    }

    private void setPlanetariumAperture(float planetariumAperture) {
        cubemapProjection.setPlanetariumAperture(planetariumAperture);
    }

    private void setCelestialSphereIndexOfRefraction(float ior) {
        cubemapProjection.setCelestialSphereIndexOfRefraction(ior);
    }

    @Override
    public void render(ISceneRenderer sgr, ICamera camera, double t, int rw, int rh, int tw, int th, FrameBuffer fb, PostProcessBean ppb) {
        // Projects the cubemap.
        super.renderCubemapSides(sgr, camera, t, rw, rh, ppb);

        if (cubemapProjection.getProjection().isSphericalMirror() && geometryWarp != null) {
            // Spherical mirror, we need two buffers.
            FrameBuffer middleBuffer = getFrameBuffer(rw, rh, 1);
            resultBuffer = fb == null ? getFrameBuffer(rw, rh, 0) : fb;
            // Project cubemap.
            cubemapProjection.setViewportSize(tw, th);
            cubemapProjection.setSides(xPosFb, xNegFb, yPosFb, yNegFb, zPosFb, zNegFb);
            cubemapProjection.render(null, middleBuffer, null);
            // Geometry warp.
            geometryWarp.setViewportSize(tw, th);
            geometryWarp.render(middleBuffer, resultBuffer, null);
        } else {
            // Render only cubemap projection.
            resultBuffer = fb == null ? getFrameBuffer(rw, rh, 0) : fb;
            cubemapProjection.setViewportSize(tw, th);
            cubemapProjection.setSides(xPosFb, xNegFb, yPosFb, yNegFb, zPosFb, zNegFb);
            cubemapProjection.render(null, resultBuffer, null);
        }

        // To screen
        if (fb == null)
            copyFilter.setInput(resultBuffer).setOutput(null).render();

        // Post render actions
        super.postRender(fb);
    }

    @Override
    public void resize(int rw, int rh, int tw, int th) {
        if (geometryWarp != null) {
            geometryWarp.setViewportSize(rw, rh);
        }
    }

    @Override
    public void dispose() {
        Set<Integer> keySet = frameBufferCubeMap.keySet();
        for (Integer key : keySet) {
            frameBufferCubeMap.get(key).dispose();
        }
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (!Settings.settings.runtime.openXr) {
            switch (event) {
            case CUBEMAP_CMD -> {
                CubemapProjection projection = (CubemapProjection) data[1];
                GaiaSky.postRunnable(() -> setProjection(projection));
            }
            case CUBEMAP_PROJECTION_CMD, PLANETARIUM_PROJECTION_CMD -> {
                CubemapProjection projection = (CubemapProjection) data[0];
                GaiaSky.postRunnable(() -> setProjection(projection));
            }
            case CUBEMAP_RESOLUTION_CMD -> {
                int res = (Integer) data[0];
                GaiaSky.postRunnable(() -> {
                    // Create new ones
                    if (!frameBufferCubeMap.containsKey(getKey(res, res, 0))) {
                        // Clear
                        dispose();
                        frameBufferCubeMap.clear();
                    }
                });
            }
            case PLANETARIUM_APERTURE_CMD -> {
                // Update projection, we may not need -Z anymore!
                GaiaSky.postRunnable(() -> {
                    setPlanetariumAperture((float) data[0]);
                    setProjection(Settings.settings.program.modeCubemap.projection);
                });
            }
            case PLANETARIUM_ANGLE_CMD -> setPlanetariumAngle((float) data[0]);
            case INDEXOFREFRACTION_CMD -> GaiaSky.postRunnable(() -> setCelestialSphereIndexOfRefraction((float) data[0]));
            case PLANETARIUM_GEOMETRYWARP_FILE_CMD -> GaiaSky.postRunnable(() -> initializeGeometryWarp((Path) data[0]));
            case SCREENSHOT_CUBEMAP_CMD -> {
                if(GaiaSky.instance.sceneRenderer.isCubemapRenderMode()) {
                    Path directory = Path.of((String) data[0]);
                    var date = getCurrentTimeStamp();
                    var name = "_cubemap_";
                    GaiaSky.postRunnable(()->{
                        // +Z
                        saveFrameBufferToImage(zPosFb, directory, date + name + "zp" );
                        // -Z
                        saveFrameBufferToImage(zNegFb, directory, date + name + "zm" );
                        // +X
                        saveFrameBufferToImage(xPosFb, directory, date + name + "xp" );
                        // -X
                        saveFrameBufferToImage(xNegFb, directory, date + name + "xm" );
                        // +Y
                        saveFrameBufferToImage(yPosFb, directory, date + name + "yp" );
                        // -Y
                        saveFrameBufferToImage(yNegFb, directory, date + name + "ym" );

                        Logger.getLogger(RenderModeCubemap.class).info(I18n.msg("gui.360.screenshot.ok", directory.resolve(date+name+"[...]")));
                    });
                } else {
                    Logger.getLogger(RenderModeCubemap.class).warn(I18n.msg("gui.360.screenshot.nomode"));
                }
            }

            default -> {
            }
            }
        }
    }

    private static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd_HHmmss");//dd/MM/yyyy
        Date now = new Date();
        return sdfDate.format(now);
    }

    private void saveFrameBufferToImage(FrameBuffer fb, Path location, String filename) {
        final var settings = Settings.settings;
        fb.begin();
        ImageRenderer.renderToImageGl20(location.toString(), filename, fb.getWidth(), fb.getHeight(), settings.screenshot.format, settings.screenshot.quality);
        fb.end();
    }

    private void initializeGeometryWarp(Path file) {
        if (file != null && Files.exists(file)) {
            var warp = WarpMeshReader.readWarpMeshAscii(Gdx.files.absolute(file.toString()));
            geometryWarp = new WarpingMesh(warp, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            logger.info("Spherical mirror geometry warp initialized with: " + file.toString());
        }
    }
}
