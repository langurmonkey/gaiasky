/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orbit;

import gaiasky.data.util.PointCloudData;
import gaiasky.util.Constants;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;

public class FileDataLoader {

    public FileDataLoader() {
        super();
    }

    /**
     * Loads the data in the input stream into an OrbitData object.
     */
    public PointCloudData load(InputStream data) throws Exception {
        PointCloudData orbitData = new PointCloudData();

        BufferedReader br = new BufferedReader(new InputStreamReader(data));
        String line;

        Timestamp last = new Timestamp(0);
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (!line.isBlank() && !line.startsWith("#")) {
                // Read line
                String[] tokens = line.split("\\s+");
                if (tokens.length >= 4) {
                    // Valid data line
                    Timestamp t = Timestamp.valueOf(tokens[0].trim().replace('_', ' '));
                    Matrix4d transform = new Matrix4d();
                    transform.scl(Constants.KM_TO_U);
                    if (!t.equals(last)) {
                        orbitData.time.add(t.toInstant());

                        /* From Data coordinates to OpenGL world coordinates
                         * Z -> -X
                         * X -> Y
                         * Y -> Z
                         */
                        Vector3d pos = new Vector3d(parsed(tokens[1]), parsed(tokens[2]), parsed(tokens[3]));
                        pos.mul(transform);
                        orbitData.x.add(pos.x);
                        orbitData.y.add(pos.y);
                        orbitData.z.add(pos.z);
                        last.setTime(t.getTime());
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
