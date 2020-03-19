/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import gaiasky.util.math.MathUtilsd;

public abstract class AbstractControllerMappings implements IControllerMappings {

    public double AXIS_VALUE_POW;

    public int AXIS_ROLL;
    public double AXIS_ROLL_SENS;
    public int AXIS_PITCH;
    public double AXIS_PITCH_SENS;
    public int AXIS_YAW;
    public double AXIS_YAW_SENS;
    public int AXIS_MOVE;
    public double AXIS_MOVE_SENS;
    public int AXIS_VEL_UP;
    public double AXIS_VEL_UP_SENS;
    public int AXIS_VEL_DOWN;
    public double AXIS_VEL_DOWN_SENS;

    public int BUTTON_VEL_UP;
    public int BUTTON_VEL_DOWN;
    public int BUTTON_VEL_MULT_TENTH;
    public int BUTTON_VEL_MULT_HALF;

    public int BUTTON_UP;
    public int BUTTON_DOWN;

    public int BUTTON_MODE_TOGGLE;

    @Override
    public double getAxisValuePower() {
        return AXIS_VALUE_POW;
    }

    @Override
    public int getAxisRoll() {
        return AXIS_ROLL;
    }

    @Override
    public int getAxisPitch() {
        return AXIS_PITCH;
    }

    @Override
    public int getAxisYaw() {
        return AXIS_YAW;
    }

    @Override
    public int getAxisMove() {
        return AXIS_MOVE;
    }

    @Override
    public int getAxisVelocityUp() {
        return AXIS_VEL_UP;
    }

    @Override
    public int getAxisVelocityDown() {
        return AXIS_VEL_DOWN;
    }

    @Override
    public int getButtonVelocityMultiplierTenth() {
        return BUTTON_VEL_MULT_HALF;
    }

    @Override
    public int getButtonVelocityMultiplierHalf() {
        return BUTTON_VEL_MULT_TENTH;
    }

    @Override
    public int getButtonVelocityUp() {
        return BUTTON_VEL_UP;
    }

    @Override
    public int getButtonVelocityDown() {
        return BUTTON_VEL_DOWN;
    }

    @Override
    public int getButtonUp() {
        return BUTTON_UP;
    }

    @Override
    public int getButtonDown() {
        return BUTTON_DOWN;
    }

    @Override
    public int getButtonModeToggle() {
        return BUTTON_MODE_TOGGLE;
    }

    @Override
    public double getAxisRollSensitivity() {
        return MathUtilsd.clamp(AXIS_ROLL_SENS, 0.01, 100.0);
    }

    @Override
    public double getAxisPitchSensitivity() {
        return MathUtilsd.clamp(AXIS_PITCH_SENS, 0.01, 100.0);
    }

    @Override
    public double getAxisYawSensitivity() {
        return MathUtilsd.clamp(AXIS_YAW_SENS, 0.01, 100.0);
    }

    @Override
    public double getAxisMoveSensitivity() {
        return MathUtilsd.clamp(AXIS_MOVE_SENS, 0.01, 100.0);
    }

    @Override
    public double getAxisVelUpSensitivity() {
        return MathUtilsd.clamp(AXIS_VEL_UP_SENS, 0.01, 100.0);
    }

    @Override
    public double getAxisVelDownSensitivity() {
        return MathUtilsd.clamp(AXIS_VEL_DOWN_SENS, 0.01, 100.0);
    }
}
