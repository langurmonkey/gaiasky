package gaiasky.util.camera.rec;

import com.badlogic.gdx.utils.LongArray;
import gaiasky.util.DoubleArray;
import gaiasky.util.i18n.I18n;
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
    int n;

    /**
     * Contains the time as a long timestamp for each step.
     */
    final LongArray times;
    /**
     * Contains the position (3), direction (3) and up (3) values for each step.
     */
    final DoubleArray data;

    /**
     * Current step number.
     */
    int i;

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
