/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.graphics.TextureView;

/**
 * Contains common functions used by render systems.
 */
public class RenderUtils {

    /**
     * Assumes the aspect ratio is fine.
     *
     * @param fb The frame buffer to render.
     * @param sb The sprite batch to use.
     * @param g  The graphics instance.
     */
    public static void renderBackbuffer(FrameBuffer fb, SpriteBatch sb, Graphics g) {
        Texture tex = fb.getColorBufferTexture();
        sb.begin();
        sb.draw(tex, 0, 0, tex.getWidth(), tex.getHeight(), 0, 0, 1, 1);
        sb.end();
    }

    /**
     * Renders the given frame buffer to screen with a fill scaling, maintaining the aspect ratio.
     *
     * @param fb The frame buffer to render.
     * @param sb The sprite batch to use.
     * @param g  The graphics instance.
     */
    public static void renderKeepAspect(FrameBuffer fb, SpriteBatch sb, Graphics g) {
        renderKeepAspect(fb, sb, g, null);
    }

    /**
     * Renders the given frame buffer to screen with a fill scaling, maintaining the aspect ratio.
     *
     * @param fb       The frame buffer to render.
     * @param sb       The sprite batch to use.
     * @param g        The graphics instance.
     * @param lastSize The previous size, for recomputing the sprite batch transform.
     */
    public static void renderKeepAspect(FrameBuffer fb, SpriteBatch sb, Graphics g, Vector2 lastSize) {
        renderKeepAspect(fb.getColorBufferTexture(), sb, g, lastSize);
    }

    /**
     * Renders the given texture to screen with a fill scaling, maintaining the aspect ratio.
     *
     * @param tex      The texture to render.
     * @param sb       The sprite batch to use.
     * @param g        The graphics instance.
     * @param lastSize The previous size, for recomputing the sprite batch transform.
     */
    public static void renderKeepAspect(Texture tex, SpriteBatch sb, Graphics g, Vector2 lastSize) {
        float tw = tex.getWidth();
        float th = tex.getHeight();
        float tar = tw / th;
        float gw = (float) (g.getWidth() * Settings.settings.graphics.backBufferScale);
        float gh = (float) (g.getHeight() * Settings.settings.graphics.backBufferScale);
        float gar = gw / gh;

        if (lastSize != null && (tw != lastSize.x || th != lastSize.y)) {
            // Adapt projection matrix to new size
            Matrix4 mat = sb.getProjectionMatrix();
            mat.setToOrtho2D(0, 0, tw, th);
        }

        // Output
        float x = 0, y = 0;
        float w = tw, h = th;
        int sx = 0, sy = 0;
        int sw = (int) gw, sh = (int) gh;

        if (gw > tw && gh > th) {
            x = 0;
            y = 0;
            // Texture contained in screen, extend texture keeping ratio
            if (gar > tar) {
                // Graphics are stretched horizontally
                sw = (int) tw;
                sh = (int) (tw / gar);

            } else {
                // Graphics are stretched vertically
                sw = (int) (th * gar);
                sh = (int) th;

            }
            sx = (int) ((tw - sw) / 2f);
            sy = (int) ((th - sh) / 2f);
        } else if (tw >= gw) {
            sx = (int) ((tw - gw) / 2f);
            if (th >= gh) {
                sy = (int) ((th - gh) / 2f);
            } else {
                x = 0;
                y = 0;
                sw = (int) (th * gar);
                sh = (int) th;

                sx = (int) ((tw - sw) / 2f);
                sy = (int) ((th - sh) / 2f);
            }
        } else {
            w = gw;
            x = 0;
            y = 0;
            w = tw;
            sw = (int) tw;
            sh = (int) (tw / gar);

            sx = (int) ((tw - sw) / 2f);
            sy = (int) ((th - sh) / 2f);
        }
        sb.begin();
        sb.draw(tex, x, y, w, h, sx, sy, sw, sh, false, true);
        sb.end();

        if (lastSize != null)
            lastSize.set(tw, th);
    }
    /**
     * Renders the given texture to screen with a fill scaling, maintaining the aspect ratio.
     *
     * @param tex      The texture to render.
     * @param sb       The sprite batch to use.
     * @param g        The graphics instance.
     * @param lastSize The previous size, for recomputing the sprite batch transform.
     */
    public static void renderKeepAspect(TextureView tex, ExtSpriteBatch sb, Graphics g, Vector2 lastSize) {
        float tw = tex.getWidth();
        float th = tex.getHeight();
        float tar = tw / th;
        float gw = (float) (g.getWidth() * Settings.settings.graphics.backBufferScale);
        float gh = (float) (g.getHeight() * Settings.settings.graphics.backBufferScale);
        float gar = gw / gh;

        if (lastSize != null && (tw != lastSize.x || th != lastSize.y)) {
            // Adapt projection matrix to new size
            Matrix4 mat = sb.getProjectionMatrix();
            mat.setToOrtho2D(0, 0, tw, th);
        }

        // Output
        float x = 0, y = 0;
        float w = tw, h = th;
        int sx = 0, sy = 0;
        int sw = (int) gw, sh = (int) gh;

        if (gw > tw && gh > th) {
            x = 0;
            y = 0;
            // Texture contained in screen, extend texture keeping ratio
            if (gar > tar) {
                // Graphics are stretched horizontally
                sw = (int) tw;
                sh = (int) (tw / gar);

            } else {
                // Graphics are stretched vertically
                sw = (int) (th * gar);
                sh = (int) th;

            }
            sx = (int) ((tw - sw) / 2f);
            sy = (int) ((th - sh) / 2f);
        } else if (tw >= gw) {
            sx = (int) ((tw - gw) / 2f);
            if (th >= gh) {
                sy = (int) ((th - gh) / 2f);
            } else {
                x = 0;
                y = 0;
                sw = (int) (th * gar);
                sh = (int) th;

                sx = (int) ((tw - sw) / 2f);
                sy = (int) ((th - sh) / 2f);
            }
        } else {
            w = gw;
            x = 0;
            y = 0;
            w = tw;
            sw = (int) tw;
            sh = (int) (tw / gar);

            sx = (int) ((tw - sw) / 2f);
            sy = (int) ((th - sh) / 2f);
        }
        sb.begin();
        sb.draw(tex, x, y, w, h, sx, sy, sw, sh, false, true);
        sb.end();

        if (lastSize != null)
            lastSize.set(tw, th);
    }
}
