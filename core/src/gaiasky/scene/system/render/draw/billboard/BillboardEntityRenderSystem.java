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
import gaiasky.scene.component.*;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.view.BillboardView;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.Vector3d;

public class BillboardEntityRenderSystem implements IObserver {

    private final Vector3 F31 = new Vector3();
    private final Vector3d D31 = new Vector3d();

    private final Color c = new Color();

    private final ParticleUtils utils;

    protected float thpointTimesFovfactor;
    protected float thupOverFovfactor;
    protected float thdownOverFovfactor;
    protected float fovFactor;

    public BillboardEntityRenderSystem() {
        utils = new ParticleUtils();
        EventManager.instance.subscribe(this, Event.FOV_CHANGE_NOTIFICATION);
    }

    private void initRenderAttributes() {
        if (GaiaSky.instance != null) {
            fovFactor = GaiaSky.instance.getCameraManager().getFovFactor();
        } else {
            fovFactor = 1f;
        }
        Settings settings = Settings.settings;
        thpointTimesFovfactor = (float) settings.scene.star.threshold.point;
        thupOverFovfactor = (float) Constants.THRESHOLD_UP / fovFactor;
        thdownOverFovfactor = (float) Constants.THRESHOLD_DOWN / fovFactor;
    }

    public float getRenderSizeBillboardGalaxy(ICamera camera, Body body, ModelScaffolding scaffolding) {
        return body.size * Settings.settings.scene.star.brightness * scaffolding.billboardSizeFactor;
    }

    public void renderBillboardGalaxy(BillboardView view, float alpha, ExtShaderProgram shader, IntMesh mesh, ICamera camera) {
        var entity = view.getEntity();
        var base = view.base;
        var body = view.body;
        var celestial = view.celestial;
        /*
         *  BILLBOARD GALAXIES
         */
        var scaffolding = Mapper.modelScaffolding.get(entity);
        float size = (float) (getRenderSizeBillboardGalaxy(camera, view.body, scaffolding) / Constants.DISTANCE_SCALE_FACTOR);

        shader.setUniformf("u_pos", view.graph.translation.put(F31));
        shader.setUniformf("u_size", size);

        shader.setUniformf("u_color", view.celestial.colorPale[0], celestial.colorPale[1], celestial.colorPale[2], alpha);
        shader.setUniformf("u_alpha", alpha * base.opacity);
        shader.setUniformf("u_distance", (float) body.distToCamera);
        shader.setUniformf("u_apparent_angle", (float) body.solidAngleApparent);
        shader.setUniformf("u_time", (float) GaiaSky.instance.getT() / 5f);

        shader.setUniformf("u_radius", size);

        // Render mesh
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
    }

    private void renderCloseupStar(StarSet set, Highlight highlight, DatasetDescription desc, int idx, float fovFactor, Vector3d cPosD, ExtShaderProgram shader, IntMesh mesh, double thPointTimesFovFactor, double thUpOverFovFactor, double thDownOverFovFactor, float alpha) {
        if (utils.filter(idx, set, desc) && set.isVisible(idx)) {
            IParticleRecord star = set.pointData.get(idx);
            double varScl = utils.getVariableSizeScaling(set, idx);

            double sizeOriginal = set.getSize(idx);
            double size = sizeOriginal * varScl;
            double radius = size * Constants.STAR_SIZE_FACTOR;
            Vector3d starPos = set.fetchPosition(star, cPosD, D31, set.currDeltaYears);
            double distToCamera = starPos.len();
            double viewAngle = (sizeOriginal * Constants.STAR_SIZE_FACTOR / distToCamera) / fovFactor;

            Color.abgr8888ToColor(c, utils.getColor(idx, set, highlight));
            if (viewAngle >= thPointTimesFovFactor) {
                double fuzzySize = getRenderSizeStarSet(sizeOriginal, radius, distToCamera, viewAngle, thDownOverFovFactor, thUpOverFovFactor);

                Vector3 pos = starPos.put(F31);
                shader.setUniformf("u_pos", pos);
                shader.setUniformf("u_size", (float) fuzzySize);

                shader.setUniformf("u_color", c.r, c.g, c.b, alpha);
                shader.setUniformf("u_distance", (float) distToCamera);
                shader.setUniformf("u_apparent_angle", (float) (viewAngle * Settings.settings.scene.star.pointSize));
                shader.setUniformf("u_radius", (float) radius);

                // Sprite.render
                mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);

            }
        }
    }

    public double getRenderSizeStarSet(double size, double radius, double distToCamera, double viewAngle, double thDown, double thUp) {
        double computedSize = size;
        if (viewAngle > thDown) {
            double dist = distToCamera;
            if (viewAngle > thUp) {
                dist = radius / Constants.THRESHOLD_UP;
            }
            computedSize = (size * (dist / radius) * Constants.THRESHOLD_DOWN);
        }
        // Change the factor at the end here to control the stray light of stars
        computedSize *= Settings.settings.scene.star.pointSize * 0.4;

        return computedSize;
    }

    public void renderBillboardStarSet(BillboardView view, float alpha, ExtShaderProgram shader, IntMesh mesh, ICamera camera) {
        var entity = view.getEntity();
        var base = view.base;
        var set = Mapper.starSet.get(entity);
        var desc = Mapper.datasetDescription.get(entity);
        var highlight = Mapper.highlight.get(entity);

        /*
         *  STARS IN SETS
         */

        // Star set
        double thPointTimesFovFactor = Settings.settings.scene.star.threshold.point * camera.getFovFactor();
        double thUpOverFovFactor = Constants.THRESHOLD_UP / camera.getFovFactor();
        double thDownOverFovFactor = Constants.THRESHOLD_DOWN / camera.getFovFactor();
        double innerRad = 0.006 + Settings.settings.scene.star.pointSize * 0.008;
        alpha = alpha * base.opacity;
        float fovFactor = camera.getFovFactor();

        // GENERAL UNIFORMS
        shader.setUniformf("u_thpoint", (float) thPointTimesFovFactor);
        // Light glow always disabled with star groups
        shader.setUniformi("u_lightScattering", 0);
        shader.setUniformf("u_inner_rad", (float) innerRad);

        // RENDER ACTUAL STARS
        boolean focusRendered = false;
        int n = Math.min(Settings.settings.scene.star.group.numBillboard, set.pointData.size());
        for (int i = 0; i < n; i++) {
            renderCloseupStar(set, highlight, desc, set.active[i], fovFactor, set.cPosD, shader, mesh, thPointTimesFovFactor, thUpOverFovFactor, thDownOverFovFactor, alpha);
            focusRendered = focusRendered || set.active[i] == set.focusIndex;
        }
        if (set.focus != null && !focusRendered) {
            renderCloseupStar(set, highlight, desc, set.focusIndex, fovFactor, set.cPosD, shader, mesh, thPointTimesFovFactor, thUpOverFovFactor, thDownOverFovFactor, alpha);
        }
    }

    public float getRenderSizeCelestial(ICamera camera, Entity entity, Body body, SolidAngle sa, ModelScaffolding scaffolding, ParticleExtra extra) {
        if (extra != null) {
            // Stars, particles
            boolean star = Mapper.hip.has(entity);
            extra.computedSize = body.size;
            if (body.solidAngle > thdownOverFovfactor) {
                double dist = body.distToCamera;
                if (body.solidAngle > thupOverFovfactor) {
                    dist = (float) extra.radius / Constants.THRESHOLD_UP;
                }
                extra.computedSize *= (dist / extra.radius) * Constants.THRESHOLD_DOWN;
            }

            extra.computedSize *= Settings.settings.scene.star.pointSize * (star ? 0.1f : 0.2f);
            return (float) (extra.computedSize * extra.primitiveRenderScale);
        } else if (Mapper.fade.has(entity)) {
            // Regular billboards
            return getRenderSizeBillboardGalaxy(camera, body, scaffolding);
        } else {
            // Models
            float thAngleQuad = (float) sa.thresholdQuad * camera.getFovFactor();
            double size = 0f;
            if (body.solidAngle >= sa.thresholdPoint * camera.getFovFactor()) {
                size = Math.tan(thAngleQuad) * body.distToCamera * scaffolding.billboardSizeFactor;
            }
            return (float) size;
        }
    }

    public void renderBillboardCelestial(BillboardView view, float alpha, ExtShaderProgram shader, IntMesh mesh, ICamera camera) {
        var entity = view.getEntity();
        var base = view.base;
        var body = view.body;
        var graph = view.graph;
        var celestial = view.celestial;

        /*
         *  REGULAR STARS, PLANETS, SATELLITES, BILLBOARDS and SSOs
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
            billboardPosition.nor().scl(len * 0.99f);
        }
        shader.setUniformf("u_pos", billboardPosition);
        shader.setUniformf("u_size", fuzzySize);

        // Models use the regular color
        float[] color = isModel ? body.color : celestial.colorPale;

        // Alpha channel:
        // - particles: alpha * opacity
        // - models:    alpha * (1 - fadeOpacity)
        float a = scaffolding != null ? alpha * (1f - scaffolding.fadeOpacity) : alpha * base.opacity;
        shader.setUniformf("u_color", color[0], color[1], color[2], a);
        shader.setUniformf("u_inner_rad", (float) celestial.innerRad);
        shader.setUniformf("u_distance", (float) body.distToCamera);
        shader.setUniformf("u_apparent_angle", (float) body.solidAngleApparent);
        shader.setUniformf("u_thpoint", (float) sa.thresholdPoint * camera.getFovFactor());
        shader.setUniformf("u_vrScale", (float) Constants.DISTANCE_SCALE_FACTOR);

        // Whether light scattering is enabled or not
        shader.setUniformi("u_lightScattering", (isStar && GaiaSky.instance.getPostProcessor().isLightScatterEnabled()) ? 1 : 0);

        shader.setUniformf("u_radius", radius);

        // Render the mesh
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
    }

    public void renderBillboardCluster(BillboardView view, float alpha, ExtShaderProgram shader, IntMesh mesh, ICamera camera) {
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
            shader.setUniformi("u_texture0", 0);
        }

        float fa = (1 - cluster.fadeAlpha) * 0.6f;

        shader.setUniformf("u_pos", graph.translation.put(F31));
        shader.setUniformf("u_size", body.size);
        shader.setUniformf("u_color", body.color[0] * fa, body.color[1] * fa, body.color[2] * fa, body.color[3] * alpha * base.opacity * 6.5f);
        // Sprite.render
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.FOV_CHANGE_NOTIFICATION) {
            fovFactor = (Float) data[1];
            thupOverFovfactor = (float) Constants.THRESHOLD_UP / fovFactor;
            thdownOverFovfactor = (float) Constants.THRESHOLD_DOWN / fovFactor;
        }
    }
}
