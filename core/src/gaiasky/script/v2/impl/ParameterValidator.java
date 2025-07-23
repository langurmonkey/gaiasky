/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import com.badlogic.ashley.core.Entity;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.validator.RegexpValidator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static gaiasky.util.Logger.getLogger;

/**
 * This class contains utility methods to check the validity of different parameter types.
 */
public class ParameterValidator {
    protected final static Logger.Log logger = getLogger(ParameterValidator.class);

    /** Reference to API object. **/
    private final APIv2 api;

    public ParameterValidator(APIv2 api) {
        this.api = api;
    }

    boolean checkNum(int value, int min, int max, String name) {
        if (value < min || value > max) {
            logger.error(name + " must be between " + min + " and " + max + ": " + value);
            return false;
        }
        return true;
    }

    boolean checkNum(long value, long min, long max, String name) {
        if (value < min || value > max) {
            logger.error(name + " must be between " + min + " and " + max + ": " + value);
            return false;
        }
        return true;
    }

    boolean checkNum(float value, float min, float max, String name) {
        if (value < min || value > max) {
            logger.error(name + " must be between " + min + " and " + max + ": " + value);
            return false;
        }
        return true;
    }

    boolean checkNum(double value, double min, double max, String name) {
        if (value < min || value > max) {
            logger.error(name + " must be between " + min + " and " + max + ": " + value);
            return false;
        }
        return true;
    }

    boolean checkFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            logger.error(name + " must be finite: " + value);
            return false;
        }
        return true;
    }

    boolean checkFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            logger.error(name + " must be finite: " + value);
            return false;
        }
        return true;
    }

    boolean checkLengths(double[] array, int length1, int length2, String name) {
        if (array.length != length1 && array.length != length2) {
            logger.error(name + " must have a length of " + length1 + " or " + length2 + ". Current length is " + array.length);
            return false;
        }
        return true;
    }

    boolean checkLength(double[] array, int length, String name) {
        if (array.length != length) {
            logger.error(name + " must have a length of " + length + ". Current length is " + array.length);
            return false;
        }
        return true;
    }

    boolean checkLength(float[] array, int length, String name) {
        if (array.length != length) {
            logger.error(name + " must have a length of " + length + ". Current length is " + array.length);
            return false;
        }
        return true;
    }

    boolean checkString(String value, String name) {
        if (value == null || value.isEmpty()) {
            logger.error(name + " can't be null nor empty");
            return false;
        }
        return true;
    }

    boolean checkString(String value, String[] possibleValues, String name) {
        if (checkString(value, name)) {
            for (String v : possibleValues) {
                if (value.equals(v)) return true;
            }
            logPossibleValues(value, possibleValues, name);
            return false;
        }
        logPossibleValues(value, possibleValues, name);
        return false;
    }

    boolean checkDirectoryExists(String location, String name) {
        if (location == null) {
            logger.error(name + ": location can't be null");
            return false;
        }
        Path p = Path.of(location);
        if (Files.notExists(p)) {
            logger.error(name + ": path does not exist");
            return false;
        }
        return true;
    }

    boolean checkObjectName(String name) {
        if (api.scene.get_entity(name) == null) {
            logger.error(name + ": object with this name does not exist");
            return false;
        }
        return true;
    }

    boolean checkObjectName(String name, double timeOutSeconds) {
        if (api.scene.get_entity(name, timeOutSeconds) == null) {
            logger.error(name + ": object with this name does not exist");
            return false;
        }
        return true;
    }

    boolean checkDatasetName(String name) {
        if (!api.catalogManager.contains(name)) {
            logger.error(name + ": no dataset found with the given name");
            return false;
        }
        return true;
    }

    boolean checkFocusName(String name) {
        Entity entity = api.scene.get_focus(name);
        if (entity == null) {
            logger.error(name + ": focus with this name does not exist");
        }
        return entity != null;
    }

    boolean checkDateTime(int year, int month, int day, int hour, int min, int sec, int millisec) {
        boolean ok;
        ok = checkNum(month, 1, 12, "month");
        ok = ok && checkNum(day, 1, 31, "month");
        ok = ok && checkNum(hour, 0, 23, "month");
        ok = ok && checkNum(min, 0, 59, "month");
        ok = ok && checkNum(sec, 0, 59, "month");
        ok = ok && checkNum(millisec, 0, 990, "month");

        if (ok) {
            // Try to create a date.
            try {
                var date = LocalDateTime.of(year, month, day, hour, min, sec, millisec);
            } catch (DateTimeException e) {
                logger.error("Date/time error: " + e.getLocalizedMessage());
                ok = false;
            }
        }
        return ok;
    }

    <T extends Enum<T>> boolean checkStringEnum(String value, Class<T> clazz, String name) {
        if (checkString(value, name)) {
            for (Enum<T> en : EnumSet.allOf(clazz)) {
                if (value.equalsIgnoreCase(en.toString()) || value.equalsIgnoreCase(en.name())) {
                    return true;
                }
            }
            logger.error(name + " value not valid: " + value + ". Must be a value in the enum " + clazz.getSimpleName() + ":");
            for (Enum<T> en : EnumSet.allOf(clazz)) {
                logger.error(en.toString());
            }
        }
        return false;
    }

    boolean checkNotNull(Object o, String name) {
        if (o == null) {
            logger.error(name + " can't be null");
            return false;
        }
        return true;
    }

    boolean checkDistanceUnits(String units) {
        try {
            Settings.DistanceUnits.valueOf(units.toUpperCase(Locale.ROOT));
            return true;
        } catch (Exception e) {
            logger.error("Unknown distance units: " + units);
            return false;
        }
    }

    boolean checkSmoothType(String type, String name) {
        return type.equalsIgnoreCase("logit") || type.equalsIgnoreCase("logisticsigmoid") || type.equalsIgnoreCase("none");
    }

    boolean checkRegexp(String regexp) {
        try {
            Pattern.compile(regexp);
            return true;
        } catch (PatternSyntaxException e) {
            logger.error("Invalid regular expression: " + regexp);
            return false;
        }
    }

    private void logPossibleValues(String value, String[] possibleValues, String name) {
        logger.error(name + " value not valid: " + value + ". Possible values are:");
        for (String v : possibleValues)
            logger.error(v);
    }

}
