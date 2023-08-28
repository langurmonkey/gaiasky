/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.camera.rec;

import com.badlogic.gdx.utils.Array;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.math.*;
import gaiasky.util.parse.Parser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class KeyframesManager implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(KeyframesManager.class);
    /**
     * Separator for keyframes files
     **/
    private static final String keyframeSeparator = ",";
    /**
     * Separator for camera files
     **/
    private static final String sep = " ";
    /**
     * Singleton
     **/
    public static KeyframesManager instance;

    private final Vector3d v3d1 = new Vector3d();
    private final Vector3d v3d2 = new Vector3d();

    private final QuaternionDouble q = new QuaternionDouble();
    private final QuaternionDouble q0 = new QuaternionDouble();
    private final QuaternionDouble q1 = new QuaternionDouble();

    public KeyframesManager() {
        super();

        EventManager.instance.subscribe(this, Event.KEYFRAMES_FILE_SAVE, Event.KEYFRAMES_EXPORT);
    }

    public static void initialize() {
        instance = new KeyframesManager();
    }

    private PathDouble<Vector3d> getPath(Vector3d[] data,
                                         PathType pathType) {
        if (pathType == PathType.LINEAR) {
            return new LinearDouble<>(data);
        } else if (pathType == PathType.SPLINE) {
            // Needs extra points at beginning and end
            Vector3d[] extData = new Vector3d[data.length + 2];
            System.arraycopy(data, 0, extData, 1, data.length);
            extData[0] = data[0];
            extData[data.length + 1] = data[data.length - 1];
            return new CatmullRomSplined<>(extData, false);
        }
        // Default
        return new LinearDouble<>(data);
    }

    public Array<Keyframe> loadKeyframesFile(Path file) throws RuntimeException {
        try (BufferedReader br = new BufferedReader(new FileReader(file.toFile()))) {
            Array<Keyframe> result = new Array<>();
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(keyframeSeparator);
                double secs = Parser.parseDouble(tokens[0]);
                long time = Parser.parseLong(tokens[1]);
                Vector3d pos = new Vector3d(Parser.parseDouble(tokens[2]), Parser.parseDouble(tokens[3]), Parser.parseDouble(tokens[4]));
                Vector3d dir = new Vector3d(Parser.parseDouble(tokens[5]), Parser.parseDouble(tokens[6]), Parser.parseDouble(tokens[7]));
                Vector3d up = new Vector3d(Parser.parseDouble(tokens[8]), Parser.parseDouble(tokens[9]), Parser.parseDouble(tokens[10]));
                boolean seam = Parser.parseInt(tokens[11]) == 1;
                String name = tokens[12];
                Keyframe kf = new Keyframe(name, pos, dir, up, time, secs, seam);
                result.add(kf);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveKeyframesFile(Array<Keyframe> keyframes,
                                  String fileName) {
        Path f = SysUtils.getDefaultCameraDir().resolve(fileName);
        if (Files.exists(f)) {
            try {
                Files.delete(f);
            } catch (IOException e) {
                logger.error(e);
            }
        }
        try (BufferedWriter os = new BufferedWriter(new FileWriter(f.toFile()))) {
            for (Keyframe kf : keyframes) {
                os.append(Double.toString(kf.seconds)).append(keyframeSeparator).append(Long.toString(kf.time)).append(keyframeSeparator);
                os.append(Double.toString(kf.pos.x)).append(keyframeSeparator).append(Double.toString(kf.pos.y)).append(keyframeSeparator).append(
                        Double.toString(kf.pos.z)).append(keyframeSeparator);
                os.append(Double.toString(kf.dir.x)).append(keyframeSeparator).append(Double.toString(kf.dir.y)).append(keyframeSeparator).append(
                        Double.toString(kf.dir.z)).append(keyframeSeparator);
                os.append(Double.toString(kf.up.x)).append(keyframeSeparator).append(Double.toString(kf.up.y)).append(keyframeSeparator).append(
                        Double.toString(kf.up.z)).append(keyframeSeparator);
                os.append(Integer.toString(kf.seam ? 1 : 0)).append(keyframeSeparator);
                os.append(kf.name).append("\n");
            }

        } catch (IOException e) {
            logger.error(e);
            return;
        }
        logger.info(keyframes.size + " keyframes saved to file " + f);

    }

    public double[] samplePaths(Array<Array<Vector3d>> pointsSep,
                                double[] points,
                                int samplesPerSegment,
                                PathType pathType) {
        if (pathType == PathType.LINEAR) {
            // No need to sample a linear interpolation
            double[] result = new double[points.length];
            System.arraycopy(points, 0, result, 0, points.length);
            return result;
        } else {
            Array<Double> res = new Array<>();
            for (Array<Vector3d> vec : pointsSep) {
                int nSamples = (vec.size - 1) * samplesPerSegment + 1;
                int nChunks = nSamples - 1;

                Vector3d aux = new Vector3d();
                PathDouble<Vector3d> sampler = getPath(toArray(vec), pathType);
                double step = 1d / nChunks;
                int i = 0;
                for (double t = 0d; i < nSamples * 3; t += step) {
                    sampler.valueAt(aux, t);
                    res.add(aux.x);
                    res.add(aux.y);
                    res.add(aux.z);
                    i += 3;
                }

            }

            double[] result = new double[res.size];
            int i = 0;
            for (Double d : res) {
                result[i] = d;
                i++;
            }

            return result;
        }
    }

    private Vector3d[] toArray(Array<Vector3d> v) {
        Vector3d[] out = new Vector3d[v.size];
        for (int i = 0; i < v.size; i++)
            out[i] = v.get(i);
        return out;
    }

    public void exportKeyframesFile(Array<Keyframe> keyframes,
                                    String fileName) {
        Path f = SysUtils.getDefaultCameraDir().resolve(fileName);
        if (Files.exists(f)) {
            try {
                Files.delete(f);
            } catch (IOException e) {
                logger.error(e);
            }
        }
        var cameraPath = new CameraPath(keyframes, positionsToPathParts(keyframes, Settings.settings.camrecorder.keyframe.position));

        try {
            cameraPath.persist(f);
        } catch (Exception e) {
            logger.error(e);
            return;
        }
        double frameRate = Settings.settings.camrecorder.targetFps;
        logger.info(keyframes.size + " keyframes (" + cameraPath.n + " frames, " + frameRate + " FPS) exported to camera file " + f);
    }

    /**
     * Convert an array of keyframes to a list of path parts by separating the path at seams.
     *
     * @param keyframes The array of keyframes.
     * @param pathType  The path type.
     *
     * @return Array of path parts.
     */
    private PathPart[] positionsToPathParts(Array<Keyframe> keyframes,
                                            PathType pathType) {
        double frameRate = Settings.settings.camrecorder.targetFps;
        Array<Array<Vector3d>> positionsSep = new Array<>();
        Array<Vector3d> current = new Array<>();
        Array<Double> times = new Array<>();
        int i = 0;
        double secs = 0;
        for (Keyframe kf : keyframes) {

            // Fill positions
            if (kf.seam && pathType == PathType.SPLINE) {
                if (i > 0 && i < keyframes.size - 1) {
                    current.add(kf.pos);
                    positionsSep.add(current);
                    times.add(secs + kf.seconds);
                    current = new Array<>();
                    secs = -kf.seconds;
                }
            }
            secs += kf.seconds;
            current.add(kf.pos);
            i++;
        }
        // Last
        positionsSep.add(current);
        times.add(secs);

        PathPart[] res = new PathPart[positionsSep.size];
        int j = 0;
        for (Array<Vector3d> part : positionsSep) {
            double elapsed = times.get(j);
            PathPart pp = new PathPart(getPath(toArray(part), pathType), part.size, (long) (frameRate * elapsed));
            res[j] = pp;
            j++;
        }
        return res;
    }

    @Override
    public void notify(final Event event,
                       Object source,
                       final Object... data) {

        switch (event) {
        case KEYFRAMES_FILE_SAVE -> {
            Array<Keyframe> keyframes = (Array<Keyframe>) data[0];
            String fileName = (String) data[1];
            saveKeyframesFile(keyframes, fileName);
        }
        case KEYFRAMES_EXPORT -> {
            var keyframes = (Array<Keyframe>) data[0];
            var fileName = (String) data[1];
            exportKeyframesFile(keyframes, fileName);
        }
        default -> {
        }
        }
    }

    public enum PathType {
        LINEAR,
        SPLINE
    }

    public static class PathPart {
        PathDouble<Vector3d> path;
        int nPoints, nChunks;
        long nFrames;

        public PathPart(PathDouble<Vector3d> path,
                        int nPoints,
                        long nFrames) {
            this.path = path;
            this.nPoints = nPoints;
            this.nChunks = nPoints - 1;
            this.nFrames = nFrames;
        }

    }
}
