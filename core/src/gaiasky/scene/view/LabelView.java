package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import gaiasky.render.RenderingContext;
import gaiasky.render.RenderingContext.CubemapSide;
import gaiasky.render.api.I3DTextRenderable;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.render.draw.TextRenderer;
import gaiasky.scenegraph.camera.FovCamera;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.DecalUtils;
import gaiasky.util.GlobalResources;
import gaiasky.util.Settings;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

/**
 * An entity view that implements the {@link I3DTextRenderable} methods.
 */
public class LabelView extends RenderView implements I3DTextRenderable {

    private final Vector3d D31 = new Vector3d();
    private final Vector3d D32 = new Vector3d();
    private final Vector3 F31 = new Vector3();
    private final Vector3 F32 = new Vector3();

    private GraphNode graph;
    private SolidAngle sa;
    private Text text;
    private Cluster cluster;

    public LabelView() {
    }

    @Override
    protected void entityCheck(Entity entity) {
        super.entityCheck(entity);
        check(entity, Mapper.graph, GraphNode.class);
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.graph = Mapper.graph.get(entity);
        this.sa = Mapper.sa.get(entity);
        this.text = Mapper.text.get(entity);
        this.cluster = Mapper.cluster.get(entity);
    }

    @Override
    public boolean renderText() {
        return true;
    }

    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        if (Mapper.celestial.has(entity)) {
            // Celestial: planets, single stars, etc.
            renderCelestial(batch, shader, sys, rc, camera);
        } else if (set != null) {
            // Star sets.
            renderStarSet(batch, shader, sys, rc, camera);
        } else if (cluster != null) {
            // Clusters
            renderCluster(batch, shader, sys, rc, camera);
        }
    }

    public void renderCelestial(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        if (camera.getCurrent() instanceof FovCamera) {
            render2DLabel(batch, shader, rc, ((TextRenderer) sys).font2d, camera, text(), body.pos.put(D31));
        } else {
            // 3D distance font
            Vector3d pos = D31;
            textPosition(camera, pos);
            shader.setUniformf("u_viewAngle", base.forceLabel ? 2f : (float) body.viewAngleApparent);
            shader.setUniformf("u_viewAnglePow", base.forceLabel ? 1f : text.viewAnglePow);
            shader.setUniformf("u_thLabel", base.forceLabel ? 1f : (float) sa.thresholdLabel);

            render3DLabel(batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, text(), pos, body.distToCamera, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor(), getRadius(), base.forceLabel);
        }
    }

    public void renderCluster(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        Vector3d pos = D31;
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", base.forceLabel ? 2f : (float) body.viewAngle * 500f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", 1f);

        render3DLabel(batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, text(), pos, body.distToCamera, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor(), body.size / 2d, base.forceLabel);
    }

    public void renderStarSet(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        float thresholdLabel = (float) (Settings.settings.scene.star.threshold.point / Settings.settings.scene.label.number / camera.getFovFactor());

        var pointData = set.pointData;
        var active = set.active;

        Vector3d starPosition = D31;
        int n = Math.min(pointData.size(), Settings.settings.scene.star.group.numLabel);
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
                renderStarLabel(idx, starPosition, thresholdLabel, batch, shader, sys, rc, camera);
            }
            for (Integer i : set.forceLabelStars) {
                renderStarLabel(i, starPosition, thresholdLabel, batch, shader, sys, rc, camera);
            }
        }
    }

    /**
     * Renders the label for a single star in a star group.
     */
    private void renderStarLabel(int idx, Vector3d starPosition, float thresholdLabel, ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
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
            shader.setUniform4fv("u_color", textColour(star.names()[0]), 0, 4);
            double textSize = FastMath.tanh(viewAngle) * distToCamera * 1e5d;
            float alpha = Math.min((float) FastMath.atan(textSize / distToCamera), 1.e-3f);
            textSize = (float) FastMath.tan(alpha) * distToCamera * 0.5f;
            render3DLabel(batch, shader, ((TextRenderer) sys).fontDistanceField, camera, rc, star.names()[0], starPosition, distToCamera, textScale() * camera.getFovFactor(), textSize * camera.getFovFactor(), radius, forceLabel);
        }
    }

    @Override
    public float textSize() {
        return (float) (text.labelMax * body.distToCamera * text.labelFactor);
    }

    @Override
    public float textScale() {
        if (set == null) {
            return text.textScale >= 0 ? text.textScale : (float) FastMath.atan(text.labelMax) * text.labelFactor * 4e2f;
        } else {
            return .5f / Settings.settings.scene.label.size;
        }
    }

    /**
     * Text position for single objects (models, single stars, etc.).
     *
     * @param cam The camera.
     * @param out The out parameter with the result.
     */
    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        graph.translation.put(out);
        double len = out.len();
        out.clamp(0, len - getRadius()).scl(0.9f);

        Vector3d aux = D32;
        aux.set(cam.getUp());

        aux.crs(out).nor();

        float dist = -0.02f * cam.getFovFactor() * (float) out.len();

        aux.add(cam.getUp()).nor().scl(dist);

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
    private void textPosition(ICamera cam, Vector3d out, double len, double rad) {
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

    @Override
    public String text() {
        return base.getLocalizedName();
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    @Override
    public boolean isLabel() {
        if (Mapper.label.has(entity)) {
            return Mapper.label.get(entity).label;
        } else if (Mapper.loc.has(entity) || Mapper.title.has(entity)) {
            return false;
        }
        return true;
    }

    @Override
    public float getTextOpacity() {
        if (Mapper.tagQuatOrientation.has(entity)) {
            // Billboard labels should go with the model opacity.
            return Math.min(base.opacity, Mapper.modelScaffolding.get(entity).fadeOpacity);
        } else {
            return base.opacity;
        }
    }

    protected void render2DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, Vector3d pos3d) {
        Vector3 p = F31;
        pos3d.setVector3(p);

        camera.getCamera().project(p);
        p.x += 15;
        p.y -= 15;

        shader.setUniformf("scale", 1f);
        DecalUtils.drawFont2D(font, batch, label, p);
    }

    protected void render2DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, float x, float y, float scale, int align) {
        shader.setUniformf("u_scale", scale);
        DecalUtils.drawFont2D(font, batch, rc, label, x, y, scale, align);
    }

    protected void render2DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, float x, float y) {
        render2DLabel(batch, shader, rc, font, camera, label, x, y, 1f);
    }

    protected void render2DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, float x, float y, float scale) {
        render2DLabel(batch, shader, rc, font, camera, label, x, y, scale, -1);
    }

    protected void render3DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, BitmapFont font, ICamera camera, RenderingContext rc, String label, Vector3d pos, double distToCamera, float scale, double size, double radius, boolean forceLabel) {
        render3DLabel(batch, shader, font, camera, rc, label, pos, distToCamera, scale, size, radius, -1, -1, forceLabel);
    }

    protected void render3DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, BitmapFont font, ICamera camera, RenderingContext rc, String label, Vector3d pos, double distToCamera, float scale, double size, double radius, float minSizeDegrees, float maxSizeDegrees, boolean forceLabel) {
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
            ((I3DTextRenderable) this).textDepthBuffer();

            DecalUtils.drawFont3D(font, batch, label, (float) pos.x, (float) pos.y, (float) pos.z, size, rot, camera, !rc.isCubemap(), minSizeDegrees, maxSizeDegrees);
        }
    }
}
