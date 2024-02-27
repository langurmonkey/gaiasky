/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.camera.rec;

import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.camera.rec.Camcorder.RecorderState;
import gaiasky.util.math.*;
import gaiasky.util.parse.Parser;
import gaiasky.util.time.ITimeFrameProvider;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class KeyframesManager implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(KeyframesManager.class);
    /**
     * Separator for keyframes files
     **/
    private static final String keyframeSeparator = ",";
    /**
     * Singleton
     **/
    public static KeyframesManager instance;

    /**
     * Current keyframes
     **/
    public final List<Keyframe> keyframes;
    /**
     * Reference to current camera position.
     */
    public Vector3b pos;
    /**
     * Reference to current camera orientation.
     */
    public Vector3d dir, up;
    /**
     * Reference to current time.
     */
    public ITimeFrameProvider t;
    /**
     * The current camera path, used for direct playback.
     */
    public CameraPath currentPath;
    /**
     * Play state.
     */
    public final AtomicReference<RecorderState> state = new AtomicReference<>(RecorderState.IDLE);

    public KeyframesManager() {
        super();

        this.keyframes = Collections.synchronizedList(new ArrayList<>());

        EventManager.instance.subscribe(this, Event.KEYFRAMES_FILE_SAVE, Event.KEYFRAMES_EXPORT, Event.UPDATE_CAM_RECORDER, Event.KEYFRAME_PLAY_FRAME);
    }

    public static void initialize() {
        instance = new KeyframesManager();
    }

    /**
     * Re-generates the camera path object from the current keyframes list.
     */
    public void regenerateCameraPath() {
        synchronized (keyframes) {
            currentPath = new CameraPath(keyframes, positionsToPathParts(keyframes, Settings.settings.camrecorder.keyframe.position));
        }
    }

    /**
     * Cleans the current configuration of keyframes.
     */
    public void clean() {
        keyframes.clear();
    }

    /**
     * Gets the frame number of the given keyframe using the current target frame rate setting.
     *
     * @param kf The keyframe.
     * @return The frame number corresponding to exactly this keyframe if the keyframe is valid and in the keyframes
     * list, otherwise -1.
     * The frame number is in [0,n-1].
     */
    public long getFrameNumber(Keyframe kf) {
        if (kf == null || keyframes.isEmpty() || !keyframes.contains(kf)) {
            return -1;
        }

        // We start at the first frame.
        double t = 0;
        for (int i = 0; ; i++) {
            if (keyframes.get(i).equals(kf))
                break;
            t += keyframes.get(i).seconds;
        }
        t += kf.seconds;
        return (long) (t * Settings.settings.camrecorder.targetFps);
    }

    private PathDouble<Vector3d> getPath(Vector3d[] data,
                                         PathType pathType) {
        if (pathType == PathType.LINEAR) {
            return new LinearDouble<>(data);
        } else if (pathType == PathType.CATMULL_ROM_SPLINE) {
            // Needs extra points at beginning and end.
            Vector3d[] extData = new Vector3d[data.length + 2];
            System.arraycopy(data, 0, extData, 1, data.length);
            extData[0] = data[0];
            extData[data.length + 1] = data[data.length - 1];
            return new CatmullRomSplineDouble<>(extData, false);
        } else if (pathType == PathType.B_SPLINE) {
            // Needs extra points at beginning and end.
            Vector3d[] extData = new Vector3d[data.length + 2];
            System.arraycopy(data, 0, extData, 1, data.length);
            extData[0] = data[0];
            extData[data.length + 1] = data[data.length - 1];
            return new BSplineDouble<>(extData, false);
        }
        // Default
        return new LinearDouble<>(data);
    }

    public List<Keyframe> loadKeyframesFile(Path file) throws RuntimeException {
        try (BufferedReader br = new BufferedReader(new FileReader(file.toFile()))) {
            List<Keyframe> result = Collections.synchronizedList(new ArrayList<>());
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(keyframeSeparator);
                if (tokens.length == 13) {
                    // Keyframe has no target.
                    double secs = Parser.parseDouble(tokens[0]);
                    long time = Parser.parseLong(tokens[1]);
                    Vector3d pos = new Vector3d(Parser.parseDouble(tokens[2]), Parser.parseDouble(tokens[3]), Parser.parseDouble(tokens[4]));
                    Vector3d dir = new Vector3d(Parser.parseDouble(tokens[5]), Parser.parseDouble(tokens[6]), Parser.parseDouble(tokens[7]));
                    Vector3d up = new Vector3d(Parser.parseDouble(tokens[8]), Parser.parseDouble(tokens[9]), Parser.parseDouble(tokens[10]));
                    boolean seam = Parser.parseInt(tokens[11]) == 1;
                    String name = tokens[12];
                    Keyframe kf = new Keyframe(name, pos, dir, up, time, secs, seam);
                    result.add(kf);
                } else if (tokens.length == 16) {
                    // Keyframe has target.
                    double secs = Parser.parseDouble(tokens[0]);
                    long time = Parser.parseLong(tokens[1]);
                    Vector3d pos = new Vector3d(Parser.parseDouble(tokens[2]), Parser.parseDouble(tokens[3]), Parser.parseDouble(tokens[4]));
                    Vector3d dir = new Vector3d(Parser.parseDouble(tokens[5]), Parser.parseDouble(tokens[6]), Parser.parseDouble(tokens[7]));
                    Vector3d up = new Vector3d(Parser.parseDouble(tokens[8]), Parser.parseDouble(tokens[9]), Parser.parseDouble(tokens[10]));
                    Vector3d target = new Vector3d(Parser.parseDouble(tokens[11]), Parser.parseDouble(tokens[12]), Parser.parseDouble(tokens[13]));
                    boolean seam = Parser.parseInt(tokens[14]) == 1;
                    String name = tokens[15];
                    Keyframe kf = new Keyframe(name, pos, dir, up, target, time, secs, seam);
                    result.add(kf);

                }
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveKeyframesFile(List<Keyframe> keyframes,
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
                if (kf.target != null) {
                    os.append(Double.toString(kf.target.x)).append(keyframeSeparator).append(Double.toString(kf.target.y)).append(keyframeSeparator).append(
                            Double.toString(kf.target.z)).append(keyframeSeparator);
                }
                os.append(Integer.toString(kf.seam ? 1 : 0)).append(keyframeSeparator);
                os.append(kf.name).append("\n");
            }

        } catch (IOException e) {
            logger.error(e);
            return;
        }
        logger.info(keyframes.size() + " keyframes saved to file " + f);

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

    public void exportKeyframesFile(List<Keyframe> keyframes,
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
        logger.info(keyframes.size() + " keyframes (" + cameraPath.n + " frames, " + frameRate + " FPS) exported to camera file " + f);
    }

    /**
     * Convert an array of keyframes to a list of path parts by separating the path at seams.
     *
     * @param keyframes The array of keyframes.
     * @param pathType  The path type.
     * @return Array of path parts.
     */
    private PathPart[] positionsToPathParts(List<Keyframe> keyframes,
                                            PathType pathType) {
        double frameRate = Settings.settings.camrecorder.targetFps;
        Array<Array<Vector3d>> positionsSep = new Array<>();
        Array<Vector3d> current = new Array<>();
        Array<Double> times = new Array<>();
        int i = 0;
        double secs = 0;
        for (Keyframe kf : keyframes) {

            // Fill positions
            if (kf.seam && pathType == PathType.CATMULL_ROM_SPLINE) {
                if (i > 0 && i < keyframes.size() - 1) {
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

    /**
     * Sets the manager in play mode.
     */
    public void play() {
        state.set(RecorderState.PLAYING);
    }

    public boolean isPlaying() {
        return state.get() == RecorderState.PLAYING;
    }

    /**
     * Sets the manager in pause mode.
     */
    public void pause() {
        state.set(RecorderState.IDLE);
    }

    public boolean isIdle() {
        return state.get() == RecorderState.IDLE;
    }

    /**
     * Sets the manager in stepping mode.
     */
    public void stepping() {
        state.set(RecorderState.STEPPING);
    }

    public boolean isStepping() {
        return state.get() == RecorderState.STEPPING;
    }

    /**
     * Skips to the given frame.
     *
     * @param frame The frame number.
     */
    public void skip(long frame) {
        if (frame < currentPath.n) {
            // Set frame as current.
            currentPath.i = frame;
            // Set mode to stepping.
            stepping();
        }
    }

    /**
     * Sets the current frame in the current path to the camera position, direction and up. It also sets the time.
     */
    private void setFrame() {
        // Set free mode if necessary.
        if (!GaiaSky.instance.getCameraManager().getMode().isFree()) {
            EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);
        }
        // Stop camera.
        EventManager.publish(Event.CAMERA_STOP, this);

        // Set time.
        EventManager.publish(Event.TIME_CHANGE_CMD, this, Instant.ofEpochMilli(currentPath.times.get((int) currentPath.i)));

        // Set position, direction, up.
        int ip = (int) currentPath.i * 9;
        pos.set(currentPath.data.get(ip), currentPath.data.get(ip + 1), currentPath.data.get(ip + 2));
        dir.set(currentPath.data.get(ip + 3), currentPath.data.get(ip + 4), currentPath.data.get(ip + 5));
        up.set(currentPath.data.get(ip + 6), currentPath.data.get(ip + 7), currentPath.data.get(ip + 8));
    }

    /**
     * Checks that all keyframe timings fall perfectly on a frame, so that
     * t(kf) * FPS % 1 = 0 holds.
     *
     * @return True if the keyframe timings are consistent with the camcorder FPS setting.
     */
    public boolean checkKeyframeTimings() {
        if (!keyframes.isEmpty()) {
            double fPS = Settings.settings.camrecorder.targetFps;
            double sPF = 1.0 / fPS;
            long msPF = (long) (sPF * 1000L);

            for (Keyframe kf : keyframes) {
                double frames = kf.seconds * fPS;
                if (frames % 1.0 != 0.0) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void notify(final Event event,
                       Object source,
                       final Object... data) {

        switch (event) {
            case KEYFRAMES_FILE_SAVE -> {
                List<Keyframe> keyframes = (List<Keyframe>) data[0];
                String fileName = (String) data[1];
                saveKeyframesFile(keyframes, fileName);
            }
            case KEYFRAMES_EXPORT -> {
                var keyframes = (List<Keyframe>) data[0];
                var fileName = (String) data[1];
                exportKeyframesFile(keyframes, fileName);
            }
            case UPDATE_CAM_RECORDER -> {
                synchronized (this) {
                    t = (ITimeFrameProvider) data[0];
                    pos = (Vector3b) data[1];
                    dir = (Vector3d) data[2];
                    up = (Vector3d) data[3];
                }
                if (state.get() == RecorderState.PLAYING && currentPath != null) {
                    // In playing mode, we set the frame and then advance to the next.

                    // Set frame.
                    setFrame();

                    // Advance step.
                    currentPath.i = (currentPath.i + 1) % currentPath.n;

                    // Check end.
                    if (currentPath.i == 0) {
                        pause();
                    }

                    EventManager.publish(Event.KEYFRAME_PLAY_FRAME, this, currentPath.i);
                } else if (state.get() == RecorderState.STEPPING && currentPath != null) {
                    // In stepping mode, we set the frame and pause.

                    // Set frame.
                    setFrame();

                    // Pause.
                    pause();

                    EventManager.publish(Event.KEYFRAME_PLAY_FRAME, this, currentPath.i);
                }
            }
            case KEYFRAME_PLAY_FRAME -> {
                if (source != this && currentPath != null) {
                    // Actually play frame.
                    long frame = (Long) data[0];
                    skip(frame);
                }
            }
            default -> {
            }
        }
    }

    public enum PathType {
        LINEAR,
        CATMULL_ROM_SPLINE,
        B_SPLINE
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
