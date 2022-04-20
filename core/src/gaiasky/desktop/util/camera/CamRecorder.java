/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util.camera;

import gaiasky.desktop.util.SysUtils;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;
import gaiasky.util.time.ITimeFrameProvider;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

/**
 * Contains the logic to record the camera state at each frame. The format is as
 * follows: > time[s](float) cam_pos(double x3) cam_dir(double x3) cam_up(double
 * x3)
 * <p>
 * The time is the time in seconds since the start of the recording, to
 * synchronize with the current FPS in playing mode.
 *
 
 */
public class CamRecorder implements IObserver {
    private static final Log logger = Logger.getLogger(CamRecorder.class);
    /**
     * Singleton
     **/
    public static CamRecorder instance;
    private static final String sep = " ";


    Vector3d dir, upp, aux1, aux2;

    public enum RecorderState {
        // Recording in classical mode (one state per frame)
        RECORDING,
        // Playing classical recording
        PLAYING,
        // Idle
        IDLE
    }


    private RecorderState mode;
    private BufferedWriter os;
    private BufferedReader is;
    private Path f;
    private final DateFormat df;

    private long startMs;
    float time;

    public static void initialize() {
        // Initialize own
        instance = new CamRecorder();
        // Initialize keyframe manager
        CameraKeyframeManager.initialize();
    }

    public CamRecorder() {
        this.mode = RecorderState.IDLE;

        df = new SimpleDateFormat("yyyyMMdd_HH-mm-ss-SSS");

        dir = new Vector3d();
        upp = new Vector3d();
        aux1 = new Vector3d();
        aux2 = new Vector3d();

        EventManager.instance.subscribe(this, Event.RECORD_CAMERA_CMD, Event.PLAY_CAMERA_CMD, Event.UPDATE_CAM_RECORDER, Event.STOP_CAMERA_PLAY);
    }

    public void update(ITimeFrameProvider time, Vector3b position, Vector3d direction, Vector3d up) {
        switch (mode) {
            case RECORDING:
                if (os != null) {
                    try {
                        os.append(Long.toString(time.getTime().toEpochMilli())).append(sep);
                        os.append(Double.toString(position.x.doubleValue())).append(sep).append(Double.toString(position.y.doubleValue())).append(sep).append(Double.toString(position.z.doubleValue()));
                        os.append(sep).append(Double.toString(direction.x)).append(sep).append(Double.toString(direction.y)).append(sep).append(Double.toString(direction.z));
                        os.append(sep).append(Double.toString(up.x)).append(sep).append(Double.toString(up.y)).append(sep).append(Double.toString(up.z));
                        os.append("\n");
                    } catch (IOException e) {
                        logger.error(e);
                    }
                }
                break;
            case PLAYING:
                if (is != null) {
                    try {
                        while(true) {
                            String line = is.readLine();
                            if (line != null) {
                                line = line.strip();
                                if(!line.startsWith("#")) {
                                    String[] tokens = line.split("\\s+");
                                    EventManager.publish(Event.TIME_CHANGE_CMD, this, Instant.ofEpochMilli(Parser.parseLong(tokens[0])));

                                    dir.set(Parser.parseDouble(tokens[4]), Parser.parseDouble(tokens[5]), Parser.parseDouble(tokens[6]));
                                    upp.set(Parser.parseDouble(tokens[7]), Parser.parseDouble(tokens[8]), Parser.parseDouble(tokens[9]));

                                    position.set(Parser.parseDouble(tokens[1]), Parser.parseDouble(tokens[2]), Parser.parseDouble(tokens[3]));
                                    direction.set(dir);
                                    up.set(upp);
                                    break;
                                }else{
                                    // Skip comment, next line
                                }
                            } else {
                                // Finish off
                                is.close();
                                is = null;
                                mode = RecorderState.IDLE;
                                // Stop camera
                                EventManager.publish(Event.CAMERA_STOP, this);
                                // Post notification
                                logger.info(I18n.msg("notif.cameraplay.done"));

                                // Issue message informing playing has stopped
                                EventManager.publish(Event.CAMERA_PLAY_INFO, this, false);

                                // Stop frame output if it is on!
                                EventManager.publish(Event.FRAME_OUTPUT_CMD, this, false);
                                break;
                            }
                        }
                    } catch (IOException e) {
                        logger.error(e);
                    }
                }
                break;
            case IDLE:
                break;
        }

    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
            case RECORD_CAMERA_CMD:
                // Start recording
                RecorderState m;
                if (data[0] != null) {
                    if ((Boolean) data[0]) {
                        m = RecorderState.RECORDING;

                    } else {
                        m = RecorderState.IDLE;
                    }
                } else {
                    m = (mode == RecorderState.RECORDING) ? RecorderState.IDLE : RecorderState.RECORDING;

                }

                if (m == RecorderState.RECORDING) {
                    String filename;
                    if(data.length > 1 && data[1] != null && !((String)data[1]).isBlank()){
                        filename = (String) data[1];
                    } else {
                       filename = df.format(new Date());
                    }
                    // We start recording, prepare buffer!
                    if (mode == RecorderState.RECORDING) {
                        logger.info(I18n.msg("error.camerarecord.already"));
                        return;
                    }
                    // Annotate by date
                    f = SysUtils.getDefaultCameraDir().resolve(filename + ".gsc");
                    if (Files.exists(f)) {
                        try {
                            Files.delete(f);
                        } catch (IOException e) {
                            logger.error(e);
                        }
                    }
                    try {
                        Files.createFile(f);
                        os = new BufferedWriter(new FileWriter(f.toFile()));
                        // Print header
                        os.append("#time_ms").append(sep).append("pos_x").append(sep).append("pos_y").append(sep).append("pos_z").append(sep);
                        os.append("dir_x").append(sep).append("dir_y").append(sep).append("dir_z").append(sep);
                        os.append("up_x").append(sep).append("up_y").append(sep).append("up_z").append(sep);
                        os.append("\n");
                    } catch (IOException e) {
                        logger.error(e);
                        return;
                    }
                    logger.info(I18n.msg("notif.camerarecord.start"));
                    startMs = System.currentTimeMillis();
                    time = 0;
                    mode = RecorderState.RECORDING;

                } else if (m == RecorderState.IDLE) {
                    // Flush and close
                    if (mode == RecorderState.IDLE) {
                        // No recording to cancel
                        return;
                    }
                    try {
                        os.close();
                    } catch (IOException e) {
                        logger.error(e);
                    }
                    os = null;
                    long elapsed = System.currentTimeMillis() - startMs;
                    startMs = 0;
                    float secs = elapsed / 1000f;
                    logger.info(I18n.msg("notif.camerarecord.done", f.toAbsolutePath(), secs));
                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.camerarecord.done", f.toAbsolutePath(), secs));
                    f = null;
                    mode = RecorderState.IDLE;
                }
                break;
            case PLAY_CAMERA_CMD:
                // Start playing
                if (is != null) {
                    logger.warn("Hey, we are already playing another movie!");
                }
                if (mode != RecorderState.IDLE) {
                    throw new RuntimeException("The recorder is busy! The current mode is " + mode);
                }
                Object f = data[0];
                Path file;
                if (f instanceof String) {
                    file = Paths.get((String) f);
                } else {
                    file = (Path) f;
                }

                try {
                    is = new BufferedReader(new InputStreamReader(Files.newInputStream(file)));

                    logger.info(I18n.msg("notif.cameraplay.start", file));
                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.cameraplay.start", file));
                    mode = RecorderState.PLAYING;

                    // Issue message informing playing has started
                    EventManager.publish(Event.CAMERA_PLAY_INFO, this, true);

                    // Enable frame output if option is on
                    if (Settings.settings.camrecorder.auto) {
                        // Stop frame output if it is on!
                        EventManager.publish(Event.FRAME_OUTPUT_CMD, this, true);
                    }
                }catch(Exception e){
                    logger.error(e);
                }

                break;
            case UPDATE_CAM_RECORDER:
                // Update with current position
                ITimeFrameProvider dt = (ITimeFrameProvider) data[0];
                Vector3b pos = (Vector3b) data[1];
                Vector3d dir = (Vector3d) data[2];
                Vector3d up = (Vector3d) data[3];
                update(dt, pos, dir, up);
                break;
            case STOP_CAMERA_PLAY:
                // Stop playing
                mode = RecorderState.IDLE;
                // Stop camera
                EventManager.publish(Event.CAMERA_STOP, this);
                // Post notification
                logger.info(I18n.msg("notif.cameraplay.done"));
                EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.cameraplay.done"));

                // Issue message informing playing has stopped
                EventManager.publish(Event.CAMERA_PLAY_INFO, this, false);

                // Stop frame output if it is on!
                EventManager.publish(Event.FRAME_OUTPUT_CMD, this, false);
                break;
            default:
                break;
        }

    }

}
