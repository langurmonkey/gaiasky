/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import gaiasky.scenegraph.ISceneGraph;

/**
 * An abstract renderer.
 * @author Toni Sagrista
 *
 */
public abstract class AbstractRenderer {

    protected static ISceneGraph sg;

    public static void initialize(ISceneGraph sg) {
        AbstractRenderer.sg = sg;
    }

    public AbstractRenderer() {
        super();
    }

}
