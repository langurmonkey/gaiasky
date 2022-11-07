/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem.RenderSystemRunnable;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Render;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.GlobalResources;
import gaiasky.util.Settings;
import gaiasky.util.Settings.GraphicsQuality;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Vector3d;

import java.util.Arrays;
import java.util.List;

public class LightPositionUpdater implements RenderSystemRunnable, IObserver {

    private final Object lock;
    private int nLights;
    private float[] positions;
    private float[] viewAngles;
    private float[] colors;
    private final Vector3 auxV;
    private final Vector3d auxD;

    private Texture glowTex;

    public LightPositionUpdater() {
        this.lock = new Object();

        reinitialize(Settings.settings.graphics.quality.getGlowNLights());

        this.auxV = new Vector3();
        this.auxD = new Vector3d();

        EventManager.instance.subscribe(this, Event.GRAPHICS_QUALITY_UPDATED);
    }

    public void reinitialize(int nLights) {
        synchronized (lock) {
            this.nLights = nLights;
            this.positions = initializeList(null, nLights * 2);
            this.viewAngles = initializeList(null, nLights);
            this.colors = initializeList(null, nLights * 3);
        }
    }

    public float[] initializeList(final float[] list, int size) {
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
     * Sets the occlusion texture to use for the glow effect
     *
     * @param tex The texture
     */
    public void setGlowTexture(Texture tex) {
        this.glowTex = tex;
    }

    @Override
    public void run(AbstractRenderSystem renderSystem, List<IRenderable> renderables, ICamera camera) {
        synchronized (lock) {
            int size = renderables.size();
            Settings settings = Settings.settings;
            if (GaiaSky.instance.getPostProcessor().isLightScatterEnabled()) {
                // Compute light positions for light scattering or light
                // glow
                int lightIndex = 0;
                float angleEdgeDeg = camera.getAngleEdge() * MathUtils.radDeg;
                for (int i = size - 1; i >= 0; i--) {
                    IRenderable s = renderables.get(i);
                    if (s instanceof Render) {
                        Render p = (Render) s;
                        var entity = p.getEntity();

                        if (Mapper.hip.has(entity)) {
                            // Is star.
                            var graph = Mapper.graph.get(entity);
                            double angle = GaiaSky.instance.cameraManager.getDirection().angle(graph.translation);
                            if (lightIndex < nLights && (settings.program.modeCubemap.active || settings.runtime.openVr || angle < angleEdgeDeg)) {
                                Vector3d pos3d = graph.translation.put(auxD);

                                // Apply relativistic effects.
                                GlobalResources.applyRelativisticAberration(pos3d, camera);
                                RelativisticEffectsManager.getInstance().gravitationalWavePos(pos3d);

                                Vector3 pos3 = pos3d.put(auxV);

                                float w = settings.graphics.resolution[0];
                                float h = settings.graphics.resolution[1];

                                camera.getCamera().project(pos3, 0, 0, w, h);
                                // Here we **need** to use
                                // Gdx.graphics.getWidth/Height() because we use
                                // camera.project() which uses screen
                                // coordinates only
                                var body = Mapper.body.get(entity);
                                positions[lightIndex * 2] = auxV.x / w;
                                positions[lightIndex * 2 + 1] = auxV.y / h;
                                viewAngles[lightIndex] = (float) body.solidAngleApparent;
                                colors[lightIndex * 3] = body.color[0];
                                colors[lightIndex * 3 + 1] = body.color[1];
                                colors[lightIndex * 3 + 2] = body.color[2];
                                lightIndex++;
                            }
                        }
                    }
                }
                EventManager.publish(Event.LIGHT_POS_2D_UPDATE, this, lightIndex, positions, viewAngles, colors, glowTex);
            } else {
                EventManager.publish(Event.LIGHT_POS_2D_UPDATE, this, 0, positions, viewAngles, colors, glowTex);
            }
        }

    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.GRAPHICS_QUALITY_UPDATED) {// Update graphics quality
            GraphicsQuality gq = (GraphicsQuality) data[0];
            reinitialize(gq.getGlowNLights());
        }
    }
}
