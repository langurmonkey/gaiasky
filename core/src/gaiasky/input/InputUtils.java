/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.gdx.Input.Keys;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

import static com.badlogic.gdx.Input.Keys.NUM_4;


/**
 * Utilities for the input system.
 */
public class InputUtils {

    public static int physicalToLogicalKeyCode(final int physicalKeyCode) {
        var str = InputUtils.physicalKeyCodeToLogicalKeyString(physicalKeyCode);
        return str != null ? InputUtils.keyStringToKeyCode(str) : -1;
    }

    /**
     * Converts GDX physical key codes to GLFW logical strings.
     *
     * @param keyCode The physical key code.
     * @return The string representing the logical key.
     */
    public static String physicalKeyCodeToLogicalKeyString(final int keyCode) {
        String keyName = GLFW.glfwGetKeyName(getGlfwKeyCode(keyCode), 0);
        if (keyName != null && !keyName.isEmpty()) {
            return keyName.toUpperCase(Locale.ROOT);
        }
        return Keys.toString(keyCode);
    }

    /**
     * Returns the GLFW key code corresponding to a GDX key code.
     *
     * @param gdxKeyCode The GDX key code.
     * @return The GLFW key code.
     */
    public static int getGlfwKeyCode(int gdxKeyCode) {
        return switch (gdxKeyCode) {
            case Keys.SPACE -> GLFW.GLFW_KEY_SPACE;
            case Keys.APOSTROPHE -> GLFW.GLFW_KEY_APOSTROPHE;
            case Keys.COMMA -> GLFW.GLFW_KEY_COMMA;
            case Keys.MINUS -> GLFW.GLFW_KEY_MINUS;
            case Keys.PERIOD -> GLFW.GLFW_KEY_PERIOD;
            case Keys.SLASH -> GLFW.GLFW_KEY_SLASH;
            case Keys.NUM_0 -> GLFW.GLFW_KEY_0;
            case Keys.NUM_1 -> GLFW.GLFW_KEY_1;
            case Keys.NUM_2 -> GLFW.GLFW_KEY_2;
            case Keys.NUM_3 -> GLFW.GLFW_KEY_3;
            case NUM_4 -> GLFW.GLFW_KEY_4;
            case Keys.NUM_5 -> GLFW.GLFW_KEY_5;
            case Keys.NUM_6 -> GLFW.GLFW_KEY_6;
            case Keys.NUM_7 -> GLFW.GLFW_KEY_7;
            case Keys.NUM_8 -> GLFW.GLFW_KEY_8;
            case Keys.NUM_9 -> GLFW.GLFW_KEY_9;
            case Keys.SEMICOLON -> GLFW.GLFW_KEY_SEMICOLON;
            case Keys.EQUALS -> GLFW.GLFW_KEY_EQUAL;
            case Keys.A -> GLFW.GLFW_KEY_A;
            case Keys.B -> GLFW.GLFW_KEY_B;
            case Keys.C -> GLFW.GLFW_KEY_C;
            case Keys.D -> GLFW.GLFW_KEY_D;
            case Keys.E -> GLFW.GLFW_KEY_E;
            case Keys.F -> GLFW.GLFW_KEY_F;
            case Keys.G -> GLFW.GLFW_KEY_G;
            case Keys.H -> GLFW.GLFW_KEY_H;
            case Keys.I -> GLFW.GLFW_KEY_I;
            case Keys.J -> GLFW.GLFW_KEY_J;
            case Keys.K -> GLFW.GLFW_KEY_K;
            case Keys.L -> GLFW.GLFW_KEY_L;
            case Keys.M -> GLFW.GLFW_KEY_M;
            case Keys.N -> GLFW.GLFW_KEY_N;
            case Keys.O -> GLFW.GLFW_KEY_O;
            case Keys.P -> GLFW.GLFW_KEY_P;
            case Keys.Q -> GLFW.GLFW_KEY_Q;
            case Keys.R -> GLFW.GLFW_KEY_R;
            case Keys.S -> GLFW.GLFW_KEY_S;
            case Keys.T -> GLFW.GLFW_KEY_T;
            case Keys.U -> GLFW.GLFW_KEY_U;
            case Keys.V -> GLFW.GLFW_KEY_V;
            case Keys.W -> GLFW.GLFW_KEY_W;
            case Keys.X -> GLFW.GLFW_KEY_X;
            case Keys.Y -> GLFW.GLFW_KEY_Y;
            case Keys.Z -> GLFW.GLFW_KEY_Z;
            case Keys.LEFT_BRACKET -> GLFW.GLFW_KEY_LEFT_BRACKET;
            case Keys.BACKSLASH -> GLFW.GLFW_KEY_BACKSLASH;
            case Keys.RIGHT_BRACKET -> GLFW.GLFW_KEY_RIGHT_BRACKET;
            case Keys.GRAVE -> GLFW.GLFW_KEY_GRAVE_ACCENT;
            case Keys.ESCAPE -> GLFW.GLFW_KEY_ESCAPE;
            case Keys.ENTER -> GLFW.GLFW_KEY_ENTER;
            case Keys.TAB -> GLFW.GLFW_KEY_TAB;
            case Keys.BACKSPACE -> GLFW.GLFW_KEY_BACKSPACE;
            case Keys.INSERT -> GLFW.GLFW_KEY_INSERT;
            case Keys.FORWARD_DEL -> GLFW.GLFW_KEY_DELETE;
            case Keys.RIGHT -> GLFW.GLFW_KEY_RIGHT;
            case Keys.LEFT -> GLFW.GLFW_KEY_LEFT;
            case Keys.DOWN -> GLFW.GLFW_KEY_DOWN;
            case Keys.UP -> GLFW.GLFW_KEY_UP;
            case Keys.PAGE_UP -> GLFW.GLFW_KEY_PAGE_UP;
            case Keys.PAGE_DOWN -> GLFW.GLFW_KEY_PAGE_DOWN;
            case Keys.HOME -> GLFW.GLFW_KEY_HOME;
            case Keys.END -> GLFW.GLFW_KEY_END;
            case Keys.CAPS_LOCK -> GLFW.GLFW_KEY_CAPS_LOCK;
            case Keys.SCROLL_LOCK -> GLFW.GLFW_KEY_SCROLL_LOCK;
            case Keys.PRINT_SCREEN -> GLFW.GLFW_KEY_PRINT_SCREEN;
            case Keys.PAUSE -> GLFW.GLFW_KEY_PAUSE;
            case Keys.F1 -> GLFW.GLFW_KEY_F1;
            case Keys.F2 -> GLFW.GLFW_KEY_F2;
            case Keys.F3 -> GLFW.GLFW_KEY_F3;
            case Keys.F4 -> GLFW.GLFW_KEY_F4;
            case Keys.F5 -> GLFW.GLFW_KEY_F5;
            case Keys.F6 -> GLFW.GLFW_KEY_F6;
            case Keys.F7 -> GLFW.GLFW_KEY_F7;
            case Keys.F8 -> GLFW.GLFW_KEY_F8;
            case Keys.F9 -> GLFW.GLFW_KEY_F9;
            case Keys.F10 -> GLFW.GLFW_KEY_F10;
            case Keys.F11 -> GLFW.GLFW_KEY_F11;
            case Keys.F12 -> GLFW.GLFW_KEY_F12;
            case Keys.F13 -> GLFW.GLFW_KEY_F13;
            case Keys.F14 -> GLFW.GLFW_KEY_F14;
            case Keys.F15 -> GLFW.GLFW_KEY_F15;
            case Keys.F16 -> GLFW.GLFW_KEY_F16;
            case Keys.F17 -> GLFW.GLFW_KEY_F17;
            case Keys.F18 -> GLFW.GLFW_KEY_F18;
            case Keys.F19 -> GLFW.GLFW_KEY_F19;
            case Keys.F20 -> GLFW.GLFW_KEY_F20;
            case Keys.F21 -> GLFW.GLFW_KEY_F21;
            case Keys.F22 -> GLFW.GLFW_KEY_F22;
            case Keys.F23 -> GLFW.GLFW_KEY_F23;
            case Keys.F24 -> GLFW.GLFW_KEY_F24;
            case Keys.NUM_LOCK -> GLFW.GLFW_KEY_NUM_LOCK;
            case Keys.NUMPAD_0 -> GLFW.GLFW_KEY_KP_0;
            case Keys.NUMPAD_1 -> GLFW.GLFW_KEY_KP_1;
            case Keys.NUMPAD_2 -> GLFW.GLFW_KEY_KP_2;
            case Keys.NUMPAD_3 -> GLFW.GLFW_KEY_KP_3;
            case Keys.NUMPAD_4 -> GLFW.GLFW_KEY_KP_4;
            case Keys.NUMPAD_5 -> GLFW.GLFW_KEY_KP_5;
            case Keys.NUMPAD_6 -> GLFW.GLFW_KEY_KP_6;
            case Keys.NUMPAD_7 -> GLFW.GLFW_KEY_KP_7;
            case Keys.NUMPAD_8 -> GLFW.GLFW_KEY_KP_8;
            case Keys.NUMPAD_9 -> GLFW.GLFW_KEY_KP_9;
            case Keys.NUMPAD_DOT -> GLFW.GLFW_KEY_KP_DECIMAL;
            case Keys.NUMPAD_DIVIDE -> GLFW.GLFW_KEY_KP_DIVIDE;
            case Keys.NUMPAD_MULTIPLY -> GLFW.GLFW_KEY_KP_MULTIPLY;
            case Keys.NUMPAD_SUBTRACT -> GLFW.GLFW_KEY_KP_SUBTRACT;
            case Keys.NUMPAD_ADD -> GLFW.GLFW_KEY_KP_ADD;
            case Keys.NUMPAD_ENTER -> GLFW.GLFW_KEY_KP_ENTER;
            case Keys.NUMPAD_EQUALS -> GLFW.GLFW_KEY_KP_EQUAL;
            case Keys.SHIFT_LEFT -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case Keys.CONTROL_LEFT -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case Keys.ALT_LEFT -> GLFW.GLFW_KEY_LEFT_ALT;
            case Keys.SYM -> GLFW.GLFW_KEY_LEFT_SUPER;
            case Keys.SHIFT_RIGHT -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case Keys.CONTROL_RIGHT -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case Keys.ALT_RIGHT -> GLFW.GLFW_KEY_RIGHT_ALT;
            case Keys.MENU -> GLFW.GLFW_KEY_MENU;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }

    private static int[] keys(int... keys) {
        return keys;
    }

    /**
     * Converts string representations of keys into key codes (see {@link Keys}).
     *
     * @param keyString The string representation.
     * @return The key code.
     */
    public static int keyStringToKeyCode(final String keyString) {
        return switch (keyString) {
            // META* variables should not be used with this method.
            case "0" -> Keys.NUM_0;
            case "1" -> Keys.NUM_1;
            case "2" -> Keys.NUM_2;
            case "3" -> Keys.NUM_3;
            case "4" -> Keys.NUM_4;
            case "5" -> Keys.NUM_5;
            case "6" -> Keys.NUM_6;
            case "7" -> Keys.NUM_7;
            case "8" -> Keys.NUM_8;
            case "9" -> Keys.NUM_9;
            case "@" -> Keys.AT;
            case "A" -> Keys.A;
            case "B" -> Keys.B;
            case "C" -> Keys.C;
            case "D" -> Keys.D;
            case "E" -> Keys.E;
            case "F" -> Keys.F;
            case "G" -> Keys.G;
            case "H" -> Keys.H;
            case "I" -> Keys.I;
            case "J" -> Keys.J;
            case "K" -> Keys.K;
            case "L" -> Keys.L;
            case "M" -> Keys.M;
            case "N" -> Keys.N;
            case "O" -> Keys.O;
            case "P" -> Keys.P;
            case "Q" -> Keys.Q;
            case "R" -> Keys.R;
            case "S" -> Keys.S;
            case "T" -> Keys.T;
            case "U" -> Keys.U;
            case "V" -> Keys.V;
            case "W" -> Keys.W;
            case "X" -> Keys.X;
            case "Y" -> Keys.Y;
            case "Z" -> Keys.Z;
            case "," -> Keys.COMMA;
            case "." -> Keys.PERIOD;
            case "\t", "Tab" -> Keys.TAB;
            case "Caps Lock" -> Keys.CAPS_LOCK;
            case "Num Lock" -> Keys.NUM_LOCK;
            case " ", "Space" -> Keys.SPACE;
            case "\n", "Enter" -> Keys.ENTER;
            case "Delete" -> Keys.DEL; // also BACKSPACE
            case "`" -> Keys.GRAVE;
            case "-" -> Keys.MINUS;
            case "=" -> Keys.EQUALS;
            case "[" -> Keys.LEFT_BRACKET;
            case "]" -> Keys.RIGHT_BRACKET;
            case "\\" -> Keys.BACKSLASH;
            case ";" -> Keys.SEMICOLON;
            case ":" -> Keys.COLON;
            case "'" -> Keys.APOSTROPHE;
            case "/" -> Keys.SLASH;
            case "Num" -> Keys.NUM;
            case "Headset Hook" -> Keys.HEADSETHOOK;
            case "Focus" -> Keys.FOCUS;
            case "+" -> Keys.PLUS;
            case "Menu" -> Keys.MENU;
            case "L-Shift" -> Keys.SHIFT_LEFT;
            case "R-Shift" -> Keys.SHIFT_RIGHT;
            case "L-Ctrl" -> Keys.CONTROL_LEFT;
            case "R-Ctrl" -> Keys.CONTROL_RIGHT;
            case "L-Alt" -> Keys.ALT_LEFT;
            case "R-Alt" -> Keys.ALT_RIGHT;
            case "SYM" -> Keys.SYM;
            case "Insert" -> Keys.INSERT;
            case "Forward Delete" -> Keys.FORWARD_DEL;
            case "Home" -> Keys.HOME;
            case "End" -> Keys.END;
            case "Escape" -> Keys.ESCAPE;
            case "Page Up" -> Keys.PAGE_UP;
            case "Page Down" -> Keys.PAGE_DOWN;
            case "F1" -> Keys.F1;
            case "F2" -> Keys.F2;
            case "F3" -> Keys.F3;
            case "F4" -> Keys.F4;
            case "F5" -> Keys.F5;
            case "F6" -> Keys.F6;
            case "F7" -> Keys.F7;
            case "F8" -> Keys.F8;
            case "F9" -> Keys.F9;
            case "F10" -> Keys.F10;
            case "F11" -> Keys.F11;
            case "F12" -> Keys.F12;
            case "F13" -> Keys.F13;
            case "F14" -> Keys.F14;
            case "F15" -> Keys.F15;
            case "Scroll Lock" -> Keys.SCROLL_LOCK;
            case "Pause" -> Keys.PAUSE;
            case "Up" -> Keys.UP;
            case "Down" -> Keys.DOWN;
            case "Left" -> Keys.LEFT;
            case "Right" -> Keys.RIGHT;
            default ->
                // key name not found
                    -1;
        };
    }
}
