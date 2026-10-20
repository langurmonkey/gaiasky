/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem.RenderSystemRunnable;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.data.util.GlobalResources;
import gaiasky.util.Constants;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Vector3D;

import java.util.Arrays;
import java.util.List;

public class LightPositionUpdater implements RenderSystemRunnable {

    /** Apparent magnitude of the faintest light that still glows. */
    private static final double GLOW_MAG_FAINT = 7.5;
    /** Apparent magnitude of the brightest light: the Sun at one astronomical unit. */
    private static final double GLOW_MAG_BRIGHT = -26.7;
    private static final double GLOW_MAG_SPAN = GLOW_MAG_FAINT - GLOW_MAG_BRIGHT;
    /** Apparent magnitude of a light whose magnitude is unknown: faint, so it does not compete. */
    private static final double GLOW_MAG_UNKNOWN = 4.0;

    private final Object lock;
    private final Vector3 auxV;
    private final Vector3D auxD;
    private int nLights;
    private float[] positions;
    private float[] solidAngles;
    private float[] depths;
    private float[] intensities;
    private float[] colors;

    public LightPositionUpdater() {
        this.lock = new Object();

        reinitialize(GaiaSky.settings().graphics.quality.getGlowNLights());

        this.auxV = new Vector3();
        this.auxD = new Vector3D();
    }

    public void reinitialize(int nLights) {
        synchronized (lock) {
            this.nLights = nLights;
            this.positions = initializeList(null, nLights * 2);
            this.solidAngles = initializeList(null, nLights);
            this.depths = initializeList(null, nLights);
            this.intensities = initializeList(null, nLights);
            this.colors = initializeList(null, nLights * 3);
        }
    }

    public float[] initializeList(float[] list,
                                  int size) {
        if (list == null) {
            return new float[size];
        } else {
            if (list.length == size) {
                return list;
            } else {
                synchronized (list) {
                    return Arrays.copyOf(list, size);
                }
            }
        }
    }

    /**
     * Distance along the camera view direction, in the same units as the logarithmic depth buffer.
     *
     * @param pos    Camera-relative position.
     * @param camDir Direction the camera looks at.
     * @return The distance along the view direction.
     */
    private float viewDepth(Vector3D pos, Vector3 camDir) {
        return (float) (pos.x * camDir.x + pos.y * camDir.y + pos.z * camDir.z);
    }

    /**
     * Apparent magnitude of a light as seen from the camera, from its absolute magnitude and its
     * distance: m = M + 5*log10(d_pc) - 5. The stored apparent magnitude refers to the catalogue
     * position, so it does not follow the camera.
     *
     * @param absMag Absolute magnitude, may be NaN.
     * @param appMag Stored apparent magnitude, may be NaN. Used when the absolute one is missing.
     * @param distU  Distance from the camera to the light, in internal units.
     * @return The apparent magnitude, or {@link #GLOW_MAG_UNKNOWN} when neither is available.
     */
    private double apparentMagnitude(double absMag, double appMag, double distU) {
        if (Double.isFinite(absMag) && distU > 0.0) {
            double m = absMag + 5.0 * Math.log10(distU * Constants.U_TO_PC) - 5.0;
            if (Double.isFinite(m)) {
                return m;
            }
        }
        return Double.isFinite(appMag) ? appMag : GLOW_MAG_UNKNOWN;
    }

    /**
     * Glow strength in [0,1] of a light with the given apparent magnitude, linear in magnitude (which
     * is logarithmic in flux) from the faintest light that glows up to the Sun at 1 AU.
     *
     * @param absMag     Absolute magnitude of the light.
     * @param appMag     Stored apparent magnitude, used when the absolute one is missing.
     * @param distU      Distance from the camera to the light, in internal units.
     * @param brightness Global star brightness setting.
     * @return The strength, in [0,1].
     */
    private float glowStrength(double absMag, double appMag, double distU, double brightness) {
        double mag = apparentMagnitude(absMag, appMag, distU);
        double u = (GLOW_MAG_FAINT - mag) / GLOW_MAG_SPAN;
        u = Math.max(0.0, Math.min(1.0, u));
        return (float) Math.min(1.0, u * brightness);
    }

    @Override
    public void run(AbstractRenderSystem renderSystem,
                    List<IRenderable> renderables,
                    ICamera camera) {
        synchronized (lock) {
            int size = renderables.size();
            var settings = GaiaSky.settings();
            if (GaiaSky.instance.getPostProcessor().isLightScatterEnabled() ||
                    GaiaSky.instance.getPostProcessor().isLensFlareEnabled()) {
                // Compute light positions for light scattering or light
                // glow
                int lightIndex = 0;
                float angleEdgeDeg = camera.getAngleEdge() * MathUtils.radDeg;
                // Direction the camera looks at, in the same frame as the positions below. The depth
                // buffer stores distances along this direction.
                var camDir = camera.getCamera().direction;
                // Global star brightness, so that the glow of the lights follows the user setting.
                double brightness = settings.scene.star.brightness;
                for (int i = size - 1; i >= 0; i--) {
                    IRenderable s = renderables.get(i);
                    if (s instanceof Render p) {
                        var entity = p.getEntity();

                        if (Mapper.hip.has(entity)) {
                            // Is star.
                            var graph = Mapper.graph.get(entity);
                            double angle = GaiaSky.instance.cameraManager.getDirection().angle(graph.translation);
                            if (lightIndex < nLights && (settings.program.modeCubemap.active || settings.runtime.openXr || angle < angleEdgeDeg)) {
                                Vector3D pos3d = graph.translation.put(auxD);

                                // Apply relativistic effects.
                                GlobalResources.applyRelativisticAberration(pos3d, camera);
                                RelativisticEffectsManager.getInstance().gravitationalWavePos(pos3d);

                                // Distance at the current position, not at the catalogue position.
                                double distU = pos3d.len();

                                Vector3 pos3 = pos3d.put(auxV);

                                float w = settings.graphics.resolution[0];
                                float h = settings.graphics.resolution[1];

                                camera.getCamera().project(pos3, 0, 0, w, h);
                                // Here we **need** to use
                                // Gdx.graphics.getWidth/Height() because we use
                                // camera.project() which uses screen
                                // coordinates only
                                var body = Mapper.body.get(entity);
                                var mag = Mapper.magnitude.has(entity) ? Mapper.magnitude.get(entity) : null;
                                positions[lightIndex * 2] = auxV.x / w;
                                positions[lightIndex * 2 + 1] = auxV.y / h;
                                depths[lightIndex] = viewDepth(pos3d, camDir);
                                solidAngles[lightIndex] = (float) body.solidAngleApparent;
                                intensities[lightIndex] = glowStrength(mag != null ? mag.absMag : Double.NaN,
                                                                       mag != null ? mag.appMag : Double.NaN,
                                                                       distU, brightness);
                                colors[lightIndex * 3] = body.color[0];
                                colors[lightIndex * 3 + 1] = body.color[1];
                                colors[lightIndex * 3 + 2] = body.color[2];
                                lightIndex++;
                            }
                        } else if (Mapper.starSet.has(entity)) {
                            // Star set.
                            var starSet = Mapper.starSet.get(entity);
                            var proximityArray = starSet.proximity.updating;
                            for (var record : proximityArray) {
                                if (record != null) {

                                    Vector3D pos3d = auxD.set(record.absolutePos).sub(camera.getPos());
                                    double angle = GaiaSky.instance.cameraManager.getDirection().angle(pos3d);
                                    if (lightIndex < nLights && (settings.program.modeCubemap.active || settings.runtime.openXr || angle < angleEdgeDeg)) {

                                        // Apply relativistic effects.
                                        GlobalResources.applyRelativisticAberration(pos3d, camera);
                                        RelativisticEffectsManager.getInstance().gravitationalWavePos(pos3d);

                                        // Distance at the current position, not at the catalogue position.
                                        double distU = pos3d.len();

                                        Vector3 pos3 = pos3d.put(auxV);

                                        float w = settings.graphics.resolution[0];
                                        float h = settings.graphics.resolution[1];

                                        camera.getCamera().project(pos3, 0, 0, w, h);
                                        // Here we **need** to use
                                        // Gdx.graphics.getWidth/Height() because we use
                                        // camera.project() which uses screen
                                        // coordinates only
                                        positions[lightIndex * 2] = auxV.x / w;
                                        positions[lightIndex * 2 + 1] = auxV.y / h;
                                        depths[lightIndex] = viewDepth(pos3d, camDir);
                                        solidAngles[lightIndex] = (float) starSet.getSolidAngleApparent(record.index);
                                        var particle = starSet.pointData.get(record.index);
                                        intensities[lightIndex] = glowStrength(particle != null ? particle.absMag() : Double.NaN,
                                                                               particle != null ? particle.appMag() : Double.NaN,
                                                                               distU, brightness);
                                        colors[lightIndex * 3] = record.col[0];
                                        colors[lightIndex * 3 + 1] = record.col[1];
                                        colors[lightIndex * 3 + 2] = record.col[2];
                                        lightIndex++;
                                    }
                                }
                            }
                        }
                    }
                }
                EventManager.publish(Event.LIGHT_POS_2D_UPDATE, this, lightIndex, positions, solidAngles, colors, depths, intensities);
            } else {
                EventManager.publish(Event.LIGHT_POS_2D_UPDATE, this, 0, positions, solidAngles, colors, depths, intensities);
            }
        }

    }
}
