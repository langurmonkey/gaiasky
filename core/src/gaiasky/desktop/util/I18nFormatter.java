/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.properties.CommentedProperties;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * Format i18n bundle files according to a reference file.
 */
public class I18nFormatter {

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
            CommentedProperties op = props0.clone();

            Set<Object> keys = props0.keySet();
            for (Object key : keys) {
                boolean has = props1.getProperty((String) key) != null;
                if (has) {
                    // Substitute value
                    String val = props1.getProperty((String) key);
                    op.setProperty((String) key, val);
                } else {
                    System.err.println("Property not found: " + key);
                    op.remove(key);
                }
            }

            // Store result
            File outFile = new File(args[1].substring(0, args[1].contains(".") ? args[1].lastIndexOf(".") : args[1].length()) + ".mod.properties"); //-V6009
            Files.deleteIfExists(outFile.toPath());

            FileOutputStream fos1 = new FileOutputStream(outFile, true);
            PrintStream ps = new PrintStream(fos1, true, StandardCharsets.UTF_8);
            // Store with BOM
            op.store(ps, "\uFEFF");
            ps.close();

            System.out.println("File written to " + outFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean checkFile(File f) {
        if (!f.exists()) {
            System.err.println("File does not exist: " + f);
            return true;
        }
        if (!f.isFile()) {
            System.err.println("Not a file: " + f);
            return true;
        }
        if (!f.canRead()) {
            System.err.println("Can not read: " + f);
            return true;
        }
        return false;
    }
}
