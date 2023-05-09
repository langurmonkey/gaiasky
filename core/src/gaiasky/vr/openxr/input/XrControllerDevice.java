/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.vr.openxr.input;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.vr.openxr.XrDriver;
import gaiasky.vr.openxr.XrHelper;
import gaiasky.vr.openxr.input.actions.*;
import org.lwjgl.openxr.XrPosef;

public class XrControllerDevice {

    /**
     * Reflects the source device of this action. Either left or right.
     */
    public enum DeviceType {
        Left,
        Right;

        public boolean isLeft() {
            return this == Left;
        }

        public boolean isRight() {
            return this == Right;
        }
    }

    public final DeviceType deviceType;
    public boolean active = false;
    private boolean initialized = false;

    public Vector3 position = new Vector3();
    public Quaternion orientation = new Quaternion();
    // The model instance.
    public IntModelInstance modelInstance;
    public Matrix4 aimTransform;

    // Actions
    public BoolAction showUi, accept, cameraMode;
    public FloatAction select;
    public Vec2fAction move;
    public PoseAction gripPose;
    public PoseAction aimPose;
    public HapticsAction haptics;

    public XrControllerDevice(DeviceType type) {
        this.deviceType = type;
    }

    public void initialize(XrDriver driver) {
        var model = XrHelper.loadRenderModel(driver, this);
        if (model != null) {
            modelInstance = new IntModelInstance(model);
        }
        aimTransform = new Matrix4();
        initialized = model != null && modelInstance != null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Sends a haptic pulse to this device.
     *
     * @param driver      The XR driver.
     * @param nanoseconds The length of the pulse in nanoseconds.
     * @param frequency   The frequency in Hz.
     * @param amplitude   The amplitude in [0,1].
     */
    public void sendHapticPulse(XrDriver driver,
                                long nanoseconds,
                                float frequency,
                                float amplitude) {
        if (haptics != null) {
            haptics.sendHapticPulse(driver, nanoseconds, frequency, amplitude);
        }
    }

    public void setGripPose(XrPosef grip) {
        if (modelInstance != null) {
            setFromXrPose(grip, modelInstance.transform);
        }
    }

    public void setAim(XrPosef aim) {
        setFromXrPose(aim, aimTransform);
        aimTransform.rotate(1, 0, 0, 20);
    }

    private void setFromXrPose(XrPosef pose,
                               Matrix4 transform) {
        var pos = pose.position$();
        var ori = pose.orientation();
        position.set(pos.x(), pos.y(), pos.z());
        orientation.set(ori.x(), ori.y(), ori.z(), ori.w());
        transform.idt().translate(position).rotate(orientation);
    }

    public IntModelInstance getModelInstance() {
        return modelInstance;
    }

    public void processListener(XrInputListener listener) {
        processShowUIAction(showUi, listener);
        processCameraModeAction(cameraMode, listener);
        processAcceptAction(accept, listener);
        processSelectAction(select, listener);
        processMoveAction(move, listener);
    }

    private void processShowUIAction(BoolAction action,
                                     XrInputListener listener) {
        if (action.isActive && action.changedSinceLastSync) {
            listener.showUI(action.currentState, action.getControllerDevice());
        }
    }

    private void processCameraModeAction(BoolAction action,
                                         XrInputListener listener) {
        if (action.isActive && action.changedSinceLastSync) {
            listener.cameraMode(action.currentState, action.getControllerDevice());
        }
    }

    private void processAcceptAction(BoolAction action,
                                     XrInputListener listener) {
        if (action.isActive && action.changedSinceLastSync) {
            listener.accept(action.currentState, action.getControllerDevice());
        }
    }

    private void processSelectAction(FloatAction action,
                                     XrInputListener listener) {
        if (action.isActive && action.changedSinceLastSync) {
            listener.select(action.currentState, action.getControllerDevice());
        }
    }

    private void processMoveAction(Vec2fAction action,
                                   XrInputListener listener) {
        if (action.isActive && action.changedSinceLastSync) {
            listener.move(action.currentState, action.getControllerDevice());
        }
    }
}
