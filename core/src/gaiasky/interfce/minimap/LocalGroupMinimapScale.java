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
import gaiasky.util.Constants;
import gaiasky.util.I18n;
import gaiasky.util.coord.Coordinates;

public class LocalGroupMinimapScale extends AbstractMinimapScale {
    private Image topProjection;

    public LocalGroupMinimapScale(){
        super();
    }

    @Override
    public void updateLocal() {
        position(GaiaSky.instance.cam.getPos(), camp);
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

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeType.Filled);
        sr.setColor(1f, 1f, 0f, 1f);
        sr.circle(u2Px(side2, side2), u2Px(0, sideshort2), px(2.5f));

        renderCameraSide(0.4f);
        sr.end();

        fb.end();
    }

    @Override
    public void renderTopProjection(FrameBuffer fb) {

        ortho.setToOrtho(true, side, side);
        sr.setProjectionMatrix(ortho.combined);
        sb.setProjectionMatrix(ortho.combined);
        fb.begin();
        // Clear
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));

        // Background
        sb.begin();
        topProjection.draw(sb, 1);
        sb.end();


        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeType.Filled);
        renderCameraTop(0.1f);
        sr.end();

        fb.end();
    }

    @Override
    public String getName() {
        return I18n.txt("gui.minimap.localgroup");
    }
}
