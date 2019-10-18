/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce.minimap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import gaiasky.GaiaSky;
import gaiasky.scenegraph.Planet;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.I18n;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3d;

public class InnerSolarSystemMinimapScale extends AbstractMinimapScale {

    private float[] camf, merf, venf, earf, marf;
    private Planet mer, ven, ear, mar;
    private Color merc, venc, marc, earc, sunc;

    public InnerSolarSystemMinimapScale() {
        super();
        camf = new float[4];
        merf = new float[4];
        venf = new float[4];
        earf = new float[4];
        marf = new float[4];

        merc = new Color(0.4f, 0.5f, 0.4f, 1f);
        venc = new Color(1f, 0.6f, 0.1f, 1f);
        earc = new Color(0.4f, 0.4f, 1f, 1f);
        marc = new Color(0.8f, 0.2f, 0.2f, 1f);
        sunc = new Color(1f, 1f, 0.4f, 1f);
    }

    @Override
    public void update(){
        if(mer == null){
            mer = (Planet) GaiaSky.instance.sg.getNode("Mercury");
            ven = (Planet) GaiaSky.instance.sg.getNode("Venus");
            ear = (Planet) GaiaSky.instance.sg.getNode("Earth");
            mar = (Planet) GaiaSky.instance.sg.getNode("Mars");
        }
        project(mer.getAbsolutePosition(aux3d1), merf);
        project(ven.getAbsolutePosition(aux3d1), venf);
        project(ear.getAbsolutePosition(aux3d1), earf);
        project(mar.getAbsolutePosition(aux3d1), marf);
        project(GaiaSky.instance.cam.getPos(), camf);
    }


    @Override
    public void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort) {
        super.initialize(ortho, sb, sr, font, side, sideshort, Constants.AU_TO_U, Constants.U_TO_AU, 2.2, 0);
        trans = Coordinates.eqToEcl();
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
        float ycenter = u2Px(0, sideshort2);
        // Mars orbit
        sr.setColor(marc);
        sr.rectLine(u2Px(-1.6, side2), ycenter, u2Px(1.6, side2), ycenter, 2f);
        // Earth orbit
        sr.setColor(earc);
        sr.rectLine(u2Px(-1, side2), ycenter, u2Px(1, side2), ycenter, 2f);
        // Venus orbit
        sr.setColor(venc);
        sr.rectLine(u2Px(-0.71, side2), ycenter, u2Px(0.71, side2), ycenter, 2f);
        // Mercury orbit
        sr.setColor(merc);
        sr.rectLine(u2Px(-0.45, side2), ycenter, u2Px(0.45, side2), ycenter, 2f);
        // Sun
        sr.setColor(sunc);
        sr.circle(u2Px(0, side2), ycenter, px(5f));


        // Planet positions
        // Mercury
        sr.setColor(merc);
        sr.circle(merf[0], merf[1], px(3f));
        // Venus
        sr.setColor(venc);
        sr.circle(venf[0], venf[1], px(3f));
        // Earth
        sr.setColor(earc);
        sr.circle(earf[0], earf[1], px(3f));
        // Mars
        sr.setColor(marc);
        sr.circle(marf[0], marf[1], px(3f));

        // Camera
        sr.setColor(camc);
        sr.circle(cx, cy, 8f);
        Vector2d endx = aux2d1.set(camdir2.x, camdir2.y);
        endx.rotate(-cam.getCamera().fieldOfView / 2d);
        float c1x = (float) endx.x + cx;
        float c1y = (float) endx.y + cy;
        endx.set(camdir2.x, camdir2.y);
        endx.rotate(cam.getCamera().fieldOfView / 2d);
        sr.triangle(cx, cy, c1x, c1y, (float) endx.x + cx, (float) endx.y + cy);

        // Camera viewport
        sr.setColor(1,1,1,.4f);
        endx = aux2d1.set(camdir2.x, camdir2.y).scl(40f);
        endx.rotate(-cam.getCamera().fieldOfView / 2d);
        c1x = (float) endx.x + cx;
        c1y = (float) endx.y + cy;
        endx.set(camdir2.x, camdir2.y).scl(40f);
        endx.rotate(cam.getCamera().fieldOfView / 2d);
        sr.triangle(cx, cy, c1x, c1y, (float) endx.x + cx, (float) endx.y + cy);
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(merc);
        font.draw(sb, I18n.txt("gui.minimap.mercury"), merf[0] - px(20),  merf[1] + px(25));
        font.setColor(venc);
        font.draw(sb, I18n.txt("gui.minimap.venus"), venf[0] - px(20), venf[1] - px(25));
        font.setColor(earc);
        font.draw(sb, I18n.txt("gui.minimap.earth"), earf[0] - px(20), earf[1] + px(40));
        font.setColor(marc);
        font.draw(sb, I18n.txt("gui.minimap.mars"), marf[0] - px(20),  marf[1] - px(10));
        font.setColor(sunc);
        font.draw(sb, I18n.txt("gui.minimap.sun"), side2 + px(8), u2Px(10, sideshort2) - px(2));
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

        // Fill
        sr.begin(ShapeType.Filled);
        // Mars
        sr.setColor(marc);
        sr.getColor().mul(0.2f, 0.2f, 0.2f, 1f);
        sr.circle(side2 + px(4), side2 - px(4), (float) (1.54 / extentUp) * side2);
        // Earth
        sr.setColor(earc);
        sr.getColor().mul(0.2f, 0.2f, 0.2f, 1f);
        sr.circle(side2, side2, (float) (1 / extentUp) * side2);
        // Venus
        sr.setColor(venc);
        sr.getColor().mul(0.2f, 0.2f, 0.2f, 1f);
        sr.circle(side2, side2, (float) (0.71 / extentUp) * side2);
        // Mercury
        sr.setColor(merc);
        sr.getColor().mul(0.2f, 0.2f, 0.2f, 1f);
        sr.circle(side2 - px(3), side2, (float) (0.4 / extentUp) * side2);
        sr.end();

        sr.begin(ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        // Orbits
        // Mercury
        sr.setColor(merc);
        sr.circle(side2 - px(3), side2, (float) (0.4 / extentUp) * side2);
        // Venus
        sr.setColor(venc);
        sr.circle(side2, side2, (float) (0.71 / extentUp) * side2);
        // Earth
        sr.setColor(earc);
        sr.circle(side2, side2, (float) (1 / extentUp) * side2);
        // Mars
        sr.setColor(marc);
        sr.circle(side2 + px(4), side2 - px(4), (float) (1.54 / extentUp) * side2);
        sr.end();
        Gdx.gl.glLineWidth(1f);

        sr.begin(ShapeType.Filled);
        // Bodies
        // Sun
        sr.setColor(sunc);
        sr.circle(side2, side2, px(5f));
        // Mercury
        sr.setColor(merc);
        sr.circle(merf[2], merf[3], px(3f));
        // Venus
        sr.setColor(venc);
        sr.circle(venf[2], venf[3], px(3f));
        // Earth
        sr.setColor(earc);
        sr.circle(earf[2], earf[3], px(3f));
        // Mars
        sr.setColor(marc);
        sr.circle(marf[2], marf[3], px(3f));

        // Camera
        sr.setColor(camc);
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

        sr.end();

        // Fonts
        sb.begin();
        font.setColor(textc);
        font.draw(sb, "0.45 AU", side2, u2Px(0.45 + 0.2, side2));
        font.draw(sb, "0.71 AU", side2, u2Px(0.71 + 0.2, side2));
        font.draw(sb, "1 AU", side2, u2Px(1 + 0.2, side2));
        font.draw(sb, "1.6 AU", side2, u2Px(1.6 + 0.2, side2));
        
        font.setColor(merc);
        font.draw(sb, I18n.txt("gui.minimap.mercury"), merf[2] - px( 20),  merf[3] - px(8));
        font.setColor(venc);
        font.draw(sb, I18n.txt("gui.minimap.venus"), venf[2] - px(20), venf[3] - px(8));
        font.setColor(earc);
        font.draw(sb, I18n.txt("gui.minimap.earth"), earf[2] - px(20), earf[3] - px(8));
        font.setColor(marc);
        font.draw(sb, I18n.txt("gui.minimap.mars"), marf[2] - px(20), marf[3] - px(8));
        font.setColor(sunc);
        font.draw(sb, I18n.txt("gui.minimap.sun"), side2 + px(5), side2 - px(5));

        sb.end();

        fb.end();

    }
}
