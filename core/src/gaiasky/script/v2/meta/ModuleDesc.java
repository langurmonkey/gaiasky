/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.meta;

import com.badlogic.gdx.utils.Array;
import gaiasky.script.v2.impl.APIModule;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an APIv2 module.
 *
 * @param name      The module name.
 * @param path      The module path.
 * @param clazz     The class of the module.
 * @param methodMap The method map.
 * @param modules   References to the inner modules.
 */
public record ModuleDesc(String name, Path path, Class<?> clazz, Map<String, Array<Method>> methodMap, Array<ModuleDesc> modules) {

    /**
     * Creates a new {@link ModuleDesc} instance from the given root path and class.
     *
     * @param path  The root path.
     * @param clazz The class.
     *
     * @return The new {@link ModuleDesc}.
     */
    public static ModuleDesc of(Path path, Class<?> clazz) {
        return ModuleDesc.of(path, clazz, false);
    }

    /**
     * Creates a new {@link ModuleDesc} instance from the given root path and class.
     *
     * @param path         The root path.
     * @param clazz        The class.
     * @param prefixPath Use the full module paths in the module maps.
     *
     * @return The new {@link ModuleDesc}.
     */
    public static ModuleDesc of(Path path, Class<?> clazz, boolean prefixPath) {
        if (!APIModule.class.isAssignableFrom(clazz)) {
            // Current class is not an APIModule itself.
            return new ModuleDesc("root", path, clazz, null, getModules(clazz, path, prefixPath));
        } else {
            // We are an APIModule, gather methods.
            // Add methods.
            Method[] allMethods = clazz.getDeclaredMethods();
            var map = new HashMap<String, Array<Method>>();

            for (Method method : allMethods) {
                // Only use public methods
                if (!Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                String key;
                if (prefixPath) {
                    var prefix = path.toString().replace(File.separator, ".");
                    key = prefix + "." + method.getName();
                } else {
                    key = method.getName();
                }
                Array<Method> matches;
                if (map.containsKey(key)) {
                    matches = map.get(key);
                } else {
                    matches = new Array<>(false, 1);
                }
                if (!matches.contains(method, true))
                    matches.add(method);
                map.put(key, matches);
            }
            // Extract name from module class name.
            var name = clazz.getSimpleName().substring(0, clazz.getSimpleName().indexOf("Module")).toLowerCase();
            return new ModuleDesc(name, path, clazz, map, getModules(clazz, path, prefixPath));
        }
    }

    private static Array<ModuleDesc> getModules(Class<?> clazz, Path parentPath, boolean prefixPath) {
        var l = new Array<ModuleDesc>();
        for (var field : clazz.getDeclaredFields()) {
            var fieldClass = field.getType();
            if (APIModule.class.isAssignableFrom(fieldClass)) {
                l.add(ModuleDesc.of(parentPath.resolve(field.getName()), fieldClass, prefixPath));
            }
        }

        return l.isEmpty() ? null : l;
    }
}
