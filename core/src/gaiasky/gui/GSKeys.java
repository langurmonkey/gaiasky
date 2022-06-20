/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Input.Keys;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;

import java.lang.reflect.Field;

/**
 * Contains key definitions for Gaia Sky
 */
public class GSKeys {
    private static final Log logger = Logger.getLogger(GSKeys.class);

    public static final int ANY_KEY = Keys.ANY_KEY;
    public static final int NUM_0 = Keys.NUM_0;
    public static final int NUM_1 = Keys.NUM_1;
    public static final int NUM_2 = Keys.NUM_2;
    public static final int NUM_3 = Keys.NUM_3;
    public static final int NUM_4 = Keys.NUM_4;
    public static final int NUM_5 = Keys.NUM_5;
    public static final int NUM_6 = Keys.NUM_6;
    public static final int NUM_7 = Keys.NUM_7;
    public static final int NUM_8 = Keys.NUM_8;
    public static final int NUM_9 = Keys.NUM_9;
    public static final int A = Keys.A;
    public static final int ALT_L = Keys.ALT_LEFT;
    public static final int ALT_R = Keys.ALT_RIGHT;
    public static final int APOSTROPHE = Keys.APOSTROPHE;
    public static final int AT = Keys.AT;
    public static final int B = Keys.B;
    public static final int BACK = Keys.BACK;
    public static final int BACKSLASH = Keys.BACKSLASH;
    public static final int C = Keys.C;
    public static final int CALL = Keys.CALL;
    public static final int CAMERA = Keys.CAMERA;
    public static final int CLEAR = Keys.CLEAR;
    public static final int COMMA = Keys.COMMA;
    public static final int D = Keys.D;
    public static final int DEL = Keys.DEL;
    public static final int BACKSPACE = Keys.BACKSPACE;
    public static final int FORWARD_DEL = Keys.FORWARD_DEL;
    public static final int DPAD_CENTER = Keys.DPAD_CENTER;
    public static final int DPAD_DOWN = Keys.DPAD_DOWN;
    public static final int DPAD_LEFT = Keys.DPAD_LEFT;
    public static final int DPAD_RIGHT = Keys.DPAD_RIGHT;
    public static final int DPAD_UP = Keys.DPAD_UP;
    public static final int CENTER = Keys.CENTER;
    public static final int DOWN = Keys.DOWN;
    public static final int LEFT = Keys.LEFT;
    public static final int RIGHT = Keys.RIGHT;
    public static final int UP = Keys.UP;
    public static final int E = Keys.E;
    public static final int ENDCALL = Keys.ENDCALL;
    public static final int ENTER = Keys.ENTER;
    public static final int ENVELOPE = Keys.ENVELOPE;
    public static final int EQUALS = Keys.EQUALS;
    public static final int EXPLORER = Keys.EXPLORER;
    public static final int F = Keys.F;
    public static final int FOCUS = Keys.FOCUS;
    public static final int G = Keys.G;
    public static final int GRAVE = Keys.GRAVE;
    public static final int H = Keys.H;
    public static final int HEADSETHOOK = Keys.HEADSETHOOK;
    public static final int HOME = Keys.HOME;
    public static final int I = Keys.I;
    public static final int J = Keys.J;
    public static final int K = Keys.K;
    public static final int L = Keys.L;
    public static final int L_BRACKET = Keys.LEFT_BRACKET;
    public static final int LEFT_BRACKET = Keys.LEFT_BRACKET;
    public static final int M = Keys.M;
    public static final int MEDIA_FAST_FORWARD = Keys.MEDIA_FAST_FORWARD;
    public static final int MEDIA_NEXT = Keys.MEDIA_NEXT;
    public static final int MEDIA_PLAY_PAUSE = Keys.MEDIA_PLAY_PAUSE;
    public static final int MEDIA_PREVIOUS = Keys.MEDIA_PREVIOUS;
    public static final int MEDIA_REWIND = Keys.MEDIA_REWIND;
    public static final int MEDIA_STOP = Keys.MEDIA_STOP;
    public static final int MENU = Keys.MENU;
    public static final int MINUS = Keys.MINUS;
    public static final int MUTE = Keys.MUTE;
    public static final int N = Keys.N;
    public static final int NOTIFICATION = Keys.NOTIFICATION;
    public static final int NUM = Keys.NUM;
    public static final int O = Keys.O;
    public static final int P = Keys.P;
    public static final int PERIOD = Keys.PERIOD;
    public static final int PLUS = Keys.PLUS;
    public static final int POUND = Keys.POUND;
    public static final int POWER = Keys.POWER;
    public static final int Q = Keys.Q;
    public static final int R = Keys.R;
    public static final int R_BRACKET = Keys.RIGHT_BRACKET;
    public static final int RIGHT_BRACKET = Keys.RIGHT_BRACKET;
    public static final int S = Keys.S;
    public static final int SEARCH = Keys.SEARCH;
    public static final int SEMICOLON = Keys.SEMICOLON;
    public static final int SHIFT_L = Keys.SHIFT_LEFT;
    public static final int SHIFT_R = Keys.SHIFT_RIGHT;
    public static final int SLASH = Keys.SLASH;
    public static final int SOFT_LEFT = Keys.SOFT_LEFT;
    public static final int SOFT_RIGHT = Keys.SOFT_RIGHT;
    public static final int SPACE = Keys.SPACE;
    public static final int STAR = Keys.STAR;
    public static final int SYM = Keys.SYM;
    public static final int T = Keys.T;
    public static final int TAB = Keys.TAB;
    public static final int U = Keys.U;
    public static final int V = Keys.V;
    public static final int VOLUME_DOWN = Keys.VOLUME_DOWN;
    public static final int VOLUME_UP = Keys.VOLUME_UP;
    public static final int W = Keys.W;
    public static final int X = Keys.X;
    public static final int Y = Keys.Y;
    public static final int Z = Keys.Z;
    public static final int META_ALT_LEFT_ON = Keys.META_ALT_LEFT_ON;
    public static final int META_ALT_ON = Keys.META_ALT_ON;
    public static final int META_ALT_RIGHT_ON = Keys.META_ALT_RIGHT_ON;
    public static final int META_SHIFT_LEFT_ON = Keys.META_SHIFT_LEFT_ON;
    public static final int META_SHIFT_ON = Keys.META_SHIFT_ON;
    public static final int META_SHIFT_RIGHT_ON = Keys.META_SHIFT_RIGHT_ON;
    public static final int META_SYM_ON = Keys.META_SYM_ON;
    public static final int CTRL_L = Keys.CONTROL_LEFT;
    public static final int CTRL_R = Keys.CONTROL_RIGHT;
    public static final int ESC = Keys.ESCAPE;
    public static final int END = Keys.END;
    public static final int INSERT = Keys.INSERT;
    public static final int PAGE_UP = Keys.PAGE_UP;
    public static final int PAGE_DOWN = Keys.PAGE_DOWN;
    public static final int PICTSYMBOLS = Keys.PICTSYMBOLS;
    public static final int SWITCH_CHARSET = Keys.SWITCH_CHARSET;
    public static final int CAPS_LOCK = Keys.CAPS_LOCK;

    public static final int NUMPAD_0 = Keys.NUMPAD_0;
    public static final int NUMPAD_1 = Keys.NUMPAD_1;
    public static final int NUMPAD_2 = Keys.NUMPAD_2;
    public static final int NUMPAD_3 = Keys.NUMPAD_3;
    public static final int NUMPAD_4 = Keys.NUMPAD_4;
    public static final int NUMPAD_5 = Keys.NUMPAD_5;
    public static final int NUMPAD_6 = Keys.NUMPAD_6;
    public static final int NUMPAD_7 = Keys.NUMPAD_7;
    public static final int NUMPAD_8 = Keys.NUMPAD_8;
    public static final int NUMPAD_9 = Keys.NUMPAD_9;

    public static final int COLON = Keys.COLON;
    public static final int F1 = Keys.F1;
    public static final int F2 = Keys.F2;
    public static final int F3 = Keys.F3;
    public static final int F4 = Keys.F4;
    public static final int F5 = Keys.F5;
    public static final int F6 = Keys.F6;
    public static final int F7 = Keys.F7;
    public static final int F8 = Keys.F8;
    public static final int F9 = Keys.F9;
    public static final int F10 = Keys.F10;
    public static final int F11 = Keys.F11;
    public static final int F12 = Keys.F12;

    public static int valueOf(String name) {
        int code = ANY_KEY;

        try {
            Field f = GSKeys.class.getField(name.toUpperCase());
            code = f.getInt(null);
        } catch (Exception e) {
            logger.error(e, "Error getting value of field GSKeys." + name);
        }

        return code;
    }

    /**
     * Intercepts the toString() method in {@link Keys} and injects our
     * own I18n.
     * @param keycode The key code.
     * @return The string representation, internationalized.
     */
    public static String toString(int keycode) {
        String eng = Keys.toString(keycode);
        String key = "key." + eng.replaceAll(" ", "_");
        if (I18n.hasMessage(key)) {
            return I18n.msg(key);
        } else {
            return eng;
        }
    }
}
