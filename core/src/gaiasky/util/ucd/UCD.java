/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.ucd;

/**
 * Very naive class that represents and UCD and does the parsing.
 */
public class UCD {

    public String originalucd, converted, colname, unit;
    public String[][] ucd;
    public String[] ucdstrings;
    public UCDType type;
    public int index;
    public UCD(String originalucd, String colname, String unit, int index) {
        super();

        this.index = index;
        this.colname = colname;
        this.unit = unit;
        if (originalucd != null && !originalucd.isEmpty()) {
            this.originalucd = originalucd;
            // Convert UCD1 to
            this.converted = originalucd.toLowerCase().replace("_", ".");

            this.ucdstrings = this.converted.split(";");

            this.ucd = new String[ucdstrings.length][];

            for (int i = 0; i < ucdstrings.length; i++) {
                String singleucd = ucdstrings[i];
                String[] ssplit = singleucd.split("\\.");
                this.ucd[i] = ssplit;
            }

            // Type
            String currtype = this.ucd[0][0];
            try {
                this.type = UCDType.valueOf(currtype.toUpperCase());
            } catch (Exception e) {
                this.type = UCDType.UNKNOWN;
            }
        } else {
            this.type = UCDType.MISSING;
        }

    }

    @Override
    public String toString() {
        return colname + (this.originalucd == null ? "" : " - " + this.originalucd);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UCD ucd = (UCD) o;
        return index == ucd.index &&
                originalucd.equals(ucd.originalucd) &&
                colname.equals(ucd.colname) &&
                unit.equals(ucd.unit);
    }

    enum UCDType {
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
