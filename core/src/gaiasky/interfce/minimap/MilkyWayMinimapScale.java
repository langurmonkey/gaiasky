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
import gaiasky.util.Constants;
import gaiasky.util.I18n;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3d;

public class MilkyWayMinimapScale extends AbstractMinimapScale {
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
        trans = Coordinates.eqToGal();
        sunPos = new Vector2(u2Px(0, side2), u2Px(-8000, side2));
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
        Gdx.gl.glEnable(GL20.GL_BLEND);

        ortho.setToOrtho(true, side, sideshort);
        sr.setProjectionMatrix(ortho.combined);
        sb.setProjectionMatrix(ortho.combined);
        fb.begin();
        // Clear
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.begin(ShapeType.Filled);
        // Mw disk
        sr.setColor(0.15f, 0.15f, 0.35f, 1f);
        sr.ellipse(0, sideshort2 - side * 0.015f, side, side * 0.03f);
        // Mw bulge
        sr.setColor(0.05f, 0.05f, 0.2f, 1f);
        sr.circle(side2, sideshort2, side * 0.08f);


        // Sun position, 8 Kpc to do galactocentric
        sr.setColor(sunc);
        sr.circle(u2Px(-8000, side2), sideshort2, px(suns));
        // GC
        sr.setColor(0f, 0f, 0f, 1f);
        sr.circle(side2, sideshort2, px(2.5f));

        renderCameraSide(0.4f);
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(sunc);
        font.draw(sb, I18n.txt("gui.minimap.sun"), u2Px(-8000, side2), sideshort2 - px(8));
        font.setColor(textbc);
        font.draw(sb, I18n.txt("gui.minimap.gc"), side2, sideshort2 - px(4));
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


        sr.begin(ShapeType.Filled);
        // Grid
        float col = 0.0f;
        for (int i = 16000; i >= 4000; i -= 4000) {
            sr.setColor(0.15f-col, 0.15f-col, 0.35f-col, 1f);
            sr.circle(side2, side2, i * side / 32000);
            col += 0.05f;
        }
        sr.circle(side2, side2, 1.5f);

        sr.end();

        // Grid
        sr.begin(ShapeType.Line);
        sr.setColor(textbc);
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

        renderCameraTop();
        sr.end();


        // Fonts
        sb.begin();
        font.setColor(sunc);
        font.draw(sb, I18n.txt("gui.minimap.sun"), side2, sunPos.y - px(8));
        font.setColor(textbc);
        font.draw(sb, I18n.txt("gui.minimap.gc"), side2 + px(4), side2 - px(4));
        for (int i = 4000; i <= 16000; i += 4000) {
            font.draw(sb, "" + (i / 1000) + "Kpc", side2 + px(4), (16000 + i) * side / 32000 - px(6));
        }

        font.draw(sb, "0°", side2 - px(15), side - px(5));
        font.draw(sb, "270°", side - px(33), sunPos.y + px(15));
        font.draw(sb, "180°", side2 - px(32), px(15));
        font.draw(sb, "90°", px(15), sunPos.y + px(15));
        sb.end();

        fb.end();

    }

    @Override
    public String getName() {
        return I18n.txt("gui.minimap.milkyway");
    }

}
