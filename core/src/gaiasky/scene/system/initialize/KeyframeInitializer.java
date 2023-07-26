/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

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
import gaiasky.util.color.ColorUtils;

public class KeyframeInitializer extends AbstractInitSystem {

    private final KeyframeUtils utils;
    private final Scene scene;

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

        kf.path = utils.newVerts(scene, "keyframes.path", base.ct, "gaiasky.scenegraph.Polyline", ColorUtils.gGreen, RenderGroup.LINE, false, 0.5f * kf.ss);
        kf.segments = utils.newVerts(scene, "keyframes.segments", base.ct, "gaiasky.scenegraph.Polyline", ColorUtils.gYellow, RenderGroup.LINE, false, 0.6f * kf.ss);
        kf.knots = utils.newVerts(scene, "keyframes.knots", base.ct, "gaiasky.scenegraph.VertsObject", ColorUtils.gGreen, RenderGroup.POINT, false, 8f * kf.ss);
        kf.knotsSeam = utils.newVerts(scene, "keyframes.knots.seam", base.ct, "gaiasky.scenegraph.VertsObject", ColorUtils.gRed, RenderGroup.POINT, false, 8f * kf.ss, false, true);
        kf.selectedKnot = utils.newVerts(scene, "keyframes.selknot", base.ct, "gaiasky.scenegraph.VertsObject", ColorUtils.gPink, RenderGroup.POINT, false, 12f * kf.ss, false, false);
        kf.highlightedKnot = utils.newVerts(scene, "keyframes.highknot", base.ct, "gaiasky.scenegraph.VertsObject", ColorUtils.gYellow, RenderGroup.POINT, false, 12f * kf.ss, false, false);

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
