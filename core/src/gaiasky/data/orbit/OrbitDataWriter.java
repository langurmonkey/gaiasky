/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orbit;

import gaiasky.data.util.PointCloudData;
import gaiasky.util.Constants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class OrbitDataWriter {
    /**
     * Dumps the current orbit data to the given file
     *
     * @param filePath The path to the file to write
     * @param data     The OrbitData instance
     *
     * @throws IOException
     */
    public static void writeOrbitData(String filePath, PointCloudData data) throws IOException {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss").withLocale(Locale.US).withZone(ZoneOffset.UTC);

        File f = new File(filePath);
        if (f.exists() && f.isFile()) {
            f.delete();
        }

        if (f.isDirectory()) {
            throw new RuntimeException("File is directory: " + filePath);
        }

        f.createNewFile();

        FileWriter fw = new FileWriter(filePath);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("#time X Y Z");
        bw.newLine();
        int n = data.x.size();

        for (int i = 0; i < n; i++) {
            bw.write(df.format(data.time.get(i)) + " " + (data.x.get(i) * Constants.U_TO_KM) + " " + (data.y.get(i) * Constants.U_TO_KM) + " " + (data.z.get(i) * Constants.U_TO_KM));
            bw.newLine();
        }

        bw.flush();
        bw.close();

    }
}
