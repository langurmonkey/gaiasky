/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orbit;

import gaiasky.data.util.PointCloudData;
import gaiasky.util.Constants;
import gaiasky.util.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class OrbitDataWriter {
    private static final Logger.Log logger = Logger.getLogger(OrbitDataWriter.class);

    /**
     * Dumps the current orbit data to the given file
     *
     * @param filePath The path to the file to write
     * @param data     The OrbitData instance
     */
    public static void writeOrbitData(String filePath, PointCloudData data) throws IOException {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss")
                .withLocale(Locale.US)
                .withZone(ZoneOffset.UTC);

        File f = new File(filePath);
        if (f.exists() && f.isFile()) {
            if (!f.delete()) {
                logger.warn("Could not delete file: " + f);
            }
        }

        if (f.isDirectory()) {
            throw new RuntimeException("File is directory: " + filePath);
        }

        if (!f.createNewFile()) {
            logger.warn("Could not create file: " + f);
        }

        FileWriter fw = new FileWriter(filePath);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("#time X Y Z");
        bw.newLine();
        int n = data.samples.size();

        for (int i = 0; i < n; i++) {
            var p = data.samples.get(i);
            bw.write(df.format(
                    p.toInstant()) + " " + (p.x() * Constants.U_TO_KM) + " " + (p.y() * Constants.U_TO_KM) + " " + (p.z() * Constants.U_TO_KM));
            bw.newLine();
        }

        bw.flush();
        bw.close();

    }
}
