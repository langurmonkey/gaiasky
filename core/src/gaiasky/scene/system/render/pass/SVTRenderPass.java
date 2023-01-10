package gaiasky.scene.system.render.pass;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.view.ModelView;
import gaiasky.util.Settings;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;
import org.lwjgl.opengl.GL30;

import java.util.List;

import static gaiasky.render.RenderGroup.MODEL_PIX;

/**
 * Sparse virtual texture (SVT) view determination render pass.
 */
public class SVTRenderPass {
    /** The scene renderer object. **/
    private final SceneRenderer sceneRenderer;
    /** Contains the code to render models. **/
    private final ModelEntityRenderSystem modelRenderer;

    /** The model view object. **/
    private final ModelView view;

    private final Array<Entity> entities;

    /** The frame buffer to render the SVT view determination. Format should be at least RGBAF16. **/
    private FrameBuffer frameBuffer;

    public SVTRenderPass(final SceneRenderer sceneRenderer) {
        this.sceneRenderer = sceneRenderer;
        this.modelRenderer = new ModelEntityRenderSystem(sceneRenderer);
        this.view = new ModelView();
        this.entities = new Array<>();
    }

    public void initialize() {
        // Initialize frame buffer with 16 bits per channel.
        frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Settings.settings.scene.renderer.shadow.resolution, Settings.settings.scene.renderer.shadow.resolution, true);
        FrameBufferBuilder frameBufferBuilder = new FrameBufferBuilder(640, 480);
        frameBufferBuilder.addFloatAttachment(GL30.GL_RGBA16F, GL30.GL_RGBA, GL30.GL_FLOAT, false);
        frameBuffer = new GaiaSkyFrameBuffer(frameBufferBuilder, 0);
    }

    public void render(ICamera camera) {
        List<IRenderable> models = sceneRenderer.getRenderLists().get(MODEL_PIX.ordinal());
        entities.clear();
        // Collect SVT-enabled models.
        models.forEach(e -> {
            view.setEntity(((Render) e).getEntity());
            if (view.hasSVT()) {
                entities.add(view.getEntity());
            }
        });

        var renderAssets = sceneRenderer.getRenderAssets();

        // Render SVT view determination to frame buffer.
        frameBuffer.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        renderAssets.mbPixelLightingSvtView.begin(camera.getCamera());
        for (var candidate : entities) {
            modelRenderer.render(candidate, renderAssets.mbPixelLightingSvtView, camera, 1, 0, null, RenderGroup.MODEL_PIX, false);
        }
        renderAssets.mbPixelLightingSvtView.end();

        frameBuffer.end();
    }
}
