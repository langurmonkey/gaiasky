/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;

/**
 * Interface to implement by all entities that are to be rendered as lines whose
 * points are floated by the camera position in the CPU.
 *
 * @author Toni Sagrista
 */
public interface ILineRenderable extends IRenderable {

    float getLineWidth();

    void render(LineRenderSystem renderer, ICamera camera, float alpha);

    int getGlPrimitive();

}
