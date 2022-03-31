/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce.minimap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;
import gaiasky.util.Constants;
import gaiasky.util.i18n.I18n;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3d;

public class MilkyWayMinimapScale extends AbstractMinimapScale {
    private Image topProjection, sideProjection;
    private Vector2 sunPos;

    public MilkyWayMinimapScale() {
        super();
    }

    @Override
    public void updateLocal(){

    }

    @Override
    public void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort) {
        super.initialize(ortho, sb, sr, font, side, sideshort, Constants.PC_TO_U, Constants.U_TO_PC, 16000, 100000 * Constants.AU_TO_U * Constants.U_TO_PC);
        sunPos = new Vector2(u2Px(0, side2), u2Px(-8000, side2));
        Texture texTop = new Texture(Gdx.files.internal("img/minimap/mw_top.jpg"));
        texTop.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        topProjection = new Image(texTop);
        topProjection.setScaling(Scaling.fit);
        topProjection.setSize(side, side);
        Texture texSide = new Texture(Gdx.files.internal("img/minimap/mw_side.jpg"));
        texSide.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        sideProjection = new Image(texSide);
        sideProjection.setScaling(Scaling.fit);
        sideProjection.setSize(side, sideshort);
        trans = Coordinates.eqToGal();
    }

    public float[] position(Vector3d pos, float[] out) {
        Vector3d p = aux3d1.set(pos).mul(trans);
        Vector2d pos2d = aux2d1;

        // Side
        pos2d.set(p.z, p.y).scl(from);
        float cx = u2Px(pos2d.x - 8000.0, side2);
        float cy = u2Px(pos2d.y, sideshort2);
        out[0] = cx;
        out[1] = cy;

        // Top
        pos2d.set(-p.x, p.z).scl(from);
        cx = u2Px(pos2d.x, side2);
        cy = u2Px(pos2d.y - 8000.0, side2);
        out[2] = cx;
        out[3] = cy;

        return out;
    }

    @Override
    public void renderSideProjection(FrameBuffer fb) {
        ortho.setToOrtho(true, side, sideshort);
        sr.setProjectionMatrix(ortho.combined);
        sb.setProjectionMatrix(ortho.combined);
        fb.begin();
        // Clear
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));

        // Background
        sb.begin();
        sideProjection.draw(sb, 1);
        sb.end();

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Grid
        sr.begin(ShapeType.Line);
        sr.setColor(textbc);
        sr.getColor().a *= 0.2f;
        sr.circle(side2, sideshort2, side2);
        sr.circle(side2, sideshort2, side2 / 2f);
        sr.circle(side2, sideshort2, side2 * 3f / 4f);
        sr.circle(side2, sideshort2, side2 / 4f);
        sr.end();

        sr.begin(ShapeType.Filled);
        // Sun position, 8 Kpc to do galactocentric
        sr.setColor(sunc);
        sr.circle(u2Px(-8000, side2), sideshort2, px(suns));
        // GC
        sr.setColor(0f, 0f, 0f, 1f);
        sr.circle(side2, sideshort2, px(2.5f));

        renderCameraSide(.1f);
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(sunc);
        font.draw(sb, I18n.txt("gui.minimap.sun"), u2Px(-8000, side2), sideshort2 - px(8));
        font.setColor(textbc);
        font.draw(sb, I18n.txt("gui.minimap.gc"), side2, sideshort2 - px(4));
        font.setColor(textmc);
        font.draw(sb, "4Kpc", side2 + px(15), sideshort2 - px(10));
        font.draw(sb, "8Kpc", side2 + px(50), sideshort2 + px(20));
        font.draw(sb, "12Kpc", side2 + px(70), sideshort2 - px(30));
        font.draw(sb, "16Kpc", side2 + px(90), sideshort2 + px(60));
        sb.end();

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

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Grid
        sr.begin(ShapeType.Line);
        sr.setColor(textbc);
        sr.getColor().a *= 0.2f;
        sr.circle(side2, side2, side2);
        sr.circle(side2, side2, side2 / 2f);
        sr.circle(side2, side2, side2 * 3f / 4f);
        sr.circle(side2, side2, side2 / 4f);
        sr.setColor(textyc);
        sr.getColor().a *= 0.6f;
        sr.line(side2, 0, side2, side);
        sr.line(px(15), sunPos.y, side - px(15), sunPos.y);
        sr.end();

        sr.begin(ShapeType.Filled);

        // Sun position, 8 Kpc to do galactocentric
        sr.setColor(sunc);
        sr.circle(sunPos.x, sunPos.y, px(suns));
        // GC
        sr.setColor(0f, 0f, 0f, 1f);
        sr.circle(side2, side2, px(2.5f));

        renderCameraTop(.1f);
        sr.end();


        // Fonts
        sb.begin();
        font.setColor(sunc);
        font.draw(sb, I18n.txt("gui.minimap.sun"), side2, sunPos.y - px(8));
        font.setColor(0, 0, 0, 1);
        font.draw(sb, I18n.txt("gui.minimap.gc"), side2 + px(4), side2 - px(4));
        font.setColor(textmc);
        for (int i = 4000; i <= 16000; i += 4000) {
            font.draw(sb, "" + (i / 1000) + "Kpc", side2 + px(4), (16000f + i) * side / 32000f - px(6));
        }

        font.draw(sb, "0°", side2 - px(15), side - px(5));
        font.draw(sb, "270°", side - px(50), sunPos.y + px(15));
        font.draw(sb, "180°", side2 - px(32), px(15));
        font.draw(sb, "90°", px(18), sunPos.y + px(15));
        sb.end();

        fb.end();

    }

    @Override
    public String getName() {
        return I18n.txt("gui.minimap.milkyway");
    }

}
