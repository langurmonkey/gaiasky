/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
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
import gaiasky.GaiaSky;
import gaiasky.scenegraph.Planet;
import gaiasky.util.Constants;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;

public class InnerSolarSystemMinimapScale extends AbstractMinimapScale {

    private final float[] merp;
    private final float[] venp;
    private final float[] earp;
    private final float[] marp;
    private Planet mer, ven, ear, mar;
    private final Color merc;
    private final Color venc;
    private final Color marc;
    private final Color earc;

    public InnerSolarSystemMinimapScale() {
        super();
        merp = new float[4];
        venp = new float[4];
        earp = new float[4];
        marp = new float[4];

        merc = new Color(0.4f, 0.5f, 0.4f, 1f);
        venc = new Color(1f, 0.6f, 0.1f, 1f);
        earc = new Color(0.4f, 0.4f, 1f, 1f);
        marc = new Color(0.8f, 0.2f, 0.2f, 1f);
    }

    @Override
    public void updateLocal() {
        if (mer == null) {
            mer = (Planet) GaiaSky.instance.sceneGraph.getNode("Mercury");
            ven = (Planet) GaiaSky.instance.sceneGraph.getNode("Venus");
            ear = (Planet) GaiaSky.instance.sceneGraph.getNode("Earth");
            mar = (Planet) GaiaSky.instance.sceneGraph.getNode("Mars");
        }
        if (mer != null)
            position(mer.getAbsolutePosition(aux3b1).tov3d(aux3d1), merp);
        if (ven != null)
            position(ven.getAbsolutePosition(aux3b1).tov3d(aux3d1), venp);
        if (ear != null)
            position(ear.getAbsolutePosition(aux3b1).tov3d(aux3d1), earp);
        if (mar != null)
            position(mar.getAbsolutePosition(aux3b1).tov3d(aux3d1), marp);
    }

    @Override
    public void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort) {
        super.initialize(ortho, sb, sr, font, side, sideshort, Constants.AU_TO_U, Constants.U_TO_AU, 2.2, -1);
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
        sr.circle(u2Px(0, side2), ycenter, px(suns));

        // Planet positions
        // Mercury
        sr.setColor(merc);
        sr.circle(merp[0], merp[1], px(3f));
        // Venus
        sr.setColor(venc);
        sr.circle(venp[0], venp[1], px(3f));
        // Earth
        sr.setColor(earc);
        sr.circle(earp[0], earp[1], px(3f));
        // Mars
        sr.setColor(marc);
        sr.circle(marp[0], marp[1], px(3f));

        renderCameraSide(0.4f);
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(merc);
        font.draw(sb, I18n.obj("mercury"), merp[0] - px(20), merp[1] + px(25));
        font.setColor(venc);
        font.draw(sb, I18n.obj("venus"), venp[0] - px(20), venp[1] - px(25));
        font.setColor(earc);
        font.draw(sb, I18n.obj("earth"), earp[0] - px(20), earp[1] + px(40));
        font.setColor(marc);
        font.draw(sb, I18n.obj("mars"), marp[0] - px(20), marp[1] - px(10));
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
        sr.circle(side2, side2, px(suns));
        // Mercury
        sr.setColor(merc);
        sr.circle(merp[2], merp[3], px(3f));
        // Venus
        sr.setColor(venc);
        sr.circle(venp[2], venp[3], px(3f));
        // Earth
        sr.setColor(earc);
        sr.circle(earp[2], earp[3], px(3f));
        // Mars
        sr.setColor(marc);
        sr.circle(marp[2], marp[3], px(3f));

        renderCameraTop(0.1f);
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(textbc);
        font.draw(sb, "0.45 " + I18n.msg("gui.unit.au"), side2, u2Px(0.45 + 0.2, side2));
        font.draw(sb, "0.71 " + I18n.msg("gui.unit.au"), side2, u2Px(0.71 + 0.2, side2));
        font.draw(sb, "1 " + I18n.msg("gui.unit.au"), side2, u2Px(1 + 0.2, side2));
        font.draw(sb, "1.6 " + I18n.msg("gui.unit.au"), side2, u2Px(1.6 + 0.1, side2));

        font.setColor(merc);
        font.draw(sb, I18n.obj("mercury"), merp[2] - px(20), merp[3] - px(8));
        font.setColor(venc);
        font.draw(sb, I18n.obj("venus"), venp[2] - px(20), venp[3] - px(8));
        font.setColor(earc);
        font.draw(sb, I18n.obj("earth"), earp[2] - px(20), earp[3] - px(8));
        font.setColor(marc);
        font.draw(sb, I18n.obj("mars"), marp[2] - px(20), marp[3] - px(8));
        font.setColor(sunc);
        font.draw(sb, I18n.obj("sun"), side2 + px(5), side2 - px(5));

        sb.end();

        fb.end();

    }

    @Override
    public String getName() {
        return I18n.msg("gui.minimap.innerss");
    }
}
