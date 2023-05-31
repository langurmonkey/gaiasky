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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UCDParser {
    // The following column names can either be strings or regular expressions. They are checked
    // first with equals() and then with matches()
    public static String[] idcolnames = new String[] { "hip", "id", "source_id", "tycho2_id" };
    public static String[] namecolnames = new String[] { "(name|NAME|refname|REFNAME)((_|-)[\\w\\d]+)?", "name", "proper", "proper_name", "common_name", "designation" };
    public static String[] racolnames = new String[] { "ra", "right_ascension", "rightascension", "alpha", "raj2000" };
    public static String[] xcolnames = new String[] { "x", "X" };
    public static String[] decolnames = new String[] { "dec", "de", "declination", "delta", "dej2000" };
    public static String[] ycolnames = new String[] { "y", "Y" };
    public static String[] distcolnames = new String[] { "dist", "distance" };
    public static String[] zcolnames = new String[] { "z", "Z" };
    public static String[] pllxcolnames = new String[] { "plx", "parallax", "pllx", "par" };
    public static String[] magcolnames = new String[] { "phot_g_mean_mag", "mag", "g_mag", "bmag", "gmag" };
    public static String[] colorcolnames = new String[] { "b_v", "v_i", "bp_rp", "bp_g", "g_rp", "ci" };
    public static String[] teffcolnames = new String[] { "teff", "t_eff", "temperature", "effective_temperature" };
    public static String[] pmracolnames = new String[] { "pmra", "pmalpha", "pm_ra", "mualpha" };
    public static String[] pmdeccolnames = new String[] { "pmdec", "pmdelta", "pm_dec", "pm_de", "mudelta" };
    public static String[] radvelcolnames = new String[] { "radial_velocity", "radvel", "rv", "dr2_radial_velocity" };
    public static String[] sizecolnames = new String[] { "radius", "rcluster", "radi", "diameter", "size", "linear_diameter" };
    public static String[] nstarscolnames = new String[] { "n", "nstars", "n_stars", "n_star" };
    public static String[] varimagscolnames = new String[] { "g_transit_mag", "g_mag_list", "g_mag_series" };
    public static String[] varitimescolnames = new String[] { "g_transit_time", "time_list", "time_series" };
    public static String[] periodcolnames = new String[] { "pf", "period" };
    public Map<UCDType, Set<UCD>> ucdmap;
    // IDS
    public boolean hasid = false;
    public Array<UCD> ID;
    // NAME
    public boolean hasname = false;
    public Array<UCD> NAME;
    // POSITIONS
    public boolean haspos = false;
    public Array<UCD> POS1, POS2, POS3;
    // PROPER MOTIONS
    public boolean haspm = false;
    public Array<UCD> PMRA, PMDEC, RADVEL;
    // MAGNITUDES
    public boolean hasmag = false;
    public Array<UCD> MAG;
    // COLORS
    public boolean hascol = false;
    public Array<UCD> COL;
    // PHYSICAL PARAMS
    public boolean hassize = false;
    public Array<UCD> SIZE;
    public boolean hasteff = false;
    public Array<UCD> TEFF;
    // VARIABILITY
    public boolean hasvari = false;
    public boolean hasperiod = false;
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

    public static boolean isName(String colname) {
        return TextUtils.contains(namecolnames, colname, true);
    }

    public static boolean isId(String colname) {
        return TextUtils.contains(idcolnames, colname, true);
    }

    public static boolean isRa(String colname) {
        return TextUtils.contains(racolnames, colname, true);
    }

    public static boolean isX(String colname) {
        return TextUtils.contains(xcolnames, colname, true);
    }

    public static boolean isDec(String colname) {
        return TextUtils.contains(decolnames, colname, true);
    }

    public static boolean isY(String colname) {
        return TextUtils.contains(ycolnames, colname, true);
    }

    public static boolean isDist(String colname) {
        return TextUtils.contains(distcolnames, colname, true);
    }

    public static boolean isZ(String colname) {
        return TextUtils.contains(zcolnames, colname, true);
    }

    public static boolean isPllx(String colname) {
        return TextUtils.contains(pllxcolnames, colname, true);
    }

    public static boolean isMag(String colname) {
        return TextUtils.contains(magcolnames, colname, true);
    }

    public static boolean isColor(String colname) {
        return TextUtils.contains(colorcolnames, colname, true);
    }

    public static boolean isTeff(String colname) {
        return TextUtils.contains(teffcolnames, colname, true);
    }

    public static boolean isPmra(String colname) {
        return TextUtils.contains(pmracolnames, colname, true);
    }

    public static boolean isPmde(String colname) {
        return TextUtils.contains(pmdeccolnames, colname, true);
    }

    public static boolean isRadvel(String colname) {
        return TextUtils.contains(radvelcolnames, colname, true);
    }

    public static boolean isSize(String colname) {
        return isRadius(colname);
    }

    public static boolean isRadius(String colname) {
        return TextUtils.contains(sizecolnames, colname, true);
    }

    public static boolean isNstars(String colname) {
        return TextUtils.contains(nstarscolnames, colname, true);
    }

    public static boolean isGVariMags(String colname) {
        return TextUtils.contains(varimagscolnames, colname, true);
    }

    public static boolean isGVariTimes(String colname) {
        return TextUtils.contains(varitimescolnames, colname, true);
    }

    public static boolean isPeriod(String colname) {
        return TextUtils.contains(periodcolnames, colname, true);
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
        ColumnInfo[] colInfo = new ColumnInfo[count];
        for (int i = 0; i < count; i++) {
            // Get column
            ColumnInfo col = table.getColumnInfo(i);
            colInfo[i] = col;

            // Parse and add
            UCD ucd = new UCD(col.getUCD(), col.getName(), col.getUnitString(), i);
            addToMap(ucd);
        }

        // ID and NAME
        Set<UCD> meta = ucdmap.get(UCDType.META);
        if (meta != null)
            for (UCD candidate : meta) {
                if (candidate.ucdstrings[0].equals("meta.id")) {
                    if (candidate.ucdstrings.length == 1 || candidate.ucdstrings[1].equals("meta.main"))
                        this.ID.add(candidate);
                }
            }
        if (this.ID.isEmpty()) {
            this.ID.addAll(getByColNames(idcolnames));
        }
        this.hasid = !this.ID.isEmpty();

        if (this.NAME.isEmpty()) {
            this.NAME.addAll(getByColNames(namecolnames));
        }
        this.hasname = !this.NAME.isEmpty();

        // POSITIONS
        Set<UCD> pos = ucdmap.get(UCDType.POS);
        if (pos != null) {
            String posrefsys = getBestRefsys(pos);
            for (UCD candidate : pos) {
                String meaning = candidate.ucd[0][1];
                final String coord = candidate.ucd[0].length > 2 ? candidate.ucd[0][2] : null;
                boolean derived = checkDerivedQuantity(candidate.ucd);

                // Filter using best reference system (posrefsys)
                if (!derived && (meaning.equals(posrefsys) || meaning.equals("parallax") || meaning.equals("distance"))) {
                    switch (meaning) {
                    case "eq":
                        switch (coord) {
                        case "ra" -> {
                            setDefaultUnit(candidate, "deg");
                            add(candidate, racolnames, this.POS1);
                        }
                        case "dec" -> {
                            setDefaultUnit(candidate, "deg");
                            add(candidate, decolnames, this.POS2);
                        }
                        }
                        break;
                    case "ecliptic":
                    case "galactic":
                        switch (coord) {
                        case "lon" -> {
                            setDefaultUnit(candidate, "deg");
                            this.POS1.add(candidate);
                        }
                        case "lat" -> {
                            setDefaultUnit(candidate, "deg");
                            this.POS2.add(candidate);
                        }
                        }
                        break;
                    case "cartesian":
                        setDefaultUnit(candidate, "pc");
                        switch (coord) {
                        case "x" -> this.POS1.add(candidate);
                        case "y" -> this.POS2.add(candidate);
                        case "z" -> this.POS3.add(candidate);
                        }
                        break;
                    case "parallax":
                        setDefaultUnit(candidate, "mas");
                        add(candidate, pllxcolnames, this.POS3);
                        break;
                    case "distance":
                        setDefaultUnit(candidate, "pc");
                        add(candidate, distcolnames, this.POS3);
                        break;
                    }
                }
            }
        }
        if (this.POS1.isEmpty() || this.POS2.isEmpty()) {
            // Try to work out from names
            this.POS1 = getByColNames(racolnames, "deg");
            if (!this.POS1.isEmpty()) {
                this.POS2 = getByColNames(decolnames, "deg");
                this.POS3 = getByColNames(distcolnames, "pc");
                if (this.POS3.isEmpty()) {
                    this.POS3 = getByColNames(pllxcolnames, "mas");
                }
            }
            // Try cartesian
            if (this.POS1.isEmpty() || this.POS2.isEmpty()) {
                this.POS1 = getByColNames(xcolnames, "pc");
                this.POS2 = getByColNames(ycolnames, "pc");
                this.POS3 = getByColNames(zcolnames, "pc");
            }
        }

        this.haspos = !this.POS1.isEmpty() && !this.POS2.isEmpty();

        // PROPER MOTIONS

        // RA/DEC
        if (pos != null) {
            for (UCD candidate : pos) {
                if (candidate.ucd[0][1].equals("pm")) {
                    // Proper motion: pos.pm
                    // Followed by pos.[refsys].[coord]
                    try {
                        String refsys = candidate.ucd[1][1];
                        String coord = candidate.ucd[1][2];
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
            this.PMRA = getByColNames(pmracolnames, "mas/yr");
            if (!this.PMRA.isEmpty()) {
                this.PMDEC = getByColNames(pmdeccolnames, "mas/yr");
                this.RADVEL = getByColNames(radvelcolnames, "km/s");
            }
        }
        this.haspm = !this.PMRA.isEmpty() && !this.PMDEC.isEmpty();

        // RADIAL VELOCITY
        Set<UCD> spect = ucdmap.get(UCDType.SPECT);
        if (spect != null)
            for (UCD candidate : spect) {
                if (candidate.ucd[0][1].equalsIgnoreCase("dopplerVeloc"))
                    this.RADVEL.add(candidate);
            }

        // MAGNITUDES
        Set<UCD> mag = ucdmap.get(UCDType.PHOT);
        if (mag != null)
            for (UCD candidate : mag) {
                if (candidate.ucd[0][1].equals("mag") && candidate.ucd[0].length < 3) {
                    if (candidate.ucd.length > 1) {
                        if (candidate.ucdstrings[1].equals("stat.mean") || candidate.ucdstrings[1].toLowerCase().startsWith("em.opt.")) {
                            this.MAG.add(candidate);
                        }
                    } else {
                        if (this.MAG != null)
                            this.MAG.add(candidate);
                    }
                }
            }
        if (this.MAG == null || this.MAG.isEmpty()) {
            this.MAG = getByColNames(magcolnames, "mag");
        }
        this.hasmag = !this.MAG.isEmpty();

        // COLORS
        Set<UCD> col = ucdmap.get(UCDType.PHOT);
        if (col != null) {
            for (UCD candidate : col) {
                if (candidate.ucd[0][1].equals("color")) {
                    this.COL.add(candidate);
                    break;
                }
            }
        }
        if (this.COL == null || this.COL.isEmpty()) {
            this.COL = getByColNames(colorcolnames);
        }
        this.hascol = !this.COL.isEmpty();

        // SIZE
        Set<UCD> phys = ucdmap.get(UCDType.PHYS);
        if (phys != null) {
            for (UCD candidate : phys) {
                if (candidate.ucd[0].length >= 2 && candidate.ucd[0][1].equals("size")) {
                    this.SIZE.add(candidate);
                    break;
                }
            }
        }
        if (this.SIZE == null || this.SIZE.isEmpty()) {
            this.SIZE = getByColNames(sizecolnames);
        }
        this.hassize = !this.SIZE.isEmpty();

        // EFFECTIVE TEMPERATURE
        if (phys != null) {
            for (UCD candidate : phys) {
                if (candidate.ucd[0].length >= 3 && candidate.ucd[0][1].equals("temperature") && candidate.ucd[0][2].equals("effective")) {
                    this.TEFF.add(candidate);
                    break;
                }
            }
        }
        if (this.TEFF == null || this.TEFF.isEmpty()) {
            this.TEFF = getByColNames(teffcolnames);
        }
        this.hasteff = !this.TEFF.isEmpty();

        // VARIABILITY
        Set<UCD> vari = ucdmap.get(UCDType.VARI);
        if (vari != null) {
            for (UCD candidate : vari) {
                if (candidate.ucd[0].length >= 3 && candidate.ucd[0][1].equals("time")) {
                    this.VARI_TIMES.add(candidate);
                    break;
                }
                if (candidate.ucd[0].length >= 3 && candidate.ucd[0][1].equals("magnitude")) {
                    this.VARI_MAGS.add(candidate);
                    break;
                }
            }
        }
        if (this.VARI_TIMES == null || this.VARI_TIMES.isEmpty()) {
            this.VARI_TIMES = getByColNames(varitimescolnames, "d");
        }
        if (this.VARI_MAGS == null || this.VARI_MAGS.isEmpty()) {
            this.VARI_MAGS = getByColNames(varimagscolnames, "mag");
        }
        this.hasvari = !this.VARI_MAGS.isEmpty();
        if (this.VARI_PERIOD == null || this.VARI_PERIOD.isEmpty()) {
            this.VARI_PERIOD = getByColNames(periodcolnames, "d");
        }
        this.hasperiod = !this.VARI_PERIOD.isEmpty();

        // REST OF COLUMNS
        Set<UCDType> keys = ucdmap.keySet();
        for (UCDType ucdType : keys) {
            Set<UCD> ucds = ucdmap.get(ucdType);
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
        if (pos1.ucd == null || pos2.ucd == null) {
            return PositionType.valueOf("EQ_SPH_" + (pos3 == null ? "PLX" : (isDist(pos3.colname) ? "DIST" : "PLX")));
        }
        String meaning = pos1.ucd[0][1];
        String postypestr = null, disttype = null;
        PositionType postype = null;
        switch (meaning) {
        case "eq" -> postypestr = "EQ_SPH_";
        case "ecliptic" -> postypestr = "ECL_SPH_";
        case "galactic" -> postypestr = "GAL_SPH_";
        case "cartesian" -> postype = PositionType.EQ_XYZ;
        }

        if (pos3 != null) {
            meaning = pos3.ucd[0][1];
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

    private Array<UCD> getByColNames(String[] colnames) {
        return getByColNames(colnames, null);
    }

    private Array<UCD> getByColNames(String[] colnames,
                                     String defaultunit) {
        return getByColNames(new UCDType[] { UCDType.UNKNOWN, UCDType.MISSING }, colnames, defaultunit);
    }

    private Array<UCD> getByColNames(UCDType[] types,
                                     String[] colnames,
                                     String defaultunit) {
        Array<UCD> candidates = new Array<>();
        for (UCDType type : types) {
            // Get all unknown and missing
            if (ucdmap.containsKey(type)) {
                Set<UCD> set = ucdmap.get(type);
                // Check column names
                for (UCD candidate : set) {
                    if (TextUtils.containsOrMatches(colnames, candidate.colname, true)) {
                        if (defaultunit != null && (candidate.unit == null || candidate.unit.isEmpty()))
                            candidate.unit = defaultunit;
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
     * @param colnames  Array of column names to check
     * @param list      The list to add
     */
    private void add(UCD candidate,
                     String[] colnames,
                     Array<UCD> list) {
        if (candidate.colname != null && TextUtils.contains(colnames, candidate.colname)) {
            list.insert(0, candidate);
        } else {
            list.add(candidate);
        }

    }

    private String getBestRefsys(Set<UCD> ucds) {
        boolean eq = false, ecl = false, gal = false, cart = false;
        for (UCD candidate : ucds) {
            eq = eq || candidate.ucd[0][1].equals("eq");
            ecl = ecl || candidate.ucd[0][1].equals("ecliptic");
            gal = gal || candidate.ucd[0][1].equals("galactic");
            cart = cart || candidate.ucd[0][1].equals("cartesian");
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
     * Checks whether this UCD is a derived quantity (ratio, etc.)
     *
     * @param ucd The UCD to test
     *
     * @return True if the given UCD is a derived quantity.
     */
    private boolean checkDerivedQuantity(String[][] ucd) {
        for (int i = 0; i < ucd.length; i++) {
            if (ucd[i] != null && ucd[i].length > 0) {
                // Check ratio or factor
                if (ucd[i][0].equals("arith")) {
                    if (ucd[i].length > 1 && (ucd[i][1].equals("ratio") || ucd[i][1].equals("factor"))) {
                        return true;
                    }
                }

                // Check statistic (correlation, etc)
                if (ucd[i][0].equals("stat")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addToMap(UCD ucd) {
        if (!ucdmap.containsKey(ucd.type)) {
            Set<UCD> set = new HashSet<>();
            set.add(ucd);
            ucdmap.put(ucd.type, set);
        } else {
            ucdmap.get(ucd.type).add(ucd);
        }
    }

}
