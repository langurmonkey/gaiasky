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
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

public class LabelEntityRenderSystem {

    private final Vector3d D31 = new Vector3d();
    private final Vector3d D32 = new Vector3d();
    private final Vector3d D33 = new Vector3d();
    private final Vector3 F31 = new Vector3();
    private final Vector3 F32 = new Vector3();
    private final Vector3b B31 = new Vector3b();

    public LabelEntityRenderSystem() {
    }

    public void renderLocation(LabelView view,
                               ExtSpriteBatch batch,
                               ExtShaderProgram shader,
                               FontRenderSystem sys,
                               RenderingContext rc,
                               ICamera camera) {
        var body = view.body;
        var graph = view.graph;

        // Parent scaffolding.
        var scaffolding = Mapper.modelScaffolding.get(graph.parent);


        Vector3d labelPosition = D31;
        view.textPosition(camera, labelPosition);
        shader.setUniformf("u_viewAngle", view.label.forceLabel ? 2f : (float) (body.solidAngleApparent * scaffolding.locVaMultiplier * Constants.U_TO_KM));
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", view.label.forceLabel ? 1f : scaffolding.locThresholdLabel / (float) Constants.DISTANCE_SCALE_FACTOR);
        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, view.text(), labelPosition, body.distToCamera,
                view.textScale() * camera.getFovFactor(), view.textSize() * camera.getFovFactor() * 0.5f, view.getRadius(), view.label.forceLabel);
    }

    public void renderShape(LabelView view,
                            ExtSpriteBatch batch,
                            ExtShaderProgram shader,
                            FontRenderSystem sys,
                            RenderingContext rc,
                            ICamera camera) {
        var body = view.body;
        var sa = Mapper.sa.get(view.getEntity());

        Vector3d labelPosition = D31;
        view.textPosition(camera, labelPosition);
        shader.setUniformf("u_viewAngle", view.label.forceLabel ? 2f : (float) body.solidAngle);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", view.label.forceLabel ? 1f : (float) sa.thresholdLabel);

        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, view.text(), labelPosition, body.distToCamera,
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
        render2DLabel(batch, shader, rc, ((TextRenderer) sys).fontTitles, view.text(), 0, 96f, title.scale * 1.6f, title.align);

        title.lineHeight = ((TextRenderer) sys).fontTitles.getLineHeight();
    }

    public void renderRuler(LabelView view,
                            ExtSpriteBatch batch,
                            ExtShaderProgram shader,
                            FontRenderSystem sys,
                            RenderingContext rc,
                            ICamera camera) {
        // 3D distance font
        Vector3d labelPosition = D31;
        view.textPosition(camera, labelPosition);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", 1f);

        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, view.text(), labelPosition, view.body.distToCamera,
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
                render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, GlobalResources.formatNumber(d.getFirst()) + " " + d.getSecond(), gr.p01,
                        body.distToCamera, view.textScale(), (float) (gr.d01 * 1e-3d * camera.getFovFactor()), view.getRadius(), min, max, view.label.forceLabel);
            }
            d = GlobalResources.doubleToDistanceString(gr.d02, du);
            if (gr.d02 / body.distToCamera > 0.1f) {
                render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, GlobalResources.formatNumber(d.getFirst()) + " " + d.getSecond(), gr.p02,
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
        var size = (float) Math.min(0.0005, dist* 2.5e-3d * camera.getFovFactor());
        // +X
        label.labelPosition.set(dist, 0d, 0d);
        if (transform.matrix != null)
            label.labelPosition.mul(transform.matrix);
        label.labelPosition.add(v).sub(camera.getPos());
        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, text, label.labelPosition.put(D33), view.body.distToCamera,
                view.textScale(), size, view.getRadius(), min, max, view.label.forceLabel);

        // -X
        label.labelPosition.set(-dist, 0d, 0d);
        if (transform.matrix != null)
            label.labelPosition.mul(transform.matrix);
        label.labelPosition.add(v).sub(camera.getPos());
        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, text, label.labelPosition.put(D33), view.body.distToCamera,
                view.textScale(), size, view.getRadius(), min, max, view.label.forceLabel);

        // +Z
        label.labelPosition.set(0d, 0d, dist);
        if (transform.matrix != null)
            label.labelPosition.mul(transform.matrix);
        label.labelPosition.add(v).sub(camera.getPos());
        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, text, label.labelPosition.put(D33), view.body.distToCamera,
                view.textScale(), size, view.getRadius(), min, max, view.label.forceLabel);

        // -Z
        label.labelPosition.set(0d, 0d, -dist);
        if (transform.matrix != null)
            label.labelPosition.mul(transform.matrix);
        label.labelPosition.add(v).sub(camera.getPos());
        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, text, label.labelPosition.put(D33), view.body.distToCamera,
                view.textScale(), size, view.getRadius(), min, max, view.label.forceLabel);
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
        // 3D distance font
        Vector3d pos = D31;
        view.textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", view.label.forceLabel ? 2f : (float) view.body.solidAngleApparent);
        shader.setUniformf("u_viewAnglePow", view.label.forceLabel ? 1f : view.label.solidAnglePow);
        shader.setUniformf("u_thLabel", view.label.forceLabel ? 1f : (float) view.sa.thresholdLabel);

        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, view.text(), pos, view.body.distToCamera,
                view.textScale() * camera.getFovFactor(), view.textSize() * camera.getFovFactor(), view.getRadius(), view.label.forceLabel);
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

        var set = view.particleSet;

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
        if (view.particleSet.renderParticleLabels && active != null) {
            float thresholdLabel = 1f;
            var pointData = view.particleSet.pointData;
            int n = Math.min(pointData.size(), view.particleSet.numLabels);
            for (int i = 0; i < n; i++) {
                if (set.metadata[i] < Double.MAX_VALUE && set.isVisible(i)) {
                    IParticleRecord pb = pointData.get(active[i]);
                    if (pb.names() != null) {
                        Vector3b particlePosition = view.particleSet.fetchPosition(pb, view.particleSet.cPosD, B31, view.particleSet.currDeltaYears);
                        float distToCamera = (float) particlePosition.lenDouble();
                        float solidAngle = (2e15f * (float) Constants.DISTANCE_SCALE_FACTOR / distToCamera) / camera.getFovFactor();

                        Vector3d labelPosition = particlePosition.put(D32);
                        if (view.particleSet.isWireframe()) {
                            textPosition(camera, labelPosition, distToCamera, solidAngle * 0.3e-6, 0);
                        } else {
                            textPosition(camera, labelPosition, distToCamera, Math.min(view.particleSet.particleSizeLimits[1], solidAngle) * 1e-6, 0);
                        }

                        shader.setUniformf("u_viewAngle", solidAngle);
                        shader.setUniformf("u_viewAnglePow", 1f);
                        shader.setUniformf("u_thLabel", thresholdLabel * camera.getFovFactor());
                        float textSize = (float) FastMath.tanh(solidAngle) * distToCamera * 1e5f;
                        float alpha = Math.min((float) FastMath.atan(textSize / distToCamera), 1.e-3f);
                        textSize = (float) FastMath.tan(alpha) * distToCamera * 0.5f;
                        render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, pb.names()[0], labelPosition, distToCamera,
                                view.textScale() * camera.getFovFactor(), textSize * camera.getFovFactor(), view.getRadius(), view.label.forceLabel);
                    }
                }
            }
        }
    }

    public void renderStarSet(LabelView view,
                              ExtSpriteBatch batch,
                              ExtShaderProgram shader,
                              FontRenderSystem sys,
                              RenderingContext rc,
                              ICamera camera) {
        var set = view.starSet;

        // Dataset label.
        if (view.starSet.renderSetLabel && view.label.labelPosition != null) {
            var pos = D31;
            pos.set(view.label.labelPosition).add(camera.getInversePos());
            shader.setUniformf("u_viewAngle", 90f);
            shader.setUniformf("u_viewAnglePow", 1f);
            shader.setUniformf("u_thLabel", 1f);
            render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, view.text(), pos, pos.len(),
                    view.textScale() * 2f * camera.getFovFactor(), view.textSize() * camera.getFovFactor(), view.getRadius(), view.label.forceLabel);
        }

        if (view.starSet.renderParticleLabels) {
            float thresholdLabel = (float) (Settings.settings.scene.star.threshold.point / Settings.settings.scene.label.number / camera.getFovFactor());

            var pointData = set.pointData;
            var active = set.active;

            Vector3b starPosition = B31;
            int n = Math.min(pointData.size(), set.numLabels);
            for (int i = 0; i < n; i++) {
                if (set.metadata[i] < Double.MAX_VALUE && set.isVisible(i)) {
                    int idx = active[i];
                    if (!renderStarLabel(view, set, idx, starPosition, thresholdLabel, batch, shader, sys, rc, camera)) {
                        // Only render until the first is not rendered.
                        break;
                    }
                }
            }
            for (Integer i : set.forceLabel) {
                if (set.metadata[i] < Double.MAX_VALUE && set.isVisible(i)) {
                    renderStarLabel(view, set, i, starPosition, thresholdLabel, batch, shader, sys, rc, camera);
                }
            }
        }
    }

    /**
     * Renders the label for a single star in a star group.
     */
    private boolean renderStarLabel(LabelView view,
                                    StarSet set,
                                    int idx,
                                    Vector3b starPosition,
                                    float thresholdLabel,
                                    ExtSpriteBatch batch,
                                    ExtShaderProgram shader,
                                    FontRenderSystem sys,
                                    RenderingContext rc,
                                    ICamera camera) {
        boolean forceLabel = set.forceLabel.contains(idx);
        IParticleRecord star = set.pointData.get(idx);
        starPosition = set.fetchPosition(star, set.cPosD, starPosition, set.currDeltaYears);

        double distToCamera = starPosition.lenDouble();
        float radius = (float) set.getRadius(idx);
        if (forceLabel) {
            radius = Math.max(radius, 1e4f);
        }
        float solidAngle = (float) (((radius / distToCamera) / camera.getFovFactor()) * Settings.settings.scene.star.brightness * 1.5f);

        var visibleCamera = camera.isVisible(solidAngle, starPosition.put(D32), distToCamera);
        if (visibleCamera) {
            if (forceLabel || solidAngle > thresholdLabel) {
                Vector3d labelPosition = D32.set(starPosition);
                textPosition(camera, labelPosition, distToCamera, solidAngle, radius);

                shader.setUniformf("u_viewAngle", solidAngle);
                shader.setUniformf("u_viewAnglePow", 1f);
                shader.setUniformf("u_thLabel", thresholdLabel * camera.getFovFactor());
                // Override object color
                shader.setUniform4fv("u_color", view.textColour(star.names()[0]), 0, 4);
                double textSize = FastMath.tanh(solidAngle) * distToCamera * 1e5d;
                float alpha = Math.min((float) FastMath.atan(textSize / distToCamera), 1.e-3f);
                textSize = (float) FastMath.tan(alpha) * distToCamera * 0.5f;
                return render3DLabel(view, batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, star.names()[0], labelPosition, distToCamera,
                        view.textScale() * camera.getFovFactor(), textSize * camera.getFovFactor(), radius, forceLabel);
            } else {
                return false;
            }
        } else {
            return true;
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
        final int stepAngle = Math.round(360f / divisionsU);

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
        var kf = Mapper.keyframes.get(view.getEntity());
        if (kf.selected != null)
            renderKeyframeLabel(view, kf, kf.selected, batch, shader, sys, rc, camera);
        if (kf.highlighted != null)
            renderKeyframeLabel(view, kf, kf.highlighted, batch, shader, sys, rc, camera);
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

    private final double rad02 = Math.toRadians(2);
    private final double rad40 = Math.toRadians(40);

    /**
     * Text position for star sets.
     *
     * @param cam          The camera.
     * @param out          Contains the object position, and is also the output vector to put the result.
     * @param distToCamera The distance to the object.
     * @param solidAngle   Solid angle of the object.
     * @param rad          The radius.
     */
    private void textPosition(ICamera cam,
                              Vector3d out,
                              double distToCamera,
                              double solidAngle,
                              double rad) {
        out.clamp(0, distToCamera - rad);

        // Offset label a bit to the bottom-right of the position.
        Vector3d offset = D33;
        offset.set(cam.getUp());
        offset.crs(out).nor();

        float displacement = (float) MathUtilsDouble.flint(solidAngle, rad02, rad40, 1, 20);
        float offsetDistance = -0.02f * displacement * cam.getFovFactor() * (float) out.len();
        offset.add(cam.getUp()).nor().scl(offsetDistance);

        out.add(offset);

        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    protected void render2DLabel(ExtSpriteBatch batch,
                                 ExtShaderProgram shader,
                                 RenderingContext rc,
                                 BitmapFont font,
                                 String label,
                                 float x,
                                 float y,
                                 float scale,
                                 int align) {
        shader.setUniformf("u_scale", scale);
        DecalUtils.drawFont2D(font, batch, rc, label, x, y, scale, align);
    }

    protected boolean render3DLabel(LabelView view,
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
        return render3DLabel(view, batch, shader, font, camera, rc, label, pos, distToCamera, scale, size, radius, -1, -1, forceLabel);
    }

    protected boolean render3DLabel(LabelView view,
                                    ExtSpriteBatch batch,
                                    ExtShaderProgram shader,
                                    BitmapFont font,
                                    ICamera camera,
                                    RenderingContext rc,
                                    String labelText,
                                    Vector3d labelPosition,
                                    double distToCamera,
                                    float scale,
                                    double size,
                                    double radius,
                                    float minSizeDegrees,
                                    float maxSizeDegrees,
                                    boolean forceLabel) {
        // The smoothing scale must be set according to the distance
        shader.setUniformf("u_scale", Settings.settings.scene.label.size * scale / camera.getFovFactor());

        if (forceLabel || radius == 0 || distToCamera > radius * 1.3) {

            size *= Settings.settings.scene.label.size;

            float rot = 0;
            if (rc.cubemapSide == CubemapSide.SIDE_UP || rc.cubemapSide == CubemapSide.SIDE_DOWN) {
                Vector3 v1 = F31;
                Vector3 v2 = F32;
                camera.getCamera().project(v1.set((float) labelPosition.x, (float) labelPosition.y, (float) labelPosition.z));
                v1.z = 0f;
                v2.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 0f);
                rot = GlobalResources.angle2d(v1, v2) + (rc.cubemapSide == CubemapSide.SIDE_UP ? 90f : -90f);
            }

            shader.setUniformf("u_pos", labelPosition.put(F31));

            // Enable or disable blending
            ((I3DTextRenderable) view).textDepthBuffer();

            DecalUtils.drawFont3D(font, batch, labelText, (float) labelPosition.x, (float) labelPosition.y, (float) labelPosition.z, size, rot, camera, !rc.isCubemap(),
                    minSizeDegrees, maxSizeDegrees);
            return true;
        }
        return false;
    }
}
