package gaiasky.data.orientation;

import com.badlogic.gdx.utils.Array;
import gaiasky.data.api.OrientationServer;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.parse.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;

/**
 * Abstract orientation server that reads a list of times and quaternions from a file, and interpolates
 * them using a specific function defined in the subclass.
 */
public abstract class QuaternionInterpolationOrientationServer implements OrientationServer {
    /** Separator regex. **/
    private static final String gscFileSeparatorRegex = "[\\s,]+";

    private final Array<Pair<Instant, QuaternionDouble>> data;

    protected final QuaternionDouble lastOrientation;

    public QuaternionInterpolationOrientationServer(String dataFile) {
        super();
        lastOrientation = new QuaternionDouble();
        var path = Paths.get(Settings.settings.data.dataFile(dataFile));
        data = initialize(path);
    }

    private Array<Pair<Instant, QuaternionDouble>> initialize(Path path) {

        if (!Files.exists(path)) {
            throw new RuntimeException(I18n.msg("error.file.exists", path.toString()));
        }
        if (!Files.isRegularFile(path)) {
            throw new RuntimeException(I18n.msg("error.file.isdir", path.toString()));
        }
        if (!Files.isReadable(path)) {
            throw new RuntimeException(I18n.msg("error.file.read", path.toString()));
        }

        Array<Pair<Instant, QuaternionDouble>> array = new Array<>();

        try (BufferedReader is = new BufferedReader(new InputStreamReader(Files.newInputStream(path)))) {
            String line;
            while ((line = is.readLine()) != null) {
                line = line.strip();
                var comment = line.startsWith("#");
                if (!comment) {
                    try {
                        String[] tokens = line.split(gscFileSeparatorRegex);
                        // Tokens:
                        // 0: time (Instant)
                        // 1: x
                        // 2: y
                        // 3: z
                        // 4: w

                        // Time.
                        Instant time;
                        try {
                            // Try with epoch millis.
                            time = Instant.ofEpochMilli(Parser.parseLongException(tokens[0]));
                        } catch (NumberFormatException ignored) {
                            time = Instant.parse(tokens[0]);
                        }

                        // Quaternion.
                        QuaternionDouble q = new QuaternionDouble(Parser.parseDouble(tokens[1]),
                                                                  Parser.parseDouble(tokens[2]),
                                                                  Parser.parseDouble(tokens[3]),
                                                                  Parser.parseDouble(tokens[4]));

                        array.add(new Pair<>(time, q));

                    } catch (NumberFormatException ignored) {
                        // We just skip the bad lines.
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return array;
    }

    /**
     * Finds the index of the start of the interval containing the given instant in the data array.
     *
     * @param instant The instant.
     *
     * @return The index of the start of the interval containing the given instant.
     */
    private int findStartIndex(Instant instant) {
        if (data != null && !data.isEmpty()) {
            if (data.size > 1) {
                // Before first or after last.
                if (instant.compareTo(data.get(0).getFirst()) <= 0) {
                    return 0;
                } else if (instant.compareTo(data.get(data.size - 1).getFirst()) >= 0) {
                    return data.size - 1;
                }

                // Find in array.
                for (int i = 1; i < data.size; i++) {
                    var i0 = data.get(i - 1);
                    var i1 = data.get(i);
                    if (instant.compareTo(i0.getFirst()) >= 0 && instant.compareTo(i1.getFirst()) < 0) {
                        return i - 1;
                    }
                }

            } else {
                return 0;
            }
        }
        return -1;
    }

    @Override
    public QuaternionDouble updateOrientation(Date date) {
        return updateOrientation(date.toInstant());
    }

    @Override
    public QuaternionDouble updateOrientation(Instant instant) {
        var idx = findStartIndex(instant);
        if (idx >= 0) {
            if (idx == data.size - 1) {
                lastOrientation.set(data.get(data.size - 1).getSecond());
                return lastOrientation;
            } else {
                var d0 = data.get(idx);
                var d1 = data.get(idx + 1);
                var t0 = d0.getFirst().toEpochMilli();
                var t1 = d1.getFirst().toEpochMilli();
                var q0 = d0.getSecond();
                var q1 = d1.getSecond();

                double alpha = (double) (instant.toEpochMilli() - t0) / (double) (t1 - t0);
                return interpolate(q0, q1, alpha);
            }
        }
        return null;
    }

    protected abstract QuaternionDouble interpolate(QuaternionDouble q0, QuaternionDouble q1, double alpha);

    @Override
    public QuaternionDouble getCurrentOrientation() {
        return lastOrientation;
    }

    @Override
    public boolean hasOrientation() {
        return data != null && !data.isEmpty();
    }
}
