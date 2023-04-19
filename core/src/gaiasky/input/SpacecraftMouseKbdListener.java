/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.gdx.Input.Keys;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.camera.SpacecraftCamera;
import gaiasky.scene.view.SpacecraftView;
import gaiasky.util.Settings;

public class SpacecraftMouseKbdListener extends AbstractMouseKbdListener {

    private final SpacecraftCamera cam;

    public SpacecraftMouseKbdListener(SpacecraftCamera spacecraftCamera, GestureListener listener) {
        super(listener, spacecraftCamera);
        this.cam = spacecraftCamera;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (isActive()) {
            SpacecraftView sc = cam.getSpacecraftView();
            if (sc != null && Settings.settings.runtime.inputEnabled) {
                double step = 0.01;
                switch (keycode) {
                case Keys.W -> {
                    // power 1
                    sc.setCurrentEnginePower(sc.currentEnginePower() + step);
                    EventManager.publish(Event.SPACECRAFT_STOP_CMD, this, false);
                }
                case Keys.S -> {
                    // power -1
                    sc.setCurrentEnginePower(sc.currentEnginePower() - step);
                    EventManager.publish(Event.SPACECRAFT_STOP_CMD, this, false);
                }
                case Keys.A -> {
                    // roll 1
                    sc.setRollPower(sc.getRollPower() + step);
                    EventManager.publish(Event.SPACECRAFT_STOP_CMD, this, false);
                }
                case Keys.D -> {
                    // roll -1
                    sc.setRollPower(sc.getRollPower() - step);
                    EventManager.publish(Event.SPACECRAFT_STABILISE_CMD, this, false);
                }
                case Keys.DOWN -> {
                    // pitch 1
                    sc.setPitchPower(sc.getPitchPower() + step);
                    EventManager.publish(Event.SPACECRAFT_STABILISE_CMD, this, false);
                }
                case Keys.UP -> {
                    // pitch -1
                    sc.setPitchPower(sc.getPitchPower() - step);
                    EventManager.publish(Event.SPACECRAFT_STABILISE_CMD, this, false);
                }
                case Keys.LEFT -> {
                    // yaw 1
                    sc.setYawPower(sc.getYawPower() + step);
                    EventManager.publish(Event.SPACECRAFT_STABILISE_CMD, this, false);
                }
                case Keys.RIGHT -> {
                    // yaw -1
                    sc.setYawPower(sc.getYawPower() - step);
                    EventManager.publish(Event.SPACECRAFT_STABILISE_CMD, this, false);
                }
                default -> {
                }
                }
            }
        }
        return false;

    }

    @Override
    public boolean keyUp(int keycode) {
        if (isActive()) {
            SpacecraftView sc = cam.getSpacecraftView();
            if (sc != null && sc.getEntity() != null && Settings.settings.runtime.inputEnabled) {
                switch (keycode) {
                case Keys.W, Keys.S ->
                    // power 0
                        sc.setCurrentEnginePower(0);
                case Keys.D, Keys.A ->
                    // roll 0
                        sc.setRollPower(0);
                case Keys.UP, Keys.DOWN ->
                    // pitch 0
                        sc.setPitchPower(0);
                case Keys.RIGHT, Keys.LEFT ->
                    // yaw 0
                        sc.setYawPower(0);
                case Keys.L ->
                    // level spaceship
                        EventManager.publish(Event.SPACECRAFT_STABILISE_CMD, this, true);
                case Keys.K ->
                    // stop spaceship
                        EventManager.publish(Event.SPACECRAFT_STOP_CMD, this, true);
                case Keys.PAGE_UP ->
                    // Increase thrust factor
                        sc.increaseThrustFactorIndex(true);
                case Keys.PAGE_DOWN ->
                    // Decrease thrust length
                        sc.decreaseThrustFactorIndex(true);
                default -> {
                }
                }
            }
        }
        return false;

    }

    @Override
    protected boolean pollKeys() {
        return false;
    }

}
