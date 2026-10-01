/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.urlprotocol;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.gui.window.ApiCallConfirmWindow;
import gaiasky.script.v2.impl.APIv2;
import gaiasky.script.v2.meta.ModuleDesc;
import gaiasky.util.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.Locale;
import java.util.Map;

/**
 * Generic reflective action handler that maps {@code gaiasky://} URLs to
 * APIv2 calls. The URL has the form:
 * <p>
 * <code>gaiasky://&lt;module&gt;/&lt;method&gt;?param1=value1&amp;param2=value2...</code>
 * <p>
 * For example:
 * <ul>
 * <li><code>gaiasky://camera/focus_mode?name=Earth</code></li>
 * <li><code>gaiasky://time/set_time?year=2026&amp;month=1&amp;day=1</code></li>
 * </ul>
 * <p>
 * The method resolution and parameter coercion follow the same conventions as
 * the REST server ({@code RESTServer.handleAPIv2Call()}): parameters are
 * matched by name (or positionally as {@code arg0}, {@code arg1}, ...) and
 * coerced from strings to the method parameter types.
 * <p>
 * For security reasons, only modules that are not in the disallow list
 * ({@link #urlApiDisallowedModules}) are
 * reachable. Individual methods can also be disallowed through
 * {@link #urlApiDisallowedMethods}, using the
 * <code>module.method</code> format (e.g. <code>camera.go_to_object</code>).
 */
public class ActionApiCall implements ActionHandler {
    private static final Logger.Log logger = Logger.getLogger(ActionApiCall.class);

    /** Maximum accepted URL length, in characters. **/
    private static final int MAX_URL_LENGTH = 4096;
    /** Maximum accepted number of URL parameters. **/
    private static final int MAX_PARAMS = 64;
    /** Maximum accepted length of a single parameter value. **/
    private static final int MAX_PARAM_VALUE_LENGTH = 1024;

    /**
     * APIv2 modules that can NOT be reached through {@code gaiasky://}
     * URLs of the form {@code gaiasky://<module>/<method>?params}.
     * This is a disallow list: everything not listed is allowed.
     **/
    private static final String[] urlApiDisallowedModules = new String[]{
            "data",
            "input",
            "output",
            "instances",
            "scene"
    };
    /**
     * Individual APIv2 methods that can NOT be reached through
     * {@code gaiasky://} URLs, in {@code module.method} format (e.g.
     * {@code camera.go_to_object}). Applied on top of the module
     * disallow list.
     * <p>
     *     Note that all methods that return a non-void type are
     *     disallowed in code.
     * </p>
     **/
    private static final String[] urlApiDisallowedMethods = new String[0];

    @Override
    public void process(String url,
                        Skin skin,
                        Stage stage,
                        boolean hotLoad,
                        Map<String, String> params) {
        // Reject absurdly long URLs before doing any parsing work: a
        // malicious page can trigger the protocol handler in a loop.
        if (url == null || url.length() > MAX_URL_LENGTH) {
            logger.warn("Rejected over-long API URL (" + (url == null ? 0 : url.length()) + " chars)");
            return;
        }
        if (params != null && params.size() > MAX_PARAMS) {
            logger.warn("Rejected API URL with too many parameters: " + params.size());
            return;
        }
        if (params != null) {
            for (var v : params.values()) {
                if (v != null && v.length() > MAX_PARAM_VALUE_LENGTH) {
                    logger.warn("Rejected API URL with an over-long parameter value");
                    return;
                }
            }
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            logger.error(e, "Malformed API URL: " + url);
            return;
        }

        // The module is the host, the method is the first path segment.
        String module = uri.getHost();
        String method = null;
        if (uri.getPath() != null && !uri.getPath().isBlank()) {
            var segments = uri.getPath().split("/");
            for (var s : segments) {
                if (!s.isBlank()) {
                    method = s;
                    break;
                }
            }
        }
        if (module == null || module.isBlank() || method == null) {
            logger.error("API URL must have the form gaiasky://<module>/<method>?params: " + url);
            postNotification("Invalid API URL: gaiasky://" + module);
            return;
        }

        // Check the disallow list.
        if (isDisallowed(module, method)) {
            String msg = "API call not allowed through gaiasky:// URLs: " + module + "/" + method;
            logger.warn(msg);
            postNotification(msg);
            return;
        }

        // Resolve the module.
        var moduleDesc = resolveModule(module);
        if (moduleDesc == null) {
            String msg = "Unknown API module: " + module;
            logger.warn(msg);
            postNotification(msg);
            return;
        }

        // Match the method.
        if (moduleDesc.methodMap() == null || !moduleDesc.methodMap().containsKey(method)) {
            String msg = "Unknown API method: " + module + "/" + method;
            logger.warn(msg);
            postNotification(msg);
            return;
        }
        var matched = matchParameters(moduleDesc.methodMap().get(method), method, params.keySet());
        var matchMethod = matched.getFirst();
        if (matchMethod == null) {
            String msg = matched.getSecond() ?
                    "API method found, but parameters are not compatible: " + module + "/" + method :
                    "Unknown API method: " + module + "/" + method;
            logger.warn(msg);
            postNotification(msg);
            return;
        }

        // Reject calls that return a value: the result would be discarded by
        // the URL invocation, so they make no sense as an action (getters,
        // queries, listings).
        if (!Void.TYPE.equals(matchMethod.getReturnType())) {
            String msg = "API method returns a value, so it cannot be used as an action: " + module + "/" + method;
            logger.warn(msg);
            postNotification(msg);
            return;
        }

        // Coerce arguments.
        Object[] arguments;
        try {
            arguments = coerceArguments(matchMethod, params);
        } catch (Exception e) {
            String msg = "Failed to convert parameters for " + module + "/" + method + ": " + e.getMessage();
            logger.warn(e, msg);
            postNotification(msg);
            return;
        }

        // Ask the user for confirmation. Any web page can trigger a
        // gaiasky:// URL without user interaction (iframe, redirect, link
        // with automatic navigation), so an API call must never be executed
        // silently.
        final String moduleName = module;
        final String methodNameFinal = method;
        var invoke = (Runnable) () -> invoke(moduleDesc, matchMethod, arguments, moduleName, methodNameFinal);
        if (skin != null && stage != null) {
            var confirm = new ApiCallConfirmWindow(url, moduleName + "/" + methodNameFinal, skin, stage, () -> Gdx.app.postRunnable(invoke),
                                                 () -> logger.info("API call cancelled by user: " + moduleName + "/" + methodNameFinal));
            confirm.show(stage);
        } else {
            // No UI available (e.g. headless or startup): do not execute.
            logger.warn("No UI available to confirm API call, ignoring: " + module + "/" + method);
        }
    }

    /**
     * Invokes the given API method on the Gaia Sky executor thread.
     *
     * @param moduleDesc The module descriptor.
     * @param method     The resolved method.
     * @param arguments  The coerced arguments.
     * @param module     The module name (for logging).
     * @param methodName The method name (for logging).
     */
    private static void invoke(ModuleDesc moduleDesc,
                               Method method,
                               Object[] arguments,
                               String module,
                               String methodName) {
        GaiaSky.instance.getExecutorService().execute(() -> {
            try {
                var apiv2 = ((gaiasky.script.EventScriptingInterface) GaiaSky.instance.scripting()).apiv2;
                var moduleInstance = apiv2.getModuleInstance(moduleDesc.clazz());
                method.invoke(moduleInstance, arguments);
            } catch (Exception e) {
                logger.error(e, "Error invoking API method: " + module + "/" + methodName);
            }
        });
    }

    /**
     * Checks whether the given module or method is disallowed by the
     * hardcoded lists of this class. Additionally, methods that return a
     * value are rejected after method resolution, see
     * {@code process()}.
     *
     * @param module The module name.
     * @param method The method name. May be null (only the module is checked).
     *
     * @return True if the call is disallowed.
     */
    public static boolean isDisallowed(String module,
                                       String method) {
        for (var m : urlApiDisallowedModules) {
            if (m.equalsIgnoreCase(module)) {
                return true;
            }
        }
        if (method != null) {
            String qualified = module.toLowerCase(Locale.ROOT) + "." + method.toLowerCase(Locale.ROOT);
            for (var m : urlApiDisallowedMethods) {
                if (m != null && qualified.equalsIgnoreCase(m.toLowerCase(Locale.ROOT).trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Resolves an APIv2 module by name, searching recursively through the
     * module tree.
     *
     * @param name The module name (e.g. "camera").
     *
     * @return The module descriptor, or null if not found.
     */
    private static ModuleDesc resolveModule(String name) {
        ModuleDesc root = ModuleDesc.of(java.nio.file.Path.of("apiv2"), APIv2.class);
        return findModule(root, name);
    }

    /**
     * Recursively searches the module tree for a module with the given name.
     *
     * @param module The current module.
     * @param name   The name to look for.
     *
     * @return The module descriptor, or null if not found.
     */
    private static ModuleDesc findModule(ModuleDesc module,
                                         String name) {
        if (module.modules() != null) {
            for (var inner : module.modules()) {
                if (inner.name().equalsIgnoreCase(name)) {
                    return inner;
                }
                var found = findModule(inner, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Matches a method by name and parameter names, following the same
     * conventions as the REST server: parameters may be given by name or
     * positionally ({@code arg0}, {@code arg1}, ...).
     *
     * @param matchMethods The candidate methods.
     * @param cmd          The method name.
     * @param paramNames   The available parameter names.
     *
     * @return A pair with the matched method (null if none) and a flag
     * indicating whether the method name matched at all.
     */
    private static gaiasky.util.Pair<Method, Boolean> matchParameters(Array<Method> matchMethods,
                                                                      String cmd,
                                                                      java.util.Set<String> paramNames) {
        Method matchMethod = null;
        boolean methodNameMatches = false;
        for (var m : matchMethods) {
            if (!m.getName().equals(cmd)) {
                continue;
            }
            methodNameMatches = true;
            var methodParams = m.getParameters();
            if (methodParams.length != paramNames.size()) {
                continue;
            }
            boolean allPresent = true;
            for (int i = 0; i < methodParams.length; i++) {
                if (!paramNames.contains(methodParams[i].getName()) && !paramNames.contains("arg" + i)) {
                    allPresent = false;
                    break;
                }
            }
            if (allPresent) {
                matchMethod = m;
                break;
            }
        }
        return new gaiasky.util.Pair<>(matchMethod, methodNameMatches);
    }

    /**
     * Coerces the string parameters to the argument types of the given
     * method, following the same conventions as the REST server: parameters
     * are looked up by name, falling back to positional {@code arg<i>} names.
     * Vectors are comma-separated strings, optionally enclosed in square
     * brackets.
     *
     * @param method The method.
     * @param params The string parameters.
     *
     * @return The argument array, ready for {@code invoke()}.
     *
     * @throws IllegalArgumentException If a value can't be converted.
     */
    private static Object[] coerceArguments(Method method,
                                            Map<String, String> params) {
        Parameter[] methodParams = method.getParameters();
        Object[] arguments = new Object[methodParams.length];
        for (int i = 0; i < methodParams.length; i++) {
            Parameter p = methodParams[i];
            String stringValue = params.get(p.getName());
            if (stringValue == null) {
                stringValue = params.get("arg" + i);
            }
            if (stringValue == null) {
                throw new IllegalArgumentException("Missing parameter: " + p.getName());
            }
            Class<?> type = p.getType();
            if (Integer.TYPE.equals(type) || Integer.class.equals(type)) {
                arguments[i] = Integer.parseInt(stringValue);
            } else if (Long.TYPE.equals(type) || Long.class.equals(type)) {
                arguments[i] = Long.parseLong(stringValue);
            } else if (Float.TYPE.equals(type) || Float.class.equals(type)) {
                arguments[i] = Float.parseFloat(stringValue);
            } else if (Double.TYPE.equals(type) || Double.class.equals(type)) {
                arguments[i] = Double.parseDouble(stringValue);
            } else if (Boolean.TYPE.equals(type) || Boolean.class.equals(type)) {
                arguments[i] = Boolean.parseBoolean(stringValue);
            } else if (int[].class.equals(type) || Integer[].class.equals(type)) {
                var svec = splitArrayString(stringValue);
                var dvec = new int[svec.length];
                for (int vi = 0; vi < svec.length; vi++) {
                    dvec[vi] = Integer.parseInt(svec[vi]);
                }
                arguments[i] = dvec;
            } else if (float[].class.equals(type) || Float[].class.equals(type)) {
                var svec = splitArrayString(stringValue);
                var dvec = new float[svec.length];
                for (int vi = 0; vi < svec.length; vi++) {
                    dvec[vi] = Float.parseFloat(svec[vi]);
                }
                arguments[i] = dvec;
            } else if (double[].class.equals(type) || Double[].class.equals(type)) {
                var svec = splitArrayString(stringValue);
                var dvec = new double[svec.length];
                for (int vi = 0; vi < svec.length; vi++) {
                    dvec[vi] = Double.parseDouble(svec[vi]);
                }
                arguments[i] = dvec;
            } else if (String[].class.equals(type)) {
                arguments[i] = splitArrayString(stringValue);
            } else {
                // String, also if it is some other type — invoke will raise an exception.
                arguments[i] = stringValue;
            }
        }
        return arguments;
    }

    /**
     * Converts an array-representing string to an array of strings. Arrays
     * are comma-separated and optionally enclosed in square brackets, e.g.
     * "[var1,var2,var3]" or "var1,var2,var3".
     *
     * @param arrayString The array string.
     *
     * @return The string array.
     */
    private static String[] splitArrayString(String arrayString) {
        String s = arrayString.trim();
        if (s.startsWith("[") && s.endsWith("]") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        if (s.isBlank()) {
            return new String[0];
        }
        if (s.length() > MAX_PARAM_VALUE_LENGTH) {
            throw new IllegalArgumentException("Array parameter is too long: " + s.length() + " characters");
        }
        // Trim each element, so that "[1, 2, 3]" works like "1,2,3".
        var parts = s.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }
}