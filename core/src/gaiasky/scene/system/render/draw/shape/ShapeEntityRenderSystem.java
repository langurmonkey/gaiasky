package gaiasky.scene.system.render.draw.shape;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import gaiasky.render.RenderingContext;
import gaiasky.render.api.IShapeRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.view.RenderView;
import gaiasky.scenegraph.camera.ICamera;

public class ShapeEntityRenderSystem extends RenderView implements IShapeRenderable {

    @Override
    public void render(ShapeRenderer shapeRenderer, RenderingContext rc, float alpha, ICamera camera) {
        var base = Mapper.base.get(entity);
        var title = Mapper.title.get(entity);

        float lenwtop = 0.5f * title.scale * rc.w();
        float x0top = (rc.w() - lenwtop) / 2f;
        float x1top = x0top + lenwtop;

        float lenwbottom = 0.6f * title.scale * rc.w();
        float x0bottom = (rc.w() - lenwbottom) / 2f;
        float x1bottom = x0bottom + lenwbottom;

        float ytop = (60f + 15f * title.scale) * 1.6f;
        float ybottom = (60f - title.lineHeight * title.scale + 10f * title.scale) * 1.6f;

        // Resize batch
        shapeRenderer.setProjectionMatrix(shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, rc.w(), rc.h()));

        // Lines
        shapeRenderer.setColor(1f, 1f, 1f, base.opacity * alpha);
        shapeRenderer.line(x0top, ytop, x1top, ytop);
        shapeRenderer.line(x0bottom, ybottom, x1bottom, ybottom);

    }
}
