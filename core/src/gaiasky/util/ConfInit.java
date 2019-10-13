/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util;

import java.io.File;

public abstract class ConfInit {

    public static ConfInit instance;
    /**
     * Used to emulate webgl in desktop. Should be set true by the WebGL
     * ConfInits
     **/
    public boolean webgl = false;

    public static void initialize(ConfInit instance) throws Exception {
        ConfInit.instance = instance;
        instance.initGlobalConf();
    }

    public abstract void initGlobalConf() throws Exception;

    public abstract void persistGlobalConf(File propsFile);

    public abstract void initialiseProperties(File confFile);

}
