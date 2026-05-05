/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.i18n;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import gaiasky.GaiaSky;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

import java.nio.file.Path;
import java.util.Locale;
import java.util.MissingResourceException;

public class I18n {
    private static final Log logger = Logger.getLogger(I18n.class);

    public static I18NBundle messages;
    public static I18NBundle objects;
    public static Locale locale;

    /**
     * Initializes the i18n system with the main and the objects bundle.
     * The main bundle contains the application messages. The objects bundle
     * contains the object names.
     */
    public static void initialize() {
        if (messages == null || objects == null) {
            if (!forceInit(Gdx.files.internal("i18n/gsbundle"), Gdx.files.internal("i18n/objects"))) {
                logger.warn("I18n resource not found.");
            }
        }
    }

    public static void initialize(Path main, Path objects) {
        if (forceInit(Gdx.files.absolute(main.toAbsolutePath()
                                                 .toString()), Gdx.files.absolute(objects.toAbsolutePath()
                                                                                          .toString()))) {
            logger.warn("I18n resource not found.");
        }
    }

    public static void initialize(FileHandle main, FileHandle objects) {
        if (!forceInit(main, objects)) {
            logger.warn("I18n resource not found.");
        }
    }

    public static boolean forceInit(FileHandle main, FileHandle objects) {
        if (GaiaSky.instance == null
                || GaiaSky.settings() == null
                || GaiaSky.settings().program == null
                || GaiaSky.settings().program.locale == null
                || GaiaSky.settings().program.locale.isEmpty()) {
            // Use system default
            locale = Locale.getDefault();
        } else {
            locale = getLocaleFromLanguageTag(GaiaSky.settings().program.locale);
            // Set as default locale
            Locale.setDefault(locale);
        }

        boolean found;
        // Messages
        try {
            I18n.messages = I18NBundle.createBundle(main, locale, "UTF-8");
            found = true;
        } catch (MissingResourceException e) {
            logger.info(e.getLocalizedMessage());
            // Use default locale - en_GB
            locale = new Locale.Builder().setLanguage("en").setRegion("GB").build();
            try {
                I18n.messages = I18NBundle.createBundle(main, locale, "UTF-8");
            } catch (Exception e2) {
                logger.error(e);
            }
            found = false;
        }

        // Objects
        try {
            I18n.objects = I18NBundle.createBundle(objects, locale, "UTF-8");
        } catch (MissingResourceException e) {
            logger.info(e.getLocalizedMessage());
            // Use default locale - en_GB
            locale = new Locale.Builder().setLanguage("en").setRegion("GB").build();
            try {
                I18n.objects = I18NBundle.createBundle(objects, locale, "UTF-8");
            } catch (Exception e2) {
                logger.error(e);
            }
            found = false;
        }

        return found;
    }

    public static Locale getLocaleFromLanguageTag(String languageTag) {
        String[] tags = languageTag.split("-");
        if (tags.length > 1) {
            return new Locale.Builder().setLanguage(tags[0]).setRegion(tags[1]).build();
        } else {
            return new Locale.Builder().setLanguage(languageTag).build();
        }
    }

    public static boolean exists(String key) {
        return messages.contains(key);
    }

    public static String get(String key) {
        return msg(key);
    }

    public static String msg(String key) {
        return get(messages, key);
    }

    public static String msgOr(String key, String defaultValue) {
        return getOr(messages, key, defaultValue);
    }

    public static String obj(String key) {
        return get(objects, key);
    }

    public static String objOr(String key, String defaultValue) {
        return getOr(objects, key, defaultValue);
    }

    public static String msg(String key, Object... params) {
        return I18n.messages.format(key, params);
    }

    public static String obj(String key, Object... params) {
        return I18n.objects.format(key, params);
    }

    public static boolean hasMessage(String key) {
        return has(messages, key);
    }

    public static boolean hasObject(String key) {
        return has(objects, key);
    }

    public static boolean hasLocalizedVersion(String nameLowerCase) {
        var base = nameLowerCase.replace(' ', '_');
        return hasObject(base);
    }

    public static String localize(String nameLowerCase) {
        if (nameLowerCase == null) {
            return null;
        }
        var base = nameLowerCase.replace(' ', '_');
        if (hasObject(base)) {
            return obj(base);
        } else {
            return nameLowerCase;
        }
    }

    /**
     * Returns the localized version of name if it exists, null otherwise.
     * Avoids the double replace() and double hasObject() of calling
     * hasLocalizedVersion() + localize() separately.
     */
    public static String localizeIfExists(String nameLowerCase) {
        if (nameLowerCase == null) return null;
        var base = nameLowerCase.replace(' ', '_');
        if (hasObject(base)) {
            return objects.get(base);  // direct bundle get, avoids synchronized obj()
        }
        return null;
    }

    private static String get(I18NBundle b, String key) {
        return b.get(key);
    }

    private static String getOr(I18NBundle b, String key, String defaultValue) {
        if (has(b, key)) {
            return b.get(key);
        } else {
            return defaultValue;
        }
    }

    private static boolean has(I18NBundle b, String key) {
        return b.contains(key);
    }

}
