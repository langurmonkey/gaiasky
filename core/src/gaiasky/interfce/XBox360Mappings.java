/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

/**
 * XBox 360 inputListener mappings
 * 
 * @author tsagrista
 *
 */
public class XBox360Mappings extends AbstractControllerMappings {

    public XBox360Mappings(){
        AXIS_LSTICK_H = 0;
        AXIS_RSTICK_H = 4;
        AXIS_RSTICK_V = 3;
        AXIS_LSTICK_V = 1;
        AXIS_RT = 5;
        AXIS_LT = 2;

        BUTTON_A = 0;
        BUTTON_B = 1;
        BUTTON_X = 5;
        BUTTON_Y = 4;
        BUTTON_DPAD_UP = 3;
        BUTTON_DPAD_DOWN = 2;
    }
}
