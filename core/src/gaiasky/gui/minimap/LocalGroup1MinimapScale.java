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
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import gaiasky.util.Constants;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3D;

public class LocalGroup1MinimapScale extends AbstractMinimapScale {
    private int ngals;
    private Vector3D[] positions;
    private float[][] galp;
    private String[] names;

    private Color galc;
    private float gals;

    public LocalGroup1MinimapScale() {
        super();
    }

    @Override
    public void updateLocal() {

        for (int i = 0; i < positions.length; i++) {
            position(positions[i], galp[i]);
        }
    }

    @Override
    public void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort) {
        super.initialize(ortho, sb, sr, font, side, sideshort, Constants.PC_TO_U, Constants.U_TO_PC, 5E6, 16000);
        trans = Coordinates.eqToGal();

        ngals = 8;
        galp = new float[ngals][];
        positions = new Vector3D[ngals];
        names = new String[ngals];

        int i = 0;
        addGalaxy(i++, "Andromeda", 10.69, 41.27, 0.778);
        addGalaxy(i++, "Triangulum", 1.565, 30.65, 3.0);
        addGalaxy(i++, "SMC", 13.158, -72.8002, 0.06);
        addGalaxy(i++, "LeoA", 149.85, 30.74, 0.81);
        addGalaxy(i++, "Bode's", 148.88, 69.06, 3.63);
        addGalaxy(i++, "Sc22", 5.96, -24.705, 4.21);
        addGalaxy(i++, "KK258", 340.18, -30.79, 2.0);
        addGalaxy(i++, "KK196", 200.44, -45.06, 3.98);

        galc = ColorUtils.gBlueC;
        gals = 3f;
    }

    private void addGalaxy(int i, String name, double raDeg, double decDeg, double distMpc) {
        galp[i] = new float[4];
        positions[i] = get(raDeg, decDeg, distMpc);
        names[i] = name;

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

        // MW
        sr.setColor(sunc);
        sr.circle(side2, sideshort2, px(suns));

        // Galaxies
        sr.setColor(galc);
        for (float[] g : galp)
            sr.circle(g[0], g[1], px(gals));

        renderCameraSide(0.4f);
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(sunc);
        font.draw(sb, I18n.msg("gui.minimap.milkyway"), side2 + px(7), sideshort2 - px(2));

        font.setColor(galc);
        for (int i = 0; i < ngals; i++) {
            font.draw(sb, names[i], galp[i][0] + px(5), galp[i][1] - px(10));
        }

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

        // Gal pointers
        for (float[] g : galp) {
            sr.line(g[2], g[3], g[2], g[3] - px(24));
        }
        sr.end();

        sr.begin(ShapeType.Filled);

        // MW
        sr.setColor(sunc);
        sr.circle(side2, side2, px(suns));

        // Galaxies
        sr.setColor(galc);
        for (float[] g : galp)
            sr.circle(g[2], g[3], px(gals));

        renderCameraTop(0.4f);
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(1, 1, 0, 1);
        font.draw(sb, I18n.msg("gui.minimap.milkyway"), side2 + px(10), side2 + px(10));

        font.setColor(galc);
        for (int i = 0; i < ngals; i++) {
            font.draw(sb, names[i], galp[i][2] + px(5), galp[i][3] - px(15));
        }

        font.setColor(textmc);
        font.draw(sb, "0" + I18n.msg("gui.unit.deg"), side2 - px(15), side - px(5));
        font.draw(sb, "270" + I18n.msg("gui.unit.deg"), side - px(30), side2 + px(15));
        font.draw(sb, "180" + I18n.msg("gui.unit.deg"), side2 + px(3), px(15));
        font.draw(sb, "90" + I18n.msg("gui.unit.deg"), px(5), side2 + px(15));

        font.setColor(textgc);
        font.draw(sb, "2.5" + I18n.msg("gui.unit.mpc"), side2 + px(15), side2 + side2 / 2f + px(10));
        font.draw(sb, "5" + I18n.msg("gui.unit.mpc"), side2 + px(25), side - px(15));
        sb.end();

        fb.end();

    }

    @Override
    public String getName() {
        return I18n.msg("gui.minimap.localgroup") + " (1)";
    }
}
