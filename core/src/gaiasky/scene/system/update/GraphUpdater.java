/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.Gdx;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.*;
import gaiasky.scene.component.tag.TagNoProcess;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.view.SpacecraftView;
import gaiasky.util.Logger;
import gaiasky.util.Nature;
import gaiasky.util.Settings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.math.Vector3D;
import gaiasky.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class GraphUpdater extends AbstractUpdateSystem {
    private static final Logger.Log logger = Logger.getLogger(GraphUpdater.class);
    private final ITimeFrameProvider time;
    int processed = 0, lastProcessed;
    private ICamera camera;
    private final Vector3D D31;
    private final Vector3Q B31;
    private final SpacecraftView view;

    /**
     * Instantiates a system that will iterate over the entities described by the Family.
     *
     * @param family The family of entities iterated over in this System. In this case, it should be just one ({@link GraphRoot}.
     */
    public GraphUpdater(Family family,
                        int priority,
                        ITimeFrameProvider time) {
        super(family, priority);
        this.time = time;
        this.D31 = new Vector3D();
        this.B31 = new Vector3Q();
        this.view = new SpacecraftView();
    }

    public void setCamera(ICamera camera) {
        this.camera = camera;
    }

    protected void processEntity(Entity entity,
                                 float deltaTime) {
        processed = 0;
        updateEntity(entity, deltaTime);
        if (lastProcessed != processed) {
            logger.debug("Number of nodes (new): " + processed);
            lastProcessed = processed;
            printTree(entity, " ", 0, new AtomicInteger(1));
        }
    }

    @Override
    public void updateEntity(Entity entity,
                             float deltaTime) {
        // This runs the root node
        var root = entity.getComponent(GraphNode.class);

        root.translation.set(camera.getInversePos());
        update(entity, time, null, 1);
    }

    public void printTree(Entity entity,
                          String tab,
                          int level,
                          AtomicInteger count) {
        var graph = Mapper.graph.get(entity);

        if (graph.mustUpdateFunction == null ||
                graph.mustUpdateFunction.apply(this, entity, graph)) {

            var base = Mapper.base.get(entity);

            logger.debug(count.getAndIncrement() + "|" + level + ":" + tab + base.getName()
                                 + " (" + (graph.children != null ? graph.children.size : 0) + ")"
                                 + (base.archetype != null ? " [" + base.archetype.getName() + "]" : ""));

            boolean processChildren = !Mapper.tagNoProcessChildren.has(entity);
            if (processChildren && graph.children != null) {
                // Go down a level
                for (int i = 0; i < graph.children.size; i++) {
                    Entity child = graph.children.get(i);
                    printTree(child, tab + "  ", level + 1, count);
                }
            }
        }
    }

    public void update(Entity entity,
                       ITimeFrameProvider time,
                       final Vector3Q parentTranslation,
                       float opacity) {
        processed++;
        var graph = Mapper.graph.get(entity);

        if (graph.mustUpdateFunction == null ||
                graph.mustUpdateFunction.apply(this, entity, graph)) {

            var base = Mapper.base.get(entity);
            var body = Mapper.body.get(entity);
            var fade = Mapper.fade.get(entity);

            // Update rotation.
            if (time.getHdiff() != 0) {
                var orientation = Mapper.orientation.get(entity);
                if (orientation != null && orientation.rotationComponent != null) {
                    orientation.rotationComponent.update(time);
                }
            }

            // Update position.
            if (graph.positionUpdaterConsumer != null) {
                graph.positionUpdaterConsumer.apply(this, entity, body, graph);
            }

            // Update translation.
            graph.translation.set(parentTranslation).add(body.pos);

            // Update position in spherical coordinates.
            B31.set(D31.set(graph.translation).add(camera.getPos()));
            gaiasky.util.coord.Coordinates.cartesianToSpherical(B31, D31);
            body.posSph.set((float) (Nature.TO_DEG * D31.x), (float) (Nature.TO_DEG * D31.y));

            // Update opacity.
            if (fade != null && (fade.fadeIn != null || fade.fadeOut != null)) {
                // If the bottom part of our fade in is mapped to a value
                // greater than zero, we do not use the parent opacity; the
                // children's fadeInMap attribute takes precedence.
                // This is so that the NEARGALCAT billboards are shown from within the MW.
                if (fade.fadeInMap == null || fade.fadeInMap.x <= 0) {
                    base.opacity = opacity;
                } else {
                    // Use parent visibility, if available.
                    if (graph.parent != null) {
                        var parentBase = Mapper.base.get(graph.parent);
                        if (parentBase.isVisible()) {
                            base.opacity = parentBase.getVisibilityOpacityFactor();
                        } else {
                            base.opacity = 0;
                        }
                    } else {
                        base.opacity = 1;
                    }
                }
                updateFadeDistance(entity, body, fade);
                updateFadeOpacity(base, fade);
            } else {
                base.opacity = opacity;
            }
            base.opacity *= base.getVisibilityOpacityFactor();

            // Update supporting attributes
            body.distToCamera = graph.translation.lenDouble();
            if (Mapper.extra.has(entity)) {
                // Particles have a special algorithm for the solid angles.
                body.solidAngle = (Mapper.extra.get(entity).radius / body.distToCamera);
                body.solidAngleApparent = body.solidAngle * Settings.settings.scene.star.brightness / camera.getFovFactor();
            } else {
                // Regular objects.
                // Take into account size of model objects.
                var model = Mapper.model.get(entity);
                double modelSize = model != null ? model.modelSize : 1.0;
                body.solidAngle = FastMath.atan(body.size * modelSize / body.distToCamera);
                body.solidAngleApparent = body.solidAngle / camera.getFovFactor();
            }

            var ds = Mapper.datasetDescription.get(entity);
            // Some elements (orbital element sets, octrees) process their own children.
            // But: not in the case of a copy!
            boolean processChildren = Mapper.tagCopy.has(entity) || !(Mapper.tagNoProcessChildren.has(entity) || (ds != null && !GaiaSky.instance.isOn(base.ct)));
            if (processChildren && graph.children != null && opacity > 0) {
                // Go down a level
                for (int i = 0; i < graph.children.size; i++) {
                    Entity child = graph.children.get(i);
                    update(child, time, graph.translation, getChildrenOpacity(entity, child, base, fade, opacity));
                }
            }
        }
    }

    private float getChildrenOpacity(Entity entity,
                                     Entity child,
                                     Base base,
                                     Fade fade,
                                     float opacity) {
        if (Mapper.billboardSet.has(entity) && Mapper.tagBackgroundModel.has(child)) {
            return 1 - base.opacity;
        } else if (fade != null) {
            return base.opacity;
        } else {
            return opacity;
        }
    }

    private void updateFadeDistance(Entity entity,
            Body body,
                                    Fade fade) {
        if (fade.fadePositionObject != null) {
            fade.currentDistance = Mapper.body.get(fade.fadePositionObject).distToCamera;
        } else if (fade.fadePosition != null) {
            fade.currentDistance = D31.set(fade.fadePosition).sub(camera.getPos()).len() * camera.getFovFactor();
        } else {
            // Here we only use the camera position!
            fade.currentDistance = D31.set(camera.getPos()).len() * camera.getFovFactor();
        }
        body.distToCamera = fade.fadePositionObject == null || fade.fadePositionObject == entity ? body.pos.dst(camera.getPos(), B31).doubleValue() : Mapper.body.get(fade.fadePositionObject).distToCamera;
    }

    private void updateFadeOpacity(Base base,
                                   Fade fade) {
        if (fade.fadeIn != null) {
            base.opacity *= (float) MathUtilsDouble.lint(fade.currentDistance, fade.fadeIn.x, fade.fadeIn.y, fade.fadeInMap.x, fade.fadeInMap.y);
        }
        if (fade.fadeOut != null) {
            base.opacity *= (float) MathUtilsDouble.lint(fade.currentDistance, fade.fadeOut.x, fade.fadeOut.y, fade.fadeOutMap.x, fade.fadeOutMap.y);
        }
    }

    protected float getVisibilityOpacityFactor(Base base) {
        long msSinceStateChange = msSinceStateChange(base);

        // Fast track
        if (msSinceStateChange > Settings.settings.scene.fadeMs)
            return base.visible ? 1 : 0;

        // Fading
        float fadeOpacity = MathUtilsDouble.lint(msSinceStateChange, 0, Settings.settings.scene.fadeMs, 0, 1);
        if (!base.visible) {
            fadeOpacity = 1 - fadeOpacity;
        }
        return fadeOpacity;
    }

    protected long msSinceStateChange(Base base) {
        return (long) (GaiaSky.instance.getT() * 1000f) - base.lastStateChangeTimeMs;
    }

    /**
     * This function quickly discards perimeters when the Countries component type is off.
     *
     * @param entity The entity.
     * @param graph  The graph component.
     *
     * @return Whether the perimeter must be processed.
     */
    public boolean mustUpdatePerimeter(Entity entity,
                                       GraphNode graph) {
        boolean enabled = GaiaSky.instance.sceneRenderer.isOn(ComponentType.Countries);
        if (enabled) {
            entity.remove(TagNoProcess.class);
        } else if (entity.getComponent(TagNoProcess.class) == null) {
            entity.add(getEngine().createComponent(TagNoProcess.class));
        }
        return enabled;
    }

    /**
     * This function quickly discards locations when the Locations component type is off
     * or when the solid angle of the parent is too small.
     *
     * @param entity The entity.
     * @param graph  The graph component.
     *
     * @return Whether the perimeter must be processed.
     */
    public boolean mustUpdateLoc(Entity entity,
                                 GraphNode graph) {
        boolean enabled = GaiaSky.instance.sceneRenderer.isOn(ComponentType.Locations);
        if (enabled) {
            var parentBody = Mapper.body.get(graph.parent);
            var parentSa = Mapper.sa.get(graph.parent);
            boolean update = parentBody.solidAngle > parentSa.thresholdQuad * 30f;
            if (update) {
                entity.remove(TagNoProcess.class);
            } else if (entity.getComponent(TagNoProcess.class) == null) {
                entity.add(getEngine().createComponent(TagNoProcess.class));
            }
            return update;
        } else {
            if (!Mapper.tagNoProcess.has(entity)) {
                entity.add(getEngine().createComponent(TagNoProcess.class));
            }
            return false;
        }
    }

    /**
     * General method to update the position of an entity.
     *
     * @param entity The entity to update.
     * @param body   The body component.
     * @param graph  The graph component.
     */
    public void updatePositionDefault(Entity entity,
                                      Body body,
                                      GraphNode graph) {
        var pm = Mapper.pm.get(entity);
        if (time.getHdiff() != 0 || (pm != null && pm.hasPm)) {
            var coordinates = Mapper.coordinates.get(entity);
            if (coordinates != null && coordinates.coordinates != null) {
                // Load this object's equatorial cartesian coordinates into pos.
                coordinates.timeOverflow = coordinates.coordinates.getEquatorialCartesianCoordinates(time.getTime(), body.pos) == null;
            } else if (!body.positionSetInScript) {
                // Just set the original position.
                body.pos.set(body.posEpoch);
            }

            // Apply proper motion if needed.
            if (pm != null && pm.hasPm) {
                Vector3D pmv = D31.set(pm.pm).scl(AstroUtils.getMsSince(time.getTime(), pm.epochJd) * Nature.MS_TO_Y);
                body.pos.add(pmv);
            }
        }

    }

    public void updatePositionLocationMark(Entity entity,
                                           Body body,
                                           GraphNode graph) {
        var loc = Mapper.loc.get(entity);
        // loc.location3d contains the position already (last frame).
        body.pos.set(loc.location3d);
    }

    /**
     * Method to update the position of a shape object.
     *
     * @param entity The entity to update.
     * @param body   The body component.
     * @param graph  The graph component.
     */
    public void updateShapeObject(Entity entity,
                                  Body body,
                                  GraphNode graph) {
        var shape = Mapper.shape.get(entity);
        if (shape != null && shape.track != null) {
            // Overwrite position if track object is set.
            EntityUtils.getAbsolutePosition(shape.track.getEntity(), shape.trackName.toLowerCase(Locale.ROOT), body.pos);
        } else {
            updatePositionDefault(entity, body, graph);
        }
    }

    /**
     * Method to update the position of a spacecraft.
     *
     * @param entity The entity to update.
     * @param body   The body component.
     * @param graph  The graph component.
     */
    public void updateSpacecraft(Entity entity,
                                 Body body,
                                 GraphNode graph) {
        view.setEntity(entity);
        var engine = view.engine;

        if (engine.yawv != 0 || engine.pitchv != 0 || engine.rollv != 0 || engine.vel.len2() != 0 || engine.render) {
            var coordinates = Mapper.coordinates.get(entity);

            // We use the simulation time for the integration
            // Poll keys
            if (camera.getMode().isSpacecraft()) {
                view.pollKeys(Gdx.graphics.getDeltaTime());
            }

            double dt = time.getDt();

            // POSITION
            coordinates.coordinates.getEquatorialCartesianCoordinates(time.getTime(), view.body.pos);

            if (engine.leveling) {
                // No velocity, we just stop Euler angle motions
                if (engine.yawv != 0) {
                    engine.yawp = -Math.signum(engine.yawv) * MathUtilsDouble.clamp(Math.abs(engine.yawv), 0, 1);
                }
                if (engine.pitchv != 0) {
                    engine.pitchp = -Math.signum(engine.pitchv) * MathUtilsDouble.clamp(Math.abs(engine.pitchv), 0, 1);
                }
                if (engine.rollv != 0) {
                    engine.rollp = -Math.signum(engine.rollv) * MathUtilsDouble.clamp(Math.abs(engine.rollv), 0, 1);
                }
                if (Math.abs(engine.yawv) < 1e-3 && FastMath.abs(engine.pitchv) < 1e-3 && FastMath.abs(engine.rollv) < 1e-3) {
                    engine.setYawPower(0);
                    engine.setPitchPower(0);
                    engine.setRollPower(0);

                    engine.yawv = 0;
                    engine.pitchv = 0;
                    engine.rollv = 0;
                    EventManager.publish(Event.SPACECRAFT_STABILISE_CMD, this, false);
                }
            }

            double rollDiff = view.computeDirectionUp(dt, engine.dirup);

            double len = engine.direction.len();
            engine.pitch = FastMath.asin(engine.direction.y / len);
            engine.yaw = FastMath.atan2(engine.direction.z, engine.direction.x);
            engine.roll += rollDiff;

            engine.pitch = FastMath.toDegrees(engine.pitch);
            engine.yaw = FastMath.toDegrees(engine.yaw);
        }
        // Update float vectors
        Vector3Q camPos = B31.set(view.body.pos).add(camera.getInversePos());
        camPos.put(engine.posf);
        engine.direction.put(engine.directionf);
        engine.up.put(engine.upf);
    }

}
