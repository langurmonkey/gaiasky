/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import gaiasky.util.Settings;
import gaiasky.util.SysUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A utility to fetch and parse TLE (Two-Line Element) orbital data for a given spacecraft
 * from a remote URL and extract its orbital elements and epoch.
 */
public class TLEParser {

    /** Timeout of 1 hour. **/
    private static final long CACHE_TIMEOUT_MS = 3_600_000;

    /**
     * Holds parsed orbital elements for a spacecraft.
     */
    public static class OrbitalElements {
        public String name;
        public double period;           // days
        public double epochJD;          // JD
        public double semiMajorAxis;    // km
        public double inclination;      // degrees
        public double ascendingNode;    // degrees
        public double eccentricity;     // unitless
        public double argOfPericenter;  // degrees
        public double meanAnomaly;      // degrees
        public int revolutionNumber;    // orbit count at epoch

        @Override
        public String toString() {
            return "OrbitalElements{" +
                    "argOfPericenter=" + argOfPericenter +
                    ", name='" + name + '\'' +
                    ", period=" + period +
                    ", epochJD=" + epochJD +
                    ", semiMajorAxis=" + semiMajorAxis +
                    ", inclination=" + inclination +
                    ", ascendingNode=" + ascendingNode +
                    ", eccentricity=" + eccentricity +
                    ", meanAnomaly=" + meanAnomaly +
                    ", revolutionNumber=" + revolutionNumber +
                    '}';
        }
    }

    /**
     * Fetches the TLE data from the given URL and extracts the orbital elements for the given target name.
     *
     * @param url        The URL to fetch the data.
     * @param targetName The name of the target object.
     *
     * @return The {@link OrbitalElements} instance with the parsed data.
     *
     * @throws Exception If the fetching or parsing fails.
     */
    public OrbitalElements getOrbitalElements(String url, String targetName) throws Exception {
        List<String> lines = fetchTLEData(url);
        return extractOrbitalElements(lines, targetName);
    }

    private String getGroupFromURL(URL url) throws RuntimeException {
        String query = url.getQuery(); // returns "GROUP=gps-ops&FORMAT=tle"

        Map<String, String> queryPairs = new HashMap<>();
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            queryPairs.put(key, value);
        }
        return queryPairs.get("GROUP");
    }

    /**
     * Downloads TLE data from the given URL.
     * <p>
     * This method implements file-level caching, where TLE data files are cached per
     * group. If a file for the given group (retrieved as a URL parameter) exists,
     * the method checks whether the file modification date is older than the
     * cache timeout ({@link #CACHE_TIMEOUT_MS}). If it is, the file is pulled
     * again from the remote server. Otherwise, the local file is used.
     *
     * @param urlString The URL to fetch TLE data from.
     *
     * @return A list of non-empty lines from the TLE file.
     *
     * @throws IOException If a network or read error occurs.
     */
    public List<String> fetchTLEData(String urlString) throws IOException {
        URL url = java.net.URI.create(urlString).toURL();
        String group = getGroupFromURL(url);
        final var cacheDir = SysUtils.getDataCacheDir(Settings.settings.data.location);
        var file = cacheDir.resolve("TLE-" + group);
        boolean download = true;
        if (Files.exists(file) && Files.isRegularFile(file)) {
            var now = System.currentTimeMillis();
            var t = Files.getLastModifiedTime(file);
            var modified = t.to(TimeUnit.MILLISECONDS);
            if (now - modified < CACHE_TIMEOUT_MS) {
                download = false;
            }
        }
        BufferedReader reader;
        if (download) {
            // Download TLE data.
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
        } else {
            // Use cached file.
            reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file)));
        }
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) lines.add(line);
        }
        reader.close();

        // We have just downloaded a new version, cache result!
        if (download) {
            Files.write(file, lines);
        }

        return lines;
    }

    /**
     * Extracts the TLE orbital elements for a spacecraft with the given name.
     *
     * @param lines list of lines from a TLE file
     * @param name  the exact name of the spacecraft to search for (case-insensitive)
     *
     * @return OrbitalElements object if found, otherwise null
     */
    public OrbitalElements extractOrbitalElements(List<String> lines, String name) {
        for (int i = 0; i < lines.size() - 2; i++) {
            String line0 = lines.get(i).trim();
            String line1 = lines.get(i + 1).trim();
            String line2 = lines.get(i + 2).trim();

            if (line0.equalsIgnoreCase(name) && line1.startsWith("1 ") && line2.startsWith("2 ")) {
                OrbitalElements elem = new OrbitalElements();
                elem.name = name;

                // Parse epoch from line 1
                String epochStr = line1.substring(18, 32).trim();
                elem.epochJD = parseEpoch(epochStr);

                // Parse orbital elements from line 2
                elem.inclination = Double.parseDouble(line2.substring(8, 16).trim());
                elem.ascendingNode = Double.parseDouble(line2.substring(17, 25).trim());
                elem.eccentricity = Double.parseDouble("0." + line2.substring(26, 33).trim());
                elem.argOfPericenter = Double.parseDouble(line2.substring(34, 42).trim());
                elem.meanAnomaly = Double.parseDouble(line2.substring(43, 51).trim());
                var meanMotion = Double.parseDouble(line2.substring(52, 63).trim());
                elem.period = 1.0 / meanMotion;
                elem.semiMajorAxis = getSemiMajorAxisKm(meanMotion);
                elem.revolutionNumber = Integer.parseInt(line2.substring(63, 68).trim());

                return elem;
            }
        }
        return null;
    }

    /**
     * Computes the semi-major axis (in km) from TLE line 2 using Kepler's Third Law.
     *
     * @param meanMotion The mean motion in rev/day.
     *
     * @return Semi-major axis in kilometers.
     *
     * @throws IllegalArgumentException if the TLE line is invalid.
     */
    private double getSemiMajorAxisKm(double meanMotion) {
        // Constants
        final double mu = 398600.4418; // km^3/s^2 (Earth)
        final double secondsPerDay = 86400.0;
        double meanMotionRadPerSec = 2.0 * Math.PI * meanMotion / secondsPerDay;

        // Kepler's Third Law
        double semiMajorAxis = Math.cbrt(mu / (meanMotionRadPerSec * meanMotionRadPerSec));

        return semiMajorAxis;
    }

    /**
     * Converts a TLE epoch string (YYDDD.DDDDDDDD) into a human-readable year + day format.
     *
     * @param epoch the raw epoch string from TLE line 1
     *
     * @return The epoch in Julian days.
     */
    private double parseEpoch(String epoch) {
        int year = Integer.parseInt(epoch.substring(0, 2));
        double day = Double.parseDouble(epoch.substring(2));

        // Per TLE standard, 00-56 => 2000+, 57-99 => 1900+
        year += (year < 57) ? 2000 : 1900;

        var jd = AstroUtils.getJulianDateUTC(year, 1, 1, 0, 0, 0, 0);

        return jd + day;
    }
}
