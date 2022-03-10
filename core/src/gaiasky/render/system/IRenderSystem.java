/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.render.IRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;

/**
 * A component that renders a type of objects.
 */
public interface IRenderSystem extends Disposable {

    RenderGroup getRenderGroup();

    void render(Array<IRenderable> renderables, ICamera camera, double t, RenderingContext rc);

    void resize(int w, int h);

    void updateBatchSize(int w, int h);

}
