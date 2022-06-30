package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.entity.KeyframeUtils;
import gaiasky.util.color.ColorUtils;

public class KeyframeInitializer extends InitSystem {

    private KeyframeUtils utils;

    public KeyframeInitializer(Scene scene, boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        this.utils = new KeyframeUtils(scene);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var kf = Mapper.keyframes.get(entity);

        kf.orientations = new Array<>();

        kf.path = utils.newVerts("Keyframes.path", base.ct, ColorUtils.gGreen, RenderGroup.LINE, false, 0.5f * kf.ss);
        kf.segments = utils.newVerts("Keyframes.segments", base.ct, ColorUtils.gYellow, RenderGroup.LINE, false, 0.6f * kf.ss);
        kf.knots = utils.newVerts("Keyframes.knots", base.ct, ColorUtils.gGreen, RenderGroup.POINT, false, 8f * kf.ss);
        kf.knotsSeam = utils.newVerts("Keyframes.knots.seam", base.ct, ColorUtils.gRed, RenderGroup.POINT, false, 8f * kf.ss, false, true);
        kf.selectedKnot = utils.newVerts("Keyframes.selknot", base.ct, ColorUtils.gPink, RenderGroup.POINT, false, 12f * kf.ss, false, false);
        kf.highlightedKnot = utils.newVerts("Keyframes.highknot", base.ct, ColorUtils.gYellow, RenderGroup.POINT, false, 12f * kf.ss, false, false);

        kf.objects = new Array<>();
        kf.objects.add(kf.path);
        kf.objects.add(kf.segments);
        kf.objects.add(kf.knots);
        kf.objects.add(kf.knotsSeam);
        kf.objects.add(kf.selectedKnot);
        kf.objects.add(kf.highlightedKnot);
    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
