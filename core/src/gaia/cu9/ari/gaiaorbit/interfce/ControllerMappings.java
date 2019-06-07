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

            AXIS_ROLL = Integer.parseInt(mappings.getProperty("axis.roll", "-1"));
            AXIS_PITCH = Integer.parseInt(mappings.getProperty("axis.pitch", "-1"));
            AXIS_YAW = Integer.parseInt(mappings.getProperty("axis.yaw", "-1"));
            AXIS_MOVE = Integer.parseInt(mappings.getProperty("axis.move", "-1"));
            AXIS_VEL_UP = Integer.parseInt(mappings.getProperty("axis.velocityup", "-1"));
            AXIS_VEL_DOWN = Integer.parseInt(mappings.getProperty("axis.velocitydown", "-1"));

            BUTTON_VEL_UP = Integer.parseInt(mappings.getProperty("button.velocityup", "-1"));
            BUTTON_VEL_DOWN = Integer.parseInt(mappings.getProperty("button.velocitydown", "-1"));
            BUTTON_VEL_MULT_TENTH = Integer.parseInt(mappings.getProperty("button.velocitytenth", "-1"));
            BUTTON_VEL_MULT_HALF = Integer.parseInt(mappings.getProperty("button.velocityhalf", "-1"));
            BUTTON_UP = Integer.parseInt(mappings.getProperty("button.up", "-1"));
            BUTTON_DOWN = Integer.parseInt(mappings.getProperty("button.down", "-1"));
            BUTTON_MODE_TOGGLE = Integer.parseInt(mappings.getProperty("button.mode.toggle", "-1"));

        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e, "Error reading inputListener mappings");
        }
    }


}
