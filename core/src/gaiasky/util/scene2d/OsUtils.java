/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import gaiasky.gui.main.GSKeys;

public class OsUtils {
    private static final String OS = System.getProperty("os.name", "").toLowerCase();
    private static final boolean WINDOWS = OS.contains("win");
    private static final boolean MAC = OS.contains("mac");
    private static final boolean UNIX = OS.contains("nix") || OS.contains("nux") || OS.contains("aix");

    /** @return {@code true} if the current OS is Windows */
    public static boolean isWindows() {
        return WINDOWS;
    }

    /** @return {@code true} if the current OS is Mac */
    public static boolean isMac() {
        return MAC;
    }

    /** @return {@code true} if the current OS is Unix */
    public static boolean isUnix() {
        return UNIX;
    }

    /** @return {@code true} if the current OS is iOS */
    public static boolean isIos() {
        return Gdx.app.getType() == ApplicationType.iOS;
    }

    /** @return {@code true} if the current OS is Android */
    public static boolean isAndroid() {
        return Gdx.app.getType() == ApplicationType.Android;
    }

    /**
     * Returns the Android API level it's basically the same as android.os.Build.VERSION.SDK_INT
     *
     * @return the API level. Returns 0 if the current OS isn't Android
     */
    public static int getAndroidApiLevel() {
        if (isAndroid()) {
            return Gdx.app.getVersion();
        } else {
            return 0;
        }
    }

    /**
     * Creates platform dependent shortcut text. Converts int keycodes to String text. Eg. Keys.CONTROL_LEFT,
     * Keys.SHIFT_LEFT, Keys.F5 will be converted to Ctrl+Shift+F5 on Windows and Linux, and to ⌘⇧F5 on Mac.
     * <p>
     * CONTROL_LEFT and CONTROL_RIGHT and SYM are mapped to Ctrl. The same goes for Alt (ALT_LEFT, ALT_RIGHT) and Shift (SHIFT_LEFT, SHIFT_RIGHT).
     * <p>
     * Keycodes equal to {@link Integer#MIN_VALUE} will be ignored.
     *
     * @param keycodes keycodes from {@link Keys} that are used to create shortcut text
     *
     * @return the platform dependent shortcut text
     */
    public static String getShortcutFor(int... keycodes) {
        StringBuilder builder = new StringBuilder();

        String separatorString = "+";
        String ctrlKey = "Ctrl";
        String altKey = "Alt";
        String shiftKey = "Shift";

        if (OsUtils.isMac()) {
            separatorString = "";
            ctrlKey = "\u2318";
            altKey = "\u2325";
            shiftKey = "\u21E7";
        }

        for (int i = 0; i < keycodes.length; i++) {
            if (keycodes[i] == Integer.MIN_VALUE) {
                continue;
            }

            if (keycodes[i] == Keys.CONTROL_LEFT || keycodes[i] == Keys.CONTROL_RIGHT || keycodes[i] == Keys.SYM) {
                builder.append(ctrlKey);
            } else if (keycodes[i] == Keys.SHIFT_LEFT || keycodes[i] == Keys.SHIFT_RIGHT) {
                builder.append(shiftKey);
            } else if (keycodes[i] == Keys.ALT_LEFT || keycodes[i] == Keys.ALT_RIGHT) {
                builder.append(altKey);
            } else {
                builder.append(GSKeys.toString(keycodes[i]));
            }

            if (i < keycodes.length - 1) { // Is this NOT the last key
                builder.append(separatorString);
            }
        }

        return builder.toString();
    }

}
