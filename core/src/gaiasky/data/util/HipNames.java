/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.util;

import com.badlogic.gdx.utils.Array;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.parse.Parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Loads HIP star names. If given a folder, it will recursively find and load all files which end with '_To_HIP.dat'.
 */
public class HipNames {
    private static final Log logger = Logger.getLogger(HipNames.class);

    private final Map<Integer, Array<String>> hipNames;

    public HipNames() {
        super();
        hipNames = new ConcurrentHashMap<>();
    }

    public Map<Integer, Array<String>> getHipNames() {
        return hipNames;
    }

    public void reset() {
        hipNames.clear();
    }

    public void dispose() {
        reset();
    }

    public void load(Path path) {
        if (Files.isDirectory(path)) {
            try {
                Files.list(path).filter(file -> file.getFileName().toString().endsWith("_To_HIP.dat")).sorted().forEach(this::load);
            } catch (IOException e) {
                logger.error(e);
            }
        } else {
            try (Stream<String> lines = Files.lines(path)) {
                lines.forEach(line -> {
                    try {
                        String[] tokens = line.split(Constants.nameSeparatorRegex);
                        String name = tokens[0].trim().replace("_", " ");
                        int hip = Parser.parseInt(tokens[1].trim());
                        if (hipNames.containsKey(hip)) {
                            Array<String> l = hipNames.get(hip);
                            if (!l.contains(name, false))
                                l.add(name);
                        } else {
                            Array<String> l = new Array<>(false, 1);
                            l.add(name);
                            hipNames.put(hip, l);
                        }
                    } catch (Exception e) {
                        // Not right format
                    }
                });
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }

    public void load(Path... paths) {
        for (Path path : paths)
            load(path);
    }
}
