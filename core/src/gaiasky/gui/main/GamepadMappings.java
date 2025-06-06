/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.main;

import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.parse.Parser;
import gaiasky.util.properties.SortedProperties;
import org.apache.commons.io.FilenameUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

/**
 * Contains mappings from actions to input targets for gamepads. The information is typically
 * loaded from <code>gamepad_name.controller</code> files.
 */
public class GamepadMappings extends AbstractGamepadMappings {
    private String controllerName;

    /**
     * Create empty controller mappings.
     */
    public GamepadMappings(String controllerName) {
        super();
        this.controllerName = controllerName;
    }

    /**
     * Create a controller mappings instance from a *.controller file
     *
     * @param controllerName Controller name, or null
     * @param mappingsFile   The mappings file
     */
    public GamepadMappings(String controllerName, Path mappingsFile) {
        this(controllerName);
        // If no controller name
        if (controllerName == null || controllerName.isBlank()) {
            this.controllerName = FilenameUtils.removeExtension(mappingsFile.getFileName().toString());
        }
        Properties mappings = new SortedProperties();
        try {
            if (!Files.exists(mappingsFile)) {
                Path internalMappings = Path.of(Settings.ASSETS_LOC).resolve(mappingsFile);
                if (Files.exists(internalMappings))
                    mappingsFile = internalMappings;
            }
            InputStream is = Files.newInputStream(mappingsFile);
            mappings.load(is);
            is.close();

            AXIS_VALUE_POW = parseDouble(mappings, "4.0", "axis.value.pow");

            AXIS_LSTICK_H = parseInt(mappings, "axis.roll", "axis.lstick.h");
            AXIS_LSTICK_H_SENS = parseDouble(mappings, "1.0", "axis.roll.sensitivity", "axis.lstick.h.sensitivity");
            AXIS_RSTICK_H = parseInt(mappings, "axis.pitch", "axis.rstick.h");
            AXIS_RSTICK_H_SENS = parseDouble(mappings, "1.0", "axis.pitch.sensitivity", "axis.rstick.h.sensitivity");
            AXIS_RSTICK_V = parseInt(mappings, "axis.yaw", "axis.rstick.v");
            AXIS_RSTICK_V_SENS = parseDouble(mappings, "1.0", "axis.yaw.sensitivity", "axis.rstick.v.sensitivity");
            AXIS_LSTICK_V = parseInt(mappings, "axis.move", "axis.lstick.v");
            AXIS_LSTICK_V_SENS = parseDouble(mappings, "1.0", "axis.move.sensitivity", "axis.lstick.v.sensitivity");
            AXIS_RB = parseInt(mappings, "axis.rb");
            AXIS_LB = parseInt(mappings, "axis.lb");
            AXIS_RT = parseInt(mappings, "axis.velocityup", "axis.rt");
            AXIS_RT_SENS = parseDouble(mappings, "-1", "axis.velocityup.sensitivity", "axis.rt.sensitivity");
            AXIS_LT = parseInt(mappings, "axis.velocitydown", "axis.lt");
            AXIS_LT_SENS = parseDouble(mappings, "1.0", "axis.velocitydown.sensitivity", "axis.lt.sensitivity");
            AXIS_DPAD_H = parseInt(mappings, "axis.dpad.h");
            AXIS_DPAD_V = parseInt(mappings, "axis.dpad.v");

            BUTTON_DPAD_UP = parseInt(mappings, "button.up", "button.dpad.u");
            BUTTON_DPAD_DOWN = parseInt(mappings, "button.down", "button.dpad.d");
            BUTTON_DPAD_LEFT = parseInt(mappings, "button.dpad.l");
            BUTTON_DPAD_RIGHT = parseInt(mappings, "button.dpad.r");
            BUTTON_A = parseInt(mappings, "button.velocityup", "button.a");
            BUTTON_B = parseInt(mappings, "button.velocitydown", "button.b");
            BUTTON_X = parseInt(mappings, "button.velocitytenth", "button.x");
            BUTTON_Y = parseInt(mappings, "button.velocityhalf", "button.y");
            BUTTON_RSTICK = parseInt(mappings, "button.rstick", "button.mode.toggle");
            BUTTON_LSTICK = parseInt(mappings, "button.lstick");
            BUTTON_RT = parseInt(mappings, "button.rt");
            BUTTON_RB = parseInt(mappings, "button.rb");
            BUTTON_LT = parseInt(mappings, "button.lt");
            BUTTON_LB = parseInt(mappings, "button.lb");
            BUTTON_START = parseInt(mappings, "button.start");
            BUTTON_SELECT = parseInt(mappings, "button.select");

        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e, "Error reading inputListener mappings");
        }
    }

    /**
     * Persist the current mappings to the given path
     *
     * @param path Pointer to the file
     *
     * @return True if operation succeeded
     */
    public boolean persist(Path path) {

        Properties mappings = new SortedProperties();
        mappings.setProperty("axis.value.pow", Double.toString(AXIS_VALUE_POW));
        // L-stick
        mappings.setProperty("axis.lstick.h", Integer.toString(AXIS_LSTICK_H));
        mappings.setProperty("axis.lstick.h.sensitivity", Double.toString(AXIS_LSTICK_H_SENS));
        mappings.setProperty("axis.lstick.v", Integer.toString(AXIS_LSTICK_V));
        mappings.setProperty("axis.lstick.v.sensitivity", Double.toString(AXIS_LSTICK_V_SENS));
        // R-stick
        mappings.setProperty("axis.rstick.h", Integer.toString(AXIS_RSTICK_H));
        mappings.setProperty("axis.rstick.h.sensitivity", Double.toString(AXIS_RSTICK_H_SENS));
        mappings.setProperty("axis.rstick.v", Integer.toString(AXIS_RSTICK_V));
        mappings.setProperty("axis.rstick.v.sensitivity", Double.toString(AXIS_RSTICK_V_SENS));
        // Shoulder axes (RB, LB, RT, LT)
        mappings.setProperty("axis.rb", Integer.toString(AXIS_RB));
        mappings.setProperty("axis.lb", Integer.toString(AXIS_LB));
        mappings.setProperty("axis.rt", Integer.toString(AXIS_RT));
        mappings.setProperty("axis.rt.sensitivity", Double.toString(AXIS_RT_SENS));
        mappings.setProperty("axis.lt", Integer.toString(AXIS_LT));
        mappings.setProperty("axis.lt.sensitivity", Double.toString(AXIS_LT_SENS));

        // Regular buttons
        mappings.setProperty("button.a", Integer.toString(BUTTON_A));
        mappings.setProperty("button.b", Integer.toString(BUTTON_B));
        mappings.setProperty("button.x", Integer.toString(BUTTON_X));
        mappings.setProperty("button.y", Integer.toString(BUTTON_Y));
        mappings.setProperty("button.start", Integer.toString(BUTTON_START));
        mappings.setProperty("button.select", Integer.toString(BUTTON_SELECT));
        // Stick buttons
        mappings.setProperty("button.rstick", Integer.toString(BUTTON_RSTICK));
        mappings.setProperty("button.lstick", Integer.toString(BUTTON_LSTICK));
        // Shoulder buttons
        mappings.setProperty("button.rt", Integer.toString(BUTTON_RT));
        mappings.setProperty("button.rb", Integer.toString(BUTTON_RB));
        mappings.setProperty("button.lt", Integer.toString(BUTTON_LT));
        mappings.setProperty("button.lb", Integer.toString(BUTTON_LB));
        // Dpad
        mappings.setProperty("button.dpad.u", Integer.toString(BUTTON_DPAD_UP));
        mappings.setProperty("button.dpad.d", Integer.toString(BUTTON_DPAD_DOWN));
        mappings.setProperty("button.dpad.r", Integer.toString(BUTTON_DPAD_RIGHT));
        mappings.setProperty("button.dpad.l", Integer.toString(BUTTON_DPAD_LEFT));
        mappings.setProperty("axis.dpad.h", Integer.toString(AXIS_DPAD_H));
        mappings.setProperty("axis.dpad.v", Integer.toString(AXIS_DPAD_V));

        try {
            // Sort alphabetically
            OutputStream os = Files.newOutputStream(path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            mappings.store(os, "Controller mappings definition file for " + this.controllerName);
            logger.info("Controller mappings file written successfully: " + path.toAbsolutePath());
            return true;
        } catch (Exception e) {
            logger.error(e);
            return false;
        }
    }

    private int parseInt(Properties mappings, String... properties) {
        try {
            for (String property : properties) {
                if (mappings.containsKey(property)) {
                    return Integer.parseInt(mappings.getProperty(property, "-1"));
                }
            }
            throw new RuntimeException("Properties not found: " + GlobalResources.toString(properties, "", ","));
        } catch (Exception e) {
            return Integer.parseInt("-1");
        }
    }

    private double parseDouble(Properties mappings, String defaultValue, String... properties) {
        try {
            for (String property : properties) {
                if (mappings.containsKey(property)) {
                    return Double.parseDouble(mappings.getProperty(property, defaultValue));
                }
            }
            throw new RuntimeException("Properties not found: " + GlobalResources.toString(properties, "", ","));
        } catch (Exception e) {
            return Parser.parseDouble(defaultValue);
        }
    }

}
