package gaia.cu9.ari.gaiaorbit.desktop.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import com.badlogic.gdx.files.FileHandle;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.parse.Parser;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

/**
 * Contains the logic to record the camera state at each frame. The format is as
 * follows: > time[s](float) cam_pos(double x3) cam_dir(double x3) cam_up(double
 * x3)
 * 
 * The time is the time in seconds since the start of the recording, to
 * synchronize with the current FPS in playing mode.
 * 
 * @author Toni Sagrista
 *
 */
public class CamRecorder implements IObserver {
    private static final Log logger = Logger.getLogger(CamRecorder.class);
    /** Singleton **/
    public static CamRecorder instance;
    private static final String sep = " ";

    Vector3d dir, upp, aux1, aux2;

    public enum RecorderMode {
        RECORDING, PLAYING, IDLE
    }

    private RecorderMode mode;
    private BufferedWriter os;
    private BufferedReader is;
    private File f;
    private DateFormat df;

    private long startMs;
    float time;

    public static void initialize() {
        instance = new CamRecorder();
    }

    public CamRecorder() {
        this.mode = RecorderMode.IDLE;
        EventManager.instance.subscribe(this, Events.RECORD_CAMERA_CMD, Events.PLAY_CAMERA_CMD, Events.UPDATE_CAM_RECORDER, Events.STOP_CAMERA_PLAY);

        df = new SimpleDateFormat("yyyyMMdd_HH-mm-ss-SSS");

        dir = new Vector3d();
        upp = new Vector3d();
        aux1 = new Vector3d();
        aux2 = new Vector3d();
    }

    public void update(ITimeFrameProvider time, Vector3d position, Vector3d direction, Vector3d up) {
        switch (mode) {
        case RECORDING:
            if (os != null) {
                try {
                    os.append(Long.toString(time.getTime().toEpochMilli())).append(sep);
                    os.append(Double.toString(position.x)).append(sep).append(Double.toString(position.y)).append(sep).append(Double.toString(position.z));
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
                    String line;
                    if ((line = is.readLine()) != null) {
                        String[] tokens = line.split("\\s+");
                        EventManager.instance.post(Events.TIME_CHANGE_CMD, Instant.ofEpochMilli(Parser.parseLong(tokens[0])));

                        dir.set(Parser.parseDouble(tokens[4]), Parser.parseDouble(tokens[5]), Parser.parseDouble(tokens[6]));
                        upp.set(Parser.parseDouble(tokens[7]), Parser.parseDouble(tokens[8]), Parser.parseDouble(tokens[9]));

                        position.set(Parser.parseDouble(tokens[1]), Parser.parseDouble(tokens[2]), Parser.parseDouble(tokens[3]));
                        // In VR mode, direction and up must not be modified
                        //direction.set(dir);
                        //up.set(upp);

                    } else {
                        // Finish off
                        is.close();
                        is = null;
                        mode = RecorderMode.IDLE;
                        // Stop camera
                        EventManager.instance.post(Events.CAMERA_STOP);
                        // Post notification
                        logger.info(I18n.bundle.get("notif.cameraplay.done"));

                        // Issue message informing playing has stopped
                        EventManager.instance.post(Events.CAMERA_PLAY_INFO, false);

                        // Stop frame output if it is on!
                        EventManager.instance.post(Events.FRAME_OUTPUT_CMD, false);
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
    public void notify(Events event, Object... data) {
        switch (event) {
        case RECORD_CAMERA_CMD:
            RecorderMode m = null;
            if (data[0] != null) {
                if ((Boolean) data[0]) {
                    m = RecorderMode.RECORDING;

                } else {
                    m = RecorderMode.IDLE;
                }
            } else {
                m = (mode == RecorderMode.RECORDING) ? RecorderMode.IDLE : RecorderMode.RECORDING;

            }

            if (m == RecorderMode.RECORDING) {
                // We start recording, prepare buffer!
                if (mode == RecorderMode.RECORDING) {
                    logger.info(I18n.bundle.get("error.camerarecord.already"));
                    return;
                }
                // Annotate by date
                f = new File(SysUtils.getDefaultCameraDir(), df.format(new Date()) + "_gscamera.dat");
                if (f.exists()) {
                    f.delete();
                }
                try {
                    f.createNewFile();
                    os = new BufferedWriter(new FileWriter(f));
                } catch (IOException e) {
                    logger.error(e);
                    return;
                }
                logger.info(I18n.bundle.get("notif.camerarecord.start"));
                startMs = System.currentTimeMillis();
                time = 0;
                mode = RecorderMode.RECORDING;

            } else if (m == RecorderMode.IDLE) {
                // Flush and close
                if (mode == RecorderMode.IDLE) {
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
                logger.info(I18n.bundle.format("notif.camerarecord.done", f.getAbsolutePath(), secs));
                f = null;
                mode = RecorderMode.IDLE;
            }
            break;
        case PLAY_CAMERA_CMD:
            if (is != null) {
                logger.warn("Hey, we are already playing another movie!");
            }
            if (mode != RecorderMode.IDLE) {
                throw new RuntimeException("The recorder is busy! The current mode is " + mode);
            }
            FileHandle file = (FileHandle) data[0];

            is = new BufferedReader(new InputStreamReader(file.read()));

            logger.info(I18n.bundle.format("notif.cameraplay.start", file.path()));
            mode = RecorderMode.PLAYING;

            // Issue message informing playing has started
            EventManager.instance.post(Events.CAMERA_PLAY_INFO, true);

            // Enable frame output if option is on
            if (GlobalConf.frame.AUTO_FRAME_OUTPUT_CAMERA_PLAY) {
                // Stop frame output if it is on!
                EventManager.instance.post(Events.FRAME_OUTPUT_CMD, true);
            }

            break;
        case UPDATE_CAM_RECORDER:
            ITimeFrameProvider dt = (ITimeFrameProvider) data[0];
            Vector3d pos = (Vector3d) data[1];
            Vector3d dir = (Vector3d) data[2];
            Vector3d up = (Vector3d) data[3];
            update(dt, pos, dir, up);
            break;
        case STOP_CAMERA_PLAY:
            mode = RecorderMode.IDLE;
            // Stop camera
            EventManager.instance.post(Events.CAMERA_STOP);
            // Post notification
            logger.info(I18n.bundle.get("notif.cameraplay.done"));

            // Issue message informing playing has stopped
            EventManager.instance.post(Events.CAMERA_PLAY_INFO, false);

            // Stop frame output if it is on!
            EventManager.instance.post(Events.FRAME_OUTPUT_CMD, false);
            break;
        default:
            break;
        }

    }

}
