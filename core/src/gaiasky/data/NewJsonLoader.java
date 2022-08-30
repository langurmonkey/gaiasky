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
import gaiasky.scene.Archetype;
import gaiasky.scene.AttributeMap;
import gaiasky.scene.component.Base;
import gaiasky.util.Functions.Function3;
import gaiasky.util.Logger;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.coord.IBodyCoordinates;
import gaiasky.util.i18n.I18n;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class NewJsonLoader extends AbstractSceneLoader {
    private static final Logger.Log logger = Logger.getLogger(NewJsonLoader.class);

    private static final String COMPONENTS_PACKAGE = "gaiasky.scenegraph.component.";
    // Params to skip in the normal processing
    private static final List<String> PARAM_SKIP = Arrays.asList("args", "impl", "comment", "comments");

    /** Maps old attributes to components. **/
    private AttributeMap attributeMap;

    /**
     * Creates a new instance.
     */
    public NewJsonLoader() {
        this.attributeMap = new AttributeMap();
        this.attributeMap.initialize();
    }


    @Override
    public void loadData() throws FileNotFoundException {
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
                    int processed = 0;
                    int loaded = 0;
                    while (child != null) {
                        processed++;
                        String clazzName = child.getString("impl");
                        clazzName = clazzName.replace("gaia.cu9.ari.gaiaorbit", "gaiasky");

                        @SuppressWarnings("unchecked") Class<Object> clazz = (Class<Object>) ClassReflection.forName(clazzName);
                        if (!scene.archetypes().contains(clazzName)) {
                            // Do not know what to do
                            if (!loggedArchetypes.contains(clazzName)) {
                                logger.warn("Skipping " + clazz.getSimpleName() + ": no suitable archetype found.");
                                loggedArchetypes.add(clazzName);
                            }
                        } else {
                            loaded++;
                            // Create entity and fill it up
                            Archetype archetype = scene.archetypes().get(clazzName);
                            Entity entity = archetype.createEntity();
                            fillEntity(child, entity, clazz.getSimpleName());
                            // Add to engine
                            scene.engine.addEntity(entity);
                        }

                        child = child.next;
                        EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, file.name(), (float) processed / (float) count);
                    }
                    EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, file.name(), 2f);
                    logger.info(I18n.msg("notif.nodeloader", loaded, filePath));
                }
            } catch (Exception e) {
                logger.error(e);
            }
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
                // Here we have value and valueClass -> run function.
                if (function != null && !function.apply(valueClass, value, attribute)) {
                    logger.error("Failed to apply function to JSON attribute: " + attribute);
                }
            }
            attribute = attribute.next;
        }
    }

    public void fillEntity(final JsonValue json, final Entity entity, final String className) throws ReflectionException {
        processJson(json, (valueClass, value, attribute) -> {
            String key = findAttribute(attribute.name, className);
            if (key != null) {
                Class<? extends Component> componentClass = attributeMap.get(key);
                Component comp = entity.getComponent(componentClass);

                if (!set(attribute, comp, value, valueClass, componentClass)) {
                    // Setter set did not work, try setting the attribute directly
                    boolean succeed = set(comp, attribute.name, value);
                    if (!succeed) {
                        logger.error("Could not set attribute " + attribute.name + " (" + valueClass.getName() + ") in class " + componentClass + " or its superclass/interfaces.");
                    }
                    return succeed;
                }
                return true;
            } else {
                logger.warn("Component not found for attribute '" + attribute.name + "' and class '" + className + "'");
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
        processJson(json, (valueClass, value, attribute) -> set(attribute, instance, value, valueClass, clazz));
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
        Method m = searchMethod(methodName, valueClass, instanceClass, false);
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
     * @param methodName     The method name.
     * @param parameterType  The parameter class type.
     * @param source         The class of the source object.
     * @param printException Whether to print an exception if no method is found.
     *
     * @return The method, if found. Null otherwise.
     */
    private Method searchMethod(String methodName, Class<?> parameterType, Class<?> source, boolean printException) {
        Method m = null;
        try {
            m = ClassReflection.getMethod(source, methodName, parameterType);
        } catch (ReflectionException e) {
            try {
                if (methodName.contains("setCoordinates")) {
                    // Special case
                    m = ClassReflection.getMethod(source, methodName, IBodyCoordinates.class);
                }
            } catch (ReflectionException e1) {
                if (printException) {
                    logger.error(e1);
                }
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
