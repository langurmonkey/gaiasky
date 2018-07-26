package gaia.cu9.ari.gaiaorbit.data.orbit;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.parse.Parser;

public class FileDataLoader {
    int count = 0;

    public FileDataLoader() {
        super();
    }

    /**
     * Loads the data in the input stream into an OrbitData object.
     * @param data
     * @throws Exception
     */
    public PolylineData load(InputStream data) throws Exception {
        PolylineData orbitData = new PolylineData();

        BufferedReader br = new BufferedReader(new InputStreamReader(data));
        String line;

        Timestamp last = new Timestamp(0);
        while ((line = br.readLine()) != null) {
            if (!line.isEmpty() && !line.startsWith("#")) {
                // Read line
                String[] tokens = line.split("\\s+");
                if (tokens.length >= 4) {
                    // Valid data line
                    Timestamp t = Timestamp.valueOf(tokens[0].replace('_', ' '));
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
