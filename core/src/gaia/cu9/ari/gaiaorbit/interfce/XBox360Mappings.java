/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

/**
 * XBox 360 inputListener mappings
 * 
 * @author tsagrista
 *
 */
public class XBox360Mappings implements IControllerMappings {

    public static final int AXIS_JOY1VERT = 1;
    public static final int AXIS_JOY2VERT = 4;

    public static final int AXIS_JOY1HOR = 0;
    public static final int AXIS_JOY2HOR = 3;

    public static final int AXIS_LT = 2;
    public static final int AXIS_RT = 5;

    public static final int BUTTON_A = 0;
    public static final int BUTTON_B = 1;
    public static final int BUTTON_X = 2;
    public static final int BUTTON_Y = 3;
    public static final int BUTTON_LB = 4;
    public static final int BUTTON_RB = 5;
    public static final int BUTTON_BACK = 6;
    public static final int BUTTON_START = 7;
    public static final int BUTTON_XBOX_CROSS = 8;
    public static final int BUTTON_JOY1 = 9;
    public static final int BUTTON_JOY2 = 10;

    @Override
    public int getAxisRoll() {
        return AXIS_JOY2HOR;
    }

    @Override
    public int getAxisPitch() {
        return AXIS_JOY1VERT;
    }

    @Override
    public int getAxisYaw() {
        return AXIS_JOY1HOR;
    }

    @Override
    public int getAxisMove() {
        return AXIS_JOY2VERT;
    }

    @Override
    public int getAxisVelocityUp() {
        return AXIS_RT;
    }

    @Override
    public int getAxisVelocityDown() {
        return AXIS_LT;
    }

    @Override
    public int getButtonVelocityMultiplierTenth() {
        return BUTTON_RB;
    }

    @Override
    public int getButtonVelocityMultiplierHalf() {
        return BUTTON_LB;
    }

    @Override
    public int getButtonVelocityUp() {
        return BUTTON_X;
    }

    @Override
    public int getButtonVelocityDown() {
        return BUTTON_A;
    }

}
