/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.data.orientation.QuaternionNlerpOrientationServer;
import gaiasky.data.orientation.QuaternionSlerpOrientationServer;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.component.AttitudeComponent;
import gaiasky.scene.entity.TrajectoryUtils;
import gaiasky.scene.view.FocusView;
import gaiasky.scene.view.VertsView;
import gaiasky.script.v2.api.SceneAPI;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Settings;
import gaiasky.util.coord.AbstractOrbitCoordinates;
import gaiasky.util.coord.IBodyCoordinates;
import gaiasky.util.coord.IPythonCoordinatesProvider;
import gaiasky.util.coord.PythonBodyCoordinates;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The scene module contains methods and calls that modify and query the internal scene in Gaia Sky.
 */
public class SceneModule extends APIModule implements IObserver, SceneAPI {

    /** Reference to the main {@link Scene} object in Gaia Sky. **/
    private Scene scene;

    /** Focus view. **/
    private final FocusView focusView;
    /** Utilities to deal with trajectories. **/
    private TrajectoryUtils trajectoryUtils;

    /** Auxiliary vector. **/
    private final Vector3Q aux3b1 = new Vector3Q();

    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public SceneModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
        this.focusView = new FocusView();

        // Subscribe to events.
        em.subscribe(this, Event.SCENE_LOADED);
    }

    @Override
    public FocusView get_object(String name) {
        return get_object(name, 0);
    }

    @Override
    public FocusView get_object(String name, double timeOutSeconds) {
        if (api.validator.checkObjectName(name, timeOutSeconds)) {
            Entity object = get_entity(name, timeOutSeconds);
            return new FocusView(object);
        }
        return null;
    }

    @Override
    public VertsView get_line_object(String name) {
        return get_line_object(name, 0);
    }

    @Override
    public VertsView get_line_object(String name, double timeOutSeconds) {
        if (api.validator.checkObjectName(name, timeOutSeconds)) {
            Entity object = get_entity(name, timeOutSeconds);
            if (Mapper.verts.has(object)) {
                return new VertsView(object);
            } else {
                logger.error(name + " is not a verts object.");
                return null;
            }
        }
        return null;
    }

    @Override
    public Entity get_entity(String name) {
        return get_entity(name, 0);
    }

    @Override
    public Entity get_entity(String name, double timeOutSeconds) {
        Entity obj = scene.getEntity(name);
        if (obj == null) {
            if (name.matches("[0-9]+")) {
                // Check with 'HIP '
                obj = scene.getEntity("hip " + name);
            } else if (name.matches("hip [0-9]+")) {
                obj = scene.getEntity(name.substring(4));
            }
        }

        // If negative, no limit in waiting
        if (timeOutSeconds < 0) timeOutSeconds = Double.MAX_VALUE;

        double startMs = System.currentTimeMillis();
        double elapsedSeconds = 0;
        while (obj == null && elapsedSeconds < timeOutSeconds) {
            api.base.sleep_frames(1);
            obj = scene.getEntity(name);
            elapsedSeconds = (System.currentTimeMillis() - startMs) / 1000d;
        }
        return obj;
    }

    @Override
    public Entity get_focus(String name) {
        return scene.findFocus(name);
    }

    /**
     * Alias to {@link #get_focus(String)}.
     */
    public Entity get_focus_entity(String name) {
        return get_focus(name);
    }

    @Override
    public double[] get_star_parameters(String id) {
        if (api.validator.checkObjectName(id)) {
            Entity entity = get_entity(id);
            if (Mapper.starSet.has(entity)) {
                var set = Mapper.starSet.get(entity);
                // This star group contains the star
                IParticleRecord sb = set.getCandidateBean();
                if (sb != null) {
                    double[] rgb = sb.rgb();
                    return new double[]{sb.ra(), sb.dec(), sb.parallax(), sb.muAlpha(), sb.muDelta(), sb.radVel(), sb.appMag(), rgb[0], rgb[1], rgb[2]};
                }
            }
        }
        return null;
    }

    @Override
    public double[] get_object_position(String name) {
        return get_object_position(name, "internal");
    }

    @Override
    public double[] get_object_position(String name, String units) {
        if (api.validator.checkObjectName(name) && api.validator.checkDistanceUnits(units, "units")) {
            Entity entity = get_entity(name);
            focusView.setEntity(entity);
            focusView.getFocus(name);
            focusView.getAbsolutePosition(name, aux3b1);
            Settings.DistanceUnits u = Settings.DistanceUnits.valueOf(units.toUpperCase(Locale.ROOT));
            return new double[]{u.fromInternalUnits(aux3b1.x.doubleValue()), u.fromInternalUnits(aux3b1.y.doubleValue()), u.fromInternalUnits(aux3b1.z.doubleValue())};
        }
        return null;
    }

    @Override
    public double[] get_object_predicted_position(String name) {
        return get_object_predicted_position(name, "internal");
    }

    @Override
    public double[] get_object_predicted_position(String name, String units) {
        if (api.validator.checkObjectName(name) && api.validator.checkDistanceUnits(units, "units")) {
            Entity entity = get_entity(name);
            focusView.setEntity(entity);
            focusView.getFocus(name);
            focusView.getPredictedPosition(aux3b1, GaiaSky.instance.time, GaiaSky.instance.getICamera(), false);
            Settings.DistanceUnits u = Settings.DistanceUnits.valueOf(units.toUpperCase(Locale.ROOT));
            return new double[]{u.fromInternalUnits(aux3b1.x.doubleValue()), u.fromInternalUnits(aux3b1.y.doubleValue()), u.fromInternalUnits(aux3b1.z.doubleValue())};
        }
        return null;
    }

    @Override
    public void set_object_posiiton(String name, double[] position) {
        set_object_posiiton(name, position, "internal");
    }

    @Override
    public void set_object_posiiton(String name, double[] position, String units) {
        if (api.validator.checkObjectName(name)) {
            set_object_posiiton(get_object(name), position, units);
        }

    }

    public void set_object_posiiton(String name, List<?> position) {
        set_object_posiiton(name, position, "internal");
    }

    public void set_object_posiiton(String name, List<?> position, String units) {
        set_object_posiiton(name, api.dArray(position), units);
    }

    @Override
    public void set_object_posiiton(FocusView object, double[] position) {
        set_object_posiiton(object, position, "internal");
    }

    @Override
    public void set_object_posiiton(FocusView object, double[] position, String units) {
        set_object_posiiton(object.getEntity(), position, units);
    }

    public void set_object_posiiton(FocusView object, List<?> position) {
        set_object_posiiton(object, position, "internal");
    }

    public void set_object_posiiton(FocusView object, List<?> position, String units) {
        set_object_posiiton(object, api.dArray(position), units);
    }

    @Override
    public void set_object_posiiton(Entity object, double[] position) {
        set_object_posiiton(object, position, "internal");
    }

    @Override
    public void set_object_posiiton(Entity object, double[] position, String units) {
        if (api.validator.checkNotNull(object, "object")
                && api.validator.checkLength(position, 3, "position")
                && api.validator.checkDistanceUnits(units)) {

            Settings.DistanceUnits u = Settings.DistanceUnits.valueOf(units.toUpperCase(Locale.ROOT));
            double[] posUnits = new double[]{u.toInternalUnits(position[0]), u.toInternalUnits(position[1]), u.toInternalUnits(position[2])};

            var body = Mapper.body.get(object);
            body.pos.set(posUnits);
            body.positionSetInScript = true;
        }
    }

    @Override
    public void set_object_coordinates_provider(String name, IPythonCoordinatesProvider provider) {
        if (api.validator.checkObjectName(name) && api.validator.checkNotNull(provider, "provider")) {
            var object = get_object(name);
            if (api.validator.checkNotNull(object.getEntity(), "object")) {
                var coord = Mapper.coordinates.get(object.getEntity());
                if (api.validator.checkNotNull(coord, "coordinates")) {
                    IBodyCoordinates coordinates = new PythonBodyCoordinates(provider);
                    api.base.post_runnable(() -> {
                        coord.coordinates = coordinates;
                        GaiaSky.instance.touchSceneGraph();
                    });
                }
            }
        }

    }

    @Override
    public void remove_object_coordinates_provider(String name) {
        if (api.validator.checkObjectName(name)) {
            var object = get_object(name);
            if (api.validator.checkNotNull(object.getEntity(), "object")) {
                var coord = Mapper.coordinates.get(object.getEntity());
                if (api.validator.checkNotNull(coord, "coordinates")) {
                    api.base.post_runnable(() -> coord.coordinates = null);
                }

            }
        }

    }

    public void set_object_posiiton(Entity object, List<?> position) {
        set_object_posiiton(object, api.dArray(position));
    }

    @Override
    public void set_component_type_visibility(String key, boolean visible) {
        if (checkCtKeyNull(key)) {
            logger.error("Element '" + key + "' does not exist. Possible values are:");
            ComponentTypes.ComponentType[] cts = ComponentTypes.ComponentType.values();
            for (ComponentTypes.ComponentType ct : cts)
                logger.error(ct.key);
        } else {
            api.base.post_runnable(() -> em.post(Event.TOGGLE_VISIBILITY_CMD, this, key, visible));
        }
    }

    @Override
    public boolean get_component_type_visibility(String key) {
        if (checkCtKeyNull(key)) {
            logger.error("Element '" + key + "' does not exist. Possible values are:");
            ComponentTypes.ComponentType[] cts = ComponentTypes.ComponentType.values();
            for (ComponentTypes.ComponentType ct : cts)
                logger.error(ct.key);
            return false;
        } else {
            ComponentTypes.ComponentType ct = ComponentTypes.ComponentType.getFromKey(key);
            return Settings.settings.scene.visibility.get(ct.key);
        }
    }

    @Override
    public double[] get_object_screen_coordinates(String name) {
        if (api.validator.checkObjectName(name)) {
            var entity = get_entity(name);
            var camera = GaiaSky.instance.cameraManager;
            Vector3 posFloat = new Vector3();
            Vector3D posDouble = new Vector3D();
            Vector3Q posPrec = new Vector3Q();
            synchronized (focusView) {
                focusView.setEntity(entity);
                focusView.getAbsolutePosition(name, posPrec);
                posPrec.add(camera.getInversePos());
                posPrec.put(posFloat);
                posPrec.put(posDouble);
                if (camera.getCamera().direction.dot(posFloat) > 0) {
                    camera.getCamera().project(posFloat);
                    if (posFloat.x < 0 || posFloat.x > Gdx.graphics.getWidth() || posFloat.y < 0 || posFloat.y > Gdx.graphics.getHeight()) {
                        // Off screen.
                        return null;
                    }
                    return new double[]{posFloat.x, posFloat.y};
                }
            }
        }
        return null;
    }

    @Override
    public boolean set_object_visibility(String name, boolean visible) {
        if (api.validator.checkObjectName(name)) {
            Entity obj = get_entity(name);

            api.base.post_runnable(() -> EventManager.publish(Event.PER_OBJECT_VISIBILITY_CMD, this, obj, name, visible));
            return true;
        }
        return false;
    }

    @Override
    public boolean get_object_visibility(String name) {
        if (api.validator.checkObjectName(name)) {
            Entity obj = get_entity(name);

            boolean visible;
            synchronized (focusView) {
                focusView.setEntity(obj);
                visible = focusView.isVisible(name.toLowerCase(Locale.ROOT).strip());
            }
            return visible;
        }
        return false;
    }

    @Override
    public void set_object_size_scaling(String name, double scalingFactor) {
        if (api.validator.checkObjectName(name)) {
            Entity object = get_entity(name);
            set_object_size_scaling(object, scalingFactor);
        }
    }

    /**
     * Version of {@link #set_object_size_scaling(String, double)} but getting an {@link Entity} reference instead of a
     * name.
     *
     * @param object        The {@link Entity}.
     * @param scalingFactor The scaling factor.
     */
    public void set_object_size_scaling(Entity object, double scalingFactor) {
        if (api.validator.checkNotNull(object, "Entity")) {
            if (Mapper.modelScaffolding.has(object)) {
                var scaffolding = Mapper.modelScaffolding.get(object);
                scaffolding.setSizeScaleFactor(scalingFactor);
            } else if (Mapper.trajectory.has(object)) {
                var trajectory = Mapper.trajectory.get(object);
                trajectory.params.multiplier = scalingFactor;
            } else {
                var base = Mapper.base.get(object);
                logger.error("Object '" + base.getName() + "' is not a model or trajectory object");
            }
        }
    }

    @Override
    public void set_orbit_coordinates_scaling(String name, double scalingFactor) {
        int modified = 0;
        String className, objectName;
        if (name.contains(":")) {
            int idx = name.indexOf(":");
            className = name.substring(0, idx);
            objectName = name.substring(idx + 1);
        } else {
            className = name;
            objectName = null;
        }
        // Coordinates provider.
        List<AbstractOrbitCoordinates> aocs = AbstractOrbitCoordinates.getInstances();
        for (AbstractOrbitCoordinates aoc : aocs) {
            if (aoc.getClass().getSimpleName().equalsIgnoreCase(className)) {
                if (objectName != null) {
                    if (aoc.getOrbitName() != null && aoc.getOrbitName().contains(objectName)) {
                        aoc.setScaling(scalingFactor);
                        modified++;
                    }
                } else {
                    aoc.setScaling(scalingFactor);
                    modified++;
                }
            }
        }

        logger.info(name + ": modified scaling of " + modified + " orbits");
    }

    private void initializeTrajectoryUtils() {
        if (trajectoryUtils == null) {
            trajectoryUtils = new TrajectoryUtils();
        }
    }

    @Override
    public void refresh_all_orbits() {
        initializeTrajectoryUtils();
        api.base.post_runnable(() -> {
            var orbits = scene.findEntitiesByFamily(scene.getFamilies().orbits);
            for (Entity orbit : orbits) {
                var trajectory = Mapper.trajectory.get(orbit);
                var verts = Mapper.verts.get(orbit);
                trajectoryUtils.refreshOrbit(trajectory, verts, true);
            }
        });
    }

    @Override
    public void force_update_scene() {
        api.base.post_runnable(() -> em.post(Event.SCENE_FORCE_UPDATE, this));
    }

    public void refresh_object_orbit(String name) {
        String orbitName = name + " orbit";
        if (api.validator.checkObjectName(orbitName)) {
            FocusView view = get_object(orbitName);
            var trajectory = Mapper.trajectory.get(view.getEntity());
            var verts = Mapper.verts.get(view.getEntity());
            if (trajectory != null && verts != null) {
                initializeTrajectoryUtils();
                api.base.post_runnable(() -> {
                    trajectoryUtils.refreshOrbit(trajectory, verts, true);
                });
            }
        }
    }

    @Override
    public double get_object_radius(String name) {
        if (api.validator.checkObjectName(name)) {
            Entity object = scene.findFocus(name);

            focusView.setEntity(object);
            focusView.getFocus(name);
            return focusView.getRadius() * Constants.U_TO_KM;
        }
        return -1;
    }


    private boolean setObjectQuaternionOrientation(String name, String file, boolean slerp) {
        if (api.validator.checkObjectName(name)) {
            var entity = get_object(name);
            if (Mapper.orientation.has(entity.getEntity())) {
                var orientation = Mapper.orientation.get(entity.getEntity());

                api.base.post_runnable(() -> {

                    try {
                        // Set new quaternion orientation.
                        var quatOri = new AttitudeComponent();
                        if (slerp) {
                            quatOri.orientationServer = new QuaternionSlerpOrientationServer(file);
                        } else {
                            quatOri.orientationServer = new QuaternionNlerpOrientationServer(file);
                        }
                        orientation.attitudeComponent = quatOri;
                    } catch (Exception e) {
                        logger.error(e);
                    } finally {
                        // Remove rigid rotation, if it has one.
                        if (orientation.rotationComponent != null) {
                            orientation.rotationComponent = null;
                        }
                    }
                });
                return true;
            }
        }
        return false;

    }

    @Override
    public boolean set_object_quaternion_slerp_orientation(String name, String file) {
        return setObjectQuaternionOrientation(name, file, true);
    }

    @Override
    public boolean set_object_quaternion_nlerp_orientation(String name, String file) {
        return setObjectQuaternionOrientation(name, file, false);
    }

    @Override
    public void set_label_size_factor(float factor) {
        if (api.validator.checkNum(factor, Constants.MIN_LABEL_SIZE, Constants.MAX_LABEL_SIZE, "labelSizeFactor")) {
            api.base.post_runnable(() -> em.post(Event.LABEL_SIZE_CMD, this, factor));
        }
    }

    public void set_label_size_factor(int factor) {
        set_label_size_factor((float) factor);
    }

    @Override
    public void set_force_display_label(String name, boolean forceLabel) {
        if (api.validator.checkObjectName(name)) {
            Entity obj = get_entity(name);
            em.post(Event.FORCE_OBJECT_LABEL_CMD, this, obj, name, forceLabel);
        }
    }

    @Override
    public void set_label_color(String name, double[] color) {
        if (api.validator.checkObjectName(name)) {
            Entity obj = get_entity(name);
            em.post(Event.LABEL_COLOR_CMD, this, obj, name, GlobalResources.toFloatArray(color));
        }
    }

    public void set_label_color(String name, final List<?> color) {
        set_label_color(name, api.dArray(color));
    }

    @Override
    public boolean get_force_dispaly_label(String name) {
        if (api.validator.checkFocusName(name)) {
            Entity obj = get_entity(name);

            boolean ret;
            synchronized (focusView) {
                focusView.setEntity(obj);
                ret = focusView.isForceLabel(name);
            }
            return ret;
        }
        return false;
    }

    @Override
    public void set_line_width_factor(final float factor) {
        if (api.validator.checkNum(factor, Constants.MIN_LINE_WIDTH, Constants.MAX_LINE_WIDTH, "lineWidthFactor")) {
            api.base.post_runnable(() -> em.post(Event.LINE_WIDTH_CMD, this, factor));
        }
    }

    public void set_line_width_factor(int factor) {
        set_line_width_factor((float) factor);
    }

    private boolean checkCtKeyNull(String key) {
        ComponentTypes.ComponentType ct = ComponentTypes.ComponentType.getFromKey(key);
        return ct == null;
    }

    @Override
    public void set_velocity_vectors_number_factor(float factor) {
        api.base.post_runnable(() -> EventManager.publish(Event.PM_NUM_FACTOR_CMD,
                                                          this,
                                                          MathUtilsDouble.lint(factor,
                                                                               Constants.MIN_SLIDER,
                                                                               Constants.MAX_SLIDER,
                                                                               Constants.MIN_PM_NUM_FACTOR,
                                                                               Constants.MAX_PM_NUM_FACTOR)));
    }

    /**
     * Alias to {@link #set_velocity_vectors_number_factor(float)}.
     */
    public void set_velocity_vectors_number_factor(int factor) {
        set_velocity_vectors_number_factor((float) factor);
    }

    /**
     * Unfiltered version of {@link #set_velocity_vectors_number_factor(float)}. In this version, the user can submit a factor that extends
     * beyond the bounds of [{@link Constants#MIN_PM_NUM_FACTOR}, {@link Constants#MAX_PM_NUM_FACTOR}].
     *
     * @param factor The factor to set.
     */
    public void set_unfiltered_velocity_vectors_number_factor(float factor) {
        Settings.settings.scene.properMotion.number = factor;
    }

    @Override
    public void set_velocity_vectors_color_mode(int mode) {
        api.base.post_runnable(() -> EventManager.publish(Event.PM_COLOR_MODE_CMD, this, mode % 6));
    }

    @Override
    public void set_velocity_vectors_arrowheads(boolean arrowheadsEnabled) {
        api.base.post_runnable(() -> EventManager.publish(Event.PM_ARROWHEADS_CMD, this, arrowheadsEnabled));
    }

    @Override
    public void set_velocity_vectors_length_factor(float factor) {
        api.base.post_runnable(() -> EventManager.publish(Event.PM_LEN_FACTOR_CMD, this, factor));
    }

    /**
     * Alias to {@link #set_velocity_vectors_length_factor(float)}.
     */
    public void set_velocity_vectors_length_factor(int factor) {
        set_velocity_vectors_length_factor((float) factor);
    }

    @Override
    public long get_velocity_vector_max_number() {
        return Settings.settings.scene.star.group.numVelocityVector;
    }

    @Override
    public void set_velocity_vector_max_number(long maxNumber) {
        Settings.settings.scene.star.group.numVelocityVector = (int) maxNumber;
    }


    @Override
    public void notify(Event event, Object source, Object... data) {
        if (Objects.requireNonNull(event) == Event.SCENE_LOADED) {
            this.scene = (Scene) data[0];
            this.focusView.setScene(this.scene);
        }
    }
}
