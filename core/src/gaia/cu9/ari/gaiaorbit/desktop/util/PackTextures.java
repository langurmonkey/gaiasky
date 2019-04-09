/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.desktop.util;

import java.io.File;

import static com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;
import static com.badlogic.gdx.tools.texturepacker.TexturePacker.process;

public class PackTextures {
    public static void main(String[] args) throws Exception {
        Settings x1settings = new Settings();
        Settings x2settings = new Settings();
        x2settings.scale[0] = 2;

        // Use current path variable
        String gs = (new File("")).getAbsolutePath();

        // DARK-GREEN
        process(x1settings, gs + "/assets/skins/raw/dark-green/", gs + "/assets/skins/dark-green/", "dark-green");

        // DARK-GREEN x2
        process(x2settings, gs + "/assets/skins/raw/dark-green/", gs + "/assets/skins/dark-green-x2/", "dark-green-x2");

        // DARK-ORANGE
        process(x1settings, gs + "/assets/skins/raw/dark-orange/", gs + "/assets/skins/dark-orange/", "dark-orange");

        // DARK-ORANGE x2
        process(x2settings, gs + "/assets/skins/raw/dark-orange/", gs + "/assets/skins/dark-orange-x2/", "dark-orange-x2");

        // DARK-BLUE
        process(x1settings, gs + "/assets/skins/raw/dark-blue/", gs + "/assets/skins/dark-blue/", "dark-blue");

        // DARK-BLUE x2
        process(x2settings, gs + "/assets/skins/raw/dark-blue/", gs + "/assets/skins/dark-blue-x2/", "dark-blue-x2");

        // BRIGHT-GREEN
        process(x1settings, gs + "/assets/skins/raw/bright-green/", gs + "/assets/skins/bright-green/", "bright-green");

        // BRIGHT-GREEN x2
        process(x2settings, gs + "/assets/skins/raw/bright-green/", gs + "/assets/skins/bright-green-x2/", "bright-green-x2");

        // NIGHT-RED
        process(x1settings, gs + "/assets/skins/raw/night-red/", gs + "/assets/skins/night-red/", "night-red");

        // NIGHT-RED x2
        process(x2settings, gs + "/assets/skins/raw/night-red/", gs + "/assets/skins/night-red-x2/", "night-red-x2");
    }
}
