/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.units;

import gaiasky.util.units.Quantity.Angle.AngleUnit;
import gaiasky.util.units.Quantity.Brightness.BrightnessUnit;
import gaiasky.util.units.Quantity.Length.LengthUnit;

public class Quantity {

    private static LengthUnit parseLength(String unit) throws IllegalArgumentException {
        // Check format 'measure[unit]'
        if (unit.matches("[^\\[\\]]+\\[[^\\[\\]]+]")) {
            return LengthUnit.valueOf(unit.substring(unit.indexOf('[') + 1, unit.indexOf(']')).toUpperCase()); //-V6009
        } else {
            return LengthUnit.valueOf(unit.toUpperCase());
        }
    }

    private static AngleUnit parseAngle(String unit) throws IllegalArgumentException {
        // Check format 'measure[unit]'
        if (unit.matches("[^\\[\\]]+\\[[^\\[\\]]+]")) {
            return AngleUnit.valueOf(unit.substring(unit.indexOf('[') + 1, unit.indexOf(']')).toUpperCase()); //-V6009
        } else {
            return AngleUnit.valueOf(unit.toUpperCase());
        }
    }

    private static BrightnessUnit parseMag(String unit) throws IllegalArgumentException {
        // Check format 'measure[unit]'
        if (unit.matches("[^\\[\\]]+\\[[^\\[\\]]+]")) {
            return BrightnessUnit.valueOf(unit.substring(unit.indexOf('[') + 1, unit.indexOf(']')).toUpperCase()); //-V6009
        } else {
            return BrightnessUnit.valueOf(unit.toUpperCase());
        }
    }

    public static class Length {

        double value_m;

        public Length(double value, LengthUnit unit) {
            value_m = value * unit.m;
        }

        public Length(double value, String unit) {
            this(value, parseLength(unit));
        }

        public static boolean isLength(String unit) {
            try {
                parseLength(unit);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public double get(LengthUnit unit) {
            return value_m * (1d / unit.m);
        }

        public enum LengthUnit {
            /** Millimetres **/
            MM(1d / 1000d),
            /** Centimetres **/
            CM(1d / 100d),
            /** Metres **/
            M(1d),
            /** Kilometres **/
            KM(1000d),
            /** Astronomical units **/
            AU(149597870700d),
            /** Parsecs **/
            PC(3.08567758e16),
            /** Megaparsecs **/
            MPC(3.08567758e22);

            final double m;

            LengthUnit(double m) {
                this.m = m;
            }

        }

    }

    public static class Angle {

        double value_deg;

        public Angle(double value, AngleUnit unit) {
            value_deg = value * unit.deg;
        }

        public Angle(double value, String unit) {
            this(value, parseAngle(unit));
        }

        public static boolean isAngle(String unit) {
            try {
                parseAngle(unit);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public double get(AngleUnit unit) {
            return value_deg * (1d / unit.deg);
        }

        /**
         * Gets the parallax distance of this angle.
         *
         * @return A length with the distance of this angle interpreted as a parallax.
         */
        public Length getParallaxDistance() {
            double mas = get(AngleUnit.MAS);
            if (Double.isFinite(mas) && mas <= 0) {
                // What to do with zero or negative parallaxes?
                return new Length(-1, LengthUnit.PC);
            }
            return new Length(1000d / mas, LengthUnit.PC);
        }

        public enum AngleUnit {
            /** Degrees **/
            DEG(1d),
            /** Radians **/
            RAD(180d / Math.PI),
            /** Milliarcseconds **/
            MAS(1d / 3600000d),
            /** Arcseconds **/
            ARCSEC(1d / 3600d),
            ARCMIN(1d / 60d);

            final double deg;

            AngleUnit(double deg) {
                this.deg = deg;
            }

        }

    }

    public static class Brightness {

        double value_mag;

        public Brightness(double value, BrightnessUnit unit) {
            value_mag = value * unit.mag;
        }

        public Brightness(double value, String unit) {
            this(value, parseMag(unit));
        }

        public static boolean isBrightness(String unit) {
            try {
                parseMag(unit);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public double get(BrightnessUnit unit) {
            return value_mag * (1d / unit.mag);
        }

        public enum BrightnessUnit {
            MAG(1d);

            final double mag;

            BrightnessUnit(double mag) {
                this.mag = mag;
            }
        }

    }
}
