/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.api;

import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;

public interface IAnnotationsRenderable extends IRenderable {

    void render(ExtSpriteBatch spriteBatch, ICamera camera, BitmapFont font, float alpha);
}
