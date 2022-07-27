package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderingContext;
import gaiasky.render.RenderingContext.CubemapSide;
import gaiasky.render.api.I3DTextRenderable;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
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

    public Label label;
    public GraphNode graph;
    public SolidAngle sa;
    public Cluster cluster;
    public BillboardSet bbSet;
    public Constel constel;
    public Mesh mesh;
    public Ruler ruler;
    public LocationMark loc;

    private LabelEntityRenderSystem renderSystem;

    public LabelView() {
        renderSystem = new LabelEntityRenderSystem();
    }

    @Override
    protected void entityCheck(Entity entity) {
        super.entityCheck(entity);
        check(entity, Mapper.graph, GraphNode.class);
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.label = Mapper.label.get(entity);
        this.graph = Mapper.graph.get(entity);
        this.sa = Mapper.sa.get(entity);
        this.cluster = Mapper.cluster.get(entity);
        this.bbSet = Mapper.billboardSet.get(entity);
        this.constel = Mapper.constel.get(entity);
        this.mesh = Mapper.mesh.get(entity);
        this.ruler = Mapper.ruler.get(entity);
        this.loc = Mapper.loc.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.label = null;
        this.graph = null;
        this.sa = null;
        this.cluster = null;
        this.bbSet = null;
        this.constel = null;
        this.mesh = null;
        this.ruler = null;
    }

    public boolean renderTextCelestial() {
        return base.names != null && renderTextBase() && (base.forceLabel || FastMath.pow(body.solidAngleApparent, label.solidAnglePow) >= sa.thresholdLabel);
    }

    public boolean renderTextParticle() {
        return extra.computedSize > 0 &&
                renderTextBase() &&
                body.solidAngleApparent >= (sa.thresholdLabel / GaiaSky.instance.cameraManager.getFovFactor());
    }

    public boolean renderTextLocation() {
        if (renderTextBase() && (body.solidAngle >= LocationMark.LOWER_LIMIT && body.solidAngle <= LocationMark.UPPER_LIMIT * Constants.DISTANCE_SCALE_FACTOR || base.forceLabel)) {
            Vector3d aux = D31;
            graph.translation.put(aux).scl(-1);

            double cosAlpha = aux.add(loc.location3d.x, loc.location3d.y, loc.location3d.z).nor().dot(GaiaSky.instance.cameraManager.getDirection().nor());
            return cosAlpha < -0.3f;
        } else {
            return false;
        }
    }

    public boolean renderTextKeyframe() {
        var kf = Mapper.keyframes.get(entity);
        return kf.selected != null;
    }

    public boolean renderTextRuler() {
        return renderTextBase() && ruler.rulerOk;
    }

    public boolean renderTextBackgroundModel() {
        return renderTextBase() && label.label;
    }

    public boolean renderTextTitle() {
        return !Settings.settings.program.modeCubemap.active;
    }

    public boolean renderTextTrajectory() {
        return renderTextBase() && base.forceLabel;
    }

    public boolean renderTextGridRec() {
        return label.label;
    }

    public boolean renderTextEssential() {
        return base.names != null && renderTextBase() && base.opacity > 0;
    }

    public boolean renderTextBase() {
        return GaiaSky.instance.isOn(ComponentType.Labels);
    }

    @Override
    public boolean renderText() {
        return label.renderFunction == null || label.renderFunction.apply(this);
    }

    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        if (label.renderConsumer != null && renderText()) {
            label.renderConsumer.apply(renderSystem, this, batch, shader, sys, rc, camera);
        }
    }

    @Override
    public float textSize() {
        if (constel != null) {
            return .2e7f;
        } else if (loc != null) {
            return body.size / 1.5f;
        } else if (Mapper.keyframes.has(entity)) {
            return label.labelMax;
        }
        return (float) (label.labelMax * body.distToCamera * label.labelFactor);
    }

    @Override
    public float textScale() {
        if (set != null) {
            // Star sets
            return .5f / Settings.settings.scene.label.size;
        } else if (constel != null) {
            return .2f / Settings.settings.scene.label.size;
        } else if (loc != null) {
            return loc.sizeKm * label.textScale / textSize() * (float) Constants.DISTANCE_SCALE_FACTOR;
        } else {
            // Rest
            return label.textScale >= 0 ? label.textScale : (float) FastMath.atan(label.labelMax) * label.labelFactor * 4e2f;
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
        if (label != null && label.labelPosition != null) {
            out.set(label.labelPosition).add(cam.getInversePos());
        } else {
            if (ruler == null) {
                graph.translation.put(out);
            } else {
                out.set(ruler.m);
            }
            double len = out.len();
            out.clamp(0, len - getRadius()).scl(0.9f);
            if (Mapper.shape.has(entity)) {
                out.x += getRadius() * 0.5;
            }

            Vector3d aux = D32;
            aux.set(cam.getUp());
            aux.crs(out).nor();

            float dist = -0.015f * cam.getFovFactor() * (float) out.len();
            aux.add(cam.getUp()).nor().scl(dist);

            out.add(aux);
        }

        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    @Override
    public String text() {
        if (ruler != null) {
            return ruler.dist;
        } else if (loc != null) {
            return loc.displayName;
        } else {
            return base.getLocalizedName();
        }
    }

    @Override
    public void textDepthBuffer() {
        label.depthBufferConsumer.accept(this);
    }

    public void defaultTextDepthBuffer() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    public void noTextDepthBuffer() {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    public void emptyTextDepthBuffer() {
    }

    @Override
    public boolean isLabel() {
        return label != null ? label.label : true;
    }

    @Override
    public float getTextOpacity() {
        return base.opacity;
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
