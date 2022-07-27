package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.entity.KeyframeUtils;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.scenegraph.Polyline;
import gaiasky.scenegraph.VertsObject;
import gaiasky.util.color.ColorUtils;

public class KeyframeInitializer extends AbstractInitSystem {

    private KeyframeUtils utils;
    private Scene scene;

    public KeyframeInitializer(Scene scene, boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        this.scene = scene;
        this.utils = new KeyframeUtils(scene);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var kf = Mapper.keyframes.get(entity);
        var label = Mapper.label.get(entity);

        label.label = false;
        label.labelFactor = 1;
        label.labelMax = 0.5e-3f;
        label.textScale = 0.3f;
        label.renderConsumer = LabelEntityRenderSystem::renderKeyframe;
        label.renderFunction = LabelView::renderTextKeyframe;
        label.depthBufferConsumer = LabelView::noTextDepthBuffer;

        kf.orientations = new Array<>();

        kf.path = utils.newVerts(scene, "keyframes.path", base.ct, Polyline.class, ColorUtils.gGreen, RenderGroup.LINE, false, 0.5f * kf.ss);
        kf.segments = utils.newVerts(scene, "keyframes.segments", base.ct, Polyline.class, ColorUtils.gYellow, RenderGroup.LINE, false, 0.6f * kf.ss);
        kf.knots = utils.newVerts(scene, "keyframes.knots", base.ct, VertsObject.class, ColorUtils.gGreen, RenderGroup.POINT, false, 8f * kf.ss);
        kf.knotsSeam = utils.newVerts(scene, "keyframes.knots.seam", base.ct, VertsObject.class, ColorUtils.gRed, RenderGroup.POINT, false, 8f * kf.ss, false, true);
        kf.selectedKnot = utils.newVerts(scene, "keyframes.selknot", base.ct, VertsObject.class, ColorUtils.gPink, RenderGroup.POINT, false, 12f * kf.ss, false, false);
        kf.highlightedKnot = utils.newVerts(scene, "keyframes.highknot", base.ct, VertsObject.class, ColorUtils.gYellow, RenderGroup.POINT, false, 12f * kf.ss, false, false);

        kf.objects = new Array<>();
        kf.objects.add(kf.path);
        kf.objects.add(kf.segments);
        kf.objects.add(kf.knots);
        kf.objects.add(kf.knotsSeam);
        kf.objects.add(kf.selectedKnot);
        kf.objects.add(kf.highlightedKnot);
        kf.scene = scene;
    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
