/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

public enum FOV {

    FOV1 /* FoV identifier for observations from Preceding telescope with AC motion */,
    FOV2 /* FoV identifier for observations from Following telescope with AC motion */,
    FOV3 /* FoV identifier for observations from Preceding telescope without AC motion */,
    FOV4 /* FoV identifier for observations from Following telescope without AC motion */,
    ;

    /**
     * Mapping/Alias of the FOVs (with AC motion applied) to the "Following/Preceding" telescope nomenclature (mainly
     * for AGIS use)
     */
    public static final FOV P = FOV1;
    public static final FOV F = FOV2;

    /**
     * List of the telescope alias of the FOVs with AC motion applied
     */
    public static final FOV[] Telescopes = new FOV[] { P, F, };

    /**
     * Lists of FOVs grouped by associated telescope "Following/Preceding"
     */
    public static final FOV[] PrecedingFovs = new FOV[] { FOV1, FOV3, };
    public static final FOV[] FollowingFovs = new FOV[] { FOV2, FOV4, };

    /**
     * Lists of FOVs grouped by the application of AC motion in theis related observations
     */
    public static final FOV[] MotionFovs = new FOV[] { FOV1, FOV2, };
    public static final FOV[] NullMotionFovs = new FOV[] { FOV3, FOV4, };

    /**
     * Get enumeration type for the given FoV index [0-3]
     *
     * @param fovIndex FoV index [0-3]
     *
     * @return enumeration type for the desired FoV index, null if index is not valid
     */
    public static FOV getFov(int fovIndex) {

        if (validFov(fovIndex)) {

            return FOV.values()[fovIndex];
        }
        return null;
    }

    /**
     * Get enumeration type for the given FoV number [1-4]
     *
     * @param fovNumber FoV number [1-4]
     *
     * @return enumeration type for the desired FoV number, null if number is invalid
     */
    public static FOV getFovByNumber(int fovNumber) {

        int fovIndex = fovNumber - 1;
        if (validFov(fovIndex)) {

            return FOV.values()[fovIndex];
        }
        return null;
    }

    /**
     * Get the number of FoV identifiers (including the both motion configurations)
     *
     * @return Number of FoV identifiers
     */
    public static int getFovCount() {

        return FOV.values().length;
    }

    /**
     * Indicates if the FoV index is valid [0-3]
     *
     * @param fovIndex FoV [0-3]
     *
     * @return true if the FoV index is valid, false otherwise
     */
    public static boolean validFov(int fovIndex) {

        return (fovIndex >= FOV1.getIndex()) && (fovIndex <= FOV4.getIndex());
    }

    /**
     * Get FOV index [0-3]
     *
     * @return FOV index [FOV1:0, FOV2:1, FOV3:2, FOV4:3]
     */
    public byte getIndex() {

        return (byte) this.ordinal();
    }

    /**
     * Get FOV number [1-4]
     *
     * @return FOV number [FOV1:1, FOV2:2, FOV3:3, FOV4:4]
     */
    public byte getNumber() {

        return (byte) (this.ordinal() + 1);
    }

    /**
     * Get numerical field index defined as 1.0 for FOV1-3 and -1.0 for FOV2-4
     *
     * @return the numerical field index [-1.0:+1.0]
     */
    public double getNumericalFieldIndex() {

        if (this.equals(FOV1) || this.equals(FOV3)) {

            return +1.0d;
        }
        return -1.0d;
    }

    /**
     * Get telescope index [0-1]
     *
     * @return Telescope index [FOV1:0, FOV2:1, FOV3:0, FOV4:1]
     */
    public byte getTelescopeIndex() {

        return (byte) (this.ordinal() % 2);
    }

    /**
     * Get telescope number [1-2]
     *
     * @return Telescope number [FOV1:1, FOV2:2, FOV3:1, FOV4:2]
     */
    public byte getTelescopeNumber() {

        return (byte) ((this.ordinal() % 2) + 1);
    }

    /**
     * Determine whether a given field of view corresponds to the following telescope
     *
     * @return true if field of view corresponds to the following telescope, false otherwise
     */
    public boolean isFollowingTelescope() {
        return !isPrecedingTelescope();
    }

    /**
     * Indicates if this FoV has motion propagation in the Focal plane
     *
     * @return True if this FoV correspond to a motion propagation observation
     */
    public boolean isMotion() {

        return (this.ordinal() < FOV3.ordinal());
    }

    /**
     * Indicates if this FoV has Null motion propagation in the Focal plane
     *
     * @return True if this FoV correspond to a Null motion propagation observation
     */
    public boolean isNullMotion() {

        return !isMotion();
    }

    /**
     * Indicates whether this field of view corresponds to the preceding telescope
     *
     * @return true if field of view corresponds to the preceding telescope, false otherwise
     */
    public boolean isPrecedingTelescope() {

        return this.equals(FOV1) || this.equals(FOV3);
    }
}