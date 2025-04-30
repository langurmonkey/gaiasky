/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.camera;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.input.GestureDetector.GestureAdapter;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.input.SpacecraftGamepadListener;
import gaiasky.input.SpacecraftMouseKbdListener;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.view.FocusView;
import gaiasky.scene.view.SpacecraftView;
import gaiasky.util.Constants;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.coord.SpacecraftCoordinates;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;
import net.jafama.FastMath;
import org.apfloat.Apfloat;

public class SpacecraftCamera extends AbstractCamera implements IObserver {

    /**
     * The input inputListener attached to this camera
     **/
    private final SpacecraftMouseKbdListener spacecraftMouseKbdListener;
    private final Vector3d aux1, aux2;
    private final Vector3b aux1b;
    private final Vector3b toDesired;
    private final Vector3b desired;
    private final Vector3d scThrust;

    /*
     * Controller listener
     **/
    //private SpacecraftControllerListener controllerListener;
    private final Vector3d scForce;
    private final Vector3d scAccel;
    private final Vector3d scVel;
    private final Vector3d scDir;
    private final Vector3d scUp;
    private final Pair<Vector3d, Vector3d> dirUp;
    /**
     * Direction and up vectors.
     **/
    public Vector3d direction, up;
    public Vector3b relPos;
    private Entity sc;
    private final SpacecraftView view;
    /**
     * Implements gamepad camera input.
     **/
    private final SpacecraftGamepadListener gamepadListener;
    /**
     * Closest body apart from the spacecraft (second closest)
     **/
    private final FocusView secondClosest;
    private final FocusView auxView;
    private Vector3b scpos;
    private double targetDistance;

    public SpacecraftCamera(CameraManager parent) {
        super(parent);

        // Vectors
        direction = new Vector3d(1, 0, 0);
        up = new Vector3d(0, 1, 0);
        relPos = new Vector3b();
        toDesired = new Vector3b();
        desired = new Vector3b();
        aux1 = new Vector3d();
        aux2 = new Vector3d();
        aux1b = new Vector3b();
        scThrust = new Vector3d();
        scForce = new Vector3d();
        scAccel = new Vector3d();
        scVel = new Vector3d();
        scpos = new Vector3b();
        scDir = new Vector3d();
        scUp = new Vector3d();

        dirUp = new Pair<>(scDir, scUp);

        view = new SpacecraftView();
        secondClosest = new FocusView();
        auxView = new FocusView();

        // init camera
        camera = new PerspectiveCamera(40, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = (float) CAM_NEAR;
        camera.far = (float) CAM_FAR;

        // Init cameras vector.
        cameras = new PerspectiveCamera[] { camera, camLeft, camRight };

        /*
         * Camera to render the attitude indicator system.
         **/
        PerspectiveCamera guiCam = new PerspectiveCamera(30, 300, 300);
        guiCam.near = (float) CAM_NEAR;
        guiCam.far = (float) CAM_FAR;

        // aspect ratio
        ar = (float) Gdx.graphics.getWidth() / (float) Gdx.graphics.getHeight();

        // fov factor
        fovFactor = camera.fieldOfView / 40f;

        // Initialize mouse+keyboard input listener.
        spacecraftMouseKbdListener = new SpacecraftMouseKbdListener(this, new GestureAdapter());
        // Initialize gamepad input listener.
        gamepadListener = new SpacecraftGamepadListener(this, Settings.settings.controls.gamepad.mappingsFile);

        // FOCUS_MODE is changed from GUI
        EventManager.instance.subscribe(this, Event.FOV_CHANGED_CMD, Event.SPACECRAFT_LOADED, Event.SPACECRAFT_MACHINE_SELECTION_INFO);
    }

    @Override
    public void doneLoading(AssetManager manager) {
    }

    public Entity getSpacecraft() {
        return this.sc;
    }

    public SpacecraftView getSpacecraftView() {
        return this.view;
    }

    @Override
    public PerspectiveCamera getCamera() {
        return this.camera;
    }

    @Override
    public void setCamera(PerspectiveCamera perspectiveCamera) {
        this.camera = perspectiveCamera;
    }

    @Override
    public PerspectiveCamera[] getFrontCameras() {
        return new PerspectiveCamera[] { camera };
    }

    @Override
    public Vector3d getDirection() {
        return direction;
    }

    @Override
    public void setDirection(Vector3d dir) {
        this.direction.set(dir);
    }

    @Override
    public Vector3d getUp() {
        return up;
    }

    @Override
    public Vector3d[] getDirections() {
        return new Vector3d[] { direction };
    }

    @Override
    public int getNCameras() {
        return 1;
    }

    public void update(double dt, ITimeFrameProvider time) {
        spacecraftMouseKbdListener.update();
        gamepadListener.update();

        /* FUTURE POS OF SC */

        // We use the simulation time for the integration
        //double sdt = time.getDt() * Constants.H_TO_S;

        scThrust.set(view.thrust());
        scForce.set(view.force());
        scAccel.set(view.accel());
        scVel.set(view.vel());
        scpos.set(view.pos());
        scpos = ((SpacecraftCoordinates) view.getCoordinates()).computePosition(dt, secondClosest, view.currentEnginePower(), scThrust, view.direction(), scForce, scAccel, scVel, scpos);
        scDir.set(view.direction());
        scUp.set(view.up());
        view.computeDirectionUp(dt, dirUp);

        /* ACTUAL UPDATE */
        updateHard(dt);

        /* POST */
        distance = pos.lenDouble();

        // Update camera
        updatePerspectiveCamera();

        // Broadcast nearest info
        String closestName = null;
        double closestDistance = -1d;
        if (closestStar != null) {
            double closestStarDist = closestStar.getClosestDistToCamera();
            String closestStarName = closestStar.getClosestName();
            if (secondClosest != null && secondClosest.isValid()) {
                if (secondClosest.getDistToCamera() < closestStarDist) {
                    closestName = secondClosest.getName();
                    closestDistance = secondClosest.getDistToCamera();
                } else {
                    closestName = closestStarName;
                    closestDistance = closestStarDist;
                }
            } else {
                closestName = closestStarName;
                closestDistance = closestStarDist;
            }
        }
        EventManager.publish(Event.SPACECRAFT_NEAREST_INFO, this, closestName, closestDistance);

    }

    /**
     * Updates the position and direction of the camera using a hard analytical algorithm.
     */
    public void updateHard(double dt) {
        if (sc != null) {
            // POSITION
            double tDistOverFov = targetDistance / fovFactor;
            desired.set(scDir).nor().scl(-tDistOverFov);
            aux1b.set(scUp).nor().scl(tDistOverFov * 0.125d);
            desired.add(aux1b);
            toDesired.set(desired).sub(relPos);
            toDesired.scl(dt * view.getResponsiveness()).scl(1e-6d);
            relPos.add(toDesired);
            pos.set(scpos).add(relPos);

            // DIRECTION
            aux1.set(scUp).nor().scl(targetDistance);
            aux2.set(scDir).nor().scl(tDistOverFov * 50d).add(aux1);
            aux1b.set(scpos).add(aux2).sub(pos).nor();
            aux1b.put(direction);

            // UP
            desired.set(scUp);
            toDesired.set(desired).sub(up);
            toDesired.scl(dt * view.getResponsiveness()).scl(1e-8d);
            up.add(toDesired).nor();
        }
    }

    protected void updatePerspectiveCamera() {
        camera.fieldOfView = 40;
        fovFactor = camera.fieldOfView / 40f;
        camera.position.set(0, 0, 0);
        direction.put(camera.direction);
        up.put(camera.up);

        camera.update();

        posInv.set(pos).scl(new Apfloat(-1, Constants.PREC));

    }

    @Override
    public void updateMode(ICamera previousCam, CameraMode previousMode, CameraMode mode, boolean centerFocus) {
        InputProcessor ip = Gdx.input.getInputProcessor();
        if (ip instanceof InputMultiplexer im) {
            if (mode == CameraMode.SPACECRAFT_MODE && sc != null && previousMode != CameraMode.SPACECRAFT_MODE) {
                // Enter SC mode
                GaiaSky.postRunnable(() -> {
                    // Register mouse+keyboard input inputListener.
                    if (!im.getProcessors().contains(spacecraftMouseKbdListener, true))
                        im.addProcessor(spacecraftMouseKbdListener);
                    // Register gamepad listener.
                    addGamepadListener();

                    Controllers.clearListeners();
                    view.stopAllMovement();

                    // Put spacecraft at location of previous camera
                    view.pos().set(previousCam.getPos());
                    view.direction().set(previousCam.getDirection());
                    view.up().set(view.pos()).crs(view.direction());

                    pos.set(view.pos());
                    direction.set(view.direction());
                    up.set(view.up());

                    updateAngleEdge(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                });
            } else {
                // Exit SC mode
                if (view != null && view.getEntity() != null) {
                    GaiaSky.postRunnable(() -> {
                        // Remove mouse+keyboard input listener.
                        im.removeProcessor(spacecraftMouseKbdListener);
                        // Remove gamepad listener.
                        removeGamepadListener();
                        // Stop spacecraft.
                        view.stopAllMovement();
                    });
                }
            }
        }
    }

    @Override
    public CameraMode getMode() {
        return parent.mode;
    }

    @Override
    public double getSpeed() {
        return parent.getSpeed();
    }

    @Override
    public IFocus getFocus() {
        return null;
    }

    @Override
    public boolean hasFocus() {
        return false;
    }

    @Override
    public boolean isFocus(Entity entity) {
        return false;
    }

    public void addGamepadListener() {
        Settings.settings.controls.gamepad.addControllerListener(gamepadListener);
        gamepadListener.activate();
    }

    public void removeGamepadListener() {
        Settings.settings.controls.gamepad.removeControllerListener(gamepadListener);
        gamepadListener.deactivate();
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case SPACECRAFT_LOADED -> {
            sc = (Entity) data[0];
            view.setEntity(sc);
            updateTargetDistance();
        }
        case SPACECRAFT_MACHINE_SELECTION_INFO -> updateTargetDistance();
        default -> {
        }
        }
    }

    private void updateTargetDistance() {
        this.targetDistance = view.size() * 3.5;
    }

    @Override
    public void checkClosestBody(IFocus cb) {
        super.checkClosestBody(cb);
        if (sc != null && cb instanceof FocusView fv) {
            if (fv.getEntity() != sc && cb.getDistToCamera() < secondClosest.getDistToCamera()) {
                secondClosest.setEntity(fv.getEntity());
            }
        }
    }

    @Override
    public void checkClosestBody(Entity cb) {
        super.checkClosestBody(cb);
        if (sc != null && cb != null) {
            auxView.setEntity(cb);
            if (secondClosest.isEmpty() || (cb != sc && auxView.getDistToCamera() < secondClosest.getDistToCamera())) {
                secondClosest.setEntity(cb);
            }
        }
    }

    @Override
    public IFocus getSecondClosestBody() {
        return secondClosest;
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public Vector3d getVelocity() {
        return scVel;
    }

    @Override
    public double speedScaling() {
        return FastMath.max(0.001, scVel.len());
    }

}
