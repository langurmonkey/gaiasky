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
import gaiasky.util.math.CatmullRomSplined;
import gaiasky.util.math.LinearDouble;
import gaiasky.util.math.PathDouble;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class CameraKeyframeManager implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(CameraKeyframeManager.class);
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
    public static CameraKeyframeManager instance;

    public CameraKeyframeManager() {
        super();

        EventManager.instance.subscribe(this, Event.KEYFRAMES_FILE_SAVE, Event.KEYFRAMES_EXPORT);
    }

    public static void initialize() {
        instance = new CameraKeyframeManager();
    }

    private PathDouble<Vector3d> getPath(Vector3d[] data, PathType pathType) {
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
        Array<Keyframe> result = new Array<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file.toFile()));
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

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    public void saveKeyframesFile(Array<Keyframe> keyframes, String fileName) {
        Path f = SysUtils.getDefaultCameraDir().resolve(fileName);
        if (Files.exists(f)) {
            try {
                Files.delete(f);
            } catch (IOException e) {
                logger.error(e);
            }
        }
        BufferedWriter os = null;
        try {
            Files.createFile(f);
            os = new BufferedWriter(new FileWriter(f.toFile()));

            for (Keyframe kf : keyframes) {
                os.append(Double.toString(kf.seconds)).append(keyframeSeparator).append(Long.toString(kf.time)).append(keyframeSeparator);
                os.append(Double.toString(kf.pos.x)).append(keyframeSeparator).append(Double.toString(kf.pos.y)).append(keyframeSeparator).append(Double.toString(kf.pos.z)).append(keyframeSeparator);
                os.append(Double.toString(kf.dir.x)).append(keyframeSeparator).append(Double.toString(kf.dir.y)).append(keyframeSeparator).append(Double.toString(kf.dir.z)).append(keyframeSeparator);
                os.append(Double.toString(kf.up.x)).append(keyframeSeparator).append(Double.toString(kf.up.y)).append(keyframeSeparator).append(Double.toString(kf.up.z)).append(keyframeSeparator);
                os.append(Integer.toString(kf.seam ? 1 : 0)).append(keyframeSeparator);
                os.append(kf.name).append("\n");
            }

        } catch (IOException e) {
            logger.error(e);
            return;
        } finally {
            if (os != null)
                try {
                    os.close();
                } catch (Exception e) {
                    logger.error(e);
                }
        }
        logger.info(keyframes.size + " keyframes saved to file " + f);

    }

    public double[] samplePaths(Array<Array<Vector3d>> pointsSep, double[] points, int samplesPerSegment, PathType pathType) {
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

    public void exportKeyframesFile(Array<Keyframe> keyframes, String fileName) {
        Path f = SysUtils.getDefaultCameraDir().resolve(fileName);
        if (Files.exists(f)) {
            try {
                Files.delete(f);
            } catch (IOException e) {
                logger.error(e);
            }
        }
        BufferedWriter os = null;

        /* Frame counter */
        long frames = 0;
        double frameRate = Settings.settings.camrecorder.targetFps;

        try {
            Files.createFile(f);
            os = new BufferedWriter(new FileWriter(f.toFile()));

            Vector3d[] directions = new Vector3d[keyframes.size];
            Vector3d[] ups = new Vector3d[keyframes.size];

            // Fill in vectors
            for (int i = 0; i < keyframes.size; i++) {
                Keyframe k = keyframes.get(i);
                directions[i] = k.dir;
                ups[i] = k.up;
            }

            PathPart[] posSplines = positionsToPathParts(keyframes, Settings.settings.camrecorder.keyframe.position);
            PathDouble<Vector3d> dirSpline = getPath(directions, Settings.settings.camrecorder.keyframe.orientation);
            PathDouble<Vector3d> upSpline = getPath(ups, Settings.settings.camrecorder.keyframe.orientation);

            Vector3d aux = new Vector3d();

            /* Current position in the spline. Coincides with the control points */
            double splineIdx = 0d;
            /* Step length between control points */
            double splineStep = 1d / (directions.length - 1);

            PathPart currentPosSpline = posSplines[0];
            int k = 0;
            /* Position in current position spline */
            double splinePosIdx = 0d;
            /* Step length in between control positions */
            double splinePosStep = 1d / (currentPosSpline.nPoints - 1);

            for (int i = 1; i < keyframes.size; i++) {
                Keyframe k0 = keyframes.get(i - 1);
                Keyframe k1 = keyframes.get(i);

                long nFrames = (long) (k1.seconds * frameRate);
                double splineSubStep = splineStep / nFrames;
                double splinePosSubStep = splinePosStep / nFrames;

                long dt = k1.time - k0.time;
                long tStep = dt / nFrames;

                for (long fr = 0; fr < nFrames; fr++) {
                    // Global spline index in 0..1
                    double a = splineIdx + splineSubStep * fr;
                    // Partial position spline index in 0..1
                    double b = splinePosIdx + splinePosSubStep * fr;

                    // TIME
                    os.append(Long.toString(k0.time + tStep * fr)).append(sep);

                    // POS
                    currentPosSpline.path.valueAt(aux, b);
                    os.append(Double.toString(aux.x)).append(sep).append(Double.toString(aux.y)).append(sep).append(Double.toString(aux.z)).append(sep);

                    // DIR
                    dirSpline.valueAt(aux, a);
                    aux.nor();
                    os.append(Double.toString(aux.x)).append(sep).append(Double.toString(aux.y)).append(sep).append(Double.toString(aux.z)).append(sep);

                    // UP
                    upSpline.valueAt(aux, a);
                    aux.nor();
                    os.append(Double.toString(aux.x)).append(sep).append(Double.toString(aux.y)).append(sep).append(Double.toString(aux.z));

                    // New line
                    os.append("\n");

                    frames++;
                }

                splineIdx += splineStep;
                splinePosIdx += splinePosStep;

                // If k1 is seam and not last, and we're doing splines, jump to next spline
                if (k1.seam && i < keyframes.size - 1 && Settings.settings.camrecorder.keyframe.position == PathType.SPLINE) {
                    currentPosSpline = posSplines[++k];
                    splinePosIdx = 0;
                    splinePosStep = 1d / (currentPosSpline.nPoints - 1);
                }
            }

        } catch (IOException e) {
            logger.error(e);
            return;
        } finally {
            if (os != null)
                try {
                    os.close();
                } catch (Exception e) {
                    logger.error(e);
                }
        }
        logger.info(keyframes.size + " keyframes (" + frames + " frames, " + frameRate + " FPS) exported to camera file " + f);
    }

    private PathPart[] positionsToPathParts(Array<Keyframe> keyframes, PathType pathType) {
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
    public void notify(final Event event, Object source, final Object... data) {

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

    static class PathPart {
        PathDouble<Vector3d> path;
        int nPoints, nChunks;
        long nFrames;

        public PathPart(PathDouble<Vector3d> path, int nPoints, long nFrames) {
            this.path = path;
            this.nPoints = nPoints;
            this.nChunks = nPoints - 1;
            this.nFrames = nFrames;
        }

    }
}
