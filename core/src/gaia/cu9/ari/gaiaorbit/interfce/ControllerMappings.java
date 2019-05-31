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
 * Reads controller mappings from a file
 * 
 * @author tsagrista
 *
 */
public class ControllerMappings implements IControllerMappings {

    private int AXIS_ROLL;
    private int AXIS_PITCH;
    private int AXIS_YAW;
    private int AXIS_MOVE;
    private int AXIS_VEL_UP;
    private int AXIS_VEL_DOWN;

    private int BUTTON_VEL_UP;
    private int BUTTON_VEL_DOWN;
    private int BUTTON_VEL_MULT_TENTH;
    private int BUTTON_VEL_MULT_HALF;

    public ControllerMappings(FileHandle mappingsFile) {
        super();
        Properties mappings = new Properties();
        try {
            InputStream is = mappingsFile.read();
            mappings.load(is);
            is.close();

            AXIS_ROLL = Integer.parseInt(mappings.getProperty("axis.roll"));
            AXIS_PITCH = Integer.parseInt(mappings.getProperty("axis.pitch"));
            AXIS_YAW = Integer.parseInt(mappings.getProperty("axis.yaw"));
            AXIS_MOVE = Integer.parseInt(mappings.getProperty("axis.move"));
            AXIS_VEL_UP = Integer.parseInt(mappings.getProperty("axis.velocityup"));
            AXIS_VEL_DOWN = Integer.parseInt(mappings.getProperty("axis.velocitydown"));

            BUTTON_VEL_UP = Integer.parseInt(mappings.getProperty("button.velocityup"));
            BUTTON_VEL_DOWN = Integer.parseInt(mappings.getProperty("button.velocitydown"));
            BUTTON_VEL_MULT_TENTH = Integer.parseInt(mappings.getProperty("button.velocitytenth"));
            BUTTON_VEL_MULT_HALF = Integer.parseInt(mappings.getProperty("button.velocityhalf"));

        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e, "Error reading controller mappings");
        }
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

}
