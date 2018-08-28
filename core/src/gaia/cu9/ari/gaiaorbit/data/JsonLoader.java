package gaia.cu9.ari.gaiaorbit.data;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Constructor;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import gaia.cu9.ari.gaiaorbit.interfce.TextUtils;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.coord.IBodyCoordinates;

/**
 * Implements the loading of scene graph nodes using libgdx's json library.
 * It loads entities in the JSON format described in <a href="https://github.com/ari-zah/gaiasandbox/wiki/Non-particle-data-loading">this link</a>.
 * @author Toni Sagrista
 *
 * @param <T>
 */
public class JsonLoader<T extends SceneGraphNode> implements ISceneGraphLoader {
    private static final Log logger = Logger.getLogger(JsonLoader.class);
    
    private static final String COMPONENTS_PACKAGE = "gaia.cu9.ari.gaiaorbit.scenegraph.component.";
    /** Params to skip in the normal processing **/
    private static final List<String> PARAM_SKIP = Arrays.asList("args", "impl", "comment", "comments");

    /** Contains all the files to be loaded by this loader **/
    private String[] filePaths;

    @Override
    public void initialize(String[] files) {
        filePaths = files;
    }

    @Override
    public Array<? extends SceneGraphNode> loadData() {
        Array<T> bodies = new Array<T>();

        try {
            JsonReader json = new JsonReader();
            for (String filePath : filePaths) {
                FileHandle file = Gdx.files.internal(filePath);
                JsonValue model = json.parse(file.read());
                JsonValue child = model.get("objects").child;
                int size = 0;
                while (child != null) {
                    size++;
                    String clazzName = child.getString("impl");

                    @SuppressWarnings("unchecked")
                    Class<Object> clazz = (Class<Object>) ClassReflection.forName(clazzName);

                    // Convert to object and add to list
                    @SuppressWarnings("unchecked")
                    T object = (T) convertJsonToObject(child, clazz);

                    bodies.add(object);

                    child = child.next;
                }
                logger.info(I18n.bundle.format("notif.nodeloader", size, filePath));
            }

        } catch (Exception e) {
            logger.error(e);
        }

        return bodies;
    }

    /**
     * Converts the given {@link JsonValue} to a java object of the given {@link Class}.
     * @param json The {@link JsonValue} for the object to convert.
     * @param clazz The class of the object.
     * @return The java object of the given class.
     * @throws ReflectionException 
     * @throws SecurityException 
     * @throws NoSuchMethodException 
     * @throws IllegalArgumentException 
     * @throws IllegalAccessException 
     * @throws ClassNotFoundException 
     * @throws InstantiationException 
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
                        // TransformComponent
                        valueClass = Object[].class;
                        value = new Object[attribute.size];
                        JsonValue vectorattrib = attribute.child;
                        int i = 0;
                        while (vectorattrib != null) {
                            String clazzName = vectorattrib.getString("impl");
                            @SuppressWarnings("unchecked")
                            Class<Object> childclazz = (Class<Object>) ClassReflection.forName(clazzName);
                            ((Object[]) value)[i] = convertJsonToObject(vectorattrib, childclazz);
                            i++;
                            vectorattrib = vectorattrib.next;
                        }
                        break;
                    case array:
                        // Multidim array! Only 3D double supported so far
                        valueClass = double[][][].class;

                        JsonValue child = attribute.child;

                        value = convertToDoubleArray(child, attribute.size);
                        break;
                    default:
                        break;
                    }

                } else if (attribute.isObject()) {
                    String clazzName = attribute.has("impl") ? attribute.getString("impl") : COMPONENTS_PACKAGE + TextUtils.capitalise(attribute.name) + "Component";
                    try {
                        valueClass = ClassReflection.forName(clazzName);
                        value = convertJsonToObject(attribute, valueClass);
                    } catch (Exception e1) {
                        // We use a map
                        valueClass = Map.class;
                        value = convertJsonToMap(attribute);
                    }

                }
                String methodName = "set" + GlobalResources.propertyToMethodName(attribute.name);
                //                Method m = ClassReflection.getMethod(clazz, methodName, valueClass);
                Method m = searchMethod(methodName, valueClass, clazz);
                if (m != null)
                    m.invoke(instance, value);
                else
                    throw new ReflectionException("No method " + methodName + "(" + valueClass.getName() + ") in class " + clazz + " or its superclass/interfaces.");
            }
            attribute = attribute.next;
        }
        return instance;
    }

    public double[][][] convertToDoubleArray(JsonValue json, int size) {
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

        return result;
    }

    public Map<String, Object> convertJsonToMap(JsonValue json) {
        Map<String, Object> map = new TreeMap<String, Object>();

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
     * with the classe's interfaces and superclasses recursively.
     * @param methodName
     * @param clazz
     * @return
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
}
