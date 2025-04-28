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
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.Instant;

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
                        Vector3d pos = new Vector3d(parsed(tokens[1]), parsed(tokens[2]), parsed(tokens[3]));
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
