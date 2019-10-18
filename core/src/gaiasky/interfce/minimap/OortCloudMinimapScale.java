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
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.I18n;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3d;

public class OortCloudMinimapScale extends AbstractMinimapScale {

    private float[] camf;
    private Color occ, sunc;

    public OortCloudMinimapScale() {
        super();
        camf = new float[4];

        sunc = new Color(1f, 1f, 0.4f, 1f);
        occ = new Color(0.4f, 0.8f, 0.5f, 1f);
    }

    @Override
    public void update(){
        project(GaiaSky.instance.cam.getPos(), camf);
    }


    @Override
    public void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort) {
        super.initialize(ortho, sb, sr, font, side, sideshort, Constants.AU_TO_U, Constants.U_TO_AU, 100000, 1000);
        trans = new Matrix4d();
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
        Vector3d dir = aux3d2.set(cam.getDirection());
        Vector2d camdir2 = aux2d2.set(dir.z, dir.y).nor().scl(px(15f));

        sr.begin(ShapeType.Filled);
        float xcenter = u2Px(0, side2);
        float ycenter = u2Px(0, sideshort2);
        // Oort
        sr.setColor(occ);
        sr.getColor().a = 0.7f;
        sr.circle(xcenter, ycenter, (float) (100000d / extentUp) * side2);
        sr.setColor(0, 0, 0, 1);
        sr.circle(xcenter, ycenter, (float) (10000d / extentUp) * side2);

        // Sun
        sr.setColor(sunc);
        sr.circle(xcenter, ycenter, px(5f));


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
        sr.setColor(1,1,1,0.4f);
        endx = aux2d1.set(camdir2.x, camdir2.y).scl(40f);
        endx.rotate(-cam.getCamera().fieldOfView / 2d);
        c1x = (float) endx.x + cx;
        c1y = (float) endx.y + cy;
        endx.set(camdir2.x, camdir2.y).scl(40f);
        endx.rotate(cam.getCamera().fieldOfView / 2d);
        sr.triangle(cx, cy, c1x, c1y, (float) endx.x + cx, (float) endx.y + cy);
        sr.end();

        Gdx.gl.glLineWidth(2f);
        sr.begin(ShapeType.Line);
        sr.setColor(occ);
        sr.circle(side2, sideshort2, (float) (100000d / extentUp) * side2);
        sr.circle(side2, sideshort2, (float) (50000d / extentUp) * side2);
        sr.circle(side2, sideshort2, (float) (10000d / extentUp) * side2);
        sr.end();
        Gdx.gl.glLineWidth(1f);

        // Fonts
        sb.begin();
        font.setColor(occ);
        font.draw(sb, I18n.txt("gui.minimap.oort"),  side2 - px(60),  u2Px(-50000, sideshort2) + px(8));
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
        Vector3d dir = aux3d2.set(cam.getDirection());
        Vector2d camdir2 = aux2d2.set(dir.x, dir.z).nor().scl(px(15f));


        // Fill
        sr.begin(ShapeType.Filled);
        // Oort
        sr.setColor(occ);
        sr.getColor().a = 0.7f;
        sr.circle(side2, side2, (float) (100000d / extentUp) * side2);
        sr.setColor(0, 0, 0, 1);
        sr.circle(side2, side2, (float) (10000d / extentUp) * side2);

        // Sun
        sr.setColor(sunc);
        sr.circle(side2, side2, px(5f));

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
        sr.setColor(1,1,1,0.4f);
        endx = aux2d1.set(camdir2.x, camdir2.y).scl(40f);
        endx.rotate(-cam.getCamera().fieldOfView / 2d);
        c1x = (float) endx.x + cx;
        c1y = (float) endx.y + cy;
        endx.set(camdir2.x, camdir2.y).scl(40f);
        endx.rotate(cam.getCamera().fieldOfView / 2d);
        sr.triangle(cx, cy, c1x, c1y, (float) endx.x + cx, (float) endx.y + cy);

        sr.end();

        Gdx.gl.glLineWidth(2f);
        sr.begin(ShapeType.Line);
        sr.setColor(occ);
        sr.circle(side2, side2, (float) (100000d / extentUp) * side2);
        sr.circle(side2, side2, (float) (50000d / extentUp) * side2);
        sr.circle(side2, side2, (float) (10000d / extentUp) * side2);
        sr.end();
        Gdx.gl.glLineWidth(1f);


        // Fonts
        sb.begin();
        font.setColor(textc);
        font.draw(sb, "10,000 AU", side2, u2Px(19000, side2));
        font.draw(sb, "50,000 AU", side2, u2Px(60000, side2));
        font.draw(sb, "100,000 AU", side2, u2Px(95000, side2));

        font.setColor(occ);
        font.draw(sb, I18n.txt("gui.minimap.oort"),  side2 + px(15),  u2Px(-60000, side2) + px(8));
        font.setColor(sunc);
        font.draw(sb, I18n.txt("gui.minimap.sun"), side2 + px(5), side2 - px(5));

        sb.end();

        fb.end();

    }
}
