package gaia.cu9.ari.gaiaorbit.data.orbit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;

import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

public class OriginalDataLoader {
    int count = 0;

    public static void main(String[] args) {
        OriginalDataLoader l = new OriginalDataLoader();
        try {
            PolylineData od = l.load(new FileInputStream("/home/tsagrista/Workspaces/workspace-luna/GaiaSandbox-android/assets-bak/data/ORB1_20131127_000001.topcat"));
            OrbitDataWriter.writeOrbitData("/home/tsagrista/Workspaces/workspace-luna/GaiaSandbox-android/assets/data/android/orb.GAIA.dat", od);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public OriginalDataLoader() {
        super();
    }

    /**
     * Loads the data in the input stream and transforms it into Cartesian
     * <b>ecliptic</b> coordinates. The reference system of the data goes as
     * follows:
     * <ul>
     * <li>Origin of frame : Earth</li>
     * <li>X and Y axis in the EQUATORIAL PLANE with X pointing in the direction
     * of vernal equinox.</li>
     * <li>Z perpendicular to the the EQUATORIAL PLANE in the north direction
     * </li>
     * <li>The Y direction is defined to have (X,Y,Z) as a "three axis"
     * positively oriented.</li>
     * </ul>
     * 
     * The simulation reference system:
     * <ul>
     * <li>- XZ lies in the ECLIPTIC PLANE, with Z pointing to the vernal
     * equinox.</li>
     * <li>- Y perpendicular to the ECLIPTIC PLANE pointing north.</li>
     * </ul>
     * 
     * @param data
     *            The input stream with the data to load
     * @throws Exception
     */
    public PolylineData load(InputStream data) throws Exception {
        PolylineData orbitData = new PolylineData();

        BufferedReader br = new BufferedReader(new InputStreamReader(data));
        String line;

        while ((line = br.readLine()) != null) {
            if (!line.isEmpty() && !line.startsWith("#")) {
                // Read line
                String[] tokens = line.split("\\s+");
                if (tokens.length == 10) {
                    // Valid data line
                    Timestamp t = Timestamp.valueOf(tokens[0].replace('T', ' '));

                    /*
                     * From Data coordinates to OpenGL world coordinates Z -> -X
                     * X -> Y Y -> Z
                     */
                    Vector3d pos = new Vector3d(parsed(tokens[2]), parsed(tokens[3]), -parsed(tokens[1]));

                    // Transform to heliotropic using the Sun's ecliptic longitude
                    Vector3d posHel = correctSunLongitude(pos, t.toInstant(), 0);

                    // To ecliptic again
                    pos.mul(Coordinates.eqToEcl());
                    posHel.mul(Coordinates.eqToEcl());

                    if (count++ % 7 == 0) {
                        orbitData.time.add(t.toInstant());
                        orbitData.x.add(posHel.x * Constants.KM_TO_U);
                        orbitData.y.add(posHel.y * Constants.KM_TO_U);
                        orbitData.z.add(posHel.z * Constants.KM_TO_U);

                    }

                }
            }
        }

        br.close();

        return orbitData;
    }

    protected float parsef(String str) {
        return Float.valueOf(str);
    }

    protected double parsed(String str) {
        return Double.valueOf(str);
    }

    protected int parsei(String str) {
        return Integer.valueOf(str);
    }

    /**
     * Transforms the given vector to a heliotropic system using the given time.
     * 
     * @param pos
     *            Position vector
     * @param t
     *            Time
     * @return Vector3 with the position in the heliotropic reference frame
     */
    protected Vector3d correctSunLongitude(final Vector3d pos, Instant t) {
        return correctSunLongitude(pos, t, 0);
    }

    /**
     * Transforms the given vector to a heliotropic system using the given time.
     * 
     * @param pos
     *            Position vector
     * @param t
     *            Time
     * @param origin
     *            The origin angle
     * @return Vector3 with the position in the heliotropic reference frame
     */
    protected Vector3d correctSunLongitude(final Vector3d pos, Instant t, float origin) {
        Vector3d upDirection = new Vector3d(0, 1, 0);
        // We get the Up direction of the ecliptic in equatorial coordinates
        upDirection.mul(Coordinates.eclToEq());
        return pos.cpy().rotate(upDirection, AstroUtils.getSunLongitude(t) - origin);
    }

    private float getYearFraction(int year, int month, int day, int hour, int min, int sec) {
        return year + month / 12f + day / 365.242f + hour / 8765.81f + min / 525949f + sec / 31556940f;
    }

    private float getYearFraction(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        int sec = cal.get(Calendar.SECOND);

        return getYearFraction(year, month, day, hour, min, sec);
    }

    /**
     * Writes a file under the given path with the distance data
     */
    public void writeDistVsTimeData(String filePath, PolylineData data) throws Exception {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("#time[ms] time[year] dist[km]");
        bw.newLine();
        long iniTime = -1;

        int n = data.x.size;
        for (int i = 0; i < n; i++) {
            Vector3d pos = new Vector3d(data.x.get(i), data.y.get(i), data.z.get(i));
            Instant t = data.time.get(i);

            long time = iniTime < 0 ? 0 : t.toEpochMilli() - iniTime;
            if (time == 0) {
                iniTime = t.toEpochMilli();
            }
            float timey = getYearFraction(iniTime + time);

            bw.write(time + " " + timey + " " + pos.len());
            bw.newLine();

        }

        bw.close();
    }

}
