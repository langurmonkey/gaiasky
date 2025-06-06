/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Constructor;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.AttributeMap;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.MachineDefinition;
import gaiasky.scene.record.RotateTransform;
import gaiasky.util.Functions.Function3;
import gaiasky.util.Logger;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.coord.IBodyCoordinates;
import gaiasky.util.i18n.I18n;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Main JSON loader. Loads Gaia Sky dataset definition files written in JSON.
 */
public class JsonLoader extends AbstractSceneLoader {
    private static final Logger.Log logger = Logger.getLogger(JsonLoader.class);

    // Old components package. This was moved during the ECS refactor.
    private static final String COMPONENTS_PACKAGE = "gaiasky.scene.record.";
    // Params to skip in the normal processing.
    private static final List<String> PARAM_SKIP = Arrays.asList("args", "impl", "archetype", "comment", "comments");

    private static final Map<String, String> REPLACE = new HashMap<>();

    /** Maps old attributes to components. **/
    private static final AttributeMap attributeMap;

    static {
        REPLACE.put("gaiasky.scenegraph.MachineDefinition", MachineDefinition.class.getName());
        REPLACE.put("gaiasky.scenegraph.particle.BillboardDataset", BillboardDataset.class.getName());
        REPLACE.put("gaiasky.scenegraph.component.RotateTransform", RotateTransform.class.getName());

        // Initialize attribute map.
        attributeMap = new AttributeMap();
        attributeMap.initialize();
    }


    /**
     * Creates a new instance with the given index.
     */
    public JsonLoader(Map<String, Entity> index) {
        this.index = index;
    }

    /**
     * Creates a new instance with an empty index.
     */
    public JsonLoader() {
        this(null);
    }

    private static String replace(String key) {
        if (REPLACE.containsKey(key)) {
            return REPLACE.get(key);
        }
        return key;
    }

    @Override
    public Array<Entity> loadData() throws FileNotFoundException {
        Array<Entity> loadedEntities = new Array<>();
        Array<String> filePaths = new Array<>(this.filePaths);
        Array<JsonValue> updates = new Array<>();
        Array<FileHandle> updateFiles = new Array<>();

        // Actually load the files.
        JsonReader json = new JsonReader();
        for (String filePath : filePaths) {
            FileHandle file = Settings.settings.data.dataFileHandle(filePath, datasetDirectory);
            JsonValue root = json.parse(file.read());
            if (root.has("objects")) {
                // If the top element is 'objects', we have a list of new objects.
                JsonValue child = root.get("objects").child;
                final int count = root.get("objects").size;
                int processed = 0;
                int loaded = 0;
                while (child != null) {
                    processed++;
                    String archetypeName = child.has("archetype") ? child.getString("archetype") : child.getString("impl");
                    archetypeName = archetypeName.replace("gaia.cu9.ari.gaiaorbit", "gaiasky");

                    if (!scene.archetypes().contains(archetypeName)) {
                        // Do not know what to do
                        if (!loggedArchetypes.contains(archetypeName)) {
                            logger.warn("Skipping " + TextUtils.classSimpleName(archetypeName) + ": no suitable archetype found.");
                            loggedArchetypes.add(archetypeName);
                        }
                    } else {
                        loaded++;
                        // Create entity and fill it up
                        var archetype = scene.archetypes().get(archetypeName);
                        var entity = archetype.createEntity();
                        try {
                            fillEntity(child, entity, TextUtils.classSimpleName(archetypeName), false);
                            // Add to return list.
                            loadedEntities.add(entity);
                            // Add to index for possible later updates.
                            addToIndex(entity);
                        } catch (ReflectionException e) {
                            logger.error(e);
                        }
                    }

                    child = child.next;
                    EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, file.name(), (float) processed / (float) count);
                }
                EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, file.name(), 2f);
                logger.info(I18n.msg("notif.nodeloader", loaded, filePath));
            } else if (root.has("updates")) {
                // If the top element is 'updates', we update existing objects with additional attributes.
                // Store updates and run them afterward.
                updates.add(root);
                updateFiles.add(file);
            }
        }

        // Run the cached updates.
        int i = 0;
        for (var model : updates) {
            var file = updateFiles.get(i);
            JsonValue child = model.get("updates").child;
            final int count = model.get("updates").size;
            int processed = 0;
            while (child != null) {
                String name = child.getString("name");
                if (name != null) {
                    String nameLowerCase = name.toLowerCase().trim();
                    if (index.containsKey(nameLowerCase)) {
                        var entity = index.get(nameLowerCase);
                        if (entity != null) {
                            try {
                                var archetype = scene.archetypes().findArchetype(entity);
                                // Update entity.
                                fillEntity(child, entity, archetype.getName(), true);
                            } catch (ReflectionException e) {
                                logger.error(e);
                            }
                        } else {
                            logger.warn("Entity retrieved from index is null: " + nameLowerCase);
                        }
                    } else {
                        logger.warn("Update name not found in index: " + nameLowerCase);
                    }
                } else {
                    logger.warn("Update element does not contain a name attribute: " + file.name());
                }

                child = child.next;
                EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, file.name(), (float) processed / (float) count);
            }
            i++;
        }
        return loadedEntities;
    }

    /**
     * Adds the given entity to the map using its first name in lower case, trimmed.
     *
     * @param entity The entity.
     */
    private void addToIndex(Entity entity) {
        var base = Mapper.base.get(entity);
        var name = base.getName().toLowerCase().trim();
        if (!index.containsKey(name)) {
            index.put(name, entity);
        }
    }

    /**
     * Processes the given JSON tree, extracting and converting the class and value of each attribute,
     * and runs the given function on it.
     *
     * @param json     The JSON object.
     * @param function The function to run for each attribute. The parameters of the function are the value class, the value
     *                 object and the JSON attribute. It returns a boolean.
     */
    public void processJson(JsonValue json, Function3<Class<?>, Object, JsonValue, Boolean> function) throws ReflectionException {
        JsonValue attribute = json.child;
        while (attribute != null) {
            // We skip some param names
            if (!PARAM_SKIP.contains(attribute.name)) {
                Class<?> valueClass = null;
                Object value = null;
                if (attribute.isValue()) {
                    valueClass = getValueClass(attribute);
                    value = getValue(attribute);
                    if (value instanceof String) {
                        value = ((String) value).replace("gaia.cu9.ari.gaiaorbit", "gaiasky");
                    }
                } else if (attribute.isArray()) {
                    // We suppose our children are of the same type
                    switch (attribute.child.type()) {
                    case stringValue -> {
                        valueClass = String[].class;
                        value = attribute.asStringArray();
                    }
                    case doubleValue -> {
                        valueClass = double[].class;
                        value = attribute.asDoubleArray();
                    }
                    case booleanValue -> {
                        valueClass = boolean[].class;
                        value = attribute.asBooleanArray();
                    }
                    case longValue -> {
                        valueClass = int[].class;
                        value = attribute.asIntArray();
                    }
                    case object -> {
                        valueClass = Object[].class;
                        value = new Object[attribute.size];
                        JsonValue vectorAttribute = attribute.child;
                        int i = 0;
                        while (vectorAttribute != null) {
                            String clazzName = vectorAttribute.getString("impl").replace("gaia.cu9.ari.gaiaorbit", "gaiasky");
                            clazzName = replace(clazzName);
                            Class<Object> childClazz = (Class<Object>) ClassReflection.forName(clazzName);
                            ((Object[]) value)[i] = convertJsonToObject(vectorAttribute, childClazz);
                            i++;
                            vectorAttribute = vectorAttribute.next;
                        }
                    }
                    case array -> {
                        // Multi-dim array
                        Pair<Object, Class> p = toMultidimDoubleArray(attribute);
                        value = p.getFirst();
                        valueClass = p.getSecond();
                    }
                    default -> {
                    }
                    }

                } else if (attribute.isObject()) {
                    String clazzName = attribute.has("impl") ? attribute.getString("impl") : COMPONENTS_PACKAGE + TextUtils.capitalise(attribute.name) + "Component";
                    clazzName = clazzName.replace("gaia.cu9.ari.gaiaorbit", "gaiasky");
                    try {
                        valueClass = ClassReflection.forName(clazzName);
                        value = convertJsonToObject(attribute, valueClass);
                    } catch (Exception e1) {
                        // We use a map
                        try {
                            valueClass = Map.class;
                            value = convertJsonToMap(attribute);
                        } catch (Exception e2) {
                            logger.error("Could not convert attribute to value, object or map: " + attribute);
                        }
                    }
                }
                // Here we have value and valueClass -> run function.
                if (function != null && !function.apply(valueClass, value, attribute)) {
                    logger.error("Failed to apply function to JSON attribute: " + attribute);
                }
            }
            attribute = attribute.next;
        }
    }

    public void fillEntity(final JsonValue json, final Entity entity, final String className, boolean update) throws ReflectionException {
        processJson(json, (valueClass, value, attribute) -> {
            try {
                // We can't update the name!
                if (update && attribute.name.equalsIgnoreCase("name")) {
                    return true;
                }
                String key = findAttribute(attribute.name, className);
                if (key != null) {
                    Class<? extends Component> componentClass = attributeMap.get(key);
                    Component comp = entity.getComponent(componentClass);

                    if (comp != null) {
                        if (update) {
                            if (!update(attribute, comp, value, valueClass, componentClass)) {
                                logger.error("Update operation failed (unsupported?) for attribute: " + attribute.name);
                            }
                        } else {
                            if (!set(attribute, comp, value, valueClass, componentClass)) {
                                // Setter set did not work, try setting the attribute directly
                                boolean succeed = set(comp, attribute.name, value);
                                if (!succeed) {
                                    logger.error("Could not set attribute " + attribute.name + " (" + valueClass.getName() + ") in class " + componentClass + " or its superclass/interfaces.");
                                }
                                return succeed;
                            }
                        }
                    } else {
                        logger.error("Error, component of class " + componentClass + " is null: " + json.name);
                        return false;
                    }
                    return true;
                } else {
                    if (Mapper.base.has(entity)) {
                        // Use parameter map.
                        var base = Mapper.base.get(entity);
                        base.addExtraAttribute(attribute.name, value);
                        return true;
                    } else {
                        logger.warn("Component not found for attribute '" + attribute.name + "' and class '" + className + "'");
                        return false;
                    }
                }
            } catch (Exception e) {
                logger.error(e);
                return false;
            }
        });
        logger.debug(I18n.msg("notif.loading", className + ": " + entity.getComponent(Base.class).names[0]));
    }

    public String findAttribute(String attributeName, String className) {
        String mixedKey = attributeName + ":" + className;
        if (attributeMap.containsKey(mixedKey)) {
            return mixedKey;
        } else if (attributeMap.containsKey(attributeName)) {
            return attributeName;
        }
        return null;
    }

    /**
     * Converts the given {@link JsonValue} to a java object of the given {@link Class}.
     *
     * @param json  The {@link JsonValue} for the object to convert.
     * @param clazz The class of the object.
     *
     * @return The java object of the given class.
     */
    private Object convertJsonToObject(JsonValue json, Class<?> clazz) throws ReflectionException {
        Object instance;
        try {
            if (json.has("args")) {
                //Creator arguments
                JsonValue args = json.get("args");
                Class<?>[] argumentTypes = new Class[args.size];
                Object[] arguments = new Object[args.size];
                for (int i = 0; i < args.size; i++) {
                    JsonValue arg = args.get(i);
                    argumentTypes[i] = getValueClass(arg);
                    arguments[i] = getValue(arg);
                }
                Constructor constructor = ClassReflection.getConstructor(clazz, argumentTypes);
                instance = constructor.newInstance(arguments);
            } else {
                instance = ClassReflection.newInstance(clazz);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate class: " + e.getMessage());
        }
        if (instance != null) {
            processJson(json, (valueClass, value, attribute) -> set(attribute, instance, value, valueClass, clazz));
        } else {
            logger.error("Error, instance is null: " + json.name);
        }
        return instance;
    }

    public boolean set(Object instance, String fieldName, Object fieldValue) throws IllegalStateException {
        Class<?> clazz = instance.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(instance, fieldValue);
                return true;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private boolean set(JsonValue attribute, Object instance, Object value, Class<?> valueClass, Class<?> instanceClass) {
        String methodName = "set" + TextUtils.propertyToMethodName(attribute.name);
        Method m = searchMethod(methodName, valueClass, instanceClass);
        if (m != null) {
            try {
                m.invoke(instance, value);
            } catch (ReflectionException e) {
                logger.error(e);
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean update(JsonValue attribute, Object instance, Object value, Class<?> valueClass, Class<?> instanceClass) {
        String methodName = "update" + TextUtils.propertyToMethodName(attribute.name);
        Method m = searchMethod(methodName, valueClass, instanceClass);
        if (m != null) {
            try {
                m.invoke(instance, value);
            } catch (ReflectionException e) {
                throw new RuntimeException(e);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Searches for the given method with the given class. If none is found, it looks for fitting methods
     * with the class' interfaces and superclasses recursively.
     *
     * @param methodName    The method name.
     * @param parameterType The parameter class type.
     * @param source        The class of the source object.
     *
     * @return The method, if found. Null otherwise.
     */
    private Method searchMethod(String methodName, Class<?> parameterType, Class<?> source) {
        Method m = null;
        try {
            m = ClassReflection.getMethod(source, methodName, parameterType);
        } catch (ReflectionException e) {
            try {
                if (methodName.contains("setCoordinates")) {
                    // Special case
                    m = ClassReflection.getMethod(source, methodName, IBodyCoordinates.class);
                }
            } catch (ReflectionException ignored) {
            }
        }
        return m;
    }

    private Object getValue(JsonValue val) {
        Object value = null;
        switch (val.type()) {
        case stringValue -> value = val.asString();
        case doubleValue -> value = val.asDouble();
        case booleanValue -> value = val.asBoolean();
        case longValue -> value = val.asLong();
        case array -> {
            try {
                value = val.asDoubleArray();
            } catch (IllegalStateException e1) {
                try {
                    value = val.asFloatArray();
                } catch (IllegalStateException e2) {
                    try {
                        value = val.asLongArray();
                    } catch (IllegalStateException e3) {
                        try {
                            value = val.asIntArray();
                        } catch (IllegalStateException e4) {
                            try {
                                value = val.asStringArray();
                            } catch (IllegalStateException e5) {
                                try {
                                    value = val.asCharArray();
                                } catch (IllegalStateException ignored) {
                                }
                            }
                        }
                    }
                }
            }
        }
        default -> {
        }
        }
        return value;
    }

    private Class<?> getValueClass(JsonValue val) {
        Class<?> valueClass = null;
        switch (val.type()) {
        case stringValue -> valueClass = String.class;
        case doubleValue -> valueClass = Double.class;
        case booleanValue -> valueClass = Boolean.class;
        case longValue -> valueClass = Long.class;
        default -> {
        }
        }
        return valueClass;
    }

    public int depth(JsonValue attribute) {
        int d = 1;
        if (attribute.child != null) {
            return d + depth(attribute.child);
        } else {
            return d;
        }
    }

    public Pair<Object, Class> toMultidimDoubleArray(JsonValue attribute) {
        final int dim = depth(attribute) - 1;
        switch (dim) {
        case 1 -> {
            return to1DoubleArray(attribute);
        }
        case 2 -> {
            return to2DoubleArray(attribute);
        }
        case 3 -> {
            return to3DoubleArray(attribute);
        }
        }
        logger.error("Double arrays of dimension " + dim + " not supported: attribute \"" + attribute.name + "\"");
        return null;
    }

    public Pair<Object, Class> to1DoubleArray(JsonValue attribute) {
        return new Pair<>(attribute.asDouble(), double[].class);
    }

    public Pair<Object, Class> to2DoubleArray(JsonValue attribute) {
        JsonValue json = attribute.child;
        int size = attribute.size;
        double[][] result = new double[size][];
        int i = 0;
        do {
            result[i] = json.asDoubleArray();
            json = json.next();
            i++;
        } while (json != null);

        return new Pair<>(result, double[][].class);
    }

    public Pair<Object, Class> to3DoubleArray(JsonValue attribute) {
        JsonValue json = attribute.child;
        int size = attribute.size;
        double[][][] result = new double[size][][];
        int i = 0;
        do {
            double[][] l1 = new double[json.size][];
            // Fill in last level

            JsonValue child = json.child;
            int j = 0;
            do {
                double[] l2 = child.asDoubleArray();
                l1[j] = l2;

                child = child.next();
                j++;
            } while (child != null);

            result[i] = l1;

            json = json.next();
            i++;
        } while (json != null);

        return new Pair<>(result, double[][][].class);
    }

    public Map<String, Object> convertJsonToMap(JsonValue json) {
        Map<String, Object> map = new TreeMap<>();

        JsonValue child = json.child;
        while (child != null) {
            Object val = getValue(child);
            if (val != null) {
                map.put(child.name, val);
            }
            child = child.next;
        }

        return map;
    }

    @Override
    public void setName(String name) {

    }

    @Override
    public void setDescription(String description) {

    }

    @Override
    public void setParams(Map<String, Object> params) {

    }
}
