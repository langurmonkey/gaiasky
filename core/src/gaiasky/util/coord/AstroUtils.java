/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import gaiasky.util.Constants;
import gaiasky.util.LruCache;
import gaiasky.util.Nature;
import gaiasky.util.Pair;
import gaiasky.util.coord.moon.MoonMeeusCoordinates;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class AstroUtils {

    /**
     * Julian days per year.
     **/
    static final public double JD_TO_Y = 365.25;

    /**
     * Julian date of reference epoch J2000 = JD2451544.5 =
     * 2000-01-01T00:00:00Z.
     **/
    static final public double JD_J2000 = getJulianDate(2000.0);

    /**
     * Julian date of reference epoch J2010 = JD2455197.5 =
     * 2010-01-01T00:00:00Z.
     **/
    static final public double JD_J2010 = getJulianDate(2010.0);

    /**
     * Julian date of reference epoch J2015.0 = JD2457023.5 =
     * 2015-01-01T00:00:00Z.
     **/
    static final public double JD_J2015 = getJulianDate(2015.0);

    /**
     * Julian date of reference epoch J2015.5 = JD2457206.125 =
     * 2015-01-01T00:00:00Z.
     **/
    static final public double JD_J2015_5 = getJulianDate(2015.5);

    /**
     * Milliseconds of J2000 in the scale of {@link Instant}. This is the number of milliseconds
     * elapsed since 1970-01-01T00:00:00Z (UTC) until 2000-01-01T00:00:00Z (UTC).
     **/
    public static final long J2000_MS;

    /**
     * Julian date cache, since most dates are used more than once.
     **/
    private static final LruCache<Long, Double> julianDateCache = new LruCache<>(50);
    /**
     * Initialize nsl Sun
     **/
    private static final NslSun nslSun = new NslSun();
    private static Instant cacheSunLongitudeDate = Instant.now();
    private static double cacheSunLongitude;

    private static final Instant gregorianSwitch = Instant.parse("1582-10-15T00:00:00.000Z");

    static {
        Instant d = (LocalDateTime.of(2000, 1, 1, 0, 0, 0)).toInstant(ZoneOffset.UTC);
        J2000_MS = d.toEpochMilli();
    }

    /**
     * Get julian date from a double reference epoch, as a Gregorian calendar year plus fraction.
     *
     * @param refEpoch The reference epoch as a Gregorian calendar year.
     *
     * @return The julian date.
     */
    public static double getJulianDate(double refEpoch) {
        int year = (int) refEpoch;
        double part = refEpoch - year;
        return getJulianDate(year, 1, 1, 0, 0, 0, 0, true) + JD_TO_Y * part;
    }

    /**
     * Returns the Sun's ecliptic longitude in degrees for the given time.
     * Caches the last Sun's longitude for future use.
     *
     * @param date The time for which the longitude must be calculated.
     *
     * @return The Sun's longitude in [deg].
     */
    public static double getSunLongitude(Instant date) {
        if (!date.equals(cacheSunLongitudeDate)) {
            double julianDate = getJulianDateCache(date);

            nslSun.setTime(julianDate);
            double aux = FastMath.toDegrees(nslSun.getSolarLongitude()) % 360;

            cacheSunLongitudeDate = Instant.ofEpochMilli(date.toEpochMilli());
            cacheSunLongitude = aux % 360;
        }
        return cacheSunLongitude;
    }

    /**
     * Algorithm in "Astronomical Algorithms" book by Jean Meeus. Returns a
     * vector with the geocentric ecliptic longitude (&lambda;) in radians, the ecliptic
     * latitude (&beta;) in radians and the distance in kilometers.
     *
     * @param date The instant date.
     * @param aux  Auxiliary double vector.
     * @param out  The out vector with geocentric [lambda, beta, r] in radians and kilometres.
     *
     * @return The out vector with geocentric [lambda, beta, r] in radians and kilometres.
     */
    public static Vector3b moonEclipticCoordinates(Instant date, Vector3d aux, Vector3b out) {
        MoonMeeusCoordinates.moonEclipticCoordinates(getJulianDateCache(date), aux);
        return out.set(aux);
    }


    /**
     * Spherical ecliptic coordinates of Pluto at the given date.
     *
     * @param date The date.
     * @param out  The out vector with [lambda, beta, r] in radians and kilometres.
     */
    public static void plutoEclipticCoordinates(Instant date, Vector3b out) {
        if (Constants.notWithinVSOPTime(date.toEpochMilli()))
            return;
        plutoEclipticCoordinates(getDaysSinceJ2000(date), out);
    }

    /**
     * Spherical ecliptic coordinates of pluto at the given date. See
     * <a href="http://www.stjarnhimlen.se/comp/ppcomp.html">here</a>.
     *
     * @param d   Julian date.
     * @param out The out vector with [lambda, beta, r] in radians and kilometres.
     */
    private static void plutoEclipticCoordinates(double d, Vector3b out) {
        double S = FastMath.toRadians(50.03 + 0.033459652 * d);
        double P = FastMath.toRadians(238.95 + 0.003968789 * d);

        double lonEcl = 238.9508 + 0.00400703 * d - 19.799 * FastMath.sin(P) + 19.848 * FastMath.cos(P) + 0.897 * FastMath.sin(
                2.0 * P) - 4.956 * FastMath.cos(2.0 * P) + 0.610 * FastMath.sin(3.0 * P) + 1.211 * FastMath.cos(3.0 * P) - 0.341 * FastMath.sin(
                4.0 * P) - 0.190 * FastMath.cos(4.0 * P) + 0.128 * FastMath.sin(5.0 * P) - 0.034 * FastMath.cos(5.0 * P) - 0.038 * FastMath.sin(
                6.0 * P) + 0.031 * FastMath.cos(6.0 * P) + 0.020 * FastMath.sin(S - P) - 0.010 * FastMath.cos(S - P);
        double latEcl = -3.9082 - 5.453 * FastMath.sin(P) - 14.975 * FastMath.cos(P) + 3.527 * FastMath.sin(2.0 * P) + 1.673 * FastMath.cos(
                2.0 * P) - 1.051 * FastMath.sin(3.0 * P) + 0.328 * FastMath.cos(3.0 * P) + 0.179 * FastMath.sin(4.0 * P) - 0.292 * FastMath.cos(
                4.0 * P) + 0.019 * FastMath.sin(5.0 * P) + 0.100 * FastMath.cos(5.0 * P) - 0.031 * FastMath.sin(6.0 * P) - 0.026 * FastMath.cos(
                6.0 * P) + 0.011 * FastMath.cos(S - P);
        double r = 40.72 + 6.68 * FastMath.sin(P) + 6.90 * FastMath.cos(P) - 1.18 * FastMath.sin(2.0 * P) - 0.03 * FastMath.cos(
                2.0 * P) + 0.15 * FastMath.sin(3.0 * P) - 0.14 * FastMath.cos(3.0 * P);

        out.set(Math.toRadians(lonEcl), FastMath.toRadians(latEcl), Nature.AU_TO_KM * r);
    }

    /**
     * Gets the Julian date number given the Gregorian calendar quantities.
     *
     * @param gregorian Whether to use the Gregorian or the Julian calendar.
     *
     * @return The julian date number.
     */
    public static double getJulianDate(int year, int month, int day, int hour, int min, int sec, int nanos, boolean gregorian) {
        if (gregorian) {
            return getJulianDayNumberGregorian(year, month, day) + getDayFraction(hour, min, sec, nanos);
        } else {
            return getJulianDayNumberJulianCalendar(year, month, day) + getDayFraction(hour, min, sec, nanos);
        }
    }

    /**
     * Gets the Julian Date for the given date. It uses a cache.
     *
     * @param instant The date.
     *
     * @return The Julian Date.
     */
    public static synchronized double getJulianDateCache(Instant instant) {
        long time = instant.toEpochMilli();
        if (julianDateCache.containsKey(time)) {
            return julianDateCache.get(time);
        } else {
            double jd = getJulianDate(instant);
            julianDateCache.put(time, jd);
            return jd;
        }
    }

    public static double getJulianDate(Instant instant) {
        LocalDateTime date = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        int hour = date.getHour();
        int min = date.getMinute();
        int sec = date.getSecond();
        int nanos = date.getNano();
        return getJulianDate(year, month, day, hour, min, sec, nanos, instant.isAfter(gregorianSwitch));
    }

    /**
     * Returns the elapsed days since the epoch J2000 until the given date. Can
     * be negative.
     *
     * @param date The date.
     *
     * @return The elapsed days.
     */
    public static double getDaysSinceJ2000(Instant date) {
        return getJulianDateCache(date) - JD_J2000;
    }

    /**
     * Returns the elapsed milliseconds since the given julian date jd until the
     * given date. Can be negative.
     *
     * @param date     The date.
     * @param epoch_jd The reference epoch in julian days.
     *
     * @return The elapsed milliseconds.
     */
    public static double getMsSince(Instant date, double epoch_jd) {
        return (getJulianDateCache(date) - epoch_jd) * Nature.D_TO_MS;
    }

    /**
     * Returns the elapsed days since the given julian date jd until the
     * given date. Can be negative.
     *
     * @param date     The date.
     * @param epoch_jd The reference epoch in julian days.
     *
     * @return The elapsed days
     */
    public static double getDaysSince(Instant date, double epoch_jd) {
        return (getJulianDateCache(date) - epoch_jd);
    }

    /**
     * Gets the Gregorian calendar quantities given the Julian date.
     *
     * @param julianDate The Julian date.
     *
     * @return Vector with [year, month, day, hour, min, sec, nanos].
     */
    public static long[] getCalendarDay(double julianDate) {
        var X = julianDate + 0.5;
        var Z = FastMath.floor(X); //Get day without time
        var F = X - Z; //Get time
        var Y = FastMath.floor((Z - 1867216.25) / 36524.25);
        var A = Z + 1 + Y - FastMath.floor(Y / 4);
        var B = A + 1524;
        var C = FastMath.floor((B - 122.1) / JD_TO_Y);
        var D = FastMath.floor(JD_TO_Y * C);
        var G = FastMath.floor((B - D) / 30.6001);
        //must get number less than or equal to 12)
        var month = (G < 13.5) ? (G - 1) : (G - 13);
        //if Month is January or February, or the rest of year
        var year = (month < 2.5) ? (C - 4715) : (C - 4716);
        var UT = B - D - FastMath.floor(30.6001 * G) + F;
        var day = FastMath.floor(UT);
        //Determine time
        UT -= FastMath.floor(UT);
        UT *= 24;
        var hour = FastMath.floor(UT);
        UT -= FastMath.floor(UT);
        UT *= 60;
        var minute = FastMath.floor(UT);
        UT -= FastMath.floor(UT);
        UT *= 60;
        var second = FastMath.floor(UT);
        UT -= FastMath.floor(UT);
        UT *= 1.0e9;
        var ms = FastMath.round(UT);

        return new long[]{(long) year, (long) month, (long) day, (long) hour, (long) minute, (long) second, ms};

    }

    /**
     * Returns the Julian day number. Uses the method shown in "Astronomical
     * Algorithms" by Jean Meeus.
     *
     * @param year  The year
     * @param month The month in [1:12]
     * @param day   The day in the month, starting at 1
     *
     * @return The Julian date
     *
     * @deprecated This does not work well!
     */
    @Deprecated
    public static double getJulianDayNumberBook(int year, int month, int day) {
        int a = year / 100;
        int b = 2 - a + (a / 4);

        // Julian day
        return (long) (365.242 * (year + 4716)) + (long) (30.6001 * (month)) + day + b - 1524.5d;
    }

    /**
     * Returns the Julian day number of a date in the Julian calendar. Uses
     * Wikipedia's algorithm.
     *
     * @param year  The year
     * @param month The month in [1:12]
     * @param day   The day in the month, starting at 1
     *
     * @return The Julian date
     *
     * @see <a href="http://en.wikipedia.org/wiki/Julian_day">http://en.wikipedia.org/wiki/Julian_day</a>
     */
    public static double getJulianDayNumberJulianCalendar(int year, int month, int day) {
        long a = (14L - month) / 12L;
        long y = year + 4800L - a;
        long m = month + 12L * a - 3L;

        return day + ((153L * m + 2L) / 5L) + 365L * y + (y / 4L) - 32083.5;
    }

    private static final double GREGORIAN_EPOCH = 1721425.5;

    /**
     * Is a given year in the Gregorian calendar a leap year ?
     *
     * @param year The year.
     *
     * @return Whether the year in the Gregorian calendar is a leap year.
     */
    public static boolean isLeapYearGregorian(int year) {
        return ((year % 4) == 0) && (!(((year % 100) == 0) && ((year % 400) != 0)));
    }


    /**
     * Determine Julian day number from Gregorian calendar date.
     *
     * @param year  The year.
     * @param month The month.
     * @param day   The day.
     *
     * @return The julian day number.
     */
    public static double getJulianDayNumberGregorian(int year, int month, int day) {
        return (GREGORIAN_EPOCH - 1) +
                (365 * (year - 1)) +
                (double) ((year - 1) / 4) +
                (-(double) ((year - 1) / 100)) +
                (double) ((year - 1) / 400) +
                (double) ((((367 * month) - 362) / 12) +
                        ((month <= 2) ? 0 :
                                (isLeapYearGregorian(year) ? -1 : -2)
                        ) +
                        day);

    }

    public static Instant julianDateToInstant(double jd) {
        long[] cd = getCalendarDay(jd);
        LocalDateTime ldt = LocalDateTime.of((int) cd[0], (int) cd[1], (int) cd[2], (int) cd[3], (int) cd[4], (int) cd[5], (int) cd[6]);
        return ldt.toInstant(ZoneOffset.UTC);
    }

    /**
     * Gets the day fraction from the day quantities.
     *
     * @param hour  The hour in 0-24
     * @param min   The minute in 0-1440
     * @param sec   The second in 0-86400
     * @param nanos The nanoseconds
     *
     * @return The day fraction
     */
    public static double getDayFraction(int hour, int min, int sec, int nanos) {
        return hour / 24.0 + min / 1440.0 + (sec + nanos / 1.0E9) / 86400.0;
    }

    /**
     * Gets the day quantities from the day fraction
     *
     * @param dayFraction The day fraction.
     *
     * @return [hours, minutes, seconds, nanos].
     *
     * @deprecated Not used anymore.
     */
    @Deprecated
    public static long[] getDayQuantities(double dayFraction) {
        double hourFrac = dayFraction * 24.0;
        double minFrac = (hourFrac - (long) hourFrac) * 60.0;
        double secFrac = (minFrac - (long) minFrac) * 60.0;
        double nanosFrac = (secFrac - (long) secFrac) * 1.0E9;
        return new long[]{(long) hourFrac, (long) minFrac, (long) secFrac, (long) nanosFrac};
    }

    /**
     * Returns the obliquity of the ecliptic (inclination of the Earth's axis of
     * rotation) for a given date, in degrees
     *
     * @return The obliquity in degrees
     */
    public static double obliquity(double julianDate) {
        // JPL fundamental ephemeris have been continually updated. The
        // Astronomical Almanac for 2010 specifies:
        // E = 23° 26′ 21″.406 − 46″.836769 T − 0″.0001831 T2 + 0″.00200340 T3 −
        // 0″.576×10−6 T4 − 4″.34×10−8 T5
        double T = T(julianDate);
        return 23.0 + 26.0 / 60.0 + (21.406 - (46.836769 - (0.0001831 + (0.00200340 - (0.576e-6 - 4.34e-8 * T) * T) * T) * T) * T) / 3600.0;
    }

    /**
     * Time T measured in Julian centuries from the Epoch J2000.0.
     *
     * @param julianDate The julian date.
     *
     * @return The time in julian centuries.
     */
    public static double T(double julianDate) {
        return (julianDate - 2451545.0) / 36525.0;
    }

    public static double tau(double julianDate) {
        return (julianDate - 2451545.0) / 365250.0;
    }

    /**
     * Converts proper motions + radial velocity into a cartesian vector.
     * See <a href="http://www.astronexus.com/a-a/motions-long-term">this article</a>.
     *
     * @param muAlphaStar Mu alpha star, in mas/yr.
     * @param muDelta     Mu delta, in mas/yr.
     * @param radVel      Radial velocity in km/s.
     * @param ra          Right ascension in radians.
     * @param dec         Declination in radians.
     * @param distPc      Distance in parsecs to the star.
     *
     * @return The proper motion vector in internal_units/year.
     */
    public static Vector3d properMotionsToCartesian(double muAlphaStar, double muDelta, double radVel, double ra, double dec, double distPc,
                                                    Vector3d out) {
        double ma = muAlphaStar * Nature.MILLIARCSEC_TO_ARCSEC;
        double md = muDelta * Nature.MILLIARCSEC_TO_ARCSEC;

        // Multiply arcsec/yr with distance in parsecs gives a linear velocity. The factor 4.74 converts result to km/s
        double vta = ma * distPc * Nature.ARCSEC_PER_YEAR_TO_KMS;
        double vtd = md * distPc * Nature.ARCSEC_PER_YEAR_TO_KMS;

        double cosAlpha = FastMath.cos(ra);
        double sinAlpha = FastMath.sin(ra);
        double cosDelta = FastMath.cos(dec);
        double sinDelta = FastMath.sin(dec);

        // +x to delta=0, alpha=0
        // +y to delta=0, alpha=90
        // +z to delta=90
        // components in km/s

        /*
         * vx = (vR cos \delta cos \alpha) - (vTA sin \alpha) - (vTD sin \delta cos \alpha)
         * vy = (vR cos \delta sin \alpha) + (vTA cos \alpha) - (vTD sin \delta sin \alpha)
         * vz = vR sin \delta + vTD cos \delta
         */
        double vx = (radVel * cosDelta * cosAlpha) - (vta * sinAlpha) - (vtd * sinDelta * cosAlpha);
        double vy = (radVel * cosDelta * sinAlpha) + (vta * cosAlpha) - (vtd * sinDelta * sinAlpha);
        double vz = (radVel * sinDelta) + (vtd * cosDelta);

        return (out.set(vy, vz, vx)).scl(Constants.KM_TO_U / Nature.S_TO_Y);

    }

    /**
     * Converts a cartesian velocity vector [vx,vy,vz] into proper motions + radial velocity.
     * See <a href="http://www.astronexus.com/a-a/motions-long-term">this article</a>.
     *
     * @param vx     The X component of the cartesian velocity vector in internal_units/year.
     * @param vy     The Y component of the cartesian velocity vector internal_units/year.
     * @param vz     The Z component of the cartesian velocity vector internal_units/year.
     * @param ra     Right ascension in radians.
     * @param dec    Declination in radians.
     * @param distPc Distance in parsecs to the star.
     *
     * @return The proper motions (muAlpha, muDelta) in mas/yr, and the radial velocity in km/s.
     */
    public static Vector3d cartesianToProperMotions(double vx, double vy, double vz,
                                                    double ra, double dec, double distPc,
                                                    Vector3d out) {
        // Precompute constants for conversion
        double kmPerYearToInternal = Constants.U_TO_KM / Nature.Y_TO_S;
        double arcsecPerYearToKm = Nature.ARCSEC_PER_YEAR_TO_KMS * distPc;

        // Convert from internal units/year to km/s
        double vxKms = vz * kmPerYearToInternal;
        double vyKms = vx * kmPerYearToInternal;
        double vzKms = vy * kmPerYearToInternal;

        // Unit vectors
        double cosA = FastMath.cos(ra);
        double sinA = FastMath.sin(ra);
        double cosD = FastMath.cos(dec);
        double sinD = FastMath.sin(dec);

        double rx = cosD * cosA;
        double ry = cosD * sinA;
        double rz = sinD;

        double ax = -sinA;
        double ay = cosA;
        double az = 0.0;

        double dx = -cosA * sinD;
        double dy = -sinA * sinD;
        double dz =  cosD;

        double vr  = vxKms * rx + vyKms * ry + vzKms * rz;
        double vta = vxKms * ax + vyKms * ay + vzKms * az;
        double vtd = vxKms * dx + vyKms * dy + vzKms * dz;

        double muAlphaStar = (vta / arcsecPerYearToKm) * Nature.ARCSEC_TO_MILLIARCSEC;
        double muDelta     = (vtd / arcsecPerYearToKm) * Nature.ARCSEC_TO_MILLIARCSEC;

        return out.set(muAlphaStar, muDelta, vr);
    }

    /**
     * Converts an apparent magnitude to an absolute magnitude given the distance in parsecs.
     *
     * @param distPc The distance to the star in parsecs.
     * @param appMag The apparent magnitude.
     *
     * @return The absolute magnitude.
     */
    public static double apparentToAbsoluteMagnitude(double distPc, double appMag) {
        final double v = 5.0 * FastMath.log10(distPc <= 0.0 ? 10.0 : distPc);
        return appMag - v + 5.0;
    }

    /**
     * Converts an absolute magnitude to an apparent magnitude at the given distance in parsecs.
     *
     * @param distPc The distance to the star in parsecs.
     * @param absMag The absolute magnitude.
     *
     * @return The apparent magnitude at the given distance.
     */
    public static double absoluteToApparentMagnitude(double distPc, double absMag) {
        final double v = 5.0 * FastMath.log10(distPc <= 0.0 ? 10.0 : distPc);
        return absMag + v - 5.0;
    }

    /**
     * Computes the pseudo-size of a star from the absolute magnitude.
     *
     * @param absMag The absolute magnitude of the star.
     *
     * @return The pseudo-size of this star, mainly used for rendering purposes.
     * It has no physical meaning and has no relation to the actual physical size of the star.
     */
    public static double absoluteMagnitudeToPseudoSize(final double absMag) {
        // Pseudo-luminosity. Usually L = L0 * 10^(-0.4*Mbol). We omit M0 and approximate Mbol = M
        double pseudoL = FastMath.pow(10, -0.4 * absMag);
        double sizeFactor = Nature.PC_TO_M * Constants.ORIGINAL_M_TO_U * 0.15;
        return FastMath.min((Math.pow(pseudoL, 0.5) * sizeFactor), 1e10) * Constants.DISTANCE_SCALE_FACTOR;
    }

    /**
     * Get the spectral type from the effective temperature for main sequence stars.
     * More info: <a href="https://sites.uni.edu/morgans/astro/course/Notes/section2/spectraltemps.html">see here</a>.
     */
    public static String getSpectralType(final float tEff) {
        initSpectralTypeTable();

        int n = spTypeTable.size();
        for (int i = n - 1; i >= 0; i--) {
            var p = spTypeTable.get(i);
            if (tEff <= p.getFirst()) {
                return p.getSecond();
            }
        }
        return null;
    }

    private static List<Pair<Float, String>> spTypeTable;

    /**
     * Initializes the spectral type table for main sequence stars.
     * More info: <a href="https://sites.uni.edu/morgans/astro/course/Notes/section2/spectraltemps.html">see here</a>.
     */
    private static void initSpectralTypeTable() {
        if (spTypeTable == null) {
            spTypeTable = new ArrayList<>();
            addToSpTable(54000f, "O5");
            addToSpTable(45000f, "O6");
            addToSpTable(43300f, "O7");
            addToSpTable(40600f, "O8");
            addToSpTable(37800f, "O9");
            addToSpTable(29200f, "B0");
            addToSpTable(23000f, "B1");
            addToSpTable(21000f, "B2");
            addToSpTable(17600f, "B3");
            addToSpTable(15200f, "B5");
            addToSpTable(14300f, "B6");
            addToSpTable(13500f, "B7");
            addToSpTable(12300f, "B8");
            addToSpTable(11400f, "B9");
            addToSpTable(9600f, "A0");
            addToSpTable(9330f, "A1");
            addToSpTable(9040f, "A2");
            addToSpTable(8750f, "A3");
            addToSpTable(8480f, "A4");
            addToSpTable(8310f, "A5");
            addToSpTable(7920f, "A7");
            addToSpTable(7350f, "F0");
            addToSpTable(7050f, "F2");
            addToSpTable(6850f, "F3");
            addToSpTable(6700f, "F5");
            addToSpTable(6550f, "F6");
            addToSpTable(6400f, "F7");
            addToSpTable(6300f, "F8");
            addToSpTable(6050f, "G0");
            addToSpTable(5930f, "G1");
            addToSpTable(5800f, "G2");
            addToSpTable(5660f, "G5");
            addToSpTable(5440f, "G8");
            addToSpTable(5240f, "K0");
            addToSpTable(5110f, "K1");
            addToSpTable(4960f, "K2");
            addToSpTable(4800f, "K3");
            addToSpTable(4600f, "K4");
            addToSpTable(4400f, "K5");
            addToSpTable(4000f, "K7");
            addToSpTable(3750f, "M0");
            addToSpTable(3700f, "M1");
            addToSpTable(3600f, "M2");
            addToSpTable(3500f, "M3");
            addToSpTable(3400f, "M4");
            addToSpTable(3200f, "M5");
            addToSpTable(3100f, "M6");
            addToSpTable(2900f, "M7");
            addToSpTable(2700f, "M8");
            addToSpTable(2600f, "L0");
            addToSpTable(2200f, "L3");
            addToSpTable(1500f, "L8");
            addToSpTable(1400f, "T2");
            addToSpTable(1000f, "T6");
            addToSpTable(800f, "T8");
        }

    }

    private static void addToSpTable(float tEff, String spType) {
        if (spTypeTable != null) {
            spTypeTable.add(new Pair<>(tEff, spType));
        }
    }

}
