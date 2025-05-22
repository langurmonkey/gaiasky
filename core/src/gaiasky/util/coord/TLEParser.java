/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility to fetch and parse TLE (Two-Line Element) orbital data for a given spacecraft
 * from a remote URL and extract its orbital elements and epoch.
 */
public class TLEParser {

    /**
     * Holds parsed orbital elements for a spacecraft.
     */
    public static class OrbitalElements {
        public String name;
        public double epochJD;
        public double inclination;      // degrees
        public double ascendingNode;    // degrees
        public double eccentricity;     // unitless
        public double argOfPericenter;       // degrees
        public double meanAnomaly;      // degrees
        public double meanMotion;       // revolutions per day
        public int revolutionNumber;    // orbit count at epoch

        @Override
        public String toString() {
            return String.format(
                    "Name: %s\nEpoch: %s\nInclination: %.4f°\nAscending node: %.4f°\nEccentricity: %.7f\nArgument of Pericenter: %.4f°\nMean Anomaly: %.4f°\nMean Motion: %.8f rev/day\nRev #: %d",
                    name,
                    epochJD,
                    inclination,
                    ascendingNode,
                    eccentricity,
                    argOfPericenter,
                    meanAnomaly,
                    meanMotion,
                    revolutionNumber
            );
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

    /**
     * Downloads TLE data from the given URL.
     *
     * @param urlString The URL to fetch TLE data from.
     *
     * @return A list of non-empty lines from the TLE file.
     *
     * @throws IOException If a network or read error occurs.
     */
    public List<String> fetchTLEData(String urlString) throws IOException {
        URL url = java.net.URI.create(urlString).toURL();
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) lines.add(line);
        }
        reader.close();
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
                elem.meanMotion = Double.parseDouble(line2.substring(52, 63).trim());
                elem.revolutionNumber = Integer.parseInt(line2.substring(63, 68).trim());

                return elem;
            }
        }
        return null;
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
