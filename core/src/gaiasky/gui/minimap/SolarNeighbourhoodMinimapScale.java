/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.minimap;

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
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;
import gaiasky.util.Constants;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3d;

public class SolarNeighbourhoodMinimapScale extends AbstractMinimapScale {
    private Image topProjection;
    private Image sideProjection;

    public SolarNeighbourhoodMinimapScale() {
        super();
        camp = new float[4];
    }

    @Override
    public void updateLocal() {
    }

    @Override
    public void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort) {
        super.initialize(ortho, sb, sr, font, side, sideshort, Constants.PC_TO_U, Constants.U_TO_PC, 500, 100000 * Constants.AU_TO_U * Constants.U_TO_PC);
        Texture texTop = new Texture(Gdx.files.internal("img/minimap/solar_neighbourhood_top_s.jpg"));
        texTop.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        topProjection = new Image(texTop);
        topProjection.setScaling(Scaling.fit);
        topProjection.setSize(side, side);
        Texture texSide = new Texture(Gdx.files.internal("img/minimap/solar_neighbourhood_side_s.jpg"));
        texSide.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        sideProjection = new Image(texSide);
        sideProjection.setScaling(Scaling.fit);
        sideProjection.setSize(side, sideshort);
        trans = Coordinates.eqToGal();
    }

    public float[] position(Vector3d pos, float[] out) {
        Vector3d p = aux3d1.set(pos).mul(trans);
        Vector2d pos2d = aux2d1;
        pos2d.set(p.z, p.y).scl(from);
        float cx = u2Px(pos2d.x, side2);
        float cy = u2Px(pos2d.y, sideshort2);
        out[0] = cx;
        out[1] = cy;

        pos2d.set(-p.x, p.z).scl(from);
        cx = u2Px(pos2d.x, side2);
        cy = u2Px(pos2d.y, side2);
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
        sr.setColor(textyc);
        sr.getColor().a *= 0.6f;
        sr.line(0, sideshort2, side, sideshort2);
        sr.line(side2, 0, side2, side);
        sr.setColor(textbc);
        sr.getColor().a *= 0.3f;
        sr.circle(side2, sideshort2, side2);
        sr.circle(side2, sideshort2, side2 / 2f);
        sr.end();

        sr.begin(ShapeType.Filled);
        sr.setColor(sunc);
        sr.circle(side2, sideshort2, px(suns));
        renderCameraSide();
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(sunc);
        font.draw(sb, I18n.obj("sun"), side2 + px(7), sideshort2);
        font.setColor(textgc);
        font.draw(sb, I18n.msg("gui.minimap.togc"), side - px(50), sideshort2 + px(15));
        font.draw(sb, I18n.msg("gui.minimap.tooutergalaxy"), 0, sideshort2 + px(15));
        font.draw(sb, I18n.msg("gui.minimap.ngp"), side2 - px(30), sideshort);
        font.draw(sb, I18n.msg("gui.minimap.sgp"), side2 - px(30), px(30));
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
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Background
        sb.begin();
        topProjection.draw(sb, 1);
        sb.end();

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        // Grid
        sr.begin(ShapeType.Line);
        sr.setColor(textyc);
        sr.getColor().a *= 0.6f;
        sr.line(0, side2, side, side2);
        sr.line(side2, 0, side2, side);
        sr.setColor(textbc);
        sr.getColor().a *= 0.3f;
        sr.circle(side2, side2, side2);
        sr.circle(side2, side2, side2 * 3f / 4f);
        sr.circle(side2, side2, side2 / 2f);
        sr.circle(side2, side2, side2 / 4f);
        sr.end();

        sr.begin(ShapeType.Filled);
        sr.setColor(sunc);
        sr.circle(side2, side2, px(suns));
        renderCameraTop();
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(1, 1, 0, 1);
        font.draw(sb, I18n.obj("sun"), side2 + px(10), side2 + px(10));
        font.setColor(textgc);
        font.draw(sb, I18n.obj("hyades"), side2 + px(5), side2 - px(5));
        font.draw(sb, I18n.obj("pleiades"), side2, side2 - px(20));
        font.draw(sb, I18n.obj("taurus"), side2 - px(30), side2 - px(40));
        font.draw(sb, "Near\nPerseus", side2 - px(70), side2 - px(50));
        font.draw(sb, "Far\nPerseus", side2 - px(50), side2 - px(85));
        font.draw(sb, "Orion", side2 + px(20), side2 - px(85));
        font.draw(sb, "Cepheus", side2 - px(110), side2 - px(30));
        font.draw(sb, "Coalsack", side2 + px(30), side2 + px(30));
        font.draw(sb, "Ophiucus", side2, side2 + px(45));
        font.draw(sb, "Near\nAquila", side2 - px(50), side2 + px(55));
        font.draw(sb, "Serpens", side2 - px(70), side2 + px(85));

        font.setColor(textrc);
        font.draw(sb, "ORI OB1", side2 + px(30), side2 - px(70));
        font.draw(sb, "VEL OB2", side2 + px(65), side2 - px(15));

        font.setColor(textmc);
        font.draw(sb, "0" + I18n.msg("gui.unit.deg"), side2 - px(15), side - px(5));
        font.draw(sb, "270" + I18n.msg("gui.unit.deg"), side - px(30), side2 + px(5));
        font.draw(sb, "180" + I18n.msg("gui.unit.deg"), side2 + px(3), px(15));
        font.draw(sb, "90" + I18n.msg("gui.unit.deg"), px(5), side2 + px(5));

        font.setColor(textbc);
        font.draw(sb, "250" + I18n.msg("gui.unit.pc"), side2 + px(15), side2 + side2 / 2f + px(10));
        font.draw(sb, "500" + I18n.msg("gui.unit.pc"), side2 + px(25), side);
        sb.end();

        fb.end();
    }

    @Override
    public String getName() {
        return I18n.msg("gui.minimap.solarneighbourhood");
    }
}
