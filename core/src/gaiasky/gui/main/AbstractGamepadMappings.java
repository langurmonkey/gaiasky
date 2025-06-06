/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.main;

import gaiasky.gui.api.IGamepadMappings;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.math.MathUtilsDouble;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base abstract implementation for gamepad mappings.
 */
public abstract class AbstractGamepadMappings implements IGamepadMappings {
    protected static final Logger.Log logger = Logger.getLogger(AbstractGamepadMappings.class);

    public double AXIS_VALUE_POW = 4d;

    public double ZERO_POINT = 0.2;

    public int AXIS_LSTICK_H = -1;
    public double AXIS_LSTICK_H_SENS = 1d;
    public int AXIS_RSTICK_H = -1;
    public double AXIS_RSTICK_H_SENS = 1d;
    public int AXIS_RSTICK_V = -1;
    public double AXIS_RSTICK_V_SENS = 1d;
    public int AXIS_LSTICK_V = -1;
    public double AXIS_LSTICK_V_SENS = 1d;

    public int AXIS_RB = -1;
    public int AXIS_LB = -1;

    public int AXIS_RT = -1;
    public double AXIS_RT_SENS = 1d;
    public int AXIS_LT = -1;
    public double AXIS_LT_SENS = 1d;

    public int AXIS_DPAD_H = -1;
    public int AXIS_DPAD_V = -1;
    public int BUTTON_DPAD_UP = -1;
    public int BUTTON_DPAD_DOWN = -1;
    public int BUTTON_DPAD_LEFT = -1;
    public int BUTTON_DPAD_RIGHT = -1;

    public int BUTTON_A = -1;
    public int BUTTON_B = -1;
    public int BUTTON_X = -1;
    public int BUTTON_Y = -1;

    public int BUTTON_START = -1;
    public int BUTTON_SELECT = -1;

    public int BUTTON_RT = -1;
    public int BUTTON_RB = -1;
    public int BUTTON_LT = -1;
    public int BUTTON_LB = -1;

    public int BUTTON_LSTICK = -1;
    public int BUTTON_RSTICK = -1;

    public static IGamepadMappings readGamepadMappings(String mappingsFile) {
        IGamepadMappings mappings = null;
        final Path mappingsPath = Path.of(mappingsFile);
        if (Files.exists(mappingsPath)) {
            mappings = new GamepadMappings(null, mappingsPath);
        } else {
            // Check if internal mappings exist for that file.
            final Path assetsLoc = Path.of(Settings.ASSETS_LOC);
            Path internalMappings = assetsLoc.resolve(mappingsFile);
            if (Files.exists(internalMappings)) {
                mappings = new GamepadMappings(null, internalMappings);
            } else {
                // Default file.
                final Path defaultPath = assetsLoc.resolve("mappings").resolve("SDL_Controller.controller");
                if (Files.exists(defaultPath)) {
                    mappings = new GamepadMappings(null, defaultPath);
                    // Update mappings path.
                    Settings.settings.controls.gamepad.mappingsFile = defaultPath.toAbsolutePath().toString();                }
            }
        }
        return mappings;
    }

    @Override
    public double getZeroPoint() {
        return ZERO_POINT;
    }

    @Override
    public double getAxisValuePower() {
        return AXIS_VALUE_POW;
    }

    @Override
    public int getAxisLstickH() {
        return AXIS_LSTICK_H;
    }

    @Override
    public int getAxisRstickH() {
        return AXIS_RSTICK_H;
    }

    @Override
    public int getAxisRstickV() {
        return AXIS_RSTICK_V;
    }

    @Override
    public int getAxisLstickV() {
        return AXIS_LSTICK_V;
    }

    @Override
    public int getAxisRB() {
        return AXIS_RB;
    }
    @Override
    public int getAxisLB() {
        return AXIS_LB;
    }

    @Override
    public int getAxisRT() {
        return AXIS_RT;
    }

    @Override
    public int getAxisLT() {
        return AXIS_LT;
    }

    @Override
    public int getAxisDpadH() {
        return AXIS_DPAD_H;
    }

    @Override
    public int getAxisDpadV() {
        return AXIS_DPAD_V;
    }

    @Override
    public int getButtonY() {
        return BUTTON_Y;
    }

    @Override
    public int getButtonX() {
        return BUTTON_X;
    }

    @Override
    public int getButtonA() {
        return BUTTON_A;
    }

    @Override
    public int getButtonB() {
        return BUTTON_B;
    }

    @Override
    public int getButtonDpadUp() {
        return BUTTON_DPAD_UP;
    }

    @Override
    public int getButtonDpadDown() {
        return BUTTON_DPAD_DOWN;
    }

    @Override
    public int getButtonDpadLeft() {
        return BUTTON_DPAD_LEFT;
    }

    @Override
    public int getButtonDpadRight() {
        return BUTTON_DPAD_RIGHT;
    }

    @Override
    public int getButtonLstick() {
        return BUTTON_LSTICK;
    }

    @Override
    public int getButtonRstick() {
        return BUTTON_RSTICK;
    }

    @Override
    public int getButtonStart() {
        return BUTTON_START;
    }

    @Override
    public int getButtonSelect() {
        return BUTTON_SELECT;
    }

    @Override
    public int getButtonRT() {
        return BUTTON_RT;
    }

    @Override
    public int getButtonRB() {
        return BUTTON_RB;
    }

    @Override
    public int getButtonLT() {
        return BUTTON_LT;
    }

    @Override
    public int getButtonLB() {
        return BUTTON_LB;
    }

    @Override
    public double getAxisLstickHSensitivity() {
        return MathUtilsDouble.clamp(AXIS_LSTICK_H_SENS, 0.01, 100.0);
    }

    @Override
    public double getAxisRstickHSensitivity() {
        return MathUtilsDouble.clamp(AXIS_RSTICK_H_SENS, 0.01, 100.0);
    }

    @Override
    public double getAxisRstickVSensitivity() {
        return MathUtilsDouble.clamp(AXIS_RSTICK_V_SENS, 0.01, 100.0);
    }

    @Override
    public double getAxisLstickVSensitivity() {
        return MathUtilsDouble.clamp(AXIS_LSTICK_V_SENS, 0.01, 100.0);
    }

    @Override
    public double getAxisRTSensitivity() {
        return MathUtilsDouble.clamp(AXIS_RT_SENS, 0.01, 100.0);
    }

    @Override
    public double getAxisLTSensitivity() {
        return MathUtilsDouble.clamp(AXIS_LT_SENS, 0.01, 100.0);
    }
}
