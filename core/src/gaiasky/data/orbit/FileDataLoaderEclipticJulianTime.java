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
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.Calendar;

public class FileDataLoaderEclipticJulianTime {
    public FileDataLoaderEclipticJulianTime() {
        super();
    }

    /**
     * Loads the data in the input stream into an OrbitData object.
     *
     * @param data The input stream
     *
     * @return The orbit data
     */
    public PointCloudData load(InputStream data) throws Exception {
        PointCloudData orbitData = new PointCloudData();

        BufferedReader br = new BufferedReader(new InputStreamReader(data));
        String line;

        Timestamp last = new Timestamp(0);
        while ((line = br.readLine()) != null) {
            if (!line.isEmpty() && !line.startsWith("#")) {
                // Read line
                String[] tokens = line.split("\\s+");
                if (tokens.length >= 4) {
                    // Valid data line
                    Timestamp t = new Timestamp(getTime(tokens[0]));
                    Matrix4D transform = new Matrix4D();
                    transform.scl(Constants.KM_TO_U);
                    if (!t.equals(last)) {

                        Vector3D pos = new Vector3D(parsed(tokens[1]), parsed(tokens[2]), parsed(tokens[3]));
                        pos.mul(transform);
                        orbitData.addPoint(pos, t.toInstant());
                        last.setTime(t.getTime());
                    }
                }
            }
        }

        br.close();

        return orbitData;
    }

    protected float parsef(String str) {
        return Float.parseFloat(str);
    }

    protected double parsed(String str) {
        return Double.parseDouble(str);
    }

    protected int parsei(String str) {
        return Integer.parseInt(str);
    }

    private long getTime(String jds) {
        double jd = Double.parseDouble(jds);
        long[] dt = AstroUtils.getCalendarDay(jd);
        Calendar cld = Calendar.getInstance();
        cld.set((int) dt[0], (int) dt[1], (int) dt[2], (int) dt[3], (int) dt[4], (int) dt[5]);
        return cld.getTimeInMillis();
    }
}