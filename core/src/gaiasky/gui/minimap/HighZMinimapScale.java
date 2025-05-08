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
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import gaiasky.util.Constants;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3D;

public class HighZMinimapScale extends AbstractMinimapScale {

    public HighZMinimapScale() {
        super();
    }

    @Override
    public void updateLocal() {

    }

    @Override
    public void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort) {
        super.initialize(ortho, sb, sr, font, side, sideshort, Constants.PC_TO_U, Constants.U_TO_PC, 20E8, 25E6);
        trans = Coordinates.eqToGal();
    }

    private Vector3D get(double raDeg, double decDeg, double distMpc) {
        return Coordinates.sphericalToCartesian(MathUtilsDouble.degRad * raDeg, MathUtilsDouble.degRad * decDeg, distMpc * Constants.MPC_TO_U, new Vector3D());
    }

    @Override
    public void renderSideProjection(FrameBuffer fb) {
        ortho.setToOrtho(true, side, sideshort);
        sr.setProjectionMatrix(ortho.combined);
        sb.setProjectionMatrix(ortho.combined);
        fb.begin();

        // Clear
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.begin(ShapeType.Filled);
        // Galaxies top
        sr.setColor(0.5f, 0.3f, 0.0f, 0.8f);
        sr.triangle(side2, sideshort2, side2 / 4, sideshort, side2 + side2 * 0.7f, sideshort);
        sr.setColor(0.2f, 0.5f, 0.0f, 1f);
        sr.triangle(side2, sideshort2, side2 / 2, sideshort2 * 1.7f, side2 + side2 * 0.5f, sideshort2 * 1.7f);
        // Galaxies bottom
        sr.setColor(0.5f, 0.3f, 0.0f, 0.8f);
        sr.triangle(side2, sideshort2, side2 / 2, 0, side2 + side2 * 1.3f, 0);
        sr.setColor(0.2f, 0.5f, 0.0f, 1f);
        sr.triangle(side2, sideshort2, side2 * 0.65f, sideshort2 * 0.3f, side2 * 1.5f, sideshort2 * 0.6f);

        sr.end();

        // Grid
        Gdx.gl.glLineWidth(2f);
        sr.begin(ShapeType.Line);
        sr.setColor(textbc);
        sr.line(0, sideshort2, side, sideshort2);
        sr.line(side2, 0, side2, side);
        sr.circle(side2, sideshort2, side2);
        sr.circle(side2, sideshort2, side2 / 2f);
        sr.end();

        sr.begin(ShapeType.Filled);
        // Local Group
        sr.setColor(sunc);
        sr.circle(side2, sideshort2, px(suns));

        renderCameraSide(0.4f);
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(sunc);
        font.draw(sb, I18n.msg("gui.minimap.localgroup"), side2 + px(7), sideshort2 - px(4));
        font.setColor(textyc);
        font.draw(sb, "SDSS", side2 - px(20), sideshort - px(22));

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

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Grid
        Gdx.gl.glLineWidth(2f);
        sr.begin(ShapeType.Line);
        sr.setColor(textbc);
        sr.line(0, side2, side, side2);
        sr.line(side2, 0, side2, side);
        sr.circle(side2, side2, side2);
        sr.circle(side2, side2, side2 / 2f);
        sr.end();

        sr.begin(ShapeType.Filled);

        // Local Group
        sr.setColor(sunc);
        sr.circle(side2, side2, px(suns));

        // Galaxies

        renderCameraTop(0.4f);
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(1, 1, 0, 1);
        font.draw(sb, I18n.msg("gui.minimap.localgroup"), side2 + px(10), side2 + px(20));

        font.setColor(textmc);
        font.draw(sb, "0" + I18n.msg("gui.unit.deg"), side2 - px(15), side - px(5));
        font.draw(sb, "270" + I18n.msg("gui.unit.deg"), side - px(30), side2 + px(15));
        font.draw(sb, "180" + I18n.msg("gui.unit.deg"), side2 + px(3), px(15));
        font.draw(sb, "90" + I18n.msg("gui.unit.deg"), px(5), side2 + px(15));

        font.setColor(textgc);
        font.draw(sb, "1000" + I18n.msg("gui.unit.mpc"), side2 + px(15), side2 + side2 / 2f + px(10));
        font.draw(sb, "2000" + I18n.msg("gui.unit.mpc"), side2 + px(25), side - px(15));
        sb.end();

        fb.end();

    }

    @Override
    public String getName() {
        return I18n.msg("gui.minimap.highz");
    }
}
