/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.util.Logger;
import gaiasky.util.parse.Parser;

import java.io.InputStream;
import java.util.Properties;

/**
 * Reads inputListener mappings from a file
 * 
 * @author tsagrista
 *
 */
public class ControllerMappings extends AbstractControllerMappings {

    public ControllerMappings(FileHandle mappingsFile) {
        super();
        Properties mappings = new Properties();
        try {
            InputStream is = mappingsFile.read();
            mappings.load(is);
            is.close();

            AXIS_VALUE_POW = parseDouble(mappings, "axis.value.pow", "4.0");

            AXIS_ROLL = parseInt(mappings, "axis.roll", "-1");
            AXIS_ROLL_SENS = parseDouble(mappings, "axis.roll.sensitivity", "1.0");
            AXIS_PITCH = parseInt(mappings,"axis.pitch", "-1");
            AXIS_PITCH_SENS = parseDouble(mappings, "axis.pitch.sensitivity", "1.0");
            AXIS_YAW = parseInt(mappings, "axis.yaw", "-1");
            AXIS_YAW_SENS = parseDouble(mappings, "axis.yaw.sensitivity", "1.0");
            AXIS_MOVE = parseInt(mappings,"axis.move", "-1");
            AXIS_MOVE_SENS = parseDouble(mappings, "axis.move.sensitivity", "1.0");
            AXIS_VEL_UP = parseInt(mappings, "axis.velocityup", "-1");
            AXIS_VEL_UP_SENS = parseDouble(mappings, "axis.velocityup.sensitivity", "1.0");
            AXIS_VEL_DOWN = parseInt(mappings,"axis.velocitydown", "-1");
            AXIS_VEL_DOWN_SENS = parseDouble(mappings, "axis.velocitydown.sensitivity", "1.0");

            BUTTON_VEL_UP = parseInt(mappings,"button.velocityup", "-1");
            BUTTON_VEL_DOWN = parseInt(mappings,"button.velocitydown", "-1");
            BUTTON_VEL_MULT_TENTH = parseInt(mappings,"button.velocitytenth", "-1");
            BUTTON_VEL_MULT_HALF = parseInt(mappings,"button.velocityhalf", "-1");
            BUTTON_UP = parseInt(mappings,"button.up", "-1");
            BUTTON_DOWN = parseInt(mappings,"button.down", "-1");
            BUTTON_MODE_TOGGLE = parseInt(mappings,"button.mode.toggle", "-1");

        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e, "Error reading inputListener mappings");
        }
    }


    private int parseInt(Properties mappings, String property, String defaultValue){
        try{
            return Integer.parseInt(mappings.getProperty(property, defaultValue));
        } catch (Exception e){
            return Integer.parseInt(defaultValue);
        }
    }
    private double parseDouble(Properties mappings, String property, String defaultValue){
        try{
            return Parser.parseDouble(mappings.getProperty(property, defaultValue));
        } catch (Exception e){
            return Parser.parseDouble(defaultValue);
        }
    }


}
