/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import gaiasky.render.api.IPostProcessor;

public abstract class PostProcessorFactory {
    public static PostProcessorFactory instance;

    public static void initialize(PostProcessorFactory instance) {
        PostProcessorFactory.instance = instance;
    }

    public abstract IPostProcessor getPostProcessor();
}
