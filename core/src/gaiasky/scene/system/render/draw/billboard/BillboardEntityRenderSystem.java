/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw.billboard;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.*;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.view.BillboardView;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3Q;
import net.jafama.FastMath;

public class BillboardEntityRenderSystem implements IObserver {

    private final Vector3 F31 = new Vector3();
    private final Vector3Q B31 = new Vector3Q();

    private final Color c = new Color();

    private final ParticleUtils utils;

    protected float solidAngleThresholdTopOverFovFactor;
    protected float solidAngleThresholdBottomOverFovFactor;
    protected float fovFactor;

    public BillboardEntityRenderSystem() {
        utils = new ParticleUtils();
        EventManager.instance.subscribe(this, Event.FOV_CHANGED_CMD);
        initRenderAttributes();
    }

    private void initRenderAttributes() {
        if (GaiaSky.instance != null) {
            fovFactor = GaiaSky.instance.getCameraManager()
                    .getFovFactor();
        } else {
            fovFactor = 1f;
        }
        solidAngleThresholdTopOverFovFactor = (float) Constants.STAR_SOLID_ANGLE_THRESHOLD_TOP / fovFactor;
        solidAngleThresholdBottomOverFovFactor = (float) Constants.STAR_SOLID_ANGLE_THRESHOLD_BOTTOM / fovFactor;
    }

    public float getRenderSizeBillboardGalaxy(ICamera camera,
                                              Body body,
                                              ModelScaffolding scaffolding) {
        return body.size * Settings.settings.scene.star.brightness * scaffolding.billboardSizeFactor;
    }

    public void renderBillboardGalaxy(BillboardView view,
                                      float alpha,
                                      ExtShaderProgram shader,
                                      IntMesh mesh,
                                      ICamera camera) {
        var entity = view.getEntity();
        var base = view.base;
        var body = view.body;
        /*
         *  BILLBOARD GALAXIES
         */
        var scaffolding = Mapper.modelScaffolding.get(entity);
        float size = (float) (getRenderSizeBillboardGalaxy(camera, view.body, scaffolding) / Constants.DISTANCE_SCALE_FACTOR);

        shader.setUniformf("u_pos", view.graph.translation);
        shader.setUniformf("u_size", size);

        shader.setUniformf("u_color", body.color[0], body.color[1], body.color[2], alpha);
        shader.setUniformf("u_alpha", alpha * base.opacity);
        shader.setUniformf("u_distance", (float) body.distToCamera);
        shader.setUniformf("u_apparent_angle", (float) body.solidAngleApparent);
        shader.setUniformf("u_time", (float) GaiaSky.instance.getT() / 5f);

        shader.setUniformf("u_radius", size);

        // Render mesh
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
    }

    private void renderCloseUpStar(StarSet set,
                                   Highlight highlight,
                                   DatasetDescription desc,
                                   int idx,
                                   float fovFactor,
                                   Vector3Q cPosD,
                                   ICamera camera,
                                   ExtShaderProgram shader,
                                   IntMesh mesh,
                                   double thPointTimesFovFactor,
                                   float alpha) {
        if (utils.filter(idx, set, desc) && set.isVisible(idx)) {
            IParticleRecord star = set.pointData.get(idx);
            double varScl = utils.getVariableSizeScaling(set, idx);

            double size = set.getSize(idx);
            double sizeVar = size * varScl;
            double radius = sizeVar * Constants.STAR_SIZE_FACTOR;
            Vector3Q starPos = set.fetchPosition(star, camera.getPos(), B31, set.currDeltaYears);
            double distToCamera = starPos.lenDouble();
            double solidAngle = (size * Constants.STAR_SIZE_FACTOR / distToCamera);

            Color.abgr8888ToColor(c, utils.getColor(idx, set, highlight));
            if (solidAngle >= thPointTimesFovFactor) {
                double fuzzySize = getRenderSizeStarSet(size, radius, distToCamera, solidAngle);
                // Ease into billboard.
                alpha *= (float) MathUtilsDouble.flint(solidAngle, thPointTimesFovFactor, thPointTimesFovFactor * 2f, 0, 1);

                shader.setUniformMatrix("u_matrix", camera.getCamera().view);
                shader.setUniformf("u_pos", starPos);
                shader.setUniformf("u_size", (float) fuzzySize);

                shader.setUniformf("u_color", c.r, c.g, c.b, alpha);
                shader.setUniformf("u_distance", (float) distToCamera);
                shader.setUniformf("u_apparent_angle", (float) (solidAngle * Settings.settings.scene.star.pointSize));
                shader.setUniformf("u_radius", (float) radius);

                // Sprite.render
                mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);

            }
        }
    }

    public double getRenderSizeStarSet(double size,
                                       double radius,
                                       double distToCamera,
                                       double solidAngle) {
        double computedSize = size;
        if (solidAngle > solidAngleThresholdBottomOverFovFactor) {
            double dist;
            if (solidAngle > solidAngleThresholdTopOverFovFactor) {
                dist = radius / Constants.STAR_SOLID_ANGLE_THRESHOLD_TOP;
            } else {
                dist = distToCamera / fovFactor;
            }
            computedSize = (size * (dist / radius) * Constants.STAR_SOLID_ANGLE_THRESHOLD_BOTTOM);
        }
        computedSize *= Settings.settings.scene.star.pointSize * Settings.settings.scene.star.glowFactor;

        return computedSize;
    }

    public void renderBillboardStarSet(BillboardView view,
                                       float alpha,
                                       ExtShaderProgram shader,
                                       IntMesh mesh,
                                       ICamera camera) {
        var entity = view.getEntity();
        var base = view.base;
        var set = Mapper.starSet.get(entity);
        var desc = Mapper.datasetDescription.get(entity);
        var highlight = Mapper.highlight.get(entity);

        /*
         *  STARS IN SETS
         */

        // Star set
        double thPointTimesFovFactor = Settings.settings.scene.star.threshold.point * fovFactor;
        double innerRad = 0.006 + Settings.settings.scene.star.pointSize * 0.008;
        alpha = alpha * base.opacity;

        // GENERAL UNIFORMS
        shader.setUniformf("u_th_angle_point", (float) thPointTimesFovFactor);
        // Light glow always disabled with star groups
        shader.setUniformi("u_lightScattering", 0);
        shader.setUniformf("u_inner_rad", (float) innerRad);

        // RENDER ACTUAL STARS
        boolean focusRendered = false;
        int n = FastMath.min(set.numBillboards, set.indices.length);
        for (int i = 0; i < n; i++) {
            if (set.indices[i] >= 0) {
                renderCloseUpStar(set, highlight, desc, set.indices[i], fovFactor, set.cPosD, camera, shader, mesh,
                                  thPointTimesFovFactor, alpha);
                focusRendered = focusRendered || set.indices[i] == set.focusIndex;
            }
        }
        if (set.focus != null && !focusRendered) {
            renderCloseUpStar(set, highlight, desc, set.focusIndex, fovFactor, set.cPosD, camera, shader, mesh,
                              thPointTimesFovFactor, alpha);
        }
    }

    public float getRenderSizeCelestial(ICamera camera,
                                        Entity entity,
                                        Body body,
                                        SolidAngle sa,
                                        ModelScaffolding scaffolding,
                                        ParticleExtra extra) {
        if (extra != null) {
            // Stars, particles
            boolean star = Mapper.hip.has(entity);
            extra.computedSize = body.size;

            if (body.solidAngle > solidAngleThresholdBottomOverFovFactor) {
                double dist;
                if (body.solidAngle > solidAngleThresholdTopOverFovFactor) {
                    dist = (float) extra.radius / Constants.STAR_SOLID_ANGLE_THRESHOLD_TOP;
                } else {
                    dist = body.distToCamera / fovFactor;
                }
                extra.computedSize *= (dist / extra.radius) * Constants.STAR_SOLID_ANGLE_THRESHOLD_BOTTOM;
            }

            extra.computedSize *= Settings.settings.scene.star.pointSize
                    * (star ?
                    Settings.settings.scene.star.glowFactor
                    : 0.2 / (Constants.DISTANCE_SCALE_FACTOR != 1 ? 200.0 : 1.0));
            return (float) (extra.computedSize * extra.primitiveRenderScale);
        } else if (Mapper.fade.has(entity)) {
            // Regular billboards
            return getRenderSizeBillboardGalaxy(camera, body, scaffolding);
        } else {
            // Models
            float thAngleQuad = (float) sa.thresholdQuad * camera.getFovFactor();
            double size = 0f;
            if (body.solidAngle >= sa.thresholdPoint * camera.getFovFactor()) {
                size = FastMath.tan(thAngleQuad) * body.distToCamera * scaffolding.billboardSizeFactor;
            }
            return (float) size;
        }
    }

    public void renderBillboardCelestial(BillboardView view,
                                         float alpha,
                                         ExtShaderProgram shader,
                                         IntMesh mesh,
                                         ICamera camera) {
        var entity = view.getEntity();
        var base = view.base;
        var body = view.body;
        var graph = view.graph;
        var celestial = view.celestial;

        /*
         *  REGULAR STARS, PLANETS, SATELLITES, NEBULAE, SSO and other BILLBOARDS.
         */
        var sa = Mapper.sa.get(entity);
        var extra = Mapper.extra.get(entity);
        var scaffolding = Mapper.modelScaffolding.get(entity);
        boolean isStar = Mapper.hip.has(entity);
        boolean isModel = !isStar && Mapper.model.has(entity);

        final float fuzzySize = getRenderSizeCelestial(camera, entity, body, sa, scaffolding, extra);
        final float radius = (float) (extra != null ? extra.radius : (body.size / (2d)) * scaffolding.sizeScaleFactor);

        Vector3 billboardPosition = graph.translation.put(F31);
        if (isModel) {
            // Bring it a tad closer to the camera to prevent occlusion with orbit.
            // Only for models.
            float len = billboardPosition.len();
            billboardPosition.nor()
                    .scl(len * 0.99f);
        } else {
            // Projection matrix for star corona.
            shader.setUniformMatrix("u_matrix", camera.getCamera().view);
        }

        shader.setUniformf("u_pos", billboardPosition);
        shader.setUniformf("u_size", fuzzySize);

        // Models use the regular color
        float[] color = body.color;

        // Alpha channel:
        // - models:    alpha * (1 - fadeOpacity) * base.opacity
        // - particles: alpha * base.opacity
        float a = extra == null ? alpha * (1f - scaffolding.fadeOpacity) * base.opacity : alpha * base.opacity;
        shader.setUniformf("u_color", color[0], color[1], color[2], a);
        shader.setUniformf("u_inner_rad", (float) celestial.innerRad);
        shader.setUniformf("u_distance", (float) body.distToCamera);
        shader.setUniformf("u_apparent_angle", (float) body.solidAngleApparent);
        shader.setUniformf("u_th_angle_point", (float) sa.thresholdPoint * fovFactor);
        shader.setUniformf("u_vrScale", (float) Constants.DISTANCE_SCALE_FACTOR);

        // Whether light scattering is enabled or not
        shader.setUniformi("u_lightScattering", (isStar && Settings.settings.postprocess.lightGlow.active) ? 1 : 0);

        shader.setUniformf("u_radius", radius);

        // Render the mesh
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
    }

    public void renderBillboardCluster(BillboardView view,
                                       float alpha,
                                       ExtShaderProgram shader,
                                       IntMesh mesh,
                                       ICamera camera) {
        var entity = view.getEntity();
        var base = view.base;
        var body = view.body;
        var graph = view.graph;

        /*
         * STAR CLUSTERS
         */
        var cluster = Mapper.cluster.get(entity);
        // Bind texture
        if (cluster.clusterTex != null) {
            cluster.clusterTex.bind(0);
        }

        float fa = (1 - cluster.fadeAlpha) * 0.6f;

        shader.setUniformf("u_pos", graph.translation);
        shader.setUniformf("u_size", body.size);
        shader.setUniformf("u_color", body.color[0] * fa, body.color[1] * fa, body.color[2] * fa,
                           body.color[3] * alpha * base.opacity * 6.5f);
        // Sprite.render
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
    }

    @Override
    public void notify(Event event,
                       Object source,
                       Object... data) {
        if (event == Event.FOV_CHANGED_CMD) {
            fovFactor = (Float) data[0] / 40f;
            solidAngleThresholdTopOverFovFactor = (float) Constants.STAR_SOLID_ANGLE_THRESHOLD_TOP / fovFactor;
            solidAngleThresholdBottomOverFovFactor = (float) Constants.STAR_SOLID_ANGLE_THRESHOLD_BOTTOM / fovFactor;
        }
    }
}
