/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce.minimap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import gaiasky.interfce.IMinimapScale;
import gaiasky.util.GlobalConf;
import gaiasky.util.color.ColourUtils;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3d;

public abstract class AbstractMinimapScale implements IMinimapScale {
    protected OrthographicCamera ortho;
    protected SpriteBatch sb;
    protected ShapeRenderer sr;
    protected BitmapFont font;
    protected int side, sideshort, side2, sideshort2;

    /** Extent, in whatever units, of the minimap - where the edge is **/
    protected double extentUp, extentDown;
    /** Conversions to and from internal units **/
    protected double to, from;

    protected Vector3d aux3d1, aux3d2;
    protected Vector2d aux2d1, aux2d2;

    protected Matrix4d trans;

    protected Color camc, textc;

    public AbstractMinimapScale(){
        aux3d1 = new Vector3d();
        aux3d2 = new Vector3d();
        aux2d1 = new Vector2d();
        aux2d2 = new Vector2d();
    }

    protected void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort, double to, double from, double extentUp, double extentDown) {
        this.ortho = ortho;
        this.sb = sb;
        this.sr = sr;
        this.font = font;
        this.side = side;
        this.side2 = side / 2;
        this.sideshort = sideshort;
        this.sideshort2 = sideshort / 2;
        this.to = to;
        this.from = from;
        this.extentUp = extentUp;
        this.extentDown = extentDown;

        this.camc = ColourUtils.gRedC;
        this.textc = new Color(.6f, .6f, .9f, 1f);
    }

    public float[] project(Vector3d pos, float[] out) {
        Vector3d p = aux3d1.set(pos).mul(trans);
        Vector2d pos2d = aux2d1;
        pos2d.set(p.z, p.y).scl(from);
        float cx = u2Px(pos2d.x, side2);
        float cy = u2Px(pos2d.y, sideshort2);
        out[0] = cx;
        out[1] = cy;

        pos2d.set(p.x, p.z).scl(from);
        cx = u2Px(pos2d.x, side2);
        cy = u2Px(pos2d.y, side2);
        out[2] = cx;
        out[3] = cy;
        return out;
    }

    @Override
    public boolean isActive(Vector3d campos, double distSun) {
        return distSun <= extentUp * to && distSun > extentDown * to;
    }

    protected float px(float px) {
        return px * GlobalConf.UI_SCALE_FACTOR;
    }

    /**
     * Converts units to pixels, given the side/2 of
     * the end minimap
     *
     * @param units The value in whatever units
     * @param side  Side/2 of minimap
     * @return Pixel coordinate
     */
    protected int u2Px(double units, float side) {
        return (int) ((units / extentUp) * side + side);
    }
}
