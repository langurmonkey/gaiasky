/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import java.io.File;

public abstract class ConfInit {

    public static ConfInit instance;

    public static void initialize(final ConfInit instance) throws Exception {
        ConfInit.instance = instance;
        instance.initGlobalConf();
    }

    public abstract void initGlobalConf() throws Exception;

    public abstract void persistGlobalConf(final File propsFile);

}
