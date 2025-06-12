/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.ucd;

import java.util.Locale;

/**
 * Represents the UCD (universal content descriptor) for a column of a table.
 */
public class UCD implements Comparable<UCD> {

    public String originalUCD, converted, colName, unit;
    public String[][] UCD;
    public String[] UCDStrings;
    public UCDType type;
    public int index;
    public UCD(String originalUCD, String colName, String unit, int index) {
        super();

        this.index = index;
        this.colName = colName;
        this.unit = unit;
        if (originalUCD != null && !originalUCD.isEmpty()) {
            this.originalUCD = originalUCD;
            // Convert UCD1 to
            this.converted = originalUCD.toLowerCase(Locale.ROOT).replace("_", ".");

            this.UCDStrings = this.converted.split(";");

            this.UCD = new String[UCDStrings.length][];

            for (int i = 0; i < UCDStrings.length; i++) {
                String singleUCD = UCDStrings[i];
                String[] sSplit = singleUCD.split("\\.");
                this.UCD[i] = sSplit;
            }

            // Type
            String currType = this.UCD[0][0];
            try {
                this.type = UCDType.valueOf(currType.toUpperCase());
            } catch (Exception e) {
                this.type = UCDType.UNKNOWN;
            }
        } else {
            this.type = UCDType.MISSING;
        }

    }

    @Override
    public String toString() {
        return colName + (this.originalUCD == null ? "" : " - " + this.originalUCD);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UCD ucd = (UCD) o;
        return index == ucd.index &&
                originalUCD.equals(ucd.originalUCD) &&
                colName.equals(ucd.colName) &&
                unit.equals(ucd.unit);
    }

    @Override
    public int compareTo(gaiasky.util.ucd.UCD ucd) {
        return colName.compareTo(ucd.colName);
    }

    public enum UCDType {
        POS,
        PHOT,
        STAT,
        PHYS,
        META,
        ARITH,
        EM,
        OBS,
        SPECT,
        SRC,
        TIME,
        INSTR,
        VARI,
        UNKNOWN,
        MISSING
    }

}
