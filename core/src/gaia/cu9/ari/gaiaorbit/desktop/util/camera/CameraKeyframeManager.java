package gaia.cu9.ari.gaiaorbit.desktop.util.camera;

import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.desktop.util.SysUtils;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.math.Lineard;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.parse.Parser;

import java.io.*;

public class CameraKeyframeManager implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(CameraKeyframeManager.class);
    /** Singleton **/
    public static CameraKeyframeManager instance;

    /** Separator for keyframes files **/
    private static final String ksep = ",";

    /** Separator for camera files **/
    private static final String sep = " ";

    public static void initialize() {
        instance = new CameraKeyframeManager();
    }

    public CameraKeyframeManager() {
        super();

        EventManager.instance.subscribe(this, Events.KEYFRAMES_FILE_SAVE, Events.KEYFRAMES_EXPORT);
    }

    public Array<Keyframe> loadKeyframesFile(File file) throws RuntimeException {
        Array<Keyframe> result = new Array<Keyframe>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(ksep);
                long frame = Parser.parseLong(tokens[0]);
                long time = Parser.parseLong(tokens[1]);
                Vector3d pos = new Vector3d(Parser.parseDouble(tokens[2]), Parser.parseDouble(tokens[3]), Parser.parseDouble(tokens[4]));
                Vector3d dir = new Vector3d(Parser.parseDouble(tokens[5]), Parser.parseDouble(tokens[6]), Parser.parseDouble(tokens[7]));
                Vector3d up = new Vector3d(Parser.parseDouble(tokens[8]), Parser.parseDouble(tokens[9]), Parser.parseDouble(tokens[10]));
                String name = tokens[11];
                Keyframe kf = new Keyframe(name, pos, dir, up, time, frame);
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
        File f = new File(SysUtils.getDefaultCameraDir(), fileName);
        if (f.exists()) {
            f.delete();
        }
        BufferedWriter os = null;
        if (f.exists()) {
            f.delete();
        }
        try {
            f.createNewFile();
            os = new BufferedWriter(new FileWriter(f));

            for (Keyframe kf : keyframes) {
                os.append(Long.toString(kf.frame)).append(ksep).append(Long.toString(kf.time)).append(ksep);
                os.append(Double.toString(kf.pos.x)).append(ksep).append(Double.toString(kf.pos.y)).append(ksep).append(Double.toString(kf.pos.z)).append(ksep);
                os.append(Double.toString(kf.dir.x)).append(ksep).append(Double.toString(kf.dir.y)).append(ksep).append(Double.toString(kf.dir.z)).append(ksep);
                os.append(Double.toString(kf.up.x)).append(ksep).append(Double.toString(kf.up.y)).append(ksep).append(Double.toString(kf.up.z)).append(ksep);
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
        logger.info(keyframes.size + " keyframes saved to file " + f.getName());

    }

    public void exportKeyframesFile(Array<Keyframe> keyframes, String fileName) {
        File f = new File(SysUtils.getDefaultCameraDir(), fileName);
        if (f.exists()) {
            f.delete();
        }
        BufferedWriter os = null;
        if (f.exists()) {
            f.delete();
        }

        /** Frame counter **/
        long frames = 0;

        try {
            f.createNewFile();
            os = new BufferedWriter(new FileWriter(f));

            Vector3d[] positions = new Vector3d[keyframes.size ];
            Vector3d[] directions = new Vector3d[keyframes.size];
            Vector3d[] ups = new Vector3d[keyframes.size];

            // Fill in vectors, with first and last elements duplicated, as Catmull-Rom needs these
            for (int i = 0; i < keyframes.size; i++) {
                Keyframe k = keyframes.get(i);
                positions[i] = k.pos;
                directions[i] = k.dir;
                ups[i] = k.up;
            }


            // Catmull-Rom splines for pos, dir, up
            Lineard<Vector3d> posSpline = new Lineard<Vector3d>(positions);
            Lineard<Vector3d> dirSpline = new Lineard<Vector3d>(directions);
            Lineard<Vector3d> upSpline = new Lineard<Vector3d>(ups);

            Vector3d aux = new Vector3d();

            /** Current position in the spline. Coincides with the control points **/
            double splinePos = 0d;
            /** Step length between control points **/
            double splinePosStep = 1d / (positions.length - 1);


            for (int i = 1; i < keyframes.size; i++) {
                Keyframe k0 = keyframes.get(i - 1);
                Keyframe k1 = keyframes.get(i);

                long nFrames = k1.frame - k0.frame;
                double splinePosSubstep = splinePosStep / nFrames;

                long dt = k1.time - k0.time;
                long tStep = dt / nFrames;

                for (long fr = 0; fr < nFrames; fr++) {
                    double posIdx = splinePos + splinePosSubstep * fr;

                    // TIME
                    os.append(Long.toString(k0.time + tStep * fr)).append(sep);

                    // POS
                    posSpline.valueAt(aux, posIdx);
                    os.append(Double.toString(aux.x)).append(sep).append(Double.toString(aux.y)).append(sep).append(Double.toString(aux.z)).append(sep);

                    // DIR
                    dirSpline.valueAt(aux, posIdx);
                    aux.nor();
                    os.append(Double.toString(aux.x)).append(sep).append(Double.toString(aux.y)).append(sep).append(Double.toString(aux.z)).append(sep);

                    // UP
                    upSpline.valueAt(aux, posIdx);
                    aux.nor();
                    os.append(Double.toString(aux.x)).append(sep).append(Double.toString(aux.y)).append(sep).append(Double.toString(aux.z));

                    // New line
                    os.append("\n");

                    frames++;
                }

                splinePos += splinePosStep;
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
        logger.info(keyframes.size + " keyframes (" + frames + " frames) exported to camera file " + f.getName());
    }

    @Override
    public void notify(Events event, Object... data) {

        switch (event) {
        case KEYFRAMES_FILE_SAVE:
            Array<Keyframe> keyframes = (Array<Keyframe>) data[0];
            String fileName = (String) data[1];
            saveKeyframesFile(keyframes, fileName);
            break;
        case KEYFRAMES_EXPORT:
            keyframes = (Array<Keyframe>) data[0];
            fileName = (String) data[1];
            exportKeyframesFile(keyframes, fileName);
            break;
        default:
            break;
        }
    }
}
