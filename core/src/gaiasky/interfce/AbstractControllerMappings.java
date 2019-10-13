/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

public abstract class AbstractControllerMappings implements IControllerMappings {

    public int AXIS_ROLL;
    public int AXIS_PITCH;
    public int AXIS_YAW;
    public int AXIS_MOVE;
    public int AXIS_VEL_UP;
    public int AXIS_VEL_DOWN;

    public int BUTTON_VEL_UP;
    public int BUTTON_VEL_DOWN;
    public int BUTTON_VEL_MULT_TENTH;
    public int BUTTON_VEL_MULT_HALF;

    public int BUTTON_UP;
    public int BUTTON_DOWN;

    public int BUTTON_MODE_TOGGLE;

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
}
