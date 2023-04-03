/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
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
import gaiasky.util.Settings;
import gaiasky.util.gdx.contrib.postprocess.effects.CubmeapProjectionEffect;
import gaiasky.util.gdx.contrib.postprocess.effects.CubmeapProjectionEffect.CubemapProjection;
import gaiasky.util.gdx.contrib.postprocess.effects.GeometryWarp;
import gaiasky.util.gdx.contrib.postprocess.filters.Copy;
import gaiasky.util.gdx.loader.WarpMeshReader;

import java.util.Set;

/**
 * Renders the cube map projection mode. Basically, it renders the six sides of
 * the cube map (front, back, up, down, right, left) with a 90 degree fov each
 * and applies a cube map projection (spherical, cylindrical, hammer, azimuthal equidistant)
 */
public class RenderModeCubemapProjections extends RenderModeCubemap implements IRenderMode, IObserver {

    private final CubmeapProjectionEffect cubemapProjection;
    private final GeometryWarp geometryWarp;
    private final Copy copy;

    public RenderModeCubemapProjections() {
        super();

        // Geometry warp, if needed.
        var warp = WarpMeshReader.readWarpMeshAscii(Gdx.files.absolute("/home/tsagrista/Documents/spherical-mirror/standard_16x9.data"));
        geometryWarp = new GeometryWarp(warp, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Cubemap projection.
        cubemapProjection = new CubmeapProjectionEffect(0, 0);
        setPlanetariumAngle(Settings.settings.program.modeCubemap.planetarium.angle);
        setPlanetariumAperture(Settings.settings.program.modeCubemap.planetarium.aperture);
        setProjection(Settings.settings.program.modeCubemap.projection);
        setCelestialSphereIndexOfRefraction(Settings.settings.program.modeCubemap.celestialSphereIndexOfRefraction);
        copy = new Copy();

        EventManager.instance.subscribe(this, Event.CUBEMAP_RESOLUTION_CMD, Event.CUBEMAP_PROJECTION_CMD, Event.PLANETARIUM_PROJECTION_CMD, Event.CUBEMAP_CMD, Event.PLANETARIUM_APERTURE_CMD, Event.PLANETARIUM_ANGLE_CMD, Event.INDEXOFREFRACTION_CMD);
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
        // This renders the cubemap to [x|y|z][pos|neg]fb
        super.renderCubemapSides(sgr, camera, t, rw, rh, ppb);

        if (cubemapProjection.getProjection().isSphericalMirror()) {
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
            copy.setInput(resultBuffer).setOutput(null).render();

        // Post render actions
        super.postRender(fb);
    }

    @Override
    public void resize(int rw, int rh, int tw, int th) {
        if(geometryWarp != null) {
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
                setPlanetariumAperture((float) data[0]);
                // Update projection, we may not need -Z anymore!
                GaiaSky.postRunnable(() -> setProjection(Settings.settings.program.modeCubemap.projection));
            }
            case PLANETARIUM_ANGLE_CMD -> setPlanetariumAngle((float) data[0]);
            case INDEXOFREFRACTION_CMD -> GaiaSky.postRunnable(() -> setCelestialSphereIndexOfRefraction((float) data[0]));
            default -> {
            }
            }
        }
    }
}
