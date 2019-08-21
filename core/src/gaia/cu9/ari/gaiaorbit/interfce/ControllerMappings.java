/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.files.FileHandle;
import gaia.cu9.ari.gaiaorbit.util.Logger;

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

            AXIS_ROLL = parse(mappings, "axis.roll", "-1");
            AXIS_PITCH = parse(mappings,"axis.pitch", "-1");
            AXIS_YAW = parse(mappings, "axis.yaw", "-1");
            AXIS_MOVE = parse(mappings,"axis.move", "-1");
            AXIS_VEL_UP = parse(mappings, "axis.velocityup", "-1");
            AXIS_VEL_DOWN = parse(mappings,"axis.velocitydown", "-1");

            BUTTON_VEL_UP = parse(mappings,"button.velocityup", "-1");
            BUTTON_VEL_DOWN = parse(mappings,"button.velocitydown", "-1");
            BUTTON_VEL_MULT_TENTH = parse(mappings,"button.velocitytenth", "-1");
            BUTTON_VEL_MULT_HALF = parse(mappings,"button.velocityhalf", "-1");
            BUTTON_UP = parse(mappings,"button.up", "-1");
            BUTTON_DOWN = parse(mappings,"button.down", "-1");
            BUTTON_MODE_TOGGLE = parse(mappings,"button.mode.toggle", "-1");

        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e, "Error reading inputListener mappings");
        }
    }


    private int parse(Properties mappings, String property, String defaultValue){
        try{
            return Integer.parseInt(mappings.getProperty(property, defaultValue));
        } catch (Exception e){
            return Integer.parseInt(defaultValue);
        }
    }


}
