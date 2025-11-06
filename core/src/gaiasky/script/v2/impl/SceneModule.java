/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.data.orientation.QuaternionNlerpOrientationServer;
import gaiasky.data.orientation.QuaternionSlerpOrientationServer;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.beans.OrientationComboBoxBean;
import gaiasky.gui.beans.PrimitiveComboBoxBean;
import gaiasky.gui.beans.ShapeComboBoxBean;
import gaiasky.render.BlendMode;
import gaiasky.render.ComponentTypes;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Archetype;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.component.AttitudeComponent;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.entity.TrajectoryUtils;
import gaiasky.scene.record.ModelComponent;
import gaiasky.scene.view.FocusView;
import gaiasky.scene.view.VertsView;
import gaiasky.script.v2.api.SceneAPI;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Settings;
import gaiasky.util.coord.*;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;

import java.util.*;

/**
 * The scene module contains methods and calls that modify and query the internal scene in Gaia Sky.
 */
public class SceneModule extends APIModule implements IObserver, SceneAPI {

    /** Reference to the main {@link Scene} object in Gaia Sky. **/
    public Scene scene;

    /** Focus view. **/
    private final FocusView focusView;
    /** Verts view. **/
    private final VertsView vertsView;
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
        this.vertsView = new VertsView();

        // Subscribe to events.
        em.subscribe(this, Event.SCENE_LOADED);
    }

    @Override
    public FocusView get_object(String name) {
        return get_object(name, 0);
    }

    @Override
    public FocusView get_object(String name, double timeout) {
        if (api.validator.checkObjectName(name, timeout)) {
            Entity object = get_entity(name, timeout);
            return new FocusView(object);
        }
        return null;
    }

    @Override
    public VertsView get_line_object(String name) {
        return get_line_object(name, 0);
    }

    @Override
    public VertsView get_line_object(String name, double timeout) {
        if (api.validator.checkObjectName(name, timeout)) {
            Entity object = get_entity(name, timeout);
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
    public Entity get_entity(String name, double timeout) {
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
        if (timeout < 0) timeout = Double.MAX_VALUE;

        double startMs = System.currentTimeMillis();
        double elapsedSeconds = 0;
        while (obj == null && elapsedSeconds < timeout) {
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
    public double[] get_star_parameters(String name) {
        if (api.validator.checkObjectName(name)) {
            Entity entity = get_entity(name);
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
        if (api.validator.checkObjectName(name) && api.validator.checkDistanceUnits(units)) {
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
        if (api.validator.checkObjectName(name) && api.validator.checkDistanceUnits(units)) {
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
    public void set_object_posiiton(String name, double[] pos) {
        set_object_posiiton(name, pos, "internal");
    }

    @Override
    public void set_object_posiiton(String name, double[] pos, String units) {
        if (api.validator.checkObjectName(name)) {
            set_object_posiiton(get_object(name), pos, units);
        }

    }

    public void set_object_posiiton(String name, List<?> position) {
        set_object_posiiton(name, position, "internal");
    }

    public void set_object_posiiton(String name, List<?> position, String units) {
        set_object_posiiton(name, api.dArray(position), units);
    }

    @Override
    public void set_object_posiiton(FocusView object, double[] pos) {
        set_object_posiiton(object, pos, "internal");
    }

    @Override
    public void set_object_posiiton(FocusView object, double[] pos, String units) {
        set_object_posiiton(object.getEntity(), pos, units);
    }

    public void set_object_posiiton(FocusView object, List<?> position) {
        set_object_posiiton(object, position, "internal");
    }

    public void set_object_posiiton(FocusView object, List<?> position, String units) {
        set_object_posiiton(object, api.dArray(position), units);
    }

    @Override
    public void set_object_posiiton(Entity object, double[] pos) {
        set_object_posiiton(object, pos, "internal");
    }

    @Override
    public void set_object_posiiton(Entity object, double[] pos, String units) {
        if (api.validator.checkNotNull(object, "object")
                && api.validator.checkLength(pos, 3, "position")
                && api.validator.checkDistanceUnits(units)) {

            Settings.DistanceUnits u = Settings.DistanceUnits.valueOf(units.toUpperCase(Locale.ROOT));
            double[] posUnits = new double[]{u.toInternalUnits(pos[0]), u.toInternalUnits(pos[1]), u.toInternalUnits(pos[2])};

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
        if (api.validator.checkCtKeyNull(key)) {
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
        if (api.validator.checkCtKeyNull(key)) {
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
                        // Off-screen.
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
    public void set_object_size_scaling(String name, double factor) {
        if (api.validator.checkObjectName(name)) {
            Entity object = get_entity(name);
            set_object_size_scaling(object, factor);
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
    public void set_orbit_coordinates_scaling(String name, double factor) {
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
                        aoc.setScaling(factor);
                        modified++;
                    }
                } else {
                    aoc.setScaling(factor);
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
    public boolean set_object_quaternion_slerp_orientation(String name, String path) {
        return setObjectQuaternionOrientation(name, path, true);
    }

    @Override
    public boolean set_object_quaternion_nlerp_orientation(String name, String path) {
        return setObjectQuaternionOrientation(name, path, false);
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
    public void set_force_display_label(String name, boolean force) {
        if (api.validator.checkObjectName(name)) {
            Entity obj = get_entity(name);
            em.post(Event.FORCE_OBJECT_LABEL_CMD, this, obj, name, force);
        }
    }

    @Override
    public void set_label_include_regexp(String regexp) {
        if (api.validator.checkRegexp(regexp)) {
            em.post(Event.LABEL_INCLUDE_REGEX_CMD, this, regexp);
        }
    }

    @Override
    public void set_label_exclude_regexp(String regexp) {
        if (api.validator.checkRegexp(regexp)) {
            em.post(Event.LABEL_EXCLUDE_REGEX_CMD, this, regexp);
        }
    }

    @Override
    public void clear_label_filter_regexps() {
        em.post(Event.LABEL_CLEAR_FILTER_REGEX_CMD, this);
    }

    @Override
    public void set_mute_label(String name, boolean mute) {
        if (api.validator.checkObjectName(name)) {
            Entity obj = get_entity(name);
            em.post(Event.MUTE_OBJECT_LABEL_CMD, this, obj, name, mute);
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
    public boolean get_force_display_label(String name) {
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
    public void set_velocity_vectors_arrowheads(boolean enabled) {
        api.base.post_runnable(() -> EventManager.publish(Event.PM_ARROWHEADS_CMD, this, enabled));
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
    public void set_velocity_vector_max_number(long num) {
        Settings.settings.scene.star.group.numVelocityVector = (int) num;
    }

    @Override
    public void add_trajectory_line(String name, double[] points, double[] color) {
        var ignored = addLineObject(name, points, color, 1.5f, GL20.GL_LINE_STRIP, false, -1, "Orbit");
    }

    public void add_trajectory_line(String name, List<?> points, List<?> color) {
        var ignored = addLineObject(name, points, color, 1.5f, GL20.GL_LINE_STRIP, false, -1, "Orbit");
    }

    @Override
    public void add_trajectory_line(String name, double[] points, double[] color, double trail) {
        var entity = addLineObject(name, points, color, 1.5f, GL20.GL_LINE_STRIP, false, trail, "Orbit");
    }

    public void add_trajectory_line(String name, List<?> points, List<?> color, double trailMap) {
        add_trajectory_line(name, api.dArray(points), api.dArray(color), trailMap);
    }

    @Override
    public void add_polyline(String name, double[] points, double[] color) {
        add_polyline(name, points, color, 1f);
    }

    public void add_polyline(String name, List<?> points, List<?> color) {
        add_polyline(name, points, color, 1f);
    }

    @Override
    public void add_polyline(String name, double[] points, double[] color, double width) {
        add_polyline(name, points, color, width, false);
    }

    @Override
    public void add_polyline(String name, double[] points, double[] color, double width, boolean caps) {
        add_polyline(name, points, color, width, GL20.GL_LINE_STRIP, caps);
    }

    @Override
    public void add_polyline(String name, double[] points, double[] color, double width, int primitive) {
        add_polyline(name, points, color, width, primitive, false);
    }

    @Override
    public void add_polyline(String name, double[] points, double[] color, double width, int primitive, boolean caps) {
        addLineObject(name, points, color, width, primitive, caps, -1f, "Polyline");
    }

    Entity addLineObject(String name,
                         List<?> points,
                         List<?> color,
                         double lineWidth,
                         int primitive,
                         boolean arrowCaps,
                         double trailMap,
                         String archetypeName) {
        return addLineObject(name, api.dArray(points), api.dArray(color), lineWidth, primitive, arrowCaps, trailMap, archetypeName);
    }

    Entity addLineObject(String name,
                         double[] points,
                         double[] color,
                         double lineWidth,
                         int primitive,
                         boolean arrowCaps,
                         double trailMap,
                         String archetypeName) {
        if (api.validator.checkString(name, "name") && api.validator.checkNum(lineWidth, 0.1f, 50f, "lineWidth") && api.validator.checkNum(primitive,
                                                                                                                                           1,
                                                                                                                                           3,
                                                                                                                                           "primitive")) {
            var archetype = scene.archetypes().get(archetypeName);
            var entity = archetype.createEntity();

            var base = Mapper.base.get(entity);
            base.setName(name);
            base.setComponentType(ComponentTypes.ComponentType.Orbits);

            var body = Mapper.body.get(entity);
            body.setColor(color);
            body.setLabelColor(color);
            body.setSizePc(100d);

            var line = Mapper.line.get(entity);
            line.lineWidth = (float) lineWidth;

            var arrow = Mapper.arrow.get(entity);
            arrow.arrowCap = arrowCaps;

            synchronized (vertsView) {
                vertsView.setEntity(entity);
                vertsView.setPrimitiveSize((float) lineWidth);
                vertsView.setPoints(points);
                vertsView.setRenderGroup(arrowCaps ? RenderGroup.LINE : RenderGroup.LINE_GPU);
                vertsView.setClosedLoop(false);
                vertsView.setGlPrimitive(primitive);
            }

            var trajectory = Mapper.trajectory.get(entity);
            if (trajectory != null) {
                if (trailMap < 0) {
                    // Trail disabled.
                    trajectory.orbitTrail = false;
                    trajectory.setTrailMap(trailMap);
                } else {
                    trailMap = MathUtilsDouble.clamp(trailMap, 0.0, 1.0);
                    trajectory.orbitTrail = true;
                    trajectory.setTrailMap(trailMap);
                }
            }

            var graph = Mapper.graph.get(entity);
            graph.setParent(Scene.ROOT_NAME);

            scene.initializeEntity(entity);
            scene.setUpEntity(entity);

            em.post(Event.SCENE_ADD_OBJECT_CMD, this, entity, true);

            return entity;
        }
        return null;
    }

    public void add_polyline(String name, double[] points, double[] color, int lineWidth) {
        add_polyline(name, points, color, (float) lineWidth);
    }

    public void add_polyline(String name, double[] points, double[] color, int lineWidth, int primitive) {
        add_polyline(name, points, color, (float) lineWidth, primitive);
    }

    public void add_polyline(String name, List<?> points, List<?> color, float lineWidth) {
        add_polyline(name, api.dArray(points), api.dArray(color), lineWidth);
    }

    public void add_polyline(String name, List<?> points, List<?> color, float lineWidth, boolean arrowCaps) {
        add_polyline(name, api.dArray(points), api.dArray(color), lineWidth, arrowCaps);
    }

    public void add_polyline(String name, List<?> points, List<?> color, float lineWidth, int primitive) {
        add_polyline(name, api.dArray(points), api.dArray(color), lineWidth, primitive);
    }

    public void add_polyline(String name, List<?> points, List<?> color, float lineWidth, int primitive, boolean arrowCaps) {
        add_polyline(name, api.dArray(points), api.dArray(color), lineWidth, primitive, arrowCaps);
    }

    public void add_polyline(String name, List<?> points, List<?> color, int lineWidth) {
        add_polyline(name, points, color, (float) lineWidth);
    }

    public void add_polyline(String name, List<?> points, List<?> color, int lineWidth, boolean arrowCaps) {
        add_polyline(name, points, color, (float) lineWidth, arrowCaps);
    }

    public void add_polyline(String name, List<?> points, List<?> color, int lineWidth, int primitive) {
        add_polyline(name, points, color, (float) lineWidth, primitive);
    }

    public void add_polyline(String name, List<?> points, List<?> color, int lineWidth, int primitive, boolean arrowCaps) {
        add_polyline(name, points, color, (float) lineWidth, primitive, arrowCaps);
    }

    @Override
    public void remove_object(String name) {
        if (api.validator.checkString(name, "name")) {
            em.post(Event.SCENE_REMOVE_OBJECT_CMD, this, name, true);
        }
    }

    @Override
    public void add_shape_around_object(String name,
                                        String shapeType,
                                        String primitive,
                                        double size,
                                        String obj_name,
                                        float r,
                                        float g,
                                        float b,
                                        float a,
                                        boolean label,
                                        boolean track) {
        add_shape_around_object(name,
                                shapeType,
                                primitive,
                                OrientationComboBoxBean.ShapeOrientation.EQUATORIAL.toString(),
                                size,
                                obj_name,
                                r,
                                g,
                                b,
                                a,
                                label,
                                track);
    }

    @Override
    public void add_shape_around_object(String name,
                                        String shapeType,
                                        String primitive,
                                        String ori,
                                        double size,
                                        String obj_name,
                                        float r,
                                        float g,
                                        float b,
                                        float a,
                                        boolean label,
                                        boolean track) {
        if (api.validator.checkString(name, "shapeName")
                && api.validator.checkStringEnum(shapeType, ShapeComboBoxBean.Shape.class, "shape")
                && api.validator.checkStringEnum(primitive,
                                                 PrimitiveComboBoxBean.Primitive.class,
                                                 "primitive")
                && api.validator.checkStringEnum(
                ori,
                OrientationComboBoxBean.ShapeOrientation.class,
                "orientation")
                && api.validator.checkNum(size, 0, Double.MAX_VALUE, "size")
                && api.validator.checkObjectName(obj_name)) {
            final var shapeLc = shapeType.toLowerCase(Locale.ROOT);
            api.base.post_runnable(() -> {
                Entity trackingObject = get_focus_entity(obj_name);
                float[] color = new float[]{r, g, b, a};
                int primitiveInt = PrimitiveComboBoxBean.Primitive.valueOf(primitive.toUpperCase(Locale.ROOT))
                        .equals(PrimitiveComboBoxBean.Primitive.LINES) ? GL20.GL_LINES : GL20.GL_TRIANGLES;
                // Create shape
                Archetype at = scene.archetypes().get("ShapeObject");
                Entity newShape = at.createEntity();

                var base = Mapper.base.get(newShape);
                base.setName(name.trim());
                base.ct = new ComponentTypes(ComponentTypes.ComponentType.Others.ordinal());

                var body = Mapper.body.get(newShape);
                body.setColor(color);
                body.setLabelColor(new float[]{r, g, b, a});
                body.size = (float) (size * Constants.KM_TO_U);

                var graph = Mapper.graph.get(newShape);
                graph.setParent(Scene.ROOT_NAME);

                var focus = Mapper.focus.get(newShape);
                focus.focusable = false;

                var shape = Mapper.shape.get(newShape);
                if (track) {
                    shape.track = new FocusView(trackingObject);
                } else {
                    body.setPos(EntityUtils.getAbsolutePosition(trackingObject, obj_name, new Vector3Q()));
                }
                shape.trackName = obj_name;

                var trf = Mapper.transform.get(newShape);
                var m = new Matrix4();
                var orient = OrientationComboBoxBean.ShapeOrientation.valueOf(ori.toUpperCase(Locale.ROOT));
                switch (orient) {
                    case CAMERA -> {
                        var camera = GaiaSky.instance.cameraManager.getCamera();
                        m.rotateTowardDirection(camera.direction, camera.up);
                    }
                    case EQUATORIAL -> m.idt();
                    case ECLIPTIC -> m.set(Coordinates.eclipticToEquatorialF());
                    case GALACTIC -> m.set(Coordinates.galacticToEquatorialF());
                }
                trf.setTransformMatrix(m);

                Map<String, Object> params = new HashMap<>();
                params.put("quality", 25L);
                params.put("divisions", shapeLc.equalsIgnoreCase(ShapeComboBoxBean.Shape.OCTAHEDRONSPHERE.toString()) ? 3L : 35L);
                params.put("recursion", 3L);
                params.put("diameter", 1.0);
                params.put("width", 1.0);
                params.put("height", 1.0);
                params.put("depth", 1.0);
                params.put("innerradius", 0.6);
                params.put("outerradius", 1.0);
                params.put("sphere-in-ring", false);
                params.put("flip", false);

                var model = Mapper.model.get(newShape);
                model.model = new ModelComponent();
                model.model.type = shapeLc;
                model.model.setPrimitiveType(primitiveInt);
                model.model.setParams(params);
                model.model.setStaticLight(true);
                model.model.setUseColor(true);
                model.model.setBlendMode(BlendMode.ADDITIVE);
                model.model.setCulling(false);

                var rt = Mapper.render.get(newShape);
                rt.renderGroup = RenderGroup.MODEL_VERT_ADDITIVE;

                // Initialize shape.
                scene.initializeEntity(newShape);
                scene.setUpEntity(newShape);

                // Add to scene.
                EventManager.publish(Event.SCENE_ADD_OBJECT_NO_POST_CMD, this, newShape, false);
            });
        }
    }


    @Override
    public void notify(Event event, Object source, Object... data) {
        if (Objects.requireNonNull(event) == Event.SCENE_LOADED) {
            this.scene = (Scene) data[0];
            this.focusView.setScene(this.scene);
        }
    }
}
