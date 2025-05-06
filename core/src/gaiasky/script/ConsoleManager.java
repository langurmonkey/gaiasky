/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import gaiasky.gui.iface.ConsoleInterface;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the Gaia Sky console/terminal, keeps its history and more. The console accepts all calls defined in {@link IScriptingInterface}. Aliases to the
 * most useful calls are defined in {@link ConsoleManager#shortcutMap}, and can be used as shortcuts.
 * The user interface part of the console in implemented in {@link ConsoleInterface}.
 */
public class ConsoleManager {
    private final Array<Message> messages = new Array<>();
    private final Array<String> cmdHistory = new Array<>();
    private Map<String, Array<Method>> methodMap;
    private Map<String, String> shortcutMap;

    /**
     * A single console message. Typically, this is represented visually by a single line.
     * @param msg The message string.
     * @param type The message type.
     * @param time The message creation time.
     */
    public record Message(String msg, MsgType type, Instant time) {

        /**
         * Returns the message with any ANSI codes stripped.
         * @return The message with no ANIS codes.
         */
        public String cleanMessage() {
            return TextUtils.stripAnsiCodes(msg);
        }
    }

    /**
     * Type of console message.
     */
    public enum MsgType {
        INFO("info", ColorUtils.gYellowC, ColorUtils.gWhiteC),
        ERROR("error", ColorUtils.gRedC, ColorUtils.gPinkC),
        RETURN("return", ColorUtils.gBlueC, ColorUtils.gWhiteC),
        OK("ok", ColorUtils.gGreenC, ColorUtils.gWhiteC);

        private final String code;
        private final Color msgColor;
        private final Color tagColor;

        MsgType(String code, Color tagColor, Color msgColor) {
            this.code = code;
            this.tagColor = tagColor;
            this.msgColor = msgColor;
        }

        public String getCodeString() {
            return I18n.msg("gui." + code + ".code");
        }

        public String getNameString() {
            return I18n.msg("gui." + code + ".name");
        }

        public Color getTagColor() {
            return tagColor;
        }

        public Color getMsgColor() {
            return msgColor;
        }
    }

    public ConsoleManager(){
       initializeMethodMap();
       initShortcuts();
    }

    public void addCommandToHistory(String cmd) {
        cmdHistory.add(cmd);
    }

    public Array<String> cmdHistory(){
        return cmdHistory;
    }
    public Array<Message> messages() {
        return messages;
    }

    public boolean hasMethod(String methodName) {
        return methodMap.containsKey(methodName);
    }
    public Array<Method> getMethods(String methodName) {
        return methodMap.get(methodName);
    }

    public Map<String, Array<Method>> methodMap(){
        return methodMap;
    }

    public Map<String, String> shortcutMap() {
        return shortcutMap;
    }

    public String unwrapShortcut(String command) {
        if(shortcutMap.containsKey(command)){
            return shortcutMap.get(command);
        }
        return command;
    }

    /**
     * Initialize aliases to the most useful API calls.
     */
    private void initShortcuts() {
        shortcutMap = new HashMap<>();
        shortcutMap.put("goto", "goToObject");
        shortcutMap.put("gotonow", "goToObjectInstant");
        shortcutMap.put("land", "landOnObject");
        shortcutMap.put("distance", "getDistanceTo");
        shortcutMap.put("pos", "getObjectPosition");

        shortcutMap.put("find", "setCameraFocus");
        shortcutMap.put("focus", "setCameraFocus");
        shortcutMap.put("free", "setCameraFree");

        shortcutMap.put("starttime", "startSimulationTime");
        shortcutMap.put("stoptime", "stopSimulationTime");
        shortcutMap.put("time", "setSimulationTime");
        shortcutMap.put("timewarp", "setTimeWarp");

        shortcutMap.put("startexindex", "setStarTextureIndex");

        shortcutMap.put("fov", "setFov");
        shortcutMap.put("forward", "cameraForward");
        shortcutMap.put("rotate", "cameraRotate");
        shortcutMap.put("turn", "cameraTurn");
        shortcutMap.put("stop", "cameraStop");
        shortcutMap.put("roll", "cameraRoll");
        shortcutMap.put("pitch", "cameraPitch");
        shortcutMap.put("yaw", "cameraYaw");
        shortcutMap.put("setcampos", "setCameraPosition");
        shortcutMap.put("campos", "getCameraPosition");
        shortcutMap.put("setcamdir", "setCameraDirection");
        shortcutMap.put("camdir", "getCameraDirection");
        shortcutMap.put("setcamup", "setCameraUp");
        shortcutMap.put("camup", "getCameraUp");
        shortcutMap.put("direquatorial", "setCameraDirectionEquatorial");
        shortcutMap.put("dirgalactic", "setCameraDirectionGalactic");

        shortcutMap.put("screenshot", "saveScreenshot");
        shortcutMap.put("version", "getVersion");
        shortcutMap.put("log", "log");
        shortcutMap.put("print", "print");

        shortcutMap.put("exit", "quit");
    }

    private void initializeMethodMap() {
        if (methodMap == null) {
            Class<IScriptingInterface> iScriptingInterfaceClass = IScriptingInterface.class;
            Method[] allMethods = iScriptingInterfaceClass.getDeclaredMethods();

            methodMap = new HashMap<>();
            for (Method method : allMethods) {
                Array<Method> matches;
                if (methodMap.containsKey(method.getName())) {
                    matches = methodMap.get(method.getName());
                } else {
                    matches = new Array<>(false, 1);
                }
                if (!matches.contains(method, true))
                    matches.add(method);
                methodMap.put(method.getName(), matches);
            }
        }
    }
}
