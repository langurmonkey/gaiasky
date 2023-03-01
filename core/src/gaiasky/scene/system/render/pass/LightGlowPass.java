package gaiasky.scene.system.render.pass;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.api.IRenderable;
import gaiasky.render.process.RenderModeOpenVR;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.render.system.LightPositionUpdater;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Settings;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

import static gaiasky.render.RenderGroup.*;

public class LightGlowPass {

    private final SceneRenderer sceneRenderer;
    // Light glow pre-render
    private FrameBuffer glowFrameBuffer;
    private final List<IRenderable> stars;
    private final LightPositionUpdater lpu;
    private final Array<Entity> controllers = new Array<>();
    private AbstractRenderSystem billboardStarsRenderer;

    public LightGlowPass(final SceneRenderer sceneRenderer) {
        this.sceneRenderer = sceneRenderer;
        this.stars = new ArrayList<>();
        this.lpu = new LightPositionUpdater();
    }

    public void buildLightGlowData() {
        if (glowFrameBuffer == null) {
            GLFrameBuffer.FrameBufferBuilder fbb = new GLFrameBuffer.FrameBufferBuilder(1920, 1080);
            fbb.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);
            fbb.addBasicDepthRenderBuffer();
            glowFrameBuffer = new GaiaSkyFrameBuffer(fbb, 0, 1);
        }
    }

    public void renderGlowPass(ICamera camera, FrameBuffer frameBuffer) {
        if (frameBuffer == null) {
            frameBuffer = glowFrameBuffer;
        }
        if (Settings.settings.postprocess.lightGlow.active && frameBuffer != null) {
            var renderLists = sceneRenderer.getRenderLists();
            var renderAssets = sceneRenderer.getRenderAssets();
            // Get all billboard stars
            List<IRenderable> billboardStars = renderLists.get(BILLBOARD_STAR.ordinal());

            stars.clear();
            for (IRenderable st : billboardStars) {
                if (st instanceof Render) {
                    if (Mapper.hip.has(((Render) st).getEntity())) {
                        stars.add(st);
                    }
                }
            }

            // Get all models
            List<IRenderable> models = renderLists.get(MODEL_PIX.ordinal());
            List<IRenderable> modelsTess = renderLists.get(MODEL_PIX_TESS.ordinal());

            // VR controllers
            if (Settings.settings.runtime.openXr) {
                RenderModeOpenVR sgrVR = (RenderModeOpenVR) sceneRenderer.getRenderModeOpenVR();
                if (sceneRenderer.getVrContext() != null) {
                    for (Entity m : sgrVR.controllerObjects) {
                        var render = Mapper.render.get(m);
                        if (!models.contains(render))
                            controllers.add(m);
                    }
                }
            }

            frameBuffer.begin();
            Gdx.gl.glEnable(GL30.GL_DEPTH_TEST);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            // Render billboard stars
            billboardStarsRenderer.render(stars, camera, 0, null);

            // Render models
            renderAssets.mbPixelLightingOpaque.begin(camera.getCamera());
            for (IRenderable model : models) {
                sceneRenderer.renderModel(model, renderAssets.mbPixelLightingOpaque);
            }
            renderAssets.mbPixelLightingOpaque.end();

            // Render tessellated models
            if (modelsTess.size() > 0) {
                renderAssets.mbPixelLightingOpaqueTessellation.begin(camera.getCamera());
                for (IRenderable model : modelsTess) {
                    sceneRenderer.renderModel(model, renderAssets.mbPixelLightingOpaqueTessellation);
                }
                renderAssets.mbPixelLightingOpaqueTessellation.end();
            }
            //}

            // Set texture to updater
            lpu.setGlowTexture(frameBuffer.getColorBufferTexture());

            frameBuffer.end();

        }

    }

    public void setBillboardStarsRenderer(AbstractRenderSystem system) {
        this.billboardStarsRenderer = system;
    }

    public FrameBuffer getGlowFrameBuffer(){
        return glowFrameBuffer;
    }

    public LightPositionUpdater getLpu() {
        return lpu;
    }
}
