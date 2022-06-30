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
import gaiasky.scene.system.render.draw.TextRenderer;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.camera.FovCamera;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.DecalUtils;
import gaiasky.util.GlobalResources;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.Settings.DistanceUnits;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

import java.text.DecimalFormat;

/**
 * An entity view that implements the {@link I3DTextRenderable} methods.
 */
public class LabelView extends RenderView implements I3DTextRenderable {

    private final Vector3d D32 = new Vector3d();
    private final Vector3 F31 = new Vector3();
    private final Vector3 F32 = new Vector3();

    private Label label;
    private GraphNode graph;
    private SolidAngle sa;
    private Text text;
    private Cluster cluster;
    private BillboardSet bbSet;
    private Constel constel;
    private Mesh mesh;
    private Ruler ruler;

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
        this.text = Mapper.text.get(entity);
        this.cluster = Mapper.cluster.get(entity);
        this.bbSet = Mapper.billboardSet.get(entity);
        this.constel = Mapper.constel.get(entity);
        this.mesh = Mapper.mesh.get(entity);
        this.ruler = Mapper.ruler.get(entity);
    }

    @Override
    public boolean renderText() {
        return true;
    }

    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        if (Mapper.celestial.has(entity)) {
            // Celestial: planets, single stars, particles, etc.
            renderSystem.renderCelestial(this, base, body, text, sa, batch, shader, sys, rc, camera);
        } else if (set != null) {
            // Star sets.
            renderSystem.renderStarSet(this, set, batch, shader, sys, rc, camera);
        } else if (cluster != null) {
            // Clusters
            renderSystem.renderCluster(this, base, body, batch, shader, sys, rc, camera);
        } else if (bbSet != null) {
            // Billboard sets
            renderSystem.renderBillboardSet(this, base, body, batch, shader, sys, rc, camera);
        } else if (constel != null) {
            // Constellation
            renderSystem.renderConstellation(this, base, body, batch, shader, sys, rc, camera);
        } else if (mesh != null) {
            // Mesh
            renderSystem.renderMesh(this, base, body, batch, shader, sys, rc, camera);
        } else if (Mapper.gridRec.has(entity)) {
            // Recursive grid
            renderSystem.renderRecursiveGrid(this, base, body, label, batch, shader, sys, rc, camera);
        } else if (ruler != null) {
            // Ruler
            renderSystem.renderRuler(this, base, body, batch, shader, sys, rc, camera);
        } else if(Mapper.title.has(entity)) {
            // Title
            renderSystem.renderTitle(this, body, Mapper.title.get(entity), batch, shader, sys, rc, camera);
        }
    }

    @Override
    public float textSize() {
        if (constel != null) {
            return .2e7f;
        }
        return (float) (text.labelMax * body.distToCamera * text.labelFactor);
    }

    @Override
    public float textScale() {
        if (set != null) {
            // Star sets
            return .5f / Settings.settings.scene.label.size;
        } else if (constel != null) {
            return .2f / Settings.settings.scene.label.size;
        } else {
            // Rest
            return text.textScale >= 0 ? text.textScale : (float) FastMath.atan(text.labelMax) * text.labelFactor * 4e2f;
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
        if (constel != null) {
            out.set(body.pos);
        } else if (label != null && label.labelPosition != null) {
            out.set(label.labelPosition).add(cam.getInversePos());
        } else {
            if (ruler == null) {
                graph.translation.put(out);
            } else {
                out.set(ruler.m);
            }
            double len = out.len();
            out.clamp(0, len - getRadius()).scl(0.9f);

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
        if(ruler == null) {
            return base.getLocalizedName();
        } else {
            return ruler.dist;
        }
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
