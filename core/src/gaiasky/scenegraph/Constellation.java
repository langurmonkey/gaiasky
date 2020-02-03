/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.I3DTextRenderable;
import gaiasky.render.ILineRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Nature;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.IPosition;

/**
 * Represents a constellation object.
 *
 * @author Toni Sagrista
 */
public class Constellation extends FadeNode implements ILineRenderable, I3DTextRenderable, IVisibilitySwitch {
    private static Array<Constellation> allConstellations = new Array<Constellation>(88);
    private double deltaYears;

    public static void updateConstellations() {
        for (Constellation c : allConstellations) {
            c.setUp();
        }
    }

    float alpha = .2f;
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
        this.names = new String[] { name };
        this.parentName = parentName;
    }

    @Override
    public void initialize() {
        allConstellations.add(this);
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera) {
        update(time, parentTransform, camera, 1f);
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera, float opacity) {
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
        pos.nor().scl(100d * Constants.PC_TO_U);

        super.update(time, parentTransform, camera, opacity);

        addToRenderLists(camera);

        deltaYears = AstroUtils.getMsSince(time.getTime(), AstroUtils.JD_J2015_5) * Nature.MS_TO_Y;

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
        alpha *= this.alpha * opacity;

        Vector3d p1 = aux3d1.get();
        Vector3d p2 = aux3d2.get();
        Vector3d campos = camera.getPos();

        for (IPosition[] pair : lines) {
            if (pair != null) {
                getPosition(pair[0], campos, p1);
                getPosition(pair[1], campos, p2);

                renderer.addLine(this, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, cc[0], cc[1], cc[2], alpha);
            }
        }

    }

    private void getPosition(IPosition posbean, Vector3d campos, Vector3d out) {
        Vector3d vel = aux3d3.get().setZero();
        if (posbean.getVelocity() != null && !posbean.getVelocity().hasNaN()) {
            vel.set(posbean.getVelocity()).scl(deltaYears);
        }
        out.set(posbean.getPosition()).sub(campos).add(vel);
    }

    /**
     * Label rendering.
     */
    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        Vector3d pos = aux3d1.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1);
        shader.setUniformf("u_thOverFactor", 1);
        shader.setUniformf("u_thOverFactorScl", 1);
        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor());
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (isVisible()) {
            addToRender(this, RenderGroup.LINE);
            if (renderText()) {
                addToRender(this, RenderGroup.FONT_LABEL);
            }
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
        return .2f;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        out.set(pos);
        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    @Override
    public String text() {
        return names[0];
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
    public float getTextOpacity() {
        return getOpacity();
    }

    @Override
    public float getLineWidth() {
        return 1;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public void setDescription(String name) {
    }

    @Override
    public int getGlPrimitive() {
        return GL20.GL_LINES;
    }
}
