/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce.minimap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;
import gaiasky.GaiaSky;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.color.ColourUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3d;

public class LocalGroupMinimapScale extends AbstractMinimapScale {
    private float[] camf;
    private Image topProjection;

    public LocalGroupMinimapScale(){
        super();
        camf = new float[4];
    }

    @Override
    public void update() {
        project(GaiaSky.instance.cam.getPos(), camf);
    }

    @Override
    public void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort) {
        super.initialize(ortho, sb, sr, font, side, sideshort, Constants.PC_TO_U, Constants.U_TO_PC, 6000000, 16000);
        Texture texTop = new Texture(Gdx.files.internal("img/minimap/local_group_top.jpg"));
        topProjection = new Image(texTop);
        topProjection.setScaling(Scaling.fit);
        topProjection.setSize(side, side);
        trans = Coordinates.eqToGal();
    }

    @Override
    public void renderSideProjection(FrameBuffer fb) {
        ortho.setToOrtho(true, side, sideshort);
        sr.setProjectionMatrix(ortho.combined);
        sb.setProjectionMatrix(ortho.combined);
        fb.begin();
        // Clear
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        ICamera cam = GaiaSky.instance.cam.current;

        sr.begin(ShapeType.Filled);
        sr.setColor(1f, 1f, 0f, 1f);
        sr.circle(u2Px(side2, side2), u2Px(0, sideshort2), px(2.5f));
        sr.end();

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

        // Background
        sb.begin();
        topProjection.draw(sb, 1);
        sb.end();

        // Camera
        // Position
        float cx = this.camf[2];
        float cy = this.camf[3];
        // Direction
        Vector3d dir = aux3d2.set(cam.getDirection()).mul(trans);
        Vector2d camdir2 = aux2d2.set(dir.x, dir.z).nor().scl(px(15f));

        sr.begin(ShapeType.Filled);
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
        sr.setColor(1,1,1,0.0f);
        endx = aux2d1.set(camdir2.x, camdir2.y).scl(40f);
        endx.rotate(-cam.getCamera().fieldOfView / 2d);
        c1x = (float) endx.x + cx;
        c1y = (float) endx.y + cy;
        endx.set(camdir2.x, camdir2.y).scl(40f);
        endx.rotate(cam.getCamera().fieldOfView / 2d);
        //sr.triangle(cx, cy, c1x, c1y, (float) endx.x + cx, (float) endx.y + cy);
        sr.end();

        fb.end();
    }
}
