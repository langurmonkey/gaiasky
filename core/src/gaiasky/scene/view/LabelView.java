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
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.ParticleExtra;
import gaiasky.scene.component.SolidAngle;
import gaiasky.scene.component.Text;
import gaiasky.scene.render.draw.TextRenderer;
import gaiasky.scenegraph.camera.FovCamera;
import gaiasky.scenegraph.camera.ICamera;
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
    private ParticleExtra extra;

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
        this.extra = Mapper.extra.get(entity);
    }

    @Override
    public boolean renderText() {
        return true;
    }

    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        if (Mapper.celestial.has(entity)) {
            renderCelestial(batch, shader, sys, rc, camera);
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

    @Override
    public float textSize() {
        return (float) (text.labelMax * body.distToCamera * text.labelFactor);
    }

    @Override
    public float textScale() {
        return text.textScale >= 0 ? text.textScale : (float) FastMath.atan(text.labelMax) * text.labelFactor * 4e2f;
    }

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
