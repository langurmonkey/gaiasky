/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.urlprotocol;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
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
            "scene",
            "graphics"
    };
    /**
     * Individual APIv2 methods that can NOT be reached through
     * {@code gaiasky://} URLs, in {@code module.method} format (e.g.
     * {@code camera.go_to_object}). Applied on top of the module
     * disallow list.
     **/
    private static final String[] urlApiDisallowedMethods = new String[0];

    @Override
    public void process(String url,
                        Skin skin,
                        Stage stage,
                        boolean hotLoad,
                        Map<String, String> params) {
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

        // Invoke on the executor thread.
        var args = arguments;
        var methodName = method;
        GaiaSky.instance.getExecutorService().execute(() -> {
            try {
                var apiv2 = ((gaiasky.script.EventScriptingInterface) GaiaSky.instance.scripting()).apiv2;
                var moduleInstance = apiv2.getModuleInstance(moduleDesc.clazz());
                matchMethod.invoke(moduleInstance, args);
            } catch (Exception e) {
                logger.error(e, "Error invoking API method: " + module + "/" + methodName);
            }
        });
    }

    /**
     * Checks whether the given module or method is disallowed through the
     * <code>program::url::urlApiDisallowedModules</code> and
     * <code>program::url::urlApiDisallowedMethods</code> settings.
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
        return s.split(",");
    }
}