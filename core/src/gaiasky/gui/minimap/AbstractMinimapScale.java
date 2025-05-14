/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.minimap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import gaiasky.GaiaSky;
import gaiasky.gui.api.IMinimapScale;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector2D;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.math.Vector3D;

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

    protected Vector3D aux3d1, aux3d2;
    protected Vector3Q aux3b1, aux3b2;
    protected Vector2D aux2d1, aux2d2;

    protected Matrix4D trans;

    protected Color sunc, camc, textbc, textrc, textgc, textyc, textcc, textmc;
    protected float[] camp, camd;

    protected float suns;

    protected ICamera cam;

    protected AbstractMinimapScale() {
        aux3d1 = new Vector3D();
        aux3d2 = new Vector3D();
        aux3b1 = new Vector3Q();
        aux3b2 = new Vector3Q();
        aux2d1 = new Vector2D();
        aux2d2 = new Vector2D();
    }

    protected void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort, double to, double from, double extentUp, double extentDown) {
        this.cam = GaiaSky.instance.cameraManager;
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

        this.suns = 5f;
        this.camc = ColorUtils.gRedC;
        this.camp = new float[4];
        this.camd = new float[4];
        float h = 1f;
        float l = 0.7f;
        this.textbc = new Color(l, l, h, h);
        this.textrc = new Color(h, l, l, h);
        this.textgc = new Color(l, h, l, h);
        this.textyc = new Color(h, h, l, h);
        this.textcc = new Color(l, h, h, h);
        this.textmc = new Color(h, l, h, h);

        this.sunc = new Color(1, 1, 0, 1);

        sb.enableBlending();
        sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void update() {
        // Update camera position
        position(cam.getPos().tov3d(aux3d1), camp);
        // Update camera direction
        direction(cam.getDirection(), camd);
        // Local
        updateLocal();
    }

    protected abstract void updateLocal();

    public float[] position(Vector3D pos, float[] out) {
        Vector3D p = aux3d1.set(pos).mul(trans);
        Vector2D pos2d = aux2d1;

        // Side
        pos2d.set(p.z, p.y).scl(from);
        float cx = u2Px(pos2d.x, side2);
        float cy = u2Px(pos2d.y, sideshort2);
        out[0] = cx;
        out[1] = cy;

        // Top
        pos2d.set(-p.x, p.z).scl(from);
        cx = u2Px(pos2d.x, side2);
        cy = u2Px(pos2d.y, side2);
        out[2] = cx;
        out[3] = cy;

        return out;
    }

    public float[] direction(Vector3D dir, float[] out) {
        aux3d2.set(dir).mul(trans);

        // Side
        aux2d2.set(aux3d2.z, aux3d2.y).nor().scl(px(15f));
        out[0] = (float) aux2d2.x;
        out[1] = (float) aux2d2.y;

        // Top
        aux2d2.set(-aux3d2.x, aux3d2.z).nor().scl(px(15f));
        out[2] = (float) aux2d2.x;
        out[3] = (float) aux2d2.y;

        return out;
    }

    @Override
    public boolean isActive(Vector3D campos, double distSun) {
        return distSun <= extentUp * to && distSun > extentDown * to;
    }

    protected float px(float px) {
        return px;
    }

    /**
     * Converts units to pixels, given the side/2 of
     * the end minimap
     *
     * @param units The value in whatever units
     * @param side  Side/2 of minimap
     *
     * @return Pixel coordinate
     */
    protected int u2Px(double units, float side) {
        return (int) ((units / extentUp) * side + side);
    }

    protected void renderCameraSide() {
        renderCameraSide(0.2f);
    }

    protected void renderCameraSide(float viewportAlpha) {
        // Position
        float cx = this.camp[0];
        float cy = this.camp[1];
        // Direction
        float dx = this.camd[0];
        float dy = this.camd[1];

        // Viewport
        sr.setColor(1, 1, 1, viewportAlpha);
        Vector2D endx = aux2d1.set(dx, dy).scl(40f);
        endx.rotate(-cam.getCamera().fieldOfView / 2d);
        float c1x = (float) endx.x + cx;
        float c1y = (float) endx.y + cy;
        endx.set(dx, dy).scl(40f);
        endx.rotate(cam.getCamera().fieldOfView / 2d);
        sr.triangle(cx, cy, c1x, c1y, (float) endx.x + cx, (float) endx.y + cy);

        // Position
        endx = aux2d1.set(dx, dy);
        endx.rotate(-cam.getCamera().fieldOfView / 2d);
        c1x = (float) endx.x + cx;
        c1y = (float) endx.y + cy;
        endx.set(dx, dy);
        endx.rotate(cam.getCamera().fieldOfView / 2d);
        sr.setColor(camc);
        sr.triangle(cx, cy, c1x, c1y, (float) endx.x + cx, (float) endx.y + cy);

        sr.setColor(0, 0, 0, 1);
        sr.circle(cx, cy, 8f);
        sr.setColor(camc);
        sr.circle(cx, cy, 6f);

    }

    protected void renderCameraTop() {
        renderCameraTop(0.2f);
    }

    protected void renderCameraTop(float viewportAlpha) {
        // Position
        float cx = this.camp[2];
        float cy = this.camp[3];
        // Direction
        float dx = this.camd[2];
        float dy = this.camd[3];

        // Viewport
        sr.setColor(1, 1, 1, viewportAlpha);
        Vector2D endx = aux2d1.set(dx, dy).scl(40f);
        endx.rotate(-cam.getCamera().fieldOfView / 2d);
        float c1x = (float) endx.x + cx;
        float c1y = (float) endx.y + cy;
        endx.set(dx, dy).scl(40f);
        endx.rotate(cam.getCamera().fieldOfView / 2d);
        sr.triangle(cx, cy, c1x, c1y, (float) endx.x + cx, (float) endx.y + cy);

        // Position
        endx = aux2d1.set(dx, dy);
        endx.rotate(-cam.getCamera().fieldOfView / 2d);
        c1x = (float) endx.x + cx;
        c1y = (float) endx.y + cy;
        endx.set(dx, dy);
        endx.rotate(cam.getCamera().fieldOfView / 2d);
        sr.setColor(camc);
        sr.triangle(cx, cy, c1x, c1y, (float) endx.x + cx, (float) endx.y + cy);

        sr.setColor(0, 0, 0, 1);
        sr.circle(cx, cy, 8f);
        sr.setColor(camc);
        sr.circle(cx, cy, 6f);

    }

    public void dispose() {
    }

}
