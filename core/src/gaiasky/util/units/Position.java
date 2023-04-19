/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.units;

import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector3d;
import gaiasky.util.units.Quantity.Angle;
import gaiasky.util.units.Quantity.Angle.AngleUnit;
import gaiasky.util.units.Quantity.Length;
import gaiasky.util.units.Quantity.Length.LengthUnit;

public class Position {

    public final Vector3d realPosition;

    /**
     * Works out the cartesian equatorial position in the Gaia Sandbox reference
     * system. The units of the result are parsecs.
     *
     * @throws RuntimeException On negative distance or parallax
     */
    public Position(double a, String unitA, double b, String unitB, double c, String unitC, PositionType type) throws RuntimeException {
        if (Double.isNaN(c)) {
            // Parallax not available
            c = 0.04;
            unitC = "mas";
        }
        realPosition = new Vector3d();

        switch (type) {
        case EQ_SPH_DIST:

            Angle lon = new Angle(a, unitA);
            Angle lat = new Angle(b, unitB);
            Length dist = new Length(c, unitC);

            if (dist.value_m <= 0) {
                throw new RuntimeException("Negative distance found: " + dist.value_m + " m");
            }

            Coordinates.sphericalToCartesian(lon.get(AngleUnit.RAD), lat.get(AngleUnit.RAD), dist.get(LengthUnit.PC), realPosition);

            break;
        case EQ_SPH_PLX:

            lon = new Angle(a, unitA);
            lat = new Angle(b, unitB);
            dist = new Angle(c, unitC).getParallaxDistance();

            if (dist.value_m <= 0) {
                throw new RuntimeException("Negative parallax found: " + dist.value_m + " m");
            }

            Coordinates.sphericalToCartesian(lon.get(AngleUnit.RAD), lat.get(AngleUnit.RAD), dist.get(LengthUnit.PC), realPosition);

            break;
        case GAL_SPH_DIST:

            lon = new Angle(a, unitA);
            lat = new Angle(b, unitB);
            dist = new Length(c, unitC);

            if (dist.value_m <= 0) {
                throw new RuntimeException("Negative distance found: " + dist.value_m + " m");
            }

            Coordinates.sphericalToCartesian(lon.get(AngleUnit.RAD), lat.get(AngleUnit.RAD), dist.get(LengthUnit.PC), realPosition);
            realPosition.mul(Coordinates.galToEq());
            break;
        case GAL_SPH_PLX:

            lon = new Angle(a, unitA);
            lat = new Angle(b, unitB);
            dist = new Angle(c, unitC).getParallaxDistance();

            if (dist.value_m <= 0) {
                throw new RuntimeException("Negative parallax found: " + dist.value_m + " m");
            }

            Coordinates.sphericalToCartesian(lon.get(AngleUnit.RAD), lat.get(AngleUnit.RAD), dist.get(LengthUnit.PC), realPosition);
            realPosition.mul(Coordinates.galToEq());
            break;
        case ECL_SPH_DIST:

            lon = new Angle(a, unitA);
            lat = new Angle(b, unitB);
            dist = new Length(c, unitC);

            if (dist.value_m <= 0) {
                throw new RuntimeException("Negative distance found: " + dist.value_m + " m");
            }

            Coordinates.sphericalToCartesian(lon.get(AngleUnit.RAD), lat.get(AngleUnit.RAD), dist.get(LengthUnit.PC), realPosition);
            realPosition.mul(Coordinates.eclToEq());
            break;
        case ECL_SPH_PLX:

            lon = new Angle(a, unitA);
            lat = new Angle(b, unitB);
            dist = new Angle(c, unitC).getParallaxDistance();

            if (dist.value_m <= 0) {
                throw new RuntimeException("Negative parallax found: " + dist.value_m + " m");
            }

            Coordinates.sphericalToCartesian(lon.get(AngleUnit.RAD), lat.get(AngleUnit.RAD), dist.get(LengthUnit.PC), realPosition);
            realPosition.mul(Coordinates.eclToEq());
            break;
        case EQ_XYZ:

            Length x = new Length(a, unitA);
            Length y = new Length(b, unitB);
            Length z = new Length(c, unitC);

            realPosition.set(x.get(LengthUnit.PC), y.get(LengthUnit.PC), z.get(LengthUnit.PC));

            break;
        case GAL_XYZ:

            x = new Length(a, unitA);
            y = new Length(b, unitB);
            z = new Length(c, unitC);

            realPosition.set(x.get(LengthUnit.PC), y.get(LengthUnit.PC), z.get(LengthUnit.PC));
            realPosition.mul(Coordinates.galToEq());
            break;
        case ECL_XYZ:

            x = new Length(a, unitA);
            y = new Length(b, unitB);
            z = new Length(c, unitC);

            realPosition.set(x.get(LengthUnit.PC), y.get(LengthUnit.PC), z.get(LengthUnit.PC));
            realPosition.mul(Coordinates.eclToEq());
            break;
        default:
            break;
        }

    }

    public enum PositionType {
        EQ_SPH_DIST,
        EQ_SPH_PLX,

        ECL_SPH_DIST,
        ECL_SPH_PLX,

        GAL_SPH_DIST,
        GAL_SPH_PLX,

        EQ_XYZ,
        ECL_XYZ,
        GAL_XYZ;

        public boolean isParallax() {
            return this.equals(EQ_SPH_PLX) || this.equals(ECL_SPH_PLX) || this.equals(GAL_SPH_PLX);
        }
    }
}
