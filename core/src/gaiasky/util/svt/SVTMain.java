/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.svt;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import gaiasky.gui.ConsoleLogger;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SettingsManager;
import gaiasky.util.i18n.I18n;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class SVTMain {
    private static Logger.Log logger;

    public static void main(String[] args) {
        try {
            // Assets location.
            String ASSETS_LOC = Settings.ASSETS_LOC + "/";
            I18n.locale = Locale.getDefault();
            Gdx.files = new Lwjgl3Files();

            // Initialize configuration.
            File dummyVersion = new File(ASSETS_LOC + "data/dummyversion");
            if (!dummyVersion.exists()) {
                dummyVersion = new File(ASSETS_LOC + "dummyversion");
            }
            SettingsManager.initialize(new FileInputStream(ASSETS_LOC + "/conf/config.yaml"), new FileInputStream(dummyVersion));

            // Initialize i18n.
            I18n.initialize(new FileHandle(ASSETS_LOC + "i18n/gsbundle"), new FileHandle(ASSETS_LOC + "i18n/objects"));

            // Add notification watch.
            new ConsoleLogger();

            logger = Logger.getLogger(SVTQuadtreeBuilder.class);
        } catch (Exception e) {
            System.err.println("Initialization error");
            e.printStackTrace(System.err);
            return;
        }

        var loc = Paths.get("/media/tsagrista/Daten/Gaia/gaiasky/data/virtualtex-earth-diffuse/tex/");
        var builder = new SVTQuadtreeBuilder();
        var tree = builder.build("test", loc, 512);
        int maxResolution = (int) (tree.tileSize * Math.pow(2, tree.depth));
        logger.info("SVT initialized with " + tree.root.length + " roots, " + tree.numTiles + " tiles (" + tree.tileSize + "x" + tree.tileSize + "), depth " + tree.depth + " and maximum resolution of " + (maxResolution * tree.root.length) + "x" + maxResolution);

        // Test.
        test(tree, 0, 0.55, 0.55);
        test(tree, 0, 0.25, 0.95);
        test(tree, 1, 0.55, 0.45);
        test(tree, 1, 0.75, 0.95);
        test(tree, 3, 0.75, 0.95);
        test(tree, 5, 0.05, 0.4);
        test(tree, 6, 0.75, 0.95);
    }

    private static void test(SVTQuadtree<Path> tree, int level, double u, double v) {
        logger.info("L" + level + " u: " + u + ", v: " + v);
        var tile = tree.getTileFromUV(level, u, v);
        logger.info(tile != null ? tile : "null");
    }
}
