package gaia.cu9.ari.gaiaorbit.desktop.util;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class PackTextures {
    public static void main(String[] args) throws Exception {
        TexturePacker.Settings x1settings = new TexturePacker.Settings();
        TexturePacker.Settings x2settings = new TexturePacker.Settings();
        x2settings.scale[0] = 2;

        // DARK-GREEN
        TexturePacker.process(x1settings, "/home/tsagrista/git/gaiasky/assets/skins/raw/dark-green/", "/home/tsagrista/git/gaiasky/assets/skins/dark-green/", "dark-green");

        // DARK-GREEN x2
        TexturePacker.process(x2settings, "/home/tsagrista/git/gaiasky/assets/skins/raw/dark-green/", "/home/tsagrista/git/gaiasky/assets/skins/dark-green-x2/", "dark-green-x2");

        // DARK-ORANGE
        TexturePacker.process(x1settings, "/home/tsagrista/git/gaiasky/assets/skins/raw/dark-orange/", "/home/tsagrista/git/gaiasky/assets/skins/dark-orange/", "dark-orange");

        // DARK-ORANGE x2
        TexturePacker.process(x2settings, "/home/tsagrista/git/gaiasky/assets/skins/raw/dark-orange/", "/home/tsagrista/git/gaiasky/assets/skins/dark-orange-x2/", "dark-orange-x2");

        // DARK-BLUE
        TexturePacker.process(x1settings, "/home/tsagrista/git/gaiasky/assets/skins/raw/dark-blue/", "/home/tsagrista/git/gaiasky/assets/skins/dark-blue/", "dark-blue");

        // DARK-BLUE x2
        TexturePacker.process(x2settings, "/home/tsagrista/git/gaiasky/assets/skins/raw/dark-blue/", "/home/tsagrista/git/gaiasky/assets/skins/dark-blue-x2/", "dark-blue-x2");

        // BRIGHT-GREEN
        TexturePacker.process(x1settings, "/home/tsagrista/git/gaiasky/assets/skins/raw/bright-green/", "/home/tsagrista/git/gaiasky/assets/skins/bright-green/", "bright-green");

        // BRIGHT-GREEN x2
        TexturePacker.process(x2settings, "/home/tsagrista/git/gaiasky/assets/skins/raw/bright-green/", "/home/tsagrista/git/gaiasky/assets/skins/bright-green-x2/", "bright-green-x2");

        // NIGHT-RED
        TexturePacker.process(x1settings, "/home/tsagrista/git/gaiasky/assets/skins/raw/night-red/", "/home/tsagrista/git/gaiasky/assets/skins/night-red/", "night-red");

        // NIGHT-RED x2
        TexturePacker.process(x2settings, "/home/tsagrista/git/gaiasky/assets/skins/raw/night-red/", "/home/tsagrista/git/gaiasky/assets/skins/night-red-x2/", "night-red-x2");
    }
}
