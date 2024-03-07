/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.ucd;

import com.badlogic.gdx.utils.Array;
import gaiasky.util.TextUtils;
import gaiasky.util.ucd.UCD.UCDType;
import gaiasky.util.units.Position.PositionType;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;

import java.util.*;

public class UCDParser {
    // The following column names can either be strings or regular expressions. They are checked
    // first with equals() and then with matches()
    public static String[] idColNames = new String[] { "hip", "id", "source_id", "tycho2_id", "identifier" };
    public static String[] nameColNames = new String[] { "(name|NAME|refname|REFNAME)((_|-)[\\w\\d]+)?", "name", "names", "proper", "proper_name", "common_name",
            "designation" };
    public static String[] raColNames = new String[] { "ra", "right_ascension", "rightascension", "alpha", "raj2000" };
    public static String[] xColNames = new String[] { "x", "X" };
    public static String[] deColNames = new String[] { "dec", "de", "declination", "delta", "dej2000" };
    public static String[] yColNames = new String[] { "y", "Y" };
    public static String[] distColNames = new String[] { "dist", "distance" };
    public static String[] zColNames = new String[] { "z", "Z" };
    public static String[] parallaxColNames = new String[] { "plx", "parallax", "pllx", "par" };
    public static String[] magColNames = new String[] { "phot_g_mean_mag", "mag", "g_mag", "bmag", "gmag" };
    public static String[] colorColNames = new String[] { "b_v", "v_i", "bp_rp", "bp_g", "g_rp", "ci" };
    public static String[] tEffColNames = new String[] { "teff", "t_eff", "temperature", "effective_temperature" };
    public static String[] pmRaColNames = new String[] { "pmra", "pmalpha", "pm_ra", "mualpha" };
    public static String[] pmDecColNames = new String[] { "pmdec", "pmdelta", "pm_dec", "pm_de", "mudelta" };
    public static String[] radVelColNames = new String[] { "radial_velocity", "radvel", "rv", "dr2_radial_velocity" };
    public static String[] radiusColNames = new String[] { "radius", "rcluster", "radi", "core_radius", "tidal_radius", "total_radius" };
    public static String[] sizeColNames = new String[] { "diameter", "size", "linear_diameter" };
    public static String[] nStarsColNames = new String[] { "n", "nstars", "n_stars", "n_star" };
    public static String[] variMagsColNames = new String[] { "g_transit_mag", "g_mag_list", "g_mag_series" };
    public static String[] variTimesColNames = new String[] { "g_transit_time", "time_list", "time_series" };
    public static String[] periodColNames = new String[] { "pf", "period" };
    public Map<UCDType, Array<UCD>> ucdmap;
    // IDS
    public boolean hasId = false;
    public Array<UCD> ID;
    // NAME
    public boolean hasName = false;
    public Array<UCD> NAME;
    // POSITIONS
    public boolean hasPos = false;
    public Array<UCD> POS1, POS2, POS3;
    // PROPER MOTIONS
    public boolean hasPm = false;
    public Array<UCD> PMRA, PMDEC, RADVEL;
    // MAGNITUDES
    public boolean hasMag = false;
    public Array<UCD> MAG;
    // COLORS
    public boolean hasColor = false;
    public Array<UCD> COL;
    // PHYSICAL PARAMS
    public boolean hasSize = false;
    public Array<UCD> SIZE;
    public boolean hasTEff = false;
    public Array<UCD> TEFF;
    // VARIABILITY
    public boolean hasVariability = false;
    public boolean hasPeriod = false;
    public Array<UCD> VARI_TIMES, VARI_MAGS, VARI_PERIOD;
    // REST
    public Array<UCD> extra;

    public UCDParser() {
        super();
        ucdmap = new HashMap<>();
        ID = new Array<>();
        NAME = new Array<>();
        POS1 = new Array<>();
        POS2 = new Array<>();
        POS3 = new Array<>();
        MAG = new Array<>();
        COL = new Array<>();
        PMRA = new Array<>();
        PMDEC = new Array<>();
        RADVEL = new Array<>();
        SIZE = new Array<>();
        TEFF = new Array<>();
        VARI_TIMES = new Array<>();
        VARI_MAGS = new Array<>();
        VARI_PERIOD = new Array<>();
        extra = new Array<>();
    }

    public static boolean isName(String colName) {
        return TextUtils.contains(nameColNames, colName, true);
    }

    public static boolean isId(String colName) {
        return TextUtils.contains(idColNames, colName, true);
    }

    public static boolean isRa(String colName) {
        return TextUtils.contains(raColNames, colName, true);
    }

    public static boolean isX(String colName) {
        return TextUtils.contains(xColNames, colName, true);
    }

    public static boolean isDec(String colName) {
        return TextUtils.contains(deColNames, colName, true);
    }

    public static boolean isY(String colName) {
        return TextUtils.contains(yColNames, colName, true);
    }

    public static boolean isDist(String colName) {
        return TextUtils.contains(distColNames, colName, true);
    }

    public static boolean isZ(String colName) {
        return TextUtils.contains(zColNames, colName, true);
    }

    public static boolean isPllx(String colName) {
        return TextUtils.contains(parallaxColNames, colName, true);
    }

    public static boolean isMag(String colName) {
        return TextUtils.contains(magColNames, colName, true);
    }

    public static boolean isColor(String colName) {
        return TextUtils.contains(colorColNames, colName, true);
    }

    public static boolean isTeff(String colName) {
        return TextUtils.contains(tEffColNames, colName, true);
    }

    public static boolean isPmra(String colName) {
        return TextUtils.contains(pmRaColNames, colName, true);
    }

    public static boolean isPmde(String colName) {
        return TextUtils.contains(pmDecColNames, colName, true);
    }

    public static boolean isRadvel(String colName) {
        return TextUtils.contains(radVelColNames, colName, true);
    }

    public static boolean isSize(String colName) {
        return isRadius(colName);
    }

    public static boolean isRadius(String colName) {
        return TextUtils.contains(radiusColNames, colName, true);
    }

    public static boolean isNstars(String colName) {
        return TextUtils.contains(nStarsColNames, colName, true);
    }

    public static boolean isGVariMags(String colName) {
        return TextUtils.contains(variMagsColNames, colName, true);
    }

    public static boolean isGVariTimes(String colName) {
        return TextUtils.contains(variTimesColNames, colName, true);
    }

    public static boolean isPeriod(String colName) {
        return TextUtils.contains(periodColNames, colName, true);
    }

    /**
     * Parses the given table and puts the UCD info
     * into the ucdmap. The map and all the indices are overwritten.
     *
     * @param table The {@link StarTable} to parse
     */
    public void parse(StarTable table) {
        ucdmap.clear();
        int count = table.getColumnCount();
        for (int i = 0; i < count; i++) {
            // Get column
            ColumnInfo col = table.getColumnInfo(i);

            // Parse and add
            UCD ucd = new UCD(col.getUCD(), col.getName(), col.getUnitString(), i);
            addToMap(ucd);
        }

        // ID and NAME
        Array<UCD> meta = ucdmap.get(UCDType.META);
        if (meta != null)
            for (UCD candidate : meta) {
                if (TextUtils.contains(candidate.UCDStrings, "meta.id")) {
                    if (candidate.UCDStrings.length > 1 && TextUtils.contains(candidate.UCDStrings, "meta.main"))
                        this.ID.add(candidate);
                }
            }
        if (this.ID.isEmpty()) {
            this.ID.addAll(getByColNames(idColNames));
        }
        this.hasId = !this.ID.isEmpty();

        if (this.NAME.isEmpty()) {
            this.NAME.addAll(getByColNames(new UCDType[] { UCDType.META, UCDType.UNKNOWN, UCDType.MISSING }, nameColNames, null));
        }
        this.hasName = !this.NAME.isEmpty();

        // POSITIONS
        Array<UCD> pos = ucdmap.get(UCDType.POS);
        if (pos != null) {
            String posRefSys = getBestRefSys(pos);
            for (UCD candidate : pos) {
                String meaning = candidate.UCD[0][1];
                final String coord = candidate.UCD[0].length > 2 ? candidate.UCD[0][2] : null;
                boolean derived = checkDerivedQuantity(candidate.UCD);

                // Filter using best reference system (posRefSys)
                if (!derived && (meaning.equals(posRefSys) || meaning.equals("parallax") || meaning.equals("distance"))) {
                    switch (meaning) {
                    case "eq" -> {
                        switch (Objects.requireNonNull(coord)) {
                        case "ra" -> {
                            setDefaultUnit(candidate, "deg");
                            add(candidate, raColNames, this.POS1);
                        }
                        case "dec" -> {
                            setDefaultUnit(candidate, "deg");
                            add(candidate, deColNames, this.POS2);
                        }
                        }
                    }
                    case "ecliptic", "galactic" -> {
                        switch (Objects.requireNonNull(coord)) {
                        case "lon" -> {
                            setDefaultUnit(candidate, "deg");
                            this.POS1.add(candidate);
                        }
                        case "lat" -> {
                            setDefaultUnit(candidate, "deg");
                            this.POS2.add(candidate);
                        }
                        }
                    }
                    case "cartesian" -> {
                        setDefaultUnit(candidate, "pc");
                        switch (Objects.requireNonNull(coord)) {
                        case "x" -> this.POS1.add(candidate);
                        case "y" -> this.POS2.add(candidate);
                        case "z" -> this.POS3.add(candidate);
                        }
                    }
                    case "parallax" -> {
                        setDefaultUnit(candidate, "mas");
                        add(candidate, parallaxColNames, this.POS3);
                    }
                    case "distance" -> {
                        setDefaultUnit(candidate, "pc");
                        add(candidate, distColNames, this.POS3);
                    }
                    }
                }
            }
        }
        if (this.POS1.isEmpty() || this.POS2.isEmpty()) {
            // Try to work out from names
            this.POS1 = getByColNames(raColNames, "deg");
            if (!this.POS1.isEmpty()) {
                this.POS2 = getByColNames(deColNames, "deg");
                this.POS3 = getByColNames(distColNames, "pc");
                if (this.POS3.isEmpty()) {
                    this.POS3 = getByColNames(parallaxColNames, "mas");
                }
            }
            // Try cartesian
            if (this.POS1.isEmpty() || this.POS2.isEmpty()) {
                this.POS1 = getByColNames(xColNames, "pc");
                this.POS2 = getByColNames(yColNames, "pc");
                this.POS3 = getByColNames(zColNames, "pc");
            }
        }

        this.hasPos = !this.POS1.isEmpty() && !this.POS2.isEmpty();

        // PROPER MOTIONS

        // RA/DEC
        if (pos != null) {
            for (UCD candidate : pos) {
                if (candidate.UCD[0][1].equals("pm")) {
                    // Proper motion: pos.pm
                    // Followed by pos.[refsys].[coord]
                    try {
                        String refsys = candidate.UCD[1][1];
                        String coord = candidate.UCD[1][2];
                        if (refsys.equals("eq")) {
                            switch (coord) {
                            case "ra" -> this.PMRA.add(candidate);
                            case "dec" -> this.PMDEC.add(candidate);
                            }
                        }

                    } catch (Exception e) {
                        // No proper motion
                    }
                }
            }
        }
        if (this.PMRA.isEmpty() || this.PMDEC.isEmpty()) {
            // Try to work out from names
            this.PMRA = getByColNames(pmRaColNames, "mas/yr");
            if (!this.PMRA.isEmpty()) {
                this.PMDEC = getByColNames(pmDecColNames, "mas/yr");
                this.RADVEL = getByColNames(radVelColNames, "km/s");
            }
        }
        this.hasPm = !this.PMRA.isEmpty() && !this.PMDEC.isEmpty();

        // RADIAL VELOCITY
        Array<UCD> spect = ucdmap.get(UCDType.SPECT);
        if (spect != null)
            for (UCD candidate : spect) {
                if (candidate.UCD[0][1].equalsIgnoreCase("dopplerVeloc"))
                    this.RADVEL.add(candidate);
            }

        // MAGNITUDES
        Array<UCD> mag = ucdmap.get(UCDType.PHOT);
        if (mag != null)
            for (UCD candidate : mag) {
                if (TextUtils.contains(candidate.UCDStrings, "phot.mag")) {
                    if (candidate.UCDStrings.length == 1 || TextUtils.contains(candidate.UCDStrings, "stat.mean")|| TextUtils.startsWith(candidate.UCDStrings, "em.opt.")) {
                        this.MAG.add(candidate);
                    }
                }
            }
        if (this.MAG == null || this.MAG.isEmpty()) {
            this.MAG = getByColNames(magColNames, "mag");
        }
        this.hasMag = !this.MAG.isEmpty();

        // COLORS
        Array<UCD> col = ucdmap.get(UCDType.PHOT);
        if (col != null) {
            for (UCD candidate : col) {
                if (candidate.UCD[0][1].equals("color")) {
                    this.COL.add(candidate);
                    break;
                }
            }
        }
        if (this.COL == null || this.COL.isEmpty()) {
            this.COL = getByColNames(colorColNames);
        }
        this.hasColor = !this.COL.isEmpty();

        // SIZE
        Array<UCD> phys = ucdmap.get(UCDType.PHYS);
        if (phys != null) {
            for (UCD candidate : phys) {
                if (candidate.UCD[0].length >= 2 && candidate.UCD[0][1].equals("size")) {
                    this.SIZE.add(candidate);
                    break;
                }
            }
        }
        if (this.SIZE == null || this.SIZE.isEmpty()) {
            this.SIZE = getByColNames(radiusColNames, sizeColNames);
        }
        this.hasSize = !this.SIZE.isEmpty();

        // EFFECTIVE TEMPERATURE
        if (phys != null) {
            for (UCD candidate : phys) {
                if (candidate.UCD[0].length >= 3 && candidate.UCD[0][1].equals("temperature") && candidate.UCD[0][2].equals("effective")) {
                    this.TEFF.add(candidate);
                    break;
                }
            }
        }
        if (this.TEFF == null || this.TEFF.isEmpty()) {
            this.TEFF = getByColNames(tEffColNames);
        }
        this.hasTEff = !this.TEFF.isEmpty();

        // VARIABILITY
        Array<UCD> vari = ucdmap.get(UCDType.VARI);
        if (vari != null) {
            for (UCD candidate : vari) {
                if (candidate.UCD[0].length >= 3 && candidate.UCD[0][1].equals("time")) {
                    this.VARI_TIMES.add(candidate);
                    break;
                }
                if (candidate.UCD[0].length >= 3 && candidate.UCD[0][1].equals("magnitude")) {
                    this.VARI_MAGS.add(candidate);
                    break;
                }
            }
        }
        if (this.VARI_TIMES == null || this.VARI_TIMES.isEmpty()) {
            this.VARI_TIMES = getByColNames(variTimesColNames, "d");
        }
        if (this.VARI_MAGS == null || this.VARI_MAGS.isEmpty()) {
            this.VARI_MAGS = getByColNames(variMagsColNames, "mag");
        }
        this.hasVariability = !this.VARI_MAGS.isEmpty();
        if (this.VARI_PERIOD == null || this.VARI_PERIOD.isEmpty()) {
            this.VARI_PERIOD = getByColNames(periodColNames, "d");
        }
        this.hasPeriod = !this.VARI_PERIOD.isEmpty();

        // REST OF COLUMNS
        Set<UCDType> keys = ucdmap.keySet();
        for (UCDType ucdType : keys) {
            Array<UCD> ucds = ucdmap.get(ucdType);
            for (UCD ucd : ucds) {
                if (!has(ucd)) {
                    extra.add(ucd);
                }
            }
        }
    }

    public boolean has(UCD ucd) {
        return has(ucd, POS1) || has(ucd, POS2) || has(ucd, POS3) || has(ucd, PMRA) || has(ucd, PMDEC) || has(ucd, RADVEL) || has(ucd, ID) || has(ucd, COL) || has(ucd,
                                                                                                                                                                   TEFF)
                || has(ucd, NAME) || has(ucd, MAG);
    }

    public boolean has(UCD ucd,
                       Array<UCD> a) {
        return a.contains(ucd, true);
    }

    public PositionType getPositionType(UCD pos1,
                                        UCD pos2,
                                        UCD pos3) {
        if (pos1.UCD == null || pos2.UCD == null) {
            return PositionType.valueOf("EQ_SPH_" + (pos3 == null ? "PLX" : (isDist(pos3.colName) ? "DIST" : "PLX")));
        }
        String meaning = pos1.UCD[0][1];
        String postypestr = null, disttype = null;
        PositionType postype = null;
        switch (meaning) {
        case "eq" -> postypestr = "EQ_SPH_";
        case "ecliptic" -> postypestr = "ECL_SPH_";
        case "galactic" -> postypestr = "GAL_SPH_";
        case "cartesian" -> postype = PositionType.EQ_XYZ;
        }

        if (pos3 != null) {
            meaning = pos3.UCD[0][1];
            switch (meaning) {
            case "parallax" -> disttype = "PLX";
            case "distance" -> disttype = "DIST";
            }
        } else {
            disttype = "PLX";
        }

        if (postype == null && postypestr != null && disttype != null) {
            // Construct from postypestr and disttype
            postype = PositionType.valueOf(postypestr + disttype);
        }

        return postype;
    }

    public UCD getByColumName(String columName) {
        return getByColNames(new String[] { columName }).first();
    }

    private Array<UCD> getByColNames(String[]... ColNames) {
        return getByColNames(ColNames, null);
    }

    private Array<UCD> getByColNames(String[] ColNames,
                                     String defaultUnit) {
        return getByColNames(new UCDType[] { UCDType.UNKNOWN, UCDType.MISSING }, ColNames, defaultUnit);
    }

    private Array<UCD> getByColNames(String[][] ColNames,
                                     String defaultUnit) {
        return getByColNames(new UCDType[] { UCDType.UNKNOWN, UCDType.MISSING }, ColNames, defaultUnit);
    }

    private Array<UCD> getByColNames(UCDType[] types,
                                     String[] ColNames,
                                     String defaultUnit) {
        Array<UCD> candidates = new Array<>();
        for (UCDType type : types) {
            // Get all unknown and missing
            if (ucdmap.containsKey(type)) {
                Array<UCD> set = ucdmap.get(type);
                // Check column names
                for (UCD candidate : set) {
                    if (TextUtils.containsOrMatches(ColNames, candidate.colName, true)) {
                        if (defaultUnit != null && (candidate.unit == null || candidate.unit.isEmpty()))
                            candidate.unit = defaultUnit;
                        candidates.add(candidate);
                    }
                }
            }
        }
        return candidates;

    }

    private Array<UCD> getByColNames(UCDType[] types,
                                     String[][] ColNames,
                                     String defaultUnit) {
        Array<UCD> candidates = new Array<>();
        for (UCDType type : types) {
            // Get all unknown and missing
            if (ucdmap.containsKey(type)) {
                Array<UCD> set = ucdmap.get(type);
                // Check column names
                for (UCD candidate : set) {
                    if (TextUtils.containsOrMatches(ColNames, candidate.colName, true)) {
                        if (defaultUnit != null && (candidate.unit == null || candidate.unit.isEmpty()))
                            candidate.unit = defaultUnit;
                        candidates.add(candidate);
                    }
                }
            }
        }
        return candidates;
    }

    /**
     * Adds the given UCD to the list. If the column name of the candidates is in
     * the given array of column names, then it is added at the position 0, otherwise, it is
     * added at the back of the list.
     *
     * @param candidate The candidate UCD object
     * @param ColNames  Array of column names to check
     * @param list      The list to add
     */
    private void add(UCD candidate,
                     String[] ColNames,
                     Array<UCD> list) {
        if (candidate.colName != null && TextUtils.contains(ColNames, candidate.colName)) {
            list.insert(0, candidate);
        } else {
            list.add(candidate);
        }

    }

    private String getBestRefSys(Array<UCD> ucds) {
        boolean eq = false, ecl = false, gal = false, cart = false;
        for (UCD candidate : ucds) {
            eq = eq || candidate.UCD[0][1].equals("eq");
            ecl = ecl || candidate.UCD[0][1].equals("ecliptic");
            gal = gal || candidate.UCD[0][1].equals("galactic");
            cart = cart || candidate.UCD[0][1].equals("cartesian");
        }
        if (eq)
            return "eq";
        else if (gal)
            return "galactic";
        else if (ecl)
            return "ecliptic";
        else if (cart)
            return "cartesian";
        return "";
    }

    private void setDefaultUnit(UCD candidate,
                                String unit) {
        // Default unit
        if (candidate.unit == null) {
            candidate.unit = unit;
        }
    }

    /**
     * Checks whether this UCD is a derived quantity (ratio, etc.).
     *
     * @param ucd The UCD to test.
     *
     * @return True if the given UCD is a derived quantity.
     */
    private boolean checkDerivedQuantity(String[][] ucd) {
        for (String[] strings : ucd) {
            if (strings != null && strings.length > 0) {
                // Check ratio or factor
                if (strings[0].equals("arith")) {
                    if (strings.length > 1 && (strings[1].equals("ratio") || strings[1].equals("factor"))) {
                        return true;
                    }
                }

                // Check statistic (correlation, etc.)
                if (strings[0].equals("stat")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addToMap(UCD ucd) {
        if (!ucdmap.containsKey(ucd.type)) {
            Array<UCD> set = new Array<>(2);
            set.add(ucd);
            ucdmap.put(ucd.type, set);
        } else {
            ucdmap.get(ucd.type).add(ucd);
        }
    }

}
