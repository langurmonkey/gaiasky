/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.minimap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import gaiasky.util.Constants;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Matrix4D;

public class HeliosphereMinimapScale extends AbstractMinimapScale {

    private final Color helc;
    private final Color helpc;
    private final Color intc;

    public HeliosphereMinimapScale() {
        super();
        helc = new Color(0.4f, 0.8f, 0.5f, 1f);
        helpc = new Color(1f, 1f, 0.4f, 1f);
        intc = new Color(0.8f, 0.2f, 1f, 1f);
    }

    @Override
    public void updateLocal() {
    }

    @Override
    public void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort) {
        super.initialize(ortho, sb, sr, font, side, sideshort, Constants.AU_TO_U, Constants.U_TO_AU, 1000, 50);
        trans = new Matrix4D();
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
        // Interstellar
        sr.setColor(intc);
        sr.getColor().a = 0.4f;
        sr.circle(side2, sideshort2, (float) (1000d / extentUp) * side2);
        // Heliopause
        sr.setColor(helpc);
        sr.getColor().a = 0.9f;
        sr.circle(side2, sideshort2, (float) (110d / extentUp) * side2);
        // Heliosphere
        sr.setColor(helc);
        sr.getColor().a = 0.9f;
        sr.circle(side2, sideshort2, (float) (90d / extentUp) * side2);

        // Sun
        sr.setColor(sunc);
        sr.circle(side2, sideshort2, px(suns));

        renderCameraSide(0.4f);
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(helc);
        font.draw(sb, I18n.msg("gui.minimap.heliosphere"), side2 + px(8), u2Px(100, sideshort2) + px(8));
        font.setColor(helpc);
        font.draw(sb, I18n.msg("gui.minimap.heliopause"), px(80), px(40));
        font.setColor(intc);
        font.draw(sb, I18n.msg("gui.minimap.interstellar"), px(10), px(90));
        font.setColor(sunc);
        font.draw(sb, I18n.obj("sun"), side2 + px(8), u2Px(10, sideshort2) - px(2));

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

        // Fill
        sr.begin(ShapeType.Filled);
        // Interstellar
        sr.setColor(intc);
        sr.getColor().a = 0.4f;
        sr.circle(side2, side2, (float) (1000d / extentUp) * side2);
        // Heliopause
        sr.setColor(helpc);
        sr.getColor().a = 0.9f;
        sr.circle(side2, side2, (float) (110d / extentUp) * side2);
        // Heliosphere
        sr.setColor(helc);
        sr.getColor().a = 0.9f;
        sr.circle(side2, side2, (float) (90d / extentUp) * side2);

        // Sun
        sr.setColor(sunc);
        sr.circle(side2, side2, px(suns));

        renderCameraTop(0.4f);
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(textbc);
        font.draw(sb, "100 " + I18n.msg("gui.unit.au"), side2, u2Px(200, side2));
        font.draw(sb, "1000 " + I18n.msg("gui.unit.au"), side2, u2Px(1000 + 3, side2));

        font.setColor(helc);
        font.draw(sb, I18n.msg("gui.minimap.heliosphere"), side2 + px(15), u2Px(0, side2) + px(8));
        font.setColor(helpc);
        font.draw(sb, I18n.msg("gui.minimap.heliopause"), side2 - px(80), side2 - px(40));
        font.setColor(intc);
        font.draw(sb, I18n.msg("gui.minimap.interstellar"), px(60), px(30));
        font.setColor(sunc);
        font.draw(sb, I18n.obj("sun"), side2 + px(5), side2 - px(5));

        sb.end();

        fb.end();

    }

    @Override
    public String getName() {
        return I18n.msg("gui.minimap.heliosphere");
    }
}
