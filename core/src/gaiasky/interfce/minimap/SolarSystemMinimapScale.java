/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce.minimap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import gaiasky.GaiaSky;
import gaiasky.interfce.IMinimapScale;
import gaiasky.scenegraph.Planet;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.color.ColourUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3d;

public class SolarSystemMinimapScale implements IMinimapScale {

    private OrthographicCamera ortho;
    private SpriteBatch sb;
    private ShapeRenderer sr;
    private BitmapFont font;
    private int side, sideshort, side2, sideshort2;

    /** Extent, in whatever units, of the minimap - where the edge is **/
    private double extent;
    /** Conversions to and from internal units **/
    private double to, from;


    private Vector3d aux3d1, aux3d2;
    private Vector2d aux2d1, aux2d2;
    private Vector2 sunPos;

    private Matrix4d trans;

    private float[] camf, satf, uraf, nepf, plutf;
    private Planet sat, ura, nep, plut;

    public SolarSystemMinimapScale() {
        super();
        aux3d1 = new Vector3d();
        aux3d2 = new Vector3d();
        aux2d1 = new Vector2d();
        aux2d2 = new Vector2d();
        camf = new float[4];
        satf = new float[4];
        uraf = new float[4];
        nepf = new float[4];
        plutf = new float[4];
        trans = Coordinates.eqToEcl();
    }

    @Override
    public void update(){
        if(sat == null){
            sat = (Planet) GaiaSky.instance.sg.getNode("Saturn");
            ura = (Planet) GaiaSky.instance.sg.getNode("Uranus");
            nep = (Planet) GaiaSky.instance.sg.getNode("Neptune");
            plut = (Planet) GaiaSky.instance.sg.getNode("Pluto");
        }
        project(sat.getAbsolutePosition(aux3d1), satf);
        project(ura.getAbsolutePosition(aux3d1), uraf);
        project(nep.getAbsolutePosition(aux3d1), nepf);
        project(plut.getAbsolutePosition(aux3d1), plutf);
        project(GaiaSky.instance.cam.getPos(), camf);
    }

    public float[] project(Vector3d pos, float[] out){
        Vector3d p = aux3d1.set(pos).mul(trans);
        Vector2d pos2d = aux2d1;
        pos2d.set(p.z, p.y).scl(from);
        float cx = ecl2Px(pos2d.x, side2);
        float cy = ecl2Px(pos2d.y, sideshort2);
        out[0] = cx;
        out[1] = cy;

        pos2d.set(p.x, p.z).scl(from);
        cx = ecl2Px(pos2d.x, side2);
        cy = ecl2Px(pos2d.y, side2);
        out[2] = cx;
        out[3] = cy;
        return out;
    }

    @Override
    public boolean isActive(Vector3d campos) {
        return campos.len() <= extent * Constants.AU_TO_U;
    }

    @Override
    public void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort) {
        // Units
        this.to = Constants.AU_TO_U;
        this.from = Constants.U_TO_AU;
        this.extent = 50;

        this.ortho = ortho;
        this.sb = sb;
        this.sr = sr;
        this.font = font;
        this.side = side;
        this.side2 = side / 2;
        this.sideshort = sideshort;
        this.sideshort2 = sideshort / 2;
        sunPos = new Vector2(gal2Px(0, side2), gal2Px(-8000, side2));
    }

    @Override
    public void renderSideProjection(FrameBuffer fb) {
        Gdx.gl.glEnable(GL20.GL_BLEND);

        ortho.setToOrtho(true, side, sideshort);
        sr.setProjectionMatrix(ortho.combined);
        sb.setProjectionMatrix(ortho.combined);
        fb.begin();
        // Clear
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ICamera cam = GaiaSky.instance.cam.current;
        // Position
        float cx = this.camf[0];
        float cy = this.camf[1];
        // Direction
        Vector3d dir = aux3d2.set(cam.getDirection()).mul(trans);
        Vector2d camdir2 = aux2d2.set(dir.z, dir.y).nor().scl(px(15f));

        sr.begin(ShapeType.Filled);
        float ycenter = ecl2Px(0, sideshort2);
        // Pluto orbit
        sr.setColor(0.3f, 1.0f, 0.3f, 1f);
        sr.rectLine(ecl2Px(-40, side2), ycenter +  10, ecl2Px(40, side2), ycenter -25, 2f);
        // Neptune orbit
        sr.setColor(0.6f, 0.6f, 1.0f, 1f);
        sr.rectLine(ecl2Px(-30, side2), ycenter, ecl2Px(30, side2), ycenter, 2f);
        // Uranus orbit
        sr.setColor(1.0f, 0.3f, 0.3f, 1f);
        sr.rectLine(ecl2Px(-20, side2), ycenter, ecl2Px(20, side2), ycenter, 2f);
        // Saturn orbit
        sr.setColor(0.0f, 1.f, 1.f, 1f);
        sr.rectLine(ecl2Px(-9.2, side2), ycenter, ecl2Px(9.2, side2), ycenter, 2f);
        // Sun
        sr.setColor(1f, 1f, 0f, 1f);
        sr.circle(ecl2Px(0, side2), ycenter, px(7f));

        // Camera
        sr.setColor(ColourUtils.gRedC);
        sr.circle(cx, cy, 8f);
        Vector2d endx = aux2d1.set(camdir2.x, camdir2.y);
        endx.rotate(-cam.getCamera().fieldOfView / 2d);
        float c1x = (float) endx.x + cx;
        float c1y = (float) endx.y + cy;
        endx.set(camdir2.x, camdir2.y);
        endx.rotate(cam.getCamera().fieldOfView / 2d);
        sr.triangle(cx, cy, c1x, c1y, (float) endx.x + cx, (float) endx.y + cy);

        // Camera span
        sr.setColor(1,1,1,0.1f);
        endx = aux2d1.set(camdir2.x, camdir2.y).scl(40f);
        endx.rotate(-cam.getCamera().fieldOfView / 2d);
        c1x = (float) endx.x + cx;
        c1y = (float) endx.y + cy;
        endx.set(camdir2.x, camdir2.y).scl(40f);
        endx.rotate(cam.getCamera().fieldOfView / 2d);
        sr.triangle(cx, cy, c1x, c1y, (float) endx.x + cx, (float) endx.y + cy);

        // Planet positions
        // Saturn
        sr.setColor(0f, 1f, 1f, 1f);
        sr.circle(satf[0], satf[1], px(3f));
        // Uranus
        sr.setColor(1f, 0.3f, 0.3f, 1f);
        sr.circle(uraf[0], uraf[1], px(3f));
        // Neptune
        sr.setColor(0.6f, 0.6f, 1f, 1f);
        sr.circle(nepf[0], nepf[1], px(3f));
        // Pluto
        sr.setColor(0.3f, 1.0f, 0.3f, 1f);
        sr.circle(plutf[0], plutf[1], px(3f));
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(0, 1, 1, 1);
        font.draw(sb, I18n.txt("gui.minimap.saturn"), ecl2Px(-18, side2), ecl2Px(-1, sideshort2) - px(8));
        font.setColor(1f, 0.3f, 0.3f, 1);
        font.draw(sb, I18n.txt("gui.minimap.uranus"), ecl2Px(-30.0, side2), ecl2Px(10, sideshort2) + px(8));
        font.setColor(0.6f, 0.6f, 1, 1);
        font.draw(sb, I18n.txt("gui.minimap.neptune"), ecl2Px(15.0, side2), ecl2Px(-1, sideshort2) - px(8));
        font.setColor(0.3f, 1f, .3f, 1);
        font.draw(sb, I18n.txt("gui.minimap.pluto"), ecl2Px(30.0, side2), ecl2Px(10, sideshort2) + px(8));
        font.setColor(1f, 1f, .6f, 1);
        font.draw(sb, I18n.txt("gui.minimap.sun"), side2 - px(8), ecl2Px(15, sideshort2) + px(4));
        sb.end();

        fb.end();

    }

    @Override
    public void renderTopProjection(FrameBuffer fb) {
        Gdx.gl.glEnable(GL20.GL_BLEND);

        ortho.setToOrtho(true, side, side);
        sr.setProjectionMatrix(ortho.combined);
        sb.setProjectionMatrix(ortho.combined);
        fb.begin();
        // Clear
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ICamera cam = GaiaSky.instance.cam.current;
        // Position
        float cx = this.camf[2];
        float cy = this.camf[3];
        // Direction
        Vector3d dir = aux3d2.set(cam.getDirection()).mul(trans);
        Vector2d camdir2 = aux2d2.set(dir.x, dir.z).nor().scl(px(15f));

        sr.begin(ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        // Orbits
        // Saturn
        sr.setColor(0f, 1f, 1f, 1f);
        sr.circle(side2, side2, (float) (9.2 / extent) * side2);
        // Uranus
        sr.setColor(1f, 0.3f, 0.3f, 1f);
        sr.circle(side2, side2, (float) (20 / extent) * side2);
        // Neptune
        sr.setColor(0.6f, 0.6f, 1f, 1f);
        sr.circle(side2, side2, (float) (30 / extent) * side2);
        // Pluto
        sr.setColor(0.4f, 1f, 0.4f, 1f);
        sr.circle(side2 + side2 / 6, side2 + side2 / 10, (float) (38 / extent) * side2);
        sr.end();
        Gdx.gl.glLineWidth(1f);

        sr.begin(ShapeType.Filled);
        // Bodies
        // Saturn
        sr.setColor(0f, 1f, 1f, 1f);
        sr.circle(satf[2], satf[3], px(3f));
        // Uranus
        sr.setColor(1f, 0.3f, 0.3f, 1f);
        sr.circle(uraf[2], uraf[3], px(3f));
        // Neptune
        sr.setColor(0.6f, 0.6f, 1f, 1f);
        sr.circle(nepf[2], nepf[3], px(3f));
        // Pluto
        sr.setColor(0.3f, 1.0f, 0.3f, 1f);
        sr.circle(plutf[2], plutf[3], px(3f));

        // Camera
        sr.setColor(ColourUtils.gRedC);
        sr.circle(cx, cy, 8f);
        Vector2d endx = aux2d1.set(camdir2.x, camdir2.y);
        endx.rotate(-cam.getCamera().fieldOfView / 2d);
        float c1x = (float) endx.x + cx;
        float c1y = (float) endx.y + cy;
        endx.set(camdir2.x, camdir2.y);
        endx.rotate(cam.getCamera().fieldOfView / 2d);
        sr.triangle(cx, cy, c1x, c1y, (float) endx.x + cx, (float) endx.y + cy);

        // Camera viewport
        sr.setColor(1,1,1,0.1f);
        endx = aux2d1.set(camdir2.x, camdir2.y).scl(40f);
        endx.rotate(-cam.getCamera().fieldOfView / 2d);
        c1x = (float) endx.x + cx;
        c1y = (float) endx.y + cy;
        endx.set(camdir2.x, camdir2.y).scl(40f);
        endx.rotate(cam.getCamera().fieldOfView / 2d);
        sr.triangle(cx, cy, c1x, c1y, (float) endx.x + cx, (float) endx.y + cy);

        // GC
        sr.setColor(1f, 1f, 0f, 1f);
        sr.circle(side2, side2, px(7.5f));
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(1f, 1f, 0.f, 1);
        font.draw(sb, I18n.txt("gui.minimap.sun"), side2 + px(5), side2 - px(5));
        sb.end();

        fb.end();

    }

    private float px(float px) {
        return px * GlobalConf.UI_SCALE_FACTOR;
    }

    /** 
     * Converts a galactocentric coordinate in parsecs to pixels, given the side/2 of
     * the end minimap
     * @param pc The galactocentric coordinate in parsecs
     * @param side Side/2 of minimap
     * @return Pixel coordinate
     */
    private int gal2Px(double pc, float side) {
        return (int) ((pc / 16000d) * side + side);
    }

    /**
     * Converts a ecliptic coordinate in astronomical units to pixels, given the side/2 of
     * the end minimap
     * @param au The ecliptic coordinate in astronomical units
     * @param side Side/2 of minimap
     * @return Pixel coordinate
     */
    private int ecl2Px(double au, float side) {
        return (int) ((au / extent) * side + side);
    }

}
