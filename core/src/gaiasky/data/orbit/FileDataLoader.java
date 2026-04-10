/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orbit;

import gaiasky.data.util.PointCloudData;
import gaiasky.util.Constants;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.Vector3D;
import gaiasky.util.parse.Parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Loads orbit data from an ASCII text file.
 * <p>
 * This loader parses files where each line represents a single data point in an orbit.
 * The loader ignores blank lines and lines starting with the comment character {@code #}.
 * </p>
 * <h3>Data Format</h3>
 * Each data line must contain at least 4 tokens separated by whitespace:
 * <ol>
 *     <li><b>Time:</b> The time of the observation. This can be in two formats:
 *         <ul>
 *             <li>A timestamp string compatible with {@link java.sql.Timestamp#valueOf(String)},
 *             where underscores {@code _} are treated as spaces (e.g., {@code 2023-10-27_12:00:00}).</li>
 *             <li>A Julian Date as a double-precision floating-point number.</li>
 *         </ul>
 *     </li>
 *     <li><b>X coordinate:</b> The X position in kilometers.</li>
 *     <li><b>Y coordinate:</b> The Y position in kilometers.</li>
 *     <li><b>Z coordinate:</b> The Z position in kilometers.</li>
 * </ol>
 * <p>
 * Coordinates are automatically converted from kilometers to Gaia Sky's internal units
 * using {@link gaiasky.util.Constants#KM_TO_U}.
 * </p>
 * <p>
 * If multiple consecutive lines have the same timestamp, only the first one is processed,
 * and subsequent ones are ignored to prevent duplicate data points at the same time.
 * Any additional tokens beyond the first four in a line are ignored.
 * </p>
 */
public class FileDataLoader {

    public FileDataLoader() {
        super();
    }

    /**
     * Parses the given time and returns the number of milliseconds from the epoch of 1970-01-01T00:00:00Z.
     *
     * @param token The token to parse.
     * @return time as milliseconds from the epoch of 1970-01-01T00:00:00Z
     */
    public long parseTime(String token) {
        try {
            Timestamp t = Timestamp.valueOf(token.replace('_', ' '));
            return t.getTime();
        } catch (Exception e) {
            // Not a timestamp. Possibly a julian date.
            double jd = Parser.parseDouble(token);
            return AstroUtils.julianDateToInstant(jd).toEpochMilli();
        }
    }

    /**
     * Loads the data in the input stream into an OrbitData object.
     */
    public PointCloudData load(InputStream data) throws Exception {
        PointCloudData orbitData = new PointCloudData();

        BufferedReader br = new BufferedReader(new InputStreamReader(data));
        String line;

        long last = 0L;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (!line.isBlank() && !line.startsWith("#")) {
                // Read line
                String[] tokens = line.split("\\s+");
                if (tokens.length >= 4) {
                    // Valid data line.
                    long t = parseTime(tokens[0].trim());
                    if (t != last) {
                        Vector3D pos = new Vector3D(parsed(tokens[1]), parsed(tokens[2]), parsed(tokens[3]));
                        // Kilometers to internal units.
                        pos.scl(Constants.KM_TO_U);

                        orbitData.addPoint(pos, Instant.ofEpochMilli(t));

                        last = t;
                    }
                }
            }
        }

        br.close();

        return orbitData;
    }

    protected float parsef(String str) {
        return Parser.parseFloat(str);
    }

    protected double parsed(String str) {
        return Parser.parseDouble(str);
    }

    protected int parsei(String str) {
        return Parser.parseInt(str);
    }

}
