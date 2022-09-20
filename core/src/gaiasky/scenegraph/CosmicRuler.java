/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.render.api.I3DTextRenderable;
import gaiasky.render.api.ILineRenderable;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.GlobalResources;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

import java.text.DecimalFormat;

/**
 * Cosmic ruler that shows the distance between two objects.
 */
public class CosmicRuler extends SceneGraphNode implements I3DTextRenderable, ILineRenderable, IObserver {
    private String name0, name1;
    private final double[] pos0 = new double[3];
    private final double[] pos1 = new double[3];
    private final Vector3d p0 = new Vector3d();
    private final Vector3d p1 = new Vector3d();
    private final Vector3d m = new Vector3d();
    private boolean rulerOk = false;
    private String dist;
    private final DecimalFormat nf = new DecimalFormat("0.#########E0");

    public CosmicRuler() {
        super();
        this.parentName = "Universe";
        this.setName("Cosmicruler");
        this.cc = ColorUtils.gYellow;
        setCt("Ruler");
        EventManager.instance.subscribe(this, Event.RULER_ATTACH_0, Event.RULER_ATTACH_1, Event.RULER_CLEAR);
    }

    public CosmicRuler(String name0, String name1) {
        this();
        this.name0 = name0;
        this.name1 = name1;
    }

    @Override
    public float getLineWidth() {
        return 1f;
    }

    // Render Line
    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        double va = 0.01 * camera.getFovFactor();

        // Main line
        renderer.addLine(this, p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, cc[0], cc[1], cc[2], alpha);
        // Cap 1
        addCap(p0, p1, va, renderer, alpha);
        // Cap 1
        addCap(p1, p0, va, renderer, alpha);
    }

    private void addCap(Vector3d p0, Vector3d p1, double va, LineRenderSystem renderer, float alpha) {
        // cpos-p0
        Vector3d cp = D32.get().set(p0);
        // cross(cpos-p0, p0-p1)
        Vector3d crs = D31.get().set(p1).sub(p0).crs(cp);

        double d = p0.len();
        double caplen = FastMath.tan(va) * d;
        crs.setLength(caplen);
        Vector3d aux0 = D32.get().set(p0).add(crs);
        Vector3d aux1 = D33.get().set(p0).sub(crs);
        renderer.addLine(this, aux0.x, aux0.y, aux0.z, aux1.x, aux1.y, aux1.z, cc[0], cc[1], cc[2], cc[3] * alpha);
    }

    // Render Label
    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        // 3D distance font
        Vector3d pos = D31.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", 1f);

        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, distToCamera, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor(), this.forceLabel);
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender() && rulerOk) {
            addToRender(this, RenderGroup.LINE);
            addToRender(this, RenderGroup.FONT_LABEL);
        }
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
        // Update positions
        rulerOk = (GaiaSky.instance.scene.getObjectPosition(name0, pos0) != null);
        rulerOk = rulerOk && (GaiaSky.instance.scene.getObjectPosition(name1, pos1) != null);

        if (rulerOk) {
            p0.set(pos0).add(translation);
            p1.set(pos1).add(translation);
            // Mid-point
            m.set(p1).sub(p0).scl(0.5).add(p0);
            pos.set(m).sub(translation);
            // Distance in internal units
            double dst = p0.dst(p1);
            Pair<Double, String> d = GlobalResources.doubleToDistanceString(dst, Settings.settings.program.ui.distanceUnits);
            dist = nf.format(d.getFirst()) + " " + d.getSecond();

            GaiaSky.postRunnable(() -> {
                EventManager.publish(Event.RULER_DIST, this, dst, dist);
            });
        } else {
            dist = null;
        }

    }

    public String getName0() {
        return name0;
    }

    public void setName0(String name0) {
        this.name0 = name0;
    }

    public String getName1() {
        return name1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
    }

    public boolean rulerOk() {
        return rulerOk;
    }

    /**
     * Returns true if the ruler is attached to at least one object.
     *
     * @return Ture if the ruler is attached.
     */
    public boolean hasAttached() {
        return name0 != null || name1 != null;
    }

    public boolean hasObject0() {
        return name0 != null && !name0.isEmpty();
    }

    public boolean hasObject1() {
        return name1 != null && !name1.isEmpty();
    }

    @Override
    public boolean renderText() {
        return rulerOk;
    }

    @Override
    public float[] textColour() {
        return labelcolor;
    }

    @Override
    public float textSize() {
        return (float) (0.0005 * distToCamera);
    }

    @Override
    public float textScale() {
        return 0.2f;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        out.set(m);

        double len = out.len();
        out.clamp(0, len - getRadius()).scl(0.9f);

        Vector3d aux = D32.get();
        aux.set(cam.getUp());

        aux.crs(out).nor();

        float dist = -0.025f * (float) out.len();

        aux.add(cam.getUp()).nor().scl(dist);

        out.add(aux);
    }

    @Override
    public String text() {
        return dist;
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
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
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case RULER_ATTACH_0:
            String name = (String) data[0];
            setName0(name);
            break;
        case RULER_ATTACH_1:
            name = (String) data[0];
            setName1(name);
            break;
        case RULER_CLEAR:
            setName0(null);
            setName1(null);
            break;
        default:
            break;
        }
    }

    @Override
    public int getGlPrimitive() {
        return GL20.GL_LINES;
    }
}
