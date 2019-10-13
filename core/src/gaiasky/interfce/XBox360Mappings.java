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
        AXIS_ROLL = 0;
        AXIS_PITCH = 4;
        AXIS_YAW = 3;
        AXIS_MOVE = 1;
        AXIS_VEL_UP = 5;
        AXIS_VEL_DOWN = 2;

        BUTTON_VEL_UP = 0;
        BUTTON_VEL_DOWN = 1;
        BUTTON_VEL_MULT_TENTH = 5;
        BUTTON_VEL_MULT_HALF = 4;
        BUTTON_UP = 3;
        BUTTON_DOWN = 2;
    }
}
