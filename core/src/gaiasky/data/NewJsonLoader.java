package gaiasky.data;

import com.artemis.*;
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
import gaiasky.scene.component.*;
import gaiasky.scenegraph.*;
import gaiasky.scenegraph.Satellite;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.coord.IBodyCoordinates;
import gaiasky.util.i18n.I18n;
import uk.ac.starlink.util.DataSource;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.*;

public class NewJsonLoader implements ISceneLoader {
    private static final Log logger = Logger.getLogger(NewJsonLoader.class);

    private static final String COMPONENTS_PACKAGE = "gaiasky.scenegraph.component.";
    // Params to skip in the normal processing
    private static final List<String> PARAM_SKIP = Arrays.asList("args", "impl", "comment", "comments");

    @FunctionalInterface
    interface Function6<One, Two, Three, Four> {
        public Four apply(One one, Two two, Three three);
    }

    // Contains all the files to be loaded by this loader
    private String[] filePaths;

    // Archetypes map, links old scene graph model objects to artemis archetypes
    private Map<String, Archetype> archetypes;

    // Maps old attributes to components
    private Map<String, Class<? extends Component>> attributeMap;

    private World world;

    private Set<String> loggedArchetypes;

    @Override
    public void initialize(String[] files, World world) throws RuntimeException {
        this.world = world;
        this.filePaths = files;
        this.loggedArchetypes = new HashSet<>();
        initializeArchetypes();
        initializeAttributes();
    }

    @Override
    public void initialize(DataSource ds, World world) {
        this.world = world;
        initializeArchetypes();
        initializeAttributes();
    }

    private void initializeArchetypes() {
        if (this.world != null) {
            this.archetypes = new HashMap<>();

            // SceneGraphNode
            addArchetype(SceneGraphNode.class.getName(),
                    Base.class, Flags.class, Body.class, GraphNode.class, Octant.class);

            // Celestial
            addArchetype(CelestialBody.class.getName(), SceneGraphNode.class.getName(),
                    Celestial.class, Magnitude.class, Coordinates.class,
                    ProperMotion.class, Rotation.class);

            // ModelBody
            addArchetype(ModelBody.class.getName(), CelestialBody.class.getName(),
                    Model.class, ModelScaffolding.class);

            // Planet
            addArchetype(Planet.class.getName(), ModelBody.class.getName(),
                    Atmosphere.class, Cloud.class);

            // Star
            addArchetype(Star.class.getName(), CelestialBody.class.getName(),
                    ProperMotion.class);

            // Satellite
            addArchetype(Satellite.class.getName(), ModelBody.class.getName(),
                    ParentOrientation.class);

            // Gaia
            addArchetype(Gaia.class.getName(), Satellite.class.getName(),
                    Attitude.class);

            // GenericSpacecraft
            addArchetype(GenericSpacecraft.class.getName(), Satellite.class.getName(),
                    RenderFlags.class);

            // Spacecraft
            addArchetype(Spacecraft.class.getName(), GenericSpacecraft.class.getName(),
                    Machine.class);

            // VertsObject
            addArchetype(VertsObject.class.getName(), SceneGraphNode.class.getName(),
                    Verts.class);

            // Polyline
            addArchetype(Polyline.class.getName(), VertsObject.class.getName(),
                    Arrow.class);

            // Orbit
            addArchetype(Orbit.class.getName(), Polyline.class.getName(),
                    Trajectory.class, Transform.class);

            // HeliotropicOrbit
            addArchetype(HeliotropicOrbit.class.getName(), Orbit.class.getName(),
                    Heliotropic.class);

            // FadeNode
            addArchetype(FadeNode.class.getName(), SceneGraphNode.class.getName(),
                    Fade.class, Label.class, DatasetDescription.class, Highlight.class);

            // BackgroundModel
            addArchetype(BackgroundModel.class.getName(), FadeNode.class.getName(),
                    Transform.class, Model.class, Label.class, Coordinates.class,
                    RenderType.class);

            // SphericalGrid
            addArchetype(SphericalGrid.class.getName(), BackgroundModel.class.getName(),
                    GridUV.class);

            // RecursiveGrid
            addArchetype(RecursiveGrid.class.getName(), SceneGraphNode.class.getName(),
                    GridRecursive.class, Fade.class, Transform.class, Model.class,
                    Label.class, RenderType.class);

            // BillboardGroup
            addArchetype(BillboardGroup.class.getName(), SceneGraphNode.class.getName(),
                    BillboardDatasets.class, Transform.class, Label.class, Fade.class,
                    Coordinates.class);

            // Text2D
            addArchetype(Text2D.class.getName(), SceneGraphNode.class.getName(),
                    Fade.class, Title.class);

            // Axes
            addArchetype(Axes.class.getName(), SceneGraphNode.class.getName(),
                    Axis.class, Transform.class);

            // Loc
            addArchetype(Loc.class.getName(), SceneGraphNode.class.getName(),
                    LocationMark.class);

        } else {
            logger.error("World is null, can't initialize archetypes.");
        }
    }

    private void addArchetype(String archetypeName, String parentArchetypeName, Class<? extends Component>... classes) {
        ArchetypeBuilder builder;
        if(parentArchetypeName != null && this.archetypes.containsKey(parentArchetypeName)) {
            builder = new ArchetypeBuilder(this.archetypes.get(parentArchetypeName));
        } else {
            builder = new ArchetypeBuilder();
        }
        for (Class<? extends Component> c : classes) {
            builder.add(c);
        }
        this.archetypes.put(archetypeName, builder.build(world));
    }

    private void addArchetype(String archetypeName, Class<? extends Component>... classes) {
        addArchetype(archetypeName, null, classes);
    }

    private void initializeAttributes() {
        if (this.world != null) {
            this.attributeMap = new HashMap<>();

            // Base
            putAll(Base.class,
                    "id",
                    "name",
                    "names",
                    "opacity",
                    "ct");

            // Body
            putAll(Body.class,
                    "position",
                    "size",
                    "color",
                    "labelcolor");

            // GraphNode
            putAll(GraphNode.class,
                    "parent");

            // Coordinates
            putAll(Coordinates.class,
                    "coordinates");

            // Rotation
            putAll(Rotation.class,
                    "rotation");

            // Celestial
            putAll(Celestial.class,
                    "wikiname",
                    "colorbv");

            // Magnitude
            putAll(Magnitude.class,
                    "appmag",
                    "absmag");

            // ModelScaffolding
            putAll(ModelScaffolding.class,
                    "refplane",
                    "transformations",
                    "randomize",
                    "seed",
                    "sizescalefactor",
                    "locvamultiplier",
                    "locthoverfactor",
                    "shadowvalues");

            // Model
            putAll(Model.class,
                    "model");

            // Atmosphere
            putAll(Atmosphere.class,
                    "atmosphere");
            // Cloud
            putAll(Cloud.class,
                    "cloud");

            // RenderFlags
            putAll(RenderFlags.class,
                    "renderquad");

            // Machine
            putAll(Machine.class,
                    "machines");

            // Trajectory
            putAll(Trajectory.class,
                    "provider",
                    "orbit",
                    "model:Orbit",
                    "trail",
                    "newmethod");

            // Transform
            putAll(Transform.class,
                    "transformName",
                    "transformFunction",
                    "transformValues");

            // Fade
            putAll(Fade.class,
                    "fadein",
                    "fadeout",
                    "positionobjectname");

            // Label
            putAll(Label.class,
                    "label",
                    "label2d",
                    "labelposition");

            // RenderType
            putAll(RenderType.class,
                    "rendergroup");

            // BillboardDataset
            putAll(BillboardDatasets.class,
                    "data:BillboardGroup");

            // Title
            putAll(Title.class,
                    "scale:Text2D",
                    "lines:Text2D",
                    "align:Text2D");

            // Axis
            putAll(Axis.class,
                    "axesColors");

            // LocationMark
            putAll(LocationMark.class,
                    "location",
                    "distFactor");
        } else {
            logger.error("World is null, can't initialize attributes.");
        }
    }

    private void putAll(Class<? extends Component> clazz, String... attributes) {
        for (String attribute : attributes) {
            if(attributeMap.containsKey(attribute)) {
                logger.warn("Attribute already defined: " + attribute);
                throw new RuntimeException("Attribute already defined: " + attribute);
            } else {
                attributeMap.put(attribute, clazz);
            }
        }
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
                    int current = 0;
                    while (child != null) {
                        current++;
                        String clazzName = child.getString("impl");

                        @SuppressWarnings("unchecked") Class<Object> clazz = (Class<Object>) ClassReflection.forName(clazzName);
                        if (!archetypes.containsKey(clazzName)) {
                            // Do not know what to do
                            if (!loggedArchetypes.contains(clazzName)) {
                                logger.warn("Skipping " + clazz.getSimpleName() + ": no suitable archetype found.");
                                loggedArchetypes.add(clazzName);
                            }
                        } else {
                            // Create entity and fill it up
                            Archetype archetype = archetypes.get(clazzName);
                            Entity entity = world.createEntity(archetype);
                            fillEntity(child, entity, clazz.getSimpleName());
                        }

                        child = child.next;
                        EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, file.name(), (float) current / (float) count);
                    }
                    EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, file.name(), 2f);
                    logger.info(I18n.msg("notif.nodeloader", current, filePath));
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
    public void processJson(JsonValue json, Function6<Class<?>, Object, JsonValue, Boolean> function) throws ReflectionException {
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

                if (!set(comp, attribute.name, value)) {
                    // Basic attribute set did not work, try out setter method
                    return set(attribute, comp, value, valueClass, componentClass);
                }
                return true;
            } else {
                logger.warn("Component not found for attribute '" + attribute.name + "' and class '" + className + "'");
                return false;
            }
        });
        logger.debug(I18n.msg("notif.loading", className + ": " + entity.getComponent(Base.class).names[0]));
    }

    public String findAttribute(String attributeName, String className){
        String mixedKey = attributeName + ":" + className;
        if(attributeMap.containsKey(mixedKey)) {
            return mixedKey;
        } else if(attributeMap.containsKey(attributeName)) {
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
        Method m = searchMethod(methodName, valueClass, instanceClass);
        if (m != null) {
            try {
                m.invoke(instance, value);
            } catch (ReflectionException e) {
                throw new RuntimeException(e);
            }
            return true;
        } else {
            logger.error("ERROR: No method " + methodName + "(" + valueClass.getName() + ") in class " + instanceClass + " or its superclass/interfaces.");
            return false;
        }
    }

    /**
     * Searches for the given method with the given class. If none is found, it looks for fitting methods
     * with the class' interfaces and superclasses recursively.
     *
     * @param methodName    The method name.
     * @param parameterType The parameter class type.
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
