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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class UCDParser {
    // The following column names can either be strings or regular expressions. They are checked
    // first with equals() and then with matches()
    public static String[] ID_NAMES = new String[] { "hip", "id", "source_id", "tycho2_id", "identifier" };
    public static String[] NAME_NAMES = new String[] { "(name|NAME|refname|REFNAME)((_|-)[\\w\\d]+)?", "name", "names", "proper", "proper_name", "common_name",
            "designation", "denomination" };
    public static String[] RA_NAMES = new String[] { "ra", "right_ascension", "rightascension", "alpha", "raj2000" };
    public static String[] X_NAMES = new String[] { "x", "X" };
    public static String[] DEC_NAMES = new String[] { "dec", "de", "declination", "delta", "dej2000" };
    public static String[] Y_NAMES = new String[] { "y", "Y" };
    public static String[] DIST_NAMES = new String[] { "dist", "distance" };
    public static String[] Z_NAMES = new String[] { "z", "Z" };
    public static String[] PLLX_NAMES = new String[] { "plx", "parallax", "pllx", "par" };
    public static String[] MAG_NAMES = new String[] { "phot_g_mean_mag", "mag", "g_mag", "bmag", "gmag" };
    public static String[] COLOR_NAMES = new String[] { "b_v", "v_i", "bp_rp", "bp_g", "g_rp", "ci" };
    public static String[] TEFF_NAMES = new String[] { "teff", "t_eff", "temperature", "effective_temperature" };
    public static String[] PMRA_NAMES = new String[] { "pmra", "pmalpha", "pm_ra", "mualpha" };
    public static String[] PMDEC_NAMES = new String[] { "pmdec", "pmdelta", "pm_dec", "pm_de", "mudelta" };
    public static String[] RADVEL_NAMES = new String[] { "radial_velocity", "radvel", "rv", "dr2_radial_velocity" };
    public static String[] RADIUS_NAMES = new String[] { "radius", "rcluster", "radi", "core_radius", "tidal_radius", "total_radius" };
    public static String[] SIZE_NAMES = new String[] { "diameter", "size", "linear_diameter" };
    public static String[] NSTARS_NAMES = new String[] { "n", "nstars", "n_stars", "n_star" };
    public static String[] VARIMAGS_NAMES = new String[] { "g_transit_mag", "g_mag_list", "g_mag_series" };
    public static String[] VARITIMES_NAMES = new String[] { "g_transit_time", "time_list", "time_series" };
    public static String[] PERIOD_NAMES = new String[] { "pf", "period" };
    public static String[] EPOCH_NAMES = new String[] { "epoch", "epoch_state_vector", "epoch_jd", "epochjd" };
    public static String[] SMA_NAMES = new String[]{"semimajoraxis", "sma", "semimajax"};
    public static String[] ECC_NAMES = new String[]{"e", "eccentricity", "ecc"};
    public static String[] INC_NAMES = new String[]{"i", "inclination", "inc"};
    public static String[] ASCNODE_NAMES = new String[]{"an", "ascendingnode", "ascnode", "ascending_node"};
    public static String[] ARGPERI_NAMES = new String[]{"aop", "argofpericenter", "aopericenter", "aperi", "argperi", "arg_of_pericenter"};
    public static String[] MANOMALY_NAMES = new String[]{"ma", "man", "meananomaly", "mean_anomaly"};
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
    // KEPLERIAN ELEMENTS
    public boolean hasKeplerElements = false;
    public Array<UCD> EPOCH, SMA, ECC, INC, ASCNODE, ARGPERI, MANOMALY;
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
        EPOCH = new Array<>();
        SMA = new Array<>();
        ECC = new Array<>();
        INC = new Array<>();
        ASCNODE = new Array<>();
        ARGPERI = new Array<>();
        MANOMALY = new Array<>();
        extra = new Array<>();
    }

    public static boolean isName(String colName) {
        return TextUtils.contains(NAME_NAMES, colName, true);
    }

    public static boolean isId(String colName) {
        return TextUtils.contains(ID_NAMES, colName, true);
    }

    public static boolean isRa(String colName) {
        return TextUtils.contains(RA_NAMES, colName, true);
    }

    public static boolean isX(String colName) {
        return TextUtils.contains(X_NAMES, colName, true);
    }

    public static boolean isDec(String colName) {
        return TextUtils.contains(DEC_NAMES, colName, true);
    }

    public static boolean isY(String colName) {
        return TextUtils.contains(Y_NAMES, colName, true);
    }

    public static boolean isDist(String colName) {
        return TextUtils.contains(DIST_NAMES, colName, true);
    }

    public static boolean isZ(String colName) {
        return TextUtils.contains(Z_NAMES, colName, true);
    }

    public static boolean isPllx(String colName) {
        return TextUtils.contains(PLLX_NAMES, colName, true);
    }

    public static boolean isMag(String colName) {
        return TextUtils.contains(MAG_NAMES, colName, true);
    }

    public static boolean isColor(String colName) {
        return TextUtils.contains(COLOR_NAMES, colName, true);
    }

    public static boolean isTeff(String colName) {
        return TextUtils.contains(TEFF_NAMES, colName, true);
    }

    public static boolean isPmra(String colName) {
        return TextUtils.contains(PMRA_NAMES, colName, true);
    }

    public static boolean isPmde(String colName) {
        return TextUtils.contains(PMDEC_NAMES, colName, true);
    }

    public static boolean isRadvel(String colName) {
        return TextUtils.contains(RADVEL_NAMES, colName, true);
    }

    public static boolean isSize(String colName) {
        return isRadius(colName);
    }

    public static boolean isRadius(String colName) {
        return TextUtils.contains(RADIUS_NAMES, colName, true);
    }

    public static boolean isNstars(String colName) {
        return TextUtils.contains(NSTARS_NAMES, colName, true);
    }

    public static boolean isGVariMags(String colName) {
        return TextUtils.contains(VARIMAGS_NAMES, colName, true);
    }

    public static boolean isGVariTimes(String colName) {
        return TextUtils.contains(VARITIMES_NAMES, colName, true);
    }

    public static boolean isPeriod(String colName) {
        return TextUtils.contains(PERIOD_NAMES, colName, true);
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
            this.ID.addAll(getByColNames(ID_NAMES));
        }
        this.hasId = !this.ID.isEmpty();

        if (this.NAME.isEmpty()) {
            this.NAME.addAll(getByColNames(new UCDType[] { UCDType.META, UCDType.UNKNOWN, UCDType.MISSING }, NAME_NAMES, null));
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
                            add(candidate, RA_NAMES, this.POS1);
                        }
                        case "dec" -> {
                            setDefaultUnit(candidate, "deg");
                            add(candidate, DEC_NAMES, this.POS2);
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
                        add(candidate, PLLX_NAMES, this.POS3);
                    }
                    case "distance" -> {
                        setDefaultUnit(candidate, "pc");
                        add(candidate, DIST_NAMES, this.POS3);
                    }
                    }
                }
            }
        }
        if (this.POS1.isEmpty() || this.POS2.isEmpty()) {
            // Try to work out from names
            this.POS1 = getByColNames(RA_NAMES, "deg");
            if (!this.POS1.isEmpty()) {
                this.POS2 = getByColNames(DEC_NAMES, "deg");
                this.POS3 = getByColNames(DIST_NAMES, "pc");
                if (this.POS3.isEmpty()) {
                    this.POS3 = getByColNames(PLLX_NAMES, "mas");
                }
            }
            // Try cartesian
            if (this.POS1.isEmpty() || this.POS2.isEmpty()) {
                this.POS1 = getByColNames(X_NAMES, "pc");
                this.POS2 = getByColNames(Y_NAMES, "pc");
                this.POS3 = getByColNames(Z_NAMES, "pc");
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
            this.PMRA = getByColNames(PMRA_NAMES, "mas/yr");
            if (!this.PMRA.isEmpty()) {
                this.PMDEC = getByColNames(PMDEC_NAMES, "mas/yr");
                this.RADVEL = getByColNames(RADVEL_NAMES, "km/s");
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
                    if (candidate.UCDStrings.length == 1 || TextUtils.contains(candidate.UCDStrings, "stat.mean")|| TextUtils.startsWith(candidate.UCDStrings, "em.opt")) {
                        this.MAG.add(candidate);
                    }
                }
            }
        if (this.MAG == null || this.MAG.isEmpty()) {
            this.MAG = getByColNames(MAG_NAMES, "mag");
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
            this.COL = getByColNames(COLOR_NAMES);
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
            this.SIZE = getByColNames(RADIUS_NAMES, SIZE_NAMES);
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
            this.TEFF = getByColNames(TEFF_NAMES);
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
            this.VARI_TIMES = getByColNames(VARITIMES_NAMES, "d");
        }
        if (this.VARI_MAGS == null || this.VARI_MAGS.isEmpty()) {
            this.VARI_MAGS = getByColNames(VARIMAGS_NAMES, "mag");
        }
        this.hasVariability = !this.VARI_MAGS.isEmpty();
        if (this.VARI_PERIOD == null || this.VARI_PERIOD.isEmpty()) {
            this.VARI_PERIOD = getByColNames(PERIOD_NAMES, "d");
        }
        this.hasPeriod = !this.VARI_PERIOD.isEmpty();

        // KEPLER ORBITAL ELEMENTS
        if (this.EPOCH == null || this.EPOCH.isEmpty()) {
            this.EPOCH = getByColNames(EPOCH_NAMES, "d");
        }
        if (this.SMA == null || this.SMA.isEmpty()) {
            this.SMA = getByColNames(SMA_NAMES, "au");
        }
        if (this.ECC == null || this.ECC.isEmpty()) {
            this.ECC = getByColNames(ECC_NAMES, null);
        }
        if (this.INC == null || this.INC.isEmpty()) {
            this.INC = getByColNames(INC_NAMES, "deg");
        }
        if (this.ASCNODE == null || this.ASCNODE.isEmpty()) {
            this.ASCNODE = getByColNames(ASCNODE_NAMES, "deg");
        }
        if (this.ARGPERI == null || this.ARGPERI.isEmpty()) {
            this.ARGPERI = getByColNames(ARGPERI_NAMES, "deg");
        }
        if (this.MANOMALY == null || this.MANOMALY.isEmpty()) {
            this.MANOMALY = getByColNames(MANOMALY_NAMES, "deg");
        }
        this.hasKeplerElements = !this.SMA.isEmpty() && !this.ECC.isEmpty() && !this.INC.isEmpty()
                && !this.ASCNODE.isEmpty() && !this.ARGPERI.isEmpty() && !this.MANOMALY.isEmpty();

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
        return has(ucd, POS1) || has(ucd, POS2) || has(ucd, POS3)
                || has(ucd, PMRA) || has(ucd, PMDEC) || has(ucd, RADVEL)
                || has(ucd, ID) || has(ucd, COL) || has(ucd, TEFF)
                || has(ucd, NAME) || has(ucd, MAG)
                || has(ucd, EPOCH) || has(ucd, SMA) || has(ucd, ECC)
                || has(ucd, INC) || has(ucd, ASCNODE) || has(ucd, ARGPERI)
                || has(ucd, MANOMALY);
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
        String posTypeStr = null, distType = null;
        PositionType postype = null;
        switch (meaning) {
        case "eq" -> posTypeStr = "EQ_SPH_";
        case "ecliptic" -> posTypeStr = "ECL_SPH_";
        case "galactic" -> posTypeStr = "GAL_SPH_";
        case "cartesian" -> postype = PositionType.EQ_XYZ;
        }

        if (pos3 != null) {
            meaning = pos3.UCD[0][1];
            switch (meaning) {
            case "parallax" -> distType = "PLX";
            case "distance" -> distType = "DIST";
            }
        } else {
            distType = "PLX";
        }

        if (postype == null && posTypeStr != null && distType != null) {
            // Construct from postypestr and disttype
            postype = PositionType.valueOf(posTypeStr + distType);
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
