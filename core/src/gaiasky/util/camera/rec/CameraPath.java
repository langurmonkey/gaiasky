package gaiasky.util.camera.rec;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongArray;
import gaiasky.util.DoubleArray;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.camera.rec.KeyframesManager.PathPart;
import gaiasky.util.camera.rec.KeyframesManager.PathType;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Contains the in-memory data for a specific camera path.
 */
public class CameraPath {

    /**
     * Number of steps in the current path.
     */
    public long n;

    /**
     * Contains the time as a long timestamp for each step.
     */
    public final LongArray times;
    /**
     * Contains the position (3), direction (3) and up (3) values for each step.
     */
    public final DoubleArray data;

    /**
     * Current step number.
     */
    public long i;

    /**
     * Create an empty camera path.
     */
    public CameraPath() {
        times = new LongArray();
        data = new DoubleArray();
        n = 0;
        i = 0;
    }

    /**
     * Create a camera path from a <code>.gsc</code> file.
     *
     * @param file The file.
     *
     * @throws RuntimeException If the file can't be read, is not in the right format, or does not exist.
     */
    public CameraPath(InputStream file) throws RuntimeException {
        times = new LongArray();
        data = new DoubleArray();

        try (BufferedReader is = new BufferedReader(new InputStreamReader(file))) {
            String line;
            while ((line = is.readLine()) != null) {
                line = line.strip();
                if (!line.startsWith("#")) {
                    String[] tokens = line.split("\\s+");

                    // Time.
                    times.add(Parser.parseLong(tokens[0]));

                    // Position.
                    data.add(Parser.parseDouble(tokens[1]), Parser.parseDouble(tokens[2]), Parser.parseDouble(tokens[3]));
                    // Direction.
                    data.add(Parser.parseDouble(tokens[4]), Parser.parseDouble(tokens[5]), Parser.parseDouble(tokens[6]));
                    // Up.
                    data.add(Parser.parseDouble(tokens[7]), Parser.parseDouble(tokens[8]), Parser.parseDouble(tokens[9]));

                }
            }
            n = times.size;
            i = 0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a camera path from a list of keyframes and its respective array of path parts.
     *
     * @param keyframes  Array of keyframes.
     * @param posSplines Array of path parts.
     */
    public CameraPath(Array<Keyframe> keyframes,
                      PathPart[] posSplines) {
        times = new LongArray();
        data = new DoubleArray();

        final var q = new QuaternionDouble();
        final var q0 = new QuaternionDouble();
        final var q1 = new QuaternionDouble();
        final var v3d1 = new Vector3d();
        final var v3d2 = new Vector3d();

        /* Frame counter */
        double frameRate = Settings.settings.camrecorder.targetFps;

        PathPart currentPosSpline = posSplines[0];
        int k = 0;
        /* Position in current position spline */
        double splinePosIdx = 0d;
        /* Step length in between control positions */
        double splinePosStep = 1d / (currentPosSpline.nPoints - 1);

        for (int i = 1; i < keyframes.size; i++) {
            Keyframe k0 = keyframes.get(i - 1);
            Keyframe k1 = keyframes.get(i);

            q0.setFromCamera(k0.dir, k0.up);
            q1.setFromCamera(k1.dir, k1.up);

            long nFrames = (long) (k1.seconds * frameRate);
            double splinePosSubStep = splinePosStep / (nFrames - 1);

            long dt = k1.time - k0.time;
            long tStep = dt / (nFrames - 1);

            for (long fr = 0; fr < nFrames; fr++) {
                // Local index in 0..1
                double a = (double) fr / (double) (nFrames - 1);
                // Partial position spline index in 0..1
                double b = splinePosIdx + splinePosSubStep * fr;

                // TIME
                times.add(k0.time + tStep * fr);

                // POS
                currentPosSpline.path.valueAt(v3d1, b);
                data.add(v3d1.x, v3d1.y, v3d1.z);

                // ORIENTATION
                q.set(q0).slerp(q1, a);
                // direction
                q.getDirection(v3d1);
                v3d1.nor();
                data.add(v3d1.x, v3d1.y, v3d1.z);

                // up
                q.getUp(v3d2);
                data.add(v3d2.x, v3d2.y, v3d2.z);
            }

            splinePosIdx += splinePosStep;

            // If k1 is seam and not last, and we're doing splines, jump to next spline
            if (k1.seam && i < keyframes.size - 1 && Settings.settings.camrecorder.keyframe.position == PathType.SPLINE) {
                currentPosSpline = posSplines[++k];
                splinePosIdx = 0;
                splinePosStep = 1d / (currentPosSpline.nPoints - 1);
            }
        }
        n = times.size;
    }

    /**
     * Add a new time step at the end of the path.
     *
     * @param time The time, in milliseconds.
     * @param px   The x-position.
     * @param py   The y-position.
     * @param pz   The z-position.
     * @param dx   The x-direction.
     * @param dy   The y-direction.
     * @param dz   The z-direction.
     * @param ux   The x-up.
     * @param uy   The y-up.
     * @param uz   The z-up.
     */
    public void add(long time,
                    double px,
                    double py,
                    double pz,
                    double dx,
                    double dy,
                    double dz,
                    double ux,
                    double uy,
                    double uz) {

        // Time.
        times.add(time);

        // Position.
        data.add(px, py, pz);
        // Direction.
        data.add(dx, dy, dz);
        // Up.
        data.add(ux, uy, uz);

        // Update size.
        n = times.size;
    }

    /** Separator for camera path files. **/
    private static final String sep = " ";

    /**
     * Persist the current camera path to the file pointed by the given path.
     *
     * @param f The path of the file.
     *
     * @return True if the persist operation succeeded.
     *
     * @throws IOException If the path does not point to a file, or the file could not be written.
     */
    public boolean persist(Path f) throws Exception {
        // Make sure the file does not yet exist.
        if (Files.exists(f)) {
            throw new RuntimeException(I18n.msg("error.file.exists", f.toString()));
        }

        Files.createFile(f);
        var os = new BufferedWriter(new FileWriter(f.toFile()));

        // Print header.
        os.append("#time_ms").append(sep).append("pos_x").append(sep).append("pos_y").append(sep).append("pos_z").append(sep);
        os.append("dir_x").append(sep).append("dir_y").append(sep).append("dir_z").append(sep);
        os.append("up_x").append(sep).append("up_y").append(sep).append("up_z").append(sep);
        os.append("\n");

        // Print data.
        for (int i = 0; i < n; i++) {
            os.append(Long.toString(times.get(i))).append(sep);
            int ip = i * 9;
            os.append(Double.toString(data.get(ip))).append(sep).append(Double.toString(data.get(ip + 1))).append(sep).append(Double.toString(data.get(ip + 2)));
            os.append(sep).append(Double.toString(data.get(ip + 3))).append(sep).append(Double.toString(data.get(ip + 4))).append(sep).append(
                    Double.toString(data.get(ip + 5)));
            os.append(sep).append(Double.toString(data.get(ip + 6))).append(sep).append(Double.toString(data.get(ip + 7))).append(sep).append(
                    Double.toString(data.get(ip + 8)));
            os.append("\n");
        }

        // Close stream.
        os.close();

        return true;
    }
}
