package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.I3DTextRenderable;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.render.system.FontRenderSystem;
import gaia.cu9.ari.gaiaorbit.render.system.LineRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import gaia.cu9.ari.gaiaorbit.util.tree.IPosition;

/**
 * Represents a constellation object.
 * 
 * @author Toni Sagrista
 *
 */
public class Constellation extends LineObject implements I3DTextRenderable {
    private static Array<Constellation> allConstellations = new Array<Constellation>(88);
    private double deltaYears;

    public static void updateConstellations() {
        for (Constellation c : allConstellations) {
            c.setUp();
        }
    }

    float alpha = .5f;
    float constalpha;
    boolean allLoaded = false;

    /** List of pairs of HIP identifiers **/
    public Array<int[]> ids;
    /**
     * The lines themselves as pairs of positions
     **/
    public IPosition[][] lines;

    public Constellation() {
        super();
        cc = new float[] { .5f, 1f, .5f, alpha };
    }

    public Constellation(String name, String parentName) {
        this();
        this.name = name;
        this.parentName = parentName;
    }

    @Override
    public void initialize() {
        allConstellations.add(this);
    }

    public void update(ITimeFrameProvider time, final Transform parentTransform, ICamera camera) {
        update(time, parentTransform, camera, 1f);
    }

    public void update(ITimeFrameProvider time, final Transform parentTransform, ICamera camera, float opacity) {
        // Recompute mean position
        pos.setZero();
        Vector3d p = aux3d1.get();
        int nstars = 0;
        for (int i = 0; i < lines.length; i++) {
            IPosition[] line = lines[i];
            if (line != null) {
                p.set(line[0].getPosition()).add(camera.getInversePos());
                pos.add(p);
                nstars++;
            }
        }
        pos.scl((1d / nstars));
        pos.nor().scl(100 * Constants.PC_TO_U);
        addToRenderLists(camera);

        deltaYears = AstroUtils.getMsSince(time.getTime(), AstroUtils.JD_J2015_5) * Constants.MS_TO_Y;

    }

    @Override
    public void setUp() {
        if (!allLoaded) {
            int npairs = ids.size;
            if (lines == null) {
                lines = new IPosition[npairs][];
            }
            IntMap<IPosition> hipMap = sg.getStarMap();
            allLoaded = true;
            for (int i = 0; i < npairs; i++) {
                int[] pair = ids.get(i);
                IPosition s1, s2;
                s1 = hipMap.get(pair[0]);
                s2 = hipMap.get(pair[1]);
                if (lines[i] == null && s1 != null && s2 != null) {
                    lines[i] = new IPosition[] { s1, s2 };
                } else {
                    allLoaded = false;
                }
            }
        }
    }

    /**
     * Line rendering.
     */
    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        constalpha = alpha;
        alpha *= this.alpha;

        Vector3 campos = aux3f1.get();
        Vector3 p1 = aux3f2.get();
        Vector3 p2 = aux3f3.get();
        camera.getPos().setVector3(campos);

        for (IPosition[] pair : lines) {
            if (pair != null) {
                getPosition(pair[0], campos, p1);
                getPosition(pair[1], campos, p2);

                renderer.addLine(this, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, cc[0], cc[1], cc[2], alpha);
            }
        }

    }

    private void getPosition(IPosition posbean, Vector3 campos, Vector3 out) {
        Vector3d vel = aux3d1.get().setZero();
        if (posbean.getVelocity() != null && !posbean.getVelocity().hasNaN()) {
            vel.set(posbean.getVelocity()).scl(deltaYears);
        }

        Vector3d position = aux3d2.get().set(posbean.getPosition()).sub(campos).add(vel);
        position.put(out);
    }

    /**
     * Label rendering.
     */
    @Override
    public void render(SpriteBatch batch, ShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        Vector3d pos = aux3d1.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1);
        shader.setUniformf("u_thOverFactor", 1);
        shader.setUniformf("u_thOverFactorScl", 1);
        render3DLabel(batch, shader, sys.font3d, camera, rc, text(), pos, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor());
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        addToRender(this, RenderGroup.LINE);
        if (renderText()) {
            addToRender(this, RenderGroup.FONT_LABEL);

        }
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
    }

    @Override
    public float[] textColour() {
        return cc;
    }

    @Override
    public float textSize() {
        return .2e7f;
    }

    @Override
    public float textScale() {
        return .3f;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        out.set(pos);
        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    @Override
    public String text() {
        return name;
    }

    @Override
    public boolean renderText() {
        return GaiaSky.instance.isOn(ComponentType.Labels);
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
    }

    @Override
    public boolean isLabel() {
        return true;
    }

    @Override
    public float getLineWidth() {
        return 1;
    }

}
