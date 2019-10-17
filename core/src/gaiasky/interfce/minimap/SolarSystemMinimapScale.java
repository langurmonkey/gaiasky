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
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.color.ColourUtils;
import gaiasky.util.coord.Coordinates;
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

    public SolarSystemMinimapScale() {
        super();
        aux3d1 = new Vector3d();
        aux3d2 = new Vector3d();
        aux2d1 = new Vector2d();
        aux2d2 = new Vector2d();
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
        Vector3d pos = aux3d1.set(cam.getPos()).mul(Coordinates.eqToEcl());
        Vector2d campos2 = aux2d1;
        campos2.set(pos.z, pos.y).scl(from);
        float cx = ecl2Px(campos2.x, side2);
        float cy = ecl2Px(campos2.y, sideshort2);
        // Direction
        Vector3d dir = aux3d2.set(cam.getDirection()).mul(Coordinates.eqToEcl());
        Vector2d camdir2 = aux2d2.set(dir.z, dir.y).nor().scl(px(15f));

        sr.begin(ShapeType.Filled);
        float ycenter = ecl2Px(0, sideshort2);
        // Pluto orbit
        sr.setColor(0.3f, 1.0f, 0.3f, 1f);
        sr.rectLine(ecl2Px(-49, side2), ycenter, ecl2Px(49, side2), ycenter, 2f);
        // Neptune orbit
        sr.setColor(0.6f, 0.6f, 1.0f, 1f);
        sr.rectLine(ecl2Px(-30, side2), ycenter, ecl2Px(30, side2), ycenter, 2f);
        // Uranus orbit
        sr.setColor(1.0f, 0.3f, 0.3f, 1f);
        sr.rectLine(ecl2Px(-20, side2), ycenter, ecl2Px(20, side2), ycenter, 2f);
        // Saturn orbit
        sr.setColor(0.0f, 1.f, 1.f, 1f);
        sr.rectLine(ecl2Px(-9.2, side2), ycenter, ecl2Px(9.2, side2), ycenter, 2f);
        //sr.ellipse(0, sideshort2 - side * 0.015f, side, side * 0.02f);
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
        sr.circle(ecl2Px(-9.2, side2), ycenter, px(2.5f));
        sr.circle(ecl2Px(9.2, side2), ycenter, px(2.5f));
        // Uranus
        sr.setColor(1f, 0.3f, 0.3f, 1f);
        sr.circle(ecl2Px(-20.0, side2), ycenter, px(2.5f));
        sr.circle(ecl2Px(20.0, side2), ycenter, px(2.5f));
        // Neptune
        sr.setColor(0.6f, 0.6f, 1f, 1f);
        sr.circle(ecl2Px(-30.0, side2), ycenter, px(2.5f));
        sr.circle(ecl2Px(30.0, side2), ycenter, px(2.5f));
        // Neptune
        sr.setColor(0.3f, 1.0f, 0.3f, 1f);
        sr.circle(ecl2Px(-49.0, side2), ycenter, px(2.5f));
        sr.circle(ecl2Px(49.0, side2), ycenter, px(2.5f));
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
        Vector3d pos = aux3d1.set(cam.getPos()).mul(Coordinates.eqToEcl());
        Vector2d campos2 = aux2d1;
        campos2.set(pos.x, pos.z).scl(from);
        float cx = ecl2Px(campos2.x, side2);
        float cy = ecl2Px(campos2.y, side2);
        // Direction
        Vector3d dir = aux3d2.set(cam.getDirection()).mul(Coordinates.eqToEcl());
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
        sr.circle(side2, side2, (float) (49 / extent) * side2);
        sr.end();
        Gdx.gl.glLineWidth(1f);

        sr.begin(ShapeType.Filled);
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
