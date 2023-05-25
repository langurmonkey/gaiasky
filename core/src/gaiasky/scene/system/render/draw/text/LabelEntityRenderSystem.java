/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw.text;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import gaiasky.render.RenderingContext;
import gaiasky.render.RenderingContext.CubemapSide;
import gaiasky.render.api.I3DTextRenderable;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.FovCamera;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Keyframes;
import gaiasky.scene.component.StarSet;
import gaiasky.scene.component.Title;
import gaiasky.scene.system.render.draw.TextRenderer;
import gaiasky.scene.view.LabelView;
import gaiasky.util.*;
import gaiasky.util.Settings.DistanceUnits;
import gaiasky.util.camera.rec.Keyframe;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

import java.text.DecimalFormat;

public class LabelEntityRenderSystem {

    private final Vector3d D31 = new Vector3d();
    private final Vector3d D32 = new Vector3d();
    private final Vector3d D33 = new Vector3d();
    private final Vector3 F31 = new Vector3();
    private final Vector3 F32 = new Vector3();
    private final Vector3b B31 = new Vector3b();

    private final DecimalFormat nf;

    public LabelEntityRenderSystem() {
        nf = new DecimalFormat("0.###E0");
    }

    public void renderLocation(LabelView view,
                               ExtSpriteBatch batch,
                               ExtShaderProgram shader,
                               FontRenderSystem sys,
                               RenderingContext rc,
                               ICamera camera) {
        var base = view.base;
        var body = view.body;
        var graph = view.graph;

        // Parent scaffolding.
        var scaffolding = Mapper.modelScaffolding.get(graph.parent);

        Vector3d pos = D31;
        view.textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", view.label.forceLabel ? 2f : (float) (body.solidAngleApparent * scaffolding.locVaMultiplier * Constants.U_TO_KM));
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", view.label.forceLabel ? 1f : scaffolding.locThresholdLabel / (float) Constants.DISTANCE_SCALE_FACTOR);
        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, view.text(), pos, body.distToCamera,
                      view.textScale() * camera.getFovFactor(), view.textSize() * camera.getFovFactor() * 0.5f, view.getRadius(), view.label.forceLabel);
    }

    public void renderShape(LabelView view,
                            ExtSpriteBatch batch,
                            ExtShaderProgram shader,
                            FontRenderSystem sys,
                            RenderingContext rc,
                            ICamera camera) {
        var body = view.body;
        var base = view.base;
        var sa = Mapper.sa.get(view.getEntity());

        Vector3d pos = D31;
        view.textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", view.label.forceLabel ? 2f : (float) body.solidAngle);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", view.label.forceLabel ? 1f : (float) sa.thresholdLabel);

        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, view.text(), pos, body.distToCamera,
                      view.textScale() * camera.getFovFactor(), view.textSize() * camera.getFovFactor(), view.getRadius(), view.label.forceLabel);
    }

    public void renderTitle(LabelView view,
                            ExtSpriteBatch batch,
                            ExtShaderProgram shader,
                            FontRenderSystem sys,
                            RenderingContext rc,
                            ICamera camera) {
        var title = view.getComponent(Title.class);

        shader.setUniformf("u_viewAngle", view.label.forceLabel ? 2f : (float) view.body.solidAngleApparent);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", 1f);

        // Resize batch
        batch.setProjectionMatrix(batch.getProjectionMatrix().setToOrtho2D(0, 0, rc.w(), rc.h()));

        // Text
        render2DLabel(batch, shader, rc, ((TextRenderer) sys).fontTitles, camera, view.text(), 0, 96f, title.scale * 1.6f, title.align);

        title.lineHeight = ((TextRenderer) sys).fontTitles.getLineHeight();
    }

    public void renderRuler(LabelView view,
                            ExtSpriteBatch batch,
                            ExtShaderProgram shader,
                            FontRenderSystem sys,
                            RenderingContext rc,
                            ICamera camera) {
        // 3D distance font
        Vector3d pos = D31;
        view.textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", 1f);

        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, view.text(), pos, view.body.distToCamera,
                      view.textScale() * camera.getFovFactor(), view.textSize() * camera.getFovFactor(), view.getRadius(), view.label.forceLabel);
    }

    public void renderRecursiveGrid(LabelView view,
                                    ExtSpriteBatch batch,
                                    ExtShaderProgram shader,
                                    FontRenderSystem sys,
                                    RenderingContext rc,
                                    ICamera camera) {
        var gr = Mapper.gridRec.get(view.getEntity());
        var body = view.body;

        int index = gr.annotations.size() - 1;
        for (int i = 1; i < gr.annotations.size(); i++) {
            if (body.distToCamera > gr.annotations.get(i - 1).getFirst() && body.distToCamera <= gr.annotations.get(i).getFirst()) {
                index = i;
                break;
            }
        }

        // n up, n down (if possible)
        int n = 2;
        for (int i = index - n; i < index + n; i++) {
            if (i >= 0 && i < gr.annotations.size()) {
                // Render
                renderDistanceLabel(view, batch, shader, sys, rc, camera, gr.annotations.get(i).getFirst(), gr.annotations.get(i).getSecond());
            }
        }

        // Projection lines labels
        if (Settings.settings.program.recursiveGrid.origin.isRefSys() && camera.hasFocus() && gr.d01 > 0 && gr.d02 > 0) {
            DistanceUnits du = Settings.settings.program.ui.distanceUnits;
            shader.setUniform4fv("u_color", gr.ccL, 0, 4);
            Pair<Double, String> d = GlobalResources.doubleToDistanceString(gr.d01, du);
            float ff = camera.getFovFactor();
            float min = 0.025f * ff;
            float max = 0.05f * ff;
            if (gr.d01 / body.distToCamera > 0.1f) {
                render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, nf.format(d.getFirst()) + " " + d.getSecond(), gr.p01,
                              body.distToCamera, view.textScale(), (float) (gr.d01 * 1e-3d * camera.getFovFactor()), view.getRadius(), min, max, view.label.forceLabel);
            }
            d = GlobalResources.doubleToDistanceString(gr.d02, du);
            if (gr.d02 / body.distToCamera > 0.1f) {
                render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, nf.format(d.getFirst()) + " " + d.getSecond(), gr.p02,
                              body.distToCamera, view.textScale(), (float) (gr.d02 * 1e-3d * camera.getFovFactor()), view.getRadius(), min, max, view.label.forceLabel);
            }
        }
    }

    private void renderDistanceLabel(LabelView view,
                                     ExtSpriteBatch batch,
                                     ExtShaderProgram shader,
                                     FontRenderSystem sys,
                                     RenderingContext rc,
                                     ICamera camera,
                                     double dist,
                                     String text) {
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1);
        shader.setUniformf("u_thLabel", 1);

        Vector3b v = B31.setZero();
        if (Settings.settings.program.recursiveGrid.origin.isFocus() && camera.hasFocus()) {
            IFocus focus = camera.getFocus();
            focus.getAbsolutePosition(v);
        }
        float ff = camera.getFovFactor();
        float min = 0.025f * ff;
        float max = 0.07f * ff;

        var transform = Mapper.transform.get(view.getEntity());

        var label = view.label;
        // +Z
        label.labelPosition.set(0d, 0d, dist);
        if (transform.matrix != null)
            label.labelPosition.mul(transform.matrix);
        label.labelPosition.add(v).sub(camera.getPos());
        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, text, label.labelPosition.put(D33), view.body.distToCamera,
                      view.textScale(), (float) (dist * 1.5e-3d * camera.getFovFactor()), view.getRadius(), min, max, view.label.forceLabel);

        // -Z
        label.labelPosition.set(0d, 0d, -dist);
        if (transform.matrix != null)
            label.labelPosition.mul(transform.matrix);
        label.labelPosition.add(v).sub(camera.getPos());
        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, text, label.labelPosition.put(D33), view.body.distToCamera,
                      view.textScale(), (float) (dist * 1.5e-3d * camera.getFovFactor()), view.getRadius(), min, max, view.label.forceLabel);
    }

    public void renderMesh(LabelView view,
                           ExtSpriteBatch batch,
                           ExtShaderProgram shader,
                           FontRenderSystem sys,
                           RenderingContext rc,
                           ICamera camera) {
        Vector3d pos = D31;
        view.textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", 1f);
        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, view.text(), pos, view.body.distToCamera,
                      view.textScale() * camera.getFovFactor(), view.textSize() * camera.getFovFactor(), view.getRadius(), view.label.forceLabel);
    }

    public void renderConstellation(LabelView view,
                                    ExtSpriteBatch batch,
                                    ExtShaderProgram shader,
                                    FontRenderSystem sys,
                                    RenderingContext rc,
                                    ICamera camera) {
        Vector3d pos = D31;
        view.textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1);
        shader.setUniformf("u_thLabel", 1);
        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, view.text(), pos, view.body.distToCamera,
                      view.textScale() * camera.getFovFactor(), view.textSize() * camera.getFovFactor(), view.getRadius(), view.label.forceLabel);
    }

    public void renderCelestial(LabelView view,
                                ExtSpriteBatch batch,
                                ExtShaderProgram shader,
                                FontRenderSystem sys,
                                RenderingContext rc,
                                ICamera camera) {
        if (camera.getCurrent() instanceof FovCamera) {
            render2DLabel(batch, shader, rc, ((TextRenderer) sys).font2d, camera, view.text(), view.body.pos.put(D31));
        } else {
            // 3D distance font
            Vector3d pos = D31;
            view.textPosition(camera, pos);
            shader.setUniformf("u_viewAngle", view.label.forceLabel ? 2f : (float) view.body.solidAngleApparent);
            shader.setUniformf("u_viewAnglePow", view.label.forceLabel ? 1f : view.label.solidAnglePow);
            shader.setUniformf("u_thLabel", view.label.forceLabel ? 1f : (float) view.sa.thresholdLabel);

            render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, view.text(), pos, view.body.distToCamera,
                          view.textScale() * camera.getFovFactor(), view.textSize() * camera.getFovFactor(), view.getRadius(), view.label.forceLabel);
        }
    }

    public void renderCluster(LabelView view,
                              ExtSpriteBatch batch,
                              ExtShaderProgram shader,
                              FontRenderSystem sys,
                              RenderingContext rc,
                              ICamera camera) {
        Vector3d pos = D31;
        view.textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", view.label.forceLabel ? 2f : (float) view.body.solidAngle * 500f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", 1f);

        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, view.text(), pos, view.body.distToCamera,
                      view.textScale() * camera.getFovFactor(), view.textSize() * camera.getFovFactor(), view.getRadius(), view.label.forceLabel);
    }

    public void renderBillboardSet(LabelView view,
                                   ExtSpriteBatch batch,
                                   ExtShaderProgram shader,
                                   FontRenderSystem sys,
                                   RenderingContext rc,
                                   ICamera camera) {
        var pos = D31;
        view.textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", 1f);
        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, view.text(), pos, view.body.distToCamera, view.textScale(),
                      view.textSize() * camera.getFovFactor(), view.getRadius(), view.label.forceLabel);
    }

    public void renderParticleSet(LabelView view,
                                  ExtSpriteBatch batch,
                                  ExtShaderProgram shader,
                                  FontRenderSystem sys,
                                  RenderingContext rc,
                                  ICamera camera) {
        // Dataset label.
        if (view.particleSet.renderSetLabel) {
            var pos = D31;
            pos.set(view.label.labelPosition).add(camera.getInversePos());
            shader.setUniformf("u_viewAngle", 90f);
            shader.setUniformf("u_viewAnglePow", 1f);
            shader.setUniformf("u_thLabel", 1f);
            render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, view.text(), pos, pos.len(),
                          view.textScale() * 2f * camera.getFovFactor(), view.textSize() * camera.getFovFactor(), view.getRadius(), view.label.forceLabel);
        }

        // Particle labels.
        var active = view.particleSet.active;
        if (active != null) {
            float thresholdLabel = 1f;
            var pointData = view.particleSet.pointData;
            int n = Math.min(pointData.size(), view.particleSet.numLabels);
            for (int i = 0; i < n; i++) {
                IParticleRecord pb = pointData.get(active[i]);
                if (pb.names() != null) {
                    Vector3d camPos = fetchPosition(pb, view.particleSet.cPosD, D31, 0);
                    float distToCamera = (float) camPos.len();
                    float viewAngle = (2e15f * (float) Constants.DISTANCE_SCALE_FACTOR / distToCamera) / camera.getFovFactor();

                    textPosition(camera, camPos.put(D31), distToCamera, 0);

                    shader.setUniformf("u_viewAngle", viewAngle);
                    shader.setUniformf("u_viewAnglePow", 1f);
                    shader.setUniformf("u_thLabel", thresholdLabel * camera.getFovFactor());
                    float textSize = (float) FastMath.tanh(viewAngle) * distToCamera * 1e5f;
                    float alpha = Math.min((float) FastMath.atan(textSize / distToCamera), 1.e-3f);
                    textSize = (float) FastMath.tan(alpha) * distToCamera * 0.5f;
                    render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, pb.names()[0], camPos.put(D31), distToCamera,
                                  view.textScale() * camera.getFovFactor(), textSize * camera.getFovFactor(), view.getRadius(), view.label.forceLabel);
                }
            }
        }
    }

    /**
     * Fetches the real position of the particle. It will apply the necessary
     * integrations (i.e. proper motion).
     *
     * @param pb          The particle bean
     * @param campos      The position of the camera. If null, the camera position is
     *                    not subtracted so that the coordinates are given in the global
     *                    reference system instead of the camera reference system.
     * @param destination The destination factor
     * @param deltaYears  The delta years
     *
     * @return The vector for chaining
     */
    private Vector3d fetchPosition(IParticleRecord pb,
                                   Vector3d campos,
                                   Vector3d destination,
                                   double deltaYears) {
        if (campos != null)
            return destination.set(pb.x(), pb.y(), pb.z()).sub(campos);
        else
            return destination.set(pb.x(), pb.y(), pb.z());
    }

    public void renderStarSet(LabelView view,
                              ExtSpriteBatch batch,
                              ExtShaderProgram shader,
                              FontRenderSystem sys,
                              RenderingContext rc,
                              ICamera camera) {
        var set = view.starSet;

        float thresholdLabel = (float) (Settings.settings.scene.star.threshold.point / Settings.settings.scene.label.number / camera.getFovFactor());

        var pointData = set.pointData;
        var active = set.active;

        Vector3d starPosition = D31;
        int n = Math.min(pointData.size(), set.numLabels);
        if (camera.getCurrent() instanceof FovCamera) {
            for (int i = 0; i < n; i++) {
                IParticleRecord star = pointData.get(active[i]);
                starPosition = set.fetchPosition(star, set.cPosD, starPosition, set.currDeltaYears);
                double distToCamera = starPosition.len();
                float radius = (float) set.getRadius(set.active[i]);
                float viewAngle = (float) (((radius / distToCamera) / camera.getFovFactor()) * Settings.settings.scene.star.brightness * 6f);

                if (camera.isVisible(viewAngle, starPosition, distToCamera)) {
                    render2DLabel(batch, shader, rc, ((TextRenderer) sys).font2d, camera, star.names()[0], starPosition);
                }
            }
        } else {
            for (int i = 0; i < n; i++) {
                int idx = active[i];
                renderStarLabel(view, set, idx, starPosition, thresholdLabel, batch, shader, sys, rc, camera);
            }
            for (Integer i : set.forceLabelStars) {
                renderStarLabel(view, set, i, starPosition, thresholdLabel, batch, shader, sys, rc, camera);
            }
        }
    }

    /**
     * Renders the label for a single star in a star group.
     */
    private void renderStarLabel(LabelView view,
                                 StarSet set,
                                 int idx,
                                 Vector3d starPosition,
                                 float thresholdLabel,
                                 ExtSpriteBatch batch,
                                 ExtShaderProgram shader,
                                 FontRenderSystem sys,
                                 RenderingContext rc,
                                 ICamera camera) {
        boolean forceLabel = set.forceLabelStars.contains(idx);
        IParticleRecord star = set.pointData.get(idx);
        starPosition = set.fetchPosition(star, set.cPosD, starPosition, set.currDeltaYears);

        double distToCamera = starPosition.len();
        float radius = (float) set.getRadius(idx);
        if (forceLabel) {
            radius = Math.max(radius, 1e4f);
        }
        float viewAngle = (float) (((radius / distToCamera) / camera.getFovFactor()) * Settings.settings.scene.star.brightness * 1.5f);

        if (forceLabel || viewAngle >= thresholdLabel && camera.isVisible(viewAngle, starPosition, distToCamera) && distToCamera > radius * 100) {
            textPosition(camera, starPosition, distToCamera, radius);

            shader.setUniformf("u_viewAngle", viewAngle);
            shader.setUniformf("u_viewAnglePow", 1f);
            shader.setUniformf("u_thLabel", thresholdLabel * camera.getFovFactor());
            // Override object color
            shader.setUniform4fv("u_color", view.textColour(star.names()[0]), 0, 4);
            double textSize = FastMath.tanh(viewAngle) * distToCamera * 1e5d;
            float alpha = Math.min((float) FastMath.atan(textSize / distToCamera), 1.e-3f);
            textSize = (float) FastMath.tan(alpha) * distToCamera * 0.5f;
            render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, star.names()[0], starPosition, distToCamera,
                          view.textScale() * camera.getFovFactor(), textSize * camera.getFovFactor(), radius, forceLabel);
        }
    }

    private static final int divisionsU = 36;

    public void renderGridAnnotations(LabelView view,
                                      ExtSpriteBatch batch,
                                      ExtShaderProgram shader,
                                      FontRenderSystem sys,
                                      RenderingContext rc,
                                      ICamera camera) {
        var entity = view.getEntity();
        var grid = Mapper.grid.get(entity);

        // Horizon
        final float stepAngle = 360f / divisionsU;

        // Labels at 1 parsec.
        float distToCamera = (float) (1 * Constants.PC_TO_U);
        float textSize = (float) ((Settings.settings.runtime.openXr ? 2e12 : 2e4) * Constants.DISTANCE_SCALE_FACTOR);

        shader.setUniformf("u_viewAngle", 1f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", 0f);

        for (int angle = 0; angle < 360; angle += stepAngle) {
            F31.set(Coordinates.sphericalToCartesian(Math.toRadians(angle), 0f, distToCamera, D31).valuesf()).mul(grid.annotTransform);
            effectsPos(F31, camera);
            if (F31.dot(camera.getCamera().direction.nor()) > 0) {
                D31.set(F31).scl(Constants.DISTANCE_SCALE_FACTOR);
                render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, angle(angle), D31, distToCamera,
                              view.textScale() * camera.getFovFactor(), textSize * camera.getFovFactor(), 0, true);
            }

        }
        // North-south line
        for (int angle = -90; angle <= 90; angle += stepAngle) {
            if (angle != 0) {
                F31.set(Coordinates.sphericalToCartesian(0, Math.toRadians(angle), distToCamera, D31).valuesf()).mul(grid.annotTransform);
                effectsPos(F31, camera);
                if (F31.dot(camera.getCamera().direction.nor()) > 0) {
                    D31.set(F31).scl(Constants.DISTANCE_SCALE_FACTOR);
                    render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, angleSign(angle), D31, distToCamera,
                                  view.textScale() * camera.getFovFactor(), textSize * camera.getFovFactor(), 0, true);
                }
                F31.set(Coordinates.sphericalToCartesian(0, Math.toRadians(-angle), -distToCamera, D31).valuesf()).mul(grid.annotTransform);
                effectsPos(F31, camera);
                if (F31.dot(camera.getCamera().direction.nor()) > 0) {
                    D31.set(F31).scl(Constants.DISTANCE_SCALE_FACTOR);
                    render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, angleSign(angle), D31, distToCamera,
                                  view.textScale() * camera.getFovFactor(), textSize * camera.getFovFactor(), 0, true);
                }
            }
        }
    }

    private String angle(int angle) {
        return angle + "°";
    }

    private String angleSign(int angle) {
        return (angle >= 0 ? "+" : "-") + Math.abs(angle) + "°";
    }

    private void effectsPos(Vector3 auxf,
                            ICamera camera) {
        relativisticPos(auxf, camera);
        gravwavePos(auxf);
    }

    private void relativisticPos(Vector3 auxf,
                                 ICamera camera) {
        if (Settings.settings.runtime.relativisticAberration) {
            D31.set(auxf);
            GlobalResources.applyRelativisticAberration(D31, camera);
            D31.put(auxf);
        }
    }

    private void gravwavePos(Vector3 auxf) {
        if (Settings.settings.runtime.gravitationalWaves) {
            D31.set(auxf);
            RelativisticEffectsManager.getInstance().gravitationalWavePos(D31);
            D31.put(auxf);
        }
    }

    public void renderKeyframe(LabelView view,
                               ExtSpriteBatch batch,
                               ExtShaderProgram shader,
                               FontRenderSystem sys,
                               RenderingContext rc,
                               ICamera camera) {
        if (!(camera.getCurrent() instanceof FovCamera)) {
            var kf = Mapper.keyframes.get(view.getEntity());
            if (kf.selected != null)
                renderKeyframeLabel(view, kf, kf.selected, batch, shader, sys, rc, camera);
            if (kf.highlighted != null)
                renderKeyframeLabel(view, kf, kf.highlighted, batch, shader, sys, rc, camera);
        }

    }

    private void renderKeyframeLabel(LabelView view,
                                     Keyframes kfs,
                                     Keyframe kf,
                                     ExtSpriteBatch batch,
                                     ExtShaderProgram shader,
                                     FontRenderSystem sys,
                                     RenderingContext rc,
                                     ICamera camera) {
        Vector3d pos = D31;
        getTextPositionKeyframe(camera, pos, kf);
        float distToCam = (float) D32.set(kf.pos).add(camera.getInversePos()).len();
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1);
        shader.setUniformf("u_thLabel", 1);
        shader.setUniform4fv("u_color", textColour(kfs, kf), 0, 4);

        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, getText(kf), pos, distToCam, view.textScale() * camera.getFovFactor(),
                      view.textSize() * camera.getFovFactor() * distToCam, view.getRadius(), view.label.forceLabel);
    }

    public float[] textColour(Keyframes kfs,
                              Keyframe kf) {
        if (kf == kfs.highlighted)
            return ColorUtils.gYellow;
        else
            return ColorUtils.gPink;
    }

    private String getText(Keyframe kf) {
        return kf.name;
    }

    private void getTextPositionKeyframe(ICamera cam,
                                         Vector3d out,
                                         Keyframe kf) {
        kf.pos.put(out).add(cam.getInversePos());

        Vector3d aux = D32;
        aux.set(cam.getUp());

        aux.crs(out).nor();

        aux.add(cam.getUp()).nor().scl(-Math.tan(0.00872) * out.len());

        out.add(aux);

        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    /**
     * Text position for star sets.
     *
     * @param cam The camera.
     * @param out The output vector to put the result.
     * @param len The length.
     * @param rad The radius.
     */
    private void textPosition(ICamera cam,
                              Vector3d out,
                              double len,
                              double rad) {
        out.clamp(0, len - rad);

        Vector3d aux = D32;
        aux.set(cam.getUp());

        aux.crs(out).nor();

        float dist = -0.02f * cam.getFovFactor() * (float) out.len();

        aux.add(cam.getUp()).nor().scl(dist);

        out.add(aux);

        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    protected void render2DLabel(ExtSpriteBatch batch,
                                 ExtShaderProgram shader,
                                 RenderingContext rc,
                                 BitmapFont font,
                                 ICamera camera,
                                 String label,
                                 Vector3d pos3d) {
        Vector3 p = F31;
        pos3d.setVector3(p);

        camera.getCamera().project(p);
        p.x += 15;
        p.y -= 15;

        shader.setUniformf("scale", 1f);
        DecalUtils.drawFont2D(font, batch, label, p);
    }

    protected void render2DLabel(ExtSpriteBatch batch,
                                 ExtShaderProgram shader,
                                 RenderingContext rc,
                                 BitmapFont font,
                                 ICamera camera,
                                 String label,
                                 float x,
                                 float y,
                                 float scale,
                                 int align) {
        shader.setUniformf("u_scale", scale);
        DecalUtils.drawFont2D(font, batch, rc, label, x, y, scale, align);
    }

    protected void render2DLabel(ExtSpriteBatch batch,
                                 ExtShaderProgram shader,
                                 RenderingContext rc,
                                 BitmapFont font,
                                 ICamera camera,
                                 String label,
                                 float x,
                                 float y) {
        render2DLabel(batch, shader, rc, font, camera, label, x, y, 1f);
    }

    protected void render2DLabel(ExtSpriteBatch batch,
                                 ExtShaderProgram shader,
                                 RenderingContext rc,
                                 BitmapFont font,
                                 ICamera camera,
                                 String label,
                                 float x,
                                 float y,
                                 float scale) {
        render2DLabel(batch, shader, rc, font, camera, label, x, y, scale, -1);
    }

    protected void render3DLabel(LabelView view,
                                 ExtSpriteBatch batch,
                                 ExtShaderProgram shader,
                                 BitmapFont font,
                                 ICamera camera,
                                 RenderingContext rc,
                                 String label,
                                 Vector3d pos,
                                 double distToCamera,
                                 float scale,
                                 double size,
                                 double radius,
                                 boolean forceLabel) {
        render3DLabel(view, batch, shader, font, camera, rc, label, pos, distToCamera, scale, size, radius, -1, -1, forceLabel);
    }

    protected void render3DLabel(LabelView view,
                                 ExtSpriteBatch batch,
                                 ExtShaderProgram shader,
                                 BitmapFont font,
                                 ICamera camera,
                                 RenderingContext rc,
                                 String label,
                                 Vector3d pos,
                                 double distToCamera,
                                 float scale,
                                 double size,
                                 double radius,
                                 float minSizeDegrees,
                                 float maxSizeDegrees,
                                 boolean forceLabel) {
        // The smoothing scale must be set according to the distance
        shader.setUniformf("u_scale", Settings.settings.scene.label.size * scale / camera.getFovFactor());

        if (forceLabel || radius == 0 || distToCamera > radius * 2d) {

            size *= Settings.settings.scene.label.size;

            float rot = 0;
            if (rc.cubemapSide == CubemapSide.SIDE_UP || rc.cubemapSide == CubemapSide.SIDE_DOWN) {
                Vector3 v1 = F31;
                Vector3 v2 = F32;
                camera.getCamera().project(v1.set((float) pos.x, (float) pos.y, (float) pos.z));
                v1.z = 0f;
                v2.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 0f);
                rot = GlobalResources.angle2d(v1, v2) + (rc.cubemapSide == CubemapSide.SIDE_UP ? 90f : -90f);
            }

            shader.setUniformf("u_pos", pos.put(F31));

            // Enable or disable blending
            ((I3DTextRenderable) view).textDepthBuffer();

            DecalUtils.drawFont3D(font, batch, label, (float) pos.x, (float) pos.y, (float) pos.z, size, rot, camera, !rc.isCubemap(), minSizeDegrees, maxSizeDegrees);
        }
    }
}
