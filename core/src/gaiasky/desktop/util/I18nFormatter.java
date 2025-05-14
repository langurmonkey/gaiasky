/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.properties.CommentedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Formats translation I18n files with missing keys from the model file (typically English).
 */
public class I18nFormatter {

    private static final Logger log = LoggerFactory.getLogger(I18nFormatter.class);

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage:");
            System.out.println("    I18nFormatter [REFERENCE_I18N] [OTHER_I18N]");
            System.out.println("Example:");
            System.out.println("    I18nFormatter gsbundle.properties gsbundle_ca.properties");
            return;
        }

        // Assets location
        String ASSETS_LOC = Settings.ASSETS_LOC;
        I18n.locale = Locale.getDefault();
        Gdx.files = new Lwjgl3Files();
        Path i18nDir = Path.of(ASSETS_LOC, "i18n");

        Path p0 = i18nDir.resolve(args[0]);
        Path p1 = i18nDir.resolve(args[1]);
        File f0 = p0.toFile();
        File f1 = p1.toFile();

        if (checkFile(f0) || checkFile(f1)) {
            return;
        }

        try {
            FileInputStream fis0 = new FileInputStream(f0);
            InputStreamReader isr0 = new InputStreamReader(fis0, StandardCharsets.UTF_8);
            CommentedProperties props0 = new CommentedProperties();
            props0.load(isr0);

            FileInputStream fis1 = new FileInputStream(f1);
            InputStreamReader isr1 = new InputStreamReader(fis1, StandardCharsets.UTF_8);
            Properties props1 = new Properties();
            props1.load(isr1);

            isr0.close();
            isr1.close();

            // Output properties, same as p0
            CommentedProperties outputProperties = props0.clone();

            Map<String, String> missing = new HashMap<>();
            Set<Object> keys = props0.keySet();
            for (Object key : keys) {
                var originalVal = props0.getProperty((String) key);
                var val = props1.getProperty((String) key);
                if (val != null && !val.equals(originalVal)) {
                    // Substitute value
                    outputProperties.setProperty((String) key, val);
                } else {
                    // Use default (English), commented
                    log.error("Property not found: " + key);
                    missing.put((String) key, TextUtils.escape(props0.getProperty((String) key)));
                }
            }

            // Store result
            //File outFile = new File(args[1].substring(0, args[1].contains(".") ? args[1].lastIndexOf(".") : args[1].length()) + ".mod.properties");
            File outFile = f1;
            Files.deleteIfExists(outFile.toPath());

            FileOutputStream fos1 = new FileOutputStream(outFile, true);
            PrintStream ps = new PrintStream(fos1, true, StandardCharsets.UTF_8);
            // Store with BOM
            outputProperties.store(ps, "\uFEFF", "UTF-8", missing);
            ps.close();

            log.info("File written to " + outFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Error: ", e);
        }
    }

    private static boolean checkFile(File f) {
        if (!f.exists()) {
            log.error("File does not exist: " + f);
            return true;
        }
        if (!f.isFile()) {
            log.error("Not a file: " + f);
            return true;
        }
        if (!f.canRead()) {
            log.error("Can not read: " + f);
            return true;
        }
        return false;
    }
}
