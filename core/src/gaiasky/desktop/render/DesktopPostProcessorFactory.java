/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.render;

import gaiasky.render.IPostProcessor;
import gaiasky.render.PostProcessorFactory;

public class DesktopPostProcessorFactory extends PostProcessorFactory {
	DesktopPostProcessor instance = null;

    @Override
    public IPostProcessor getPostProcessor() {
    	if(instance == null){
    		instance = new DesktopPostProcessor();
    	}
        return instance;
    }

}
