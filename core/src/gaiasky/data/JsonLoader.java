/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Constructor;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.coord.IBodyCoordinates;
import uk.ac.starlink.util.DataSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Implements the loading of scene graph nodes using libgdx's json library.
 * It loads entities in the JSON format described in <a href="https://github.com/ari-zah/gaiasandbox/wiki/Non-particle-data-loading">this link</a>.
 */
public class JsonLoader<T extends SceneGraphNode> implements ISceneGraphLoader {
    private static final Log logger = Logger.getLogger(JsonLoader.class);

    private static final String COMPONENTS_PACKAGE = "gaiasky.scenegraph.component.";
    // Params to skip in the normal processing
    private static final List<String> PARAM_SKIP = Arrays.asList("args", "impl", "comment", "comments");

    // Contains all the files to be loaded by this loader
    private String[] filePaths;

    @Override
    public void initialize(String[] files) {
        filePaths = files;
    }

    @Override
    public void initialize(DataSource ds) {
    }

    @Override
    public Array<? extends SceneGraphNode> loadData() {
        Array<T> bodies = new Array<>();

        Array<String> filePaths = new Array<>(this.filePaths);

        // Actually load the files.
        JsonReader json = new JsonReader();
        for (String filePath : filePaths) {
            try {
                FileHandle file = Settings.settings.data.dataFileHandle(filePath);
                JsonValue model = json.parse(file.read());

                // Must have an 'objects' element.
                if (model.has("objects")) {
                    JsonValue child = model.get("objects").child;
                    final int count = model.get("objects").size;
                    int current = 0;
                    while (child != null) {
                        current++;
                        String clazzName = child.getString("impl").replace("gaia.cu9.ari.gaiaorbit", "gaiasky");

                        @SuppressWarnings("unchecked") Class<Object> clazz = (Class<Object>) ClassReflection.forName(clazzName);

                        // Convert to object and add to list.
                        @SuppressWarnings("unchecked") T object = (T) convertJsonToObject(child, clazz);

                        bodies.add(object);

                        child = child.next;
                        EventManager.instance.post(Events.UPDATE_LOAD_PROGRESS, file.name(), (float) current / (float) count);
                    }
                    EventManager.instance.post(Events.UPDATE_LOAD_PROGRESS, file.name(), 2f);
                    logger.info(I18n.txt("notif.nodeloader", current, filePath));
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }

        return bodies;
    }

    /**
     * Converts the given {@link JsonValue} to a java object of the given {@link Class}.
     *
     * @param json  The {@link JsonValue} for the object to convert.
     * @param clazz The class of the object.
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
        JsonValue attribute = json.child;
        while (attribute != null) {
            // We skip some param names
            if (!PARAM_SKIP.contains(attribute.name)) {
                Class<?> valueClass = null;
                Object value = null;
                if (attribute.isValue()) {
                    valueClass = getValueClass(attribute);
                    value = getValue(attribute);
                } else if (attribute.isArray()) {
                    // We suppose our children are of the same type
                    switch (attribute.child.type()) {
                    case stringValue:
                        valueClass = String[].class;
                        value = attribute.asStringArray();
                        break;
                    case doubleValue:
                        valueClass = double[].class;
                        value = attribute.asDoubleArray();
                        break;
                    case booleanValue:
                        valueClass = boolean[].class;
                        value = attribute.asBooleanArray();
                        break;
                    case longValue:
                        valueClass = int[].class;
                        value = attribute.asIntArray();
                        break;
                    case object:
                        valueClass = Object[].class;
                        value = new Object[attribute.size];
                        JsonValue vectorattrib = attribute.child;
                        int i = 0;
                        while (vectorattrib != null) {
                            String clazzName = vectorattrib.getString("impl").replace("gaia.cu9.ari.gaiaorbit", "gaiasky");
                            @SuppressWarnings("unchecked") Class<Object> childclazz = (Class<Object>) ClassReflection.forName(clazzName);
                            ((Object[]) value)[i] = convertJsonToObject(vectorattrib, childclazz);
                            i++;
                            vectorattrib = vectorattrib.next;
                        }
                        break;
                    case array:
                        // Multi-dim array
                        Pair<Object, Class> p = toMultidimDoubleArray(attribute);
                        value = p.getFirst();
                        valueClass = p.getSecond();
                        break;
                    default:
                        break;
                    }

                } else if (attribute.isObject()) {
                    String clazzName = attribute.has("impl") ? attribute.getString("impl") : COMPONENTS_PACKAGE + TextUtils.capitalise(attribute.name) + "Component";
                    clazzName = clazzName.replace("gaia.cu9.ari.gaiaorbit", "gaiasky");
                    try {
                        valueClass = ClassReflection.forName(clazzName);
                        value = convertJsonToObject(attribute, valueClass);
                    } catch (Exception e1) {
                        // We use a map
                        valueClass = Map.class;
                        value = convertJsonToMap(attribute);
                    }

                }
                String methodName = "set" + TextUtils.propertyToMethodName(attribute.name);
                Method m = searchMethod(methodName, valueClass, clazz);
                if (m != null)
                    m.invoke(instance, value);
                else
                    logger.error("ERROR: No method " + methodName + "(" + valueClass.getName() + ") in class " + clazz + " or its superclass/interfaces.");
            }
            attribute = attribute.next;
        }
        logger.debug(I18n.txt("notif.loading", instance.getClass().getSimpleName() + ": " + instance.toString()));
        return instance;
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
        case 1:
            return to1DoubleArray(attribute);
        case 2:
            return to2DoubleArray(attribute);
        case 3:
            return to3DoubleArray(attribute);
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

    /**
     * Searches for the given method with the given class. If none is found, it looks for fitting methods
     * with the class' interfaces and superclasses recursively.
     *
     * @param methodName The method name.
     * @param clazz The class.
     * @return The method, if found. Null otherwise.
     */
    private Method searchMethod(String methodName, Class<?> clazz, Class<?> source) {
        Method m = null;
        try {
            m = ClassReflection.getMethod(source, methodName, clazz);
        } catch (ReflectionException e) {
            try {
                if (methodName.contains("setCoordinates")) {
                    // Special case
                    m = ClassReflection.getMethod(source, methodName, IBodyCoordinates.class);
                }
            } catch (ReflectionException e1) {
                logger.error(e1);
            }
        }
        return m;
    }

    private Object getValue(JsonValue val) {
        Object value = null;
        switch (val.type()) {
        case stringValue:
            value = val.asString();
            break;
        case doubleValue:
            value = val.asDouble();
            break;
        case booleanValue:
            value = val.asBoolean();
            break;
        case longValue:
            value = val.asLong();
            break;
        default:
            break;
        }
        return value;
    }

    private Class<?> getValueClass(JsonValue val) {
        Class<?> valueClass = null;
        switch (val.type()) {
        case stringValue:
            valueClass = String.class;
            break;
        case doubleValue:
            valueClass = Double.class;
            break;
        case booleanValue:
            valueClass = Boolean.class;
            break;
        case longValue:
            valueClass = Long.class;
            break;
        default:
            break;
        }
        return valueClass;
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
