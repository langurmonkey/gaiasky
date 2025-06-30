/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.gui.iface.ConsoleInterface;
import gaiasky.script.v2.impl.APIModule;
import gaiasky.script.v2.impl.APIv2;
import gaiasky.script.v2.meta.ModuleDesc;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages the Gaia Sky console/terminal, keeps its history and more. The console accepts all calls defined in {@link IScriptingInterface}. Aliases to
 * the
 * most useful calls are defined in {@link ConsoleManager#shortcutMap}, and can be used as shortcuts.
 * The user interface part of the console in implemented in {@link ConsoleInterface}.
 */
public class ConsoleManager {
    /** Messages in the console. **/
    private final Array<Message> messages = new Array<>();
    /** History of all the commands sent thus far. **/
    private final Array<String> cmdHistory = new Array<>();
    /** Contains all APIv1 methods. **/
    private Map<String, Array<Method>> apiV1Methods;
    /** Contains all APIv2 methods, prefixed with their modules (i.e. module.method). **/
    private Map<String, Array<Method>> apiV2Methods;
    /** Contains all methods from both APIs. **/
    private final Map<String, Array<Method>> allMethods;
    /** Contains the shortcuts. **/
    private Map<String, String> shortcutMap;
    /** Module instances by path. **/
    private Map<String, Object> instances;
    /** Scripting interface reference. **/
    private IScriptingInterface script;

    /**
     * A single console message. Typically, this is represented visually by a single line.
     *
     * @param msg  The message string.
     * @param type The message type.
     * @param time The message creation time.
     */
    public record Message(String msg, MsgType type, Instant time) {

        /**
         * Returns the message with any ANSI codes stripped.
         *
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

    public ConsoleManager(IScriptingInterface script) {
        this.script = script;

        initAPIv2Methods();
        initAPIv1Methods();
        initShortcuts();
        initInstances();

        // Combine.
        allMethods = new HashMap<>();
        allMethods.putAll(apiV1Methods);
        allMethods.putAll(apiV2Methods);
    }

    public void addCommandToHistory(String cmd) {
        cmdHistory.add(cmd);
    }

    public Array<String> cmdHistory() {
        return cmdHistory;
    }

    public Array<Message> messages() {
        return messages;
    }

    public boolean hasMethod(String methodName) {
        return allMethods.containsKey(methodName);
    }

    public Array<Method> getMethods(String methodName) {
        return allMethods.get(methodName);
    }

    public Map<String, Array<Method>> methodMap() {
        return allMethods;
    }

    public Map<String, Array<Method>> methodMapAPIv1() {
        return apiV1Methods;
    }

    public Map<String, Array<Method>> methodMapAPIv2() {
        return apiV2Methods;
    }

    public Map<String, String> shortcutMap() {
        return shortcutMap;
    }

    public Object getInstance(String path) {
        return instances.get(path);
    }

    public String unwrapShortcut(String command) {
        if (shortcutMap.containsKey(command)) {
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

    private void initAPIv1Methods() {
        if (apiV1Methods == null) {
            Class<IScriptingInterface> iScriptingInterfaceClass = IScriptingInterface.class;
            Method[] allMethods = iScriptingInterfaceClass.getDeclaredMethods();

            apiV1Methods = new HashMap<>();
            for (Method method : allMethods) {
                Array<Method> matches;
                if (apiV1Methods.containsKey(method.getName())) {
                    matches = apiV1Methods.get(method.getName());
                } else {
                    matches = new Array<>(false, 1);
                }
                if (!matches.contains(method, true))
                    matches.add(method);
                apiV1Methods.put(method.getName(), matches);
            }
        }
    }

    private void initAPIv2Methods() {
        if (apiV2Methods == null) {
            apiV2Methods = new HashMap<>();
            var root = ModuleDesc.of(Path.of(""), APIv2.class, true);
            addAPIv2Methods(root);
        }
    }

    private void addAPIv2Methods(ModuleDesc module) {
        if (module.methodMap() != null && !module.methodMap().isEmpty()) {
            apiV2Methods.putAll(module.methodMap());
        }

        if (module.modules() != null && !module.modules().isEmpty()) {
            for (var m : module.modules()) {
                addAPIv2Methods(m);
            }
        }
    }

    private void initInstances() {
        if (instances == null) {
            instances = new HashMap<>();
        }

        instances.put(".", script);

        initInstances(((EventScriptingInterface) script).apiv2, "");
    }

    private void initInstances(Object o, String prefix) {
        var fields = o.getClass().getDeclaredFields();
        for(var f : fields) {
            if (Modifier.isPublic(f.getModifiers()) && APIModule.class.isAssignableFrom(f.getType())) {
                var path = prefix + (prefix.isEmpty() ? "":".") + f.getName().toLowerCase(Locale.ROOT);
                try {
                    instances.put(path + ".", f.get(o));
                    initInstances(f.get(o), path);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }
}
