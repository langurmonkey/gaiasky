/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import gaiasky.util.math.Vector3d;

public interface IMinimapScale {
    boolean isActive(Vector3d campos, double distanceFromSun);

    void update();

    void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort);

    void renderSideProjection(FrameBuffer fb);

    void renderTopProjection(FrameBuffer fb);

    String getName();

    void dispose();
}
