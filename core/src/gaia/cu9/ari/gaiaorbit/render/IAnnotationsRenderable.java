/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;

public interface IAnnotationsRenderable extends IRenderable {

    void render(SpriteBatch spriteBatch, ICamera camera, float alpha);
}
