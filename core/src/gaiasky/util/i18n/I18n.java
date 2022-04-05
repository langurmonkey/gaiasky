/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.i18n;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;

import java.nio.file.Path;
import java.util.Locale;
import java.util.MissingResourceException;

/**
 * Manages the i18n (internationalization) system
 */
public class I18n {
    private static final Log logger = Logger.getLogger(I18n.class);

    public static I18NBundle bundle;
    public static Locale locale;

    /**
     * Initialises the i18n system
     */
    public static void initialize() {
        if (bundle == null) {
            forceInit(Gdx.files.internal("i18n/gsbundle"));
        }
    }

    public static void initialize(Path path) {
        forceInit(Gdx.files.absolute(path.toAbsolutePath().toString()));
    }

    public static void initialize(FileHandle fh) {
        forceInit(fh);
    }

    public static boolean forceInit(FileHandle baseFileHandle) {
        if (Settings.settings.program == null || Settings.settings.program.locale == null || Settings.settings.program.locale.isEmpty()) {
            // Use system default
            locale = Locale.getDefault();
        } else {
            locale = getLocaleFromLanguageTag(Settings.settings.program.locale);
            // Set as default locale
            Locale.setDefault(locale);
        }
        try {
            bundle = I18NBundle.createBundle(baseFileHandle, locale, "UTF-8");
            return true;
        } catch (MissingResourceException e) {
            logger.info(e.getLocalizedMessage());
            // Use default locale - en_GB
            locale = new Locale("en", "GB");
            try {
                bundle = I18NBundle.createBundle(baseFileHandle, locale, "UTF-8");
            } catch (Exception e2) {
                logger.error(e);
            }
            return false;
        }

    }

    public static Locale getLocaleFromLanguageTag(String languageTag) {
        String[] tags = languageTag.split("-");
        if (tags.length > 1) {
            return new Locale(tags[0], tags[1]);
        } else {
            return new Locale(languageTag);
        }
    }

    public static synchronized String txt(String key){
        return bundle.get(key);
    }

    public static synchronized String txt(String key, Object... params) {
        return I18n.bundle.format(key, params);
    }

    public static synchronized boolean hasKey(String key) {
        try {
            I18n.bundle.get(key);
            return true;
        }catch(MissingResourceException e) {
            // Void
        }
        return false;
    }

}
