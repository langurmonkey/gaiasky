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

public class OuterSolarSystemMinimapScale extends AbstractMinimapScale {

    private final float[] satf;
    private final float[] uraf;
    private final float[] nepf;
    private final float[] jupf;
    private Planet sat, ura, nep, jup;
    private final Color jupc;
    private final Color satc;
    private final Color nepc;
    private final Color urac;

    public OuterSolarSystemMinimapScale() {
        super();
        satf = new float[4];
        uraf = new float[4];
        nepf = new float[4];
        jupf = new float[4];

        jupc = new Color(0.4f, 0.8f, 1f, 1f);
        satc = new Color(1f, 1f, 0.4f, 1f);
        urac = new Color(0.3f, 0.4f, 1f, 1f);
        nepc = new Color(0.8f, 0.2f, 1f, 1f);
    }

    @Override
    public void updateLocal() {
        if (sat == null) {
            sat = (Planet) GaiaSky.instance.sceneGraph.getNode("Saturn");
            ura = (Planet) GaiaSky.instance.sceneGraph.getNode("Uranus");
            nep = (Planet) GaiaSky.instance.sceneGraph.getNode("Neptune");
            jup = (Planet) GaiaSky.instance.sceneGraph.getNode("Jupiter");
        }
        if (sat != null)
            position(sat.getAbsolutePosition(aux3b1).tov3d(aux3d1), satf);
        if (ura != null)
            position(ura.getAbsolutePosition(aux3b1).tov3d(aux3d1), uraf);
        if (nep != null)
            position(nep.getAbsolutePosition(aux3b1).tov3d(aux3d1), nepf);
        if (jup != null)
            position(jup.getAbsolutePosition(aux3b1).tov3d(aux3d1), jupf);
        position(GaiaSky.instance.cameraManager.getPos().tov3d(aux3d1), camp);
    }

    @Override
    public void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort) {
        super.initialize(ortho, sb, sr, font, side, sideshort, Constants.AU_TO_U, Constants.U_TO_AU, 50, 2.2);
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
        // Neptune orbit
        sr.setColor(nepc);
        sr.rectLine(u2Px(-30, side2), ycenter, u2Px(30, side2), ycenter, 2f);
        // Uranus orbit
        sr.setColor(urac);
        sr.rectLine(u2Px(-20, side2), ycenter, u2Px(20, side2), ycenter, 2f);
        // Saturn orbit
        sr.setColor(satc);
        sr.rectLine(u2Px(-9.2, side2), ycenter, u2Px(9.2, side2), ycenter, 2f);
        // Jupiter orbit
        sr.setColor(jupc);
        sr.rectLine(u2Px(-5.4, side2), ycenter, u2Px(5.4, side2), ycenter, 2f);
        // Sun
        sr.setColor(sunc);
        sr.circle(u2Px(0, side2), ycenter, px(suns));

        // Planet positions
        // Jupiter
        sr.setColor(jupc);
        sr.circle(jupf[0], jupf[1], px(3f));
        // Saturn
        sr.setColor(satc);
        sr.circle(satf[0], satf[1], px(3f));
        // Uranus
        sr.setColor(urac);
        sr.circle(uraf[0], uraf[1], px(3f));
        // Neptune
        sr.setColor(nepc);
        sr.circle(nepf[0], nepf[1], px(3f));

        renderCameraSide(0.4f);
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(jupc);
        font.draw(sb, I18n.msg("gui.minimap.jupiter"), jupf[0] - px(20), jupf[1] - px(10));
        font.setColor(satc);
        font.draw(sb, I18n.msg("gui.minimap.saturn"), satf[0] - px(20), satf[1] + px(25));
        font.setColor(urac);
        font.draw(sb, I18n.msg("gui.minimap.uranus"), uraf[0] - px(20), uraf[1] - px(25));
        font.setColor(nepc);
        font.draw(sb, I18n.msg("gui.minimap.neptune"), nepf[0] - px(20), nepf[1] + px(40));
        font.setColor(sunc);
        font.draw(sb, I18n.msg("gui.minimap.sun"), side2 + px(8), u2Px(10, sideshort2) - px(2));
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
        // Neptune
        sr.setColor(nepc);
        sr.getColor().mul(0.2f, 0.2f, 0.2f, 1f);
        sr.circle(side2, side2, (float) (30 / extentUp) * side2);
        // Uranus
        sr.setColor(urac);
        sr.getColor().mul(0.2f, 0.2f, 0.2f, 1f);
        sr.circle(side2, side2, (float) (20 / extentUp) * side2);
        // Saturn
        sr.setColor(satc);
        sr.getColor().mul(0.2f, 0.2f, 0.2f, 1f);
        sr.circle(side2, side2, (float) (9.2 / extentUp) * side2);
        // Jupiter
        sr.setColor(jupc);
        sr.getColor().mul(0.2f, 0.2f, 0.2f, 1f);
        sr.circle(side2, side2, (float) (5.4 / extentUp) * side2);
        sr.end();

        sr.begin(ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        // Orbits
        // Jupiter
        sr.setColor(jupc);
        sr.circle(side2, side2, (float) (5.4 / extentUp) * side2);
        // Saturn
        sr.setColor(satc);
        sr.circle(side2, side2, (float) (9.2 / extentUp) * side2);
        // Uranus
        sr.setColor(urac);
        sr.circle(side2, side2, (float) (20 / extentUp) * side2);
        // Neptune
        sr.setColor(nepc);
        sr.circle(side2, side2, (float) (30 / extentUp) * side2);
        sr.end();
        Gdx.gl.glLineWidth(1f);

        sr.begin(ShapeType.Filled);
        // Bodies
        // Sun
        sr.setColor(sunc);
        sr.circle(side2, side2, px(suns));
        // Jupiter
        sr.setColor(jupc);
        sr.circle(jupf[2], jupf[3], px(3f));
        // Saturn
        sr.setColor(satc);
        sr.circle(satf[2], satf[3], px(3f));
        // Uranus
        sr.setColor(urac);
        sr.circle(uraf[2], uraf[3], px(3f));
        // Neptune
        sr.setColor(nepc);
        sr.circle(nepf[2], nepf[3], px(3f));

        renderCameraTop(0.2f);
        sr.end();

        // Fonts
        sb.begin();
        font.setColor(textbc);
        font.draw(sb, "5.4 AU", side2, u2Px(5.4 + 2.6, side2));
        font.draw(sb, "9.2 AU", side2, u2Px(9.2 + 3, side2));
        font.draw(sb, "20 AU", side2, u2Px(20 + 3, side2));
        font.draw(sb, "30 AU", side2, u2Px(30 + 3, side2));

        font.setColor(jupc);
        font.draw(sb, I18n.msg("gui.minimap.jupiter"), jupf[2] - px(20), jupf[3] - px(8));
        font.setColor(satc);
        font.draw(sb, I18n.msg("gui.minimap.saturn"), satf[2] - px(20), satf[3] - px(8));
        font.setColor(urac);
        font.draw(sb, I18n.msg("gui.minimap.uranus"), uraf[2] - px(20), uraf[3] - px(8));
        font.setColor(nepc);
        font.draw(sb, I18n.msg("gui.minimap.neptune"), nepf[2] - px(20), nepf[3] - px(8));
        font.setColor(sunc);
        font.draw(sb, I18n.msg("gui.minimap.sun"), side2 + px(5), side2 - px(5));

        sb.end();

        fb.end();

    }

    @Override
    public String getName() {
        return I18n.msg("gui.minimap.outerss");
    }
}
