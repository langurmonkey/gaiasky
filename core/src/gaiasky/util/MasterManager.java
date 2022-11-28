/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.net.HttpStatus;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.util.Logger.Log;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.util.List;
import java.util.function.Function;

/**
 * Manages a master instance which makes available state information to others
 * in order to synchronize a session.
 */
public class MasterManager implements IObserver {
    private static final Log logger = Logger.getLogger(MasterManager.class);

    /** Will attempt reconnection to offline slaves with this frequency **/
    private static final long RECONNECT_TIME_MS = 5000;

    // Singleton instance
    public static MasterManager instance;

    public static void initialize() {
        if (Settings.settings.program.net.master.active)
            MasterManager.instance = new MasterManager();
    }

    public static boolean hasSlaves() {
        return instance != null && instance.slaves != null && !instance.slaves.isEmpty();
    }

    // Slave list
    private final List<String> slaves;

    public boolean isSlaveConnected(String slaveName) {
        return isSlaveConnected(getSlaveIndex(slaveName));

    }

    public int getSlaveIndex(String slaveName) {
        return slaves.indexOf(slaveName);
    }

    public boolean isSlaveConnected(int index) {
        if (index >= 0 && index < slaves.size()) {
            return slaveStates[index] == 0;
        }
        return false;
    }

    public byte[] getSlaveStates() {
        return slaveStates;
    }

    /**
     * Vector with slave states
     * <ul>
     * <li>0 - ok</li>
     * <li>-1 - error</li>
     * <li>1 - retrying</li>
     * </ul>
     */
    private byte[] slaveStates;
    /** Slave connection attempt flags **/
    private byte[] slaveFlags;
    /** Last ping times for each slave **/
    private long[] slavePingTimes;

    // Handlers
    private ResponseHandler[] responseHandlers;
    private ExceptHandler[] exceptHandlers;

    // HTTP client
    private final HttpClient http;

    private MasterManager() {
        super();

        // Slave objects
        slaves = Settings.settings.program.net.master.slaves;
        if (slaves != null && slaves.size() > 0) {
            slaveStates = new byte[slaves.size()];
            slaveFlags = new byte[slaves.size()];
            slavePingTimes = new long[slaves.size()];
            for (int i = 0; i < slaveStates.length; i++) {
                slaveStates[i] = 0;
                slaveFlags[i] = 0;
                slavePingTimes[i] = 0l;
            }
        }

        if (slaves != null && slaves.size() > 0) {
            responseHandlers = new ResponseHandler[slaves.size()];
            exceptHandlers = new ExceptHandler[slaves.size()];
            for (int i = 0; i < slaveStates.length; i++) {
                responseHandlers[i] = new ResponseHandler(i);
                exceptHandlers[i] = new ExceptHandler(i);
            }
        }

        // Initialize http client
        http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();

        // Subscribe to events that need to be broadcast
        EventManager.instance.subscribe(this, Event.FOV_CHANGED_CMD, Event.TOGGLE_VISIBILITY_CMD, Event.STAR_BRIGHTNESS_CMD, Event.STAR_BASE_LEVEL_CMD, Event.STAR_POINT_SIZE_CMD, Event.DISPOSE);
    }

    /**
     * Broadcasts the given camera state and time to all the slaves.
     *
     * @param pos  Camera position.
     * @param dir  Camera direction.
     * @param up   Camera up.
     * @param time Current time.
     */
    public void boardcastCameraAndTime(Vector3b pos, Vector3d dir, Vector3d up, ITimeFrameProvider time) {
        String spos = TextUtils.surround(pos.toString(), "[", "]");
        String sdir = TextUtils.surround(dir.toString(), "[", "]");
        String sup = TextUtils.surround(up.toString(), "[", "]");
        String stime = Long.toString(time.getTime().toEpochMilli());

        boolean slaveOffline = false;
        int i = 0;
        for (String slave : slaves) {
            if (slaveStates[i] == 0 || slaveFlags[i] == 1) {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(slave + "setCameraStateAndTime?arg0=" + spos + "&arg1=" + sdir + "&arg2=" + sup + "&arg3=" + stime)).GET().
                        build();

                try {
                    http.sendAsync(req, rhandler(i)).thenApply(HttpResponse::body).exceptionally(ehandler(i));
                }catch(Exception e){
                    logger.error(e);
                }
                if(slaveFlags[i] == 1)
                    slaveFlags[i] = 0;
            } else {
                slaveOffline = slaveStates[i] == -1;
            }
            i++;
        }

        // Retry connections after RECONNECT_TIME_MS milliseconds
        if (slaveOffline) {
            long now = System.currentTimeMillis();
            for (i = 0; i < slaveFlags.length; i++) {
                if (slaveStates[i] < 0 && now - slavePingTimes[i] > RECONNECT_TIME_MS) {
                    slaveFlags[i] = 1;
                }
            }

        }

    }

    /**
     * Broadcasts the given camera state to all the slaves
     *
     * @param pos Camera position
     * @param dir Camera direction
     * @param up  Camera up
     */
    public void boardcastCamera(Vector3d pos, Vector3d dir, Vector3d up) {
        String spos = TextUtils.surround(pos.toString(), "[", "]");
        String sdir = TextUtils.surround(dir.toString(), "[", "]");
        String sup = TextUtils.surround(up.toString(), "[", "]");

        int i = 0;
        for (String slave : slaves) {
            if (slaveStates[i] == 0) {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(slave + "setCameraState?arg0=" + spos + "&arg1=" + sdir + "&arg2=" + sup)).GET().
                        build();
                http.sendAsync(req, rhandler(i)).thenApply(HttpResponse::body).exceptionally(ehandler(i));
            }
            i++;
        }
    }

    public void setSlaveYaw(String slave, float yaw) {
        String syaw = Float.toString(yaw);
        if (isSlaveConnected(slave)) {
            int i = getSlaveIndex(slave);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(slave + "setProjectionYaw?arg0=" + syaw)).GET().
                    build();
            http.sendAsync(req, rhandler(i)).thenApply(HttpResponse::body).exceptionally(ehandler(i));
        }
    }

    public void setSlavePitch(String slave, float pitch) {
        String spitch = Float.toString(pitch);
        if (isSlaveConnected(slave)) {
            int i = getSlaveIndex(slave);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(slave + "setProjectionPitch?arg0=" + spitch)).GET().
                    build();
            http.sendAsync(req, rhandler(i)).thenApply(HttpResponse::body).exceptionally(ehandler(i));
        }
    }

    public void setSlaveRoll(String slave, float roll) {
        String sroll = Float.toString(roll);
        if (isSlaveConnected(slave)) {
            int i = getSlaveIndex(slave);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(slave + "setProjectionRoll?arg0=" + sroll)).GET().
                    build();
            http.sendAsync(req, rhandler(i)).thenApply(HttpResponse::body).exceptionally(ehandler(i));
        }
    }

    public void setSlaveFov(String slave, float fov) {
        String sfov = Float.toString(fov);
        if (isSlaveConnected(slave)) {
            int i = getSlaveIndex(slave);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(slave + "setProjectionFov?arg0=" + sfov)).GET().
                    build();
            http.sendAsync(req, rhandler(i)).thenApply(HttpResponse::body).exceptionally(ehandler(i));
        }
    }

    public List<String> getSlaves() {
        return slaves;
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        int i;
        switch (event) {
        case FOV_CHANGED_CMD:
            if (false) { // Each slave has its own fov configured via file/mpcdi
                String sfov = Float.toString((float) data[0]);
                for (String slave : slaves) {
                    if (slaveStates[i] == 0) {
                        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(slave + "setFov?arg0=" + sfov)).GET().
                                build();
                        http.sendAsync(req, rhandler(i)).thenApply(HttpResponse::body).exceptionally(ehandler(i));
                    }
                    i++;
                }
            }
            break;
        case TOGGLE_VISIBILITY_CMD:
            String key = (String) data[0];
            Boolean state;
            if (data.length == 2) {
                state = (Boolean) data[1];
            } else {
                ComponentType ct = ComponentType.getFromKey(key);
                state = Settings.settings.scene.visibility.get(ct.toString());
            }
            i = 0;
            for (String slave : slaves) {
                if (slaveStates[i] == 0) {
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(slave + "setVisibility?arg0=" + key + "&arg1=" + state.toString())).GET().
                            build();
                    http.sendAsync(req, rhandler(i)).thenApply(HttpResponse::body).exceptionally(ehandler(i));
                }
                i++;
            }
            break;
        case STAR_BRIGHTNESS_CMD:
            float brightness = MathUtilsd.lint((float) data[0], Constants.MIN_STAR_BRIGHTNESS, Constants.MAX_STAR_BRIGHTNESS, Constants.MIN_SLIDER, Constants.MAX_SLIDER);
            String sbr = Float.toString(brightness);
            i = 0;
            for (String slave : slaves) {
                if (slaveStates[i] == 0) {
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(slave + "setStarBrightness?arg0=" + sbr)).GET().
                            build();
                    http.sendAsync(req, rhandler(i)).thenApply(HttpResponse::body).exceptionally(ehandler(i));
                }
                i++;
            }
            break;
        case STAR_POINT_SIZE_CMD:
            float size = (float) data[0];
            String ssize = Float.toString(size);
            i = 0;
            for (String slave : slaves) {
                if (slaveStates[i] == 0) {
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(slave + "setStarSize?arg0=" + ssize)).GET().
                            build();
                    http.sendAsync(req, rhandler(i)).thenApply(HttpResponse::body).exceptionally(ehandler(i));
                }
                i++;
            }
            break;
        case STAR_BASE_LEVEL_CMD:
            float opacity = (float) data[0];
            String sop = Float.toString(opacity);
            i = 0;
            for (String slave : slaves) {
                if (slaveStates[i] == 0) {
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(slave + "setMinStarOpacity?arg0=" + sop)).GET().
                            build();
                    http.sendAsync(req, rhandler(i)).thenApply(HttpResponse::body).exceptionally(ehandler(i));
                }
                i++;
            }
            break;
        case DISPOSE:
            i = 0;
            for (String slave : slaves) {
                if (slaveStates[i] == 0) {
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(slave + "quit")).GET().
                            build();
                    http.sendAsync(req, rhandler(i)).thenApply(HttpResponse::body).exceptionally(ehandler(i));
                }
                i++;
            }
            break;
        default:
            break;
        }
    }



    private ResponseHandler rhandler(int index) {
        return responseHandlers[index];
    }
    private ExceptHandler ehandler(int index) {
        return exceptHandlers[index];
    }

    private class ExceptHandler implements Function<Throwable, String>{
        private final int idx;

        public ExceptHandler(int idx){
            this.idx = idx;
        }

        @Override
        public String apply(Throwable throwable) {
            //logger.error(throwable);
            logger.error("Connection failed for slave " + idx + " (" + slaves.get(idx) + ") with " + throwable.getMessage());
            markSlaveOffline(idx);
            return null;
        }
    }

    private class ResponseHandler implements BodyHandler<String> {
        private final int idx;

        public ResponseHandler(int idx) {
            this.idx = idx;
        }

        @Override
        public BodySubscriber apply(HttpResponse.ResponseInfo responseInfo) {
            if (responseInfo.statusCode() != HttpStatus.SC_OK) {
                markSlaveOffline(idx);
                logger.error("Connection failed for slave " + idx + " (" + slaves.get(idx) + ")");
            } else {
                makeSlaveOnline(idx);
            }
            return HttpResponse.BodySubscribers.discarding();
        }

    }

    private void markSlaveOffline(int index) {
        slaveEvent(index, -1);
        slaveStates[index] = -1;
        slavePingTimes[index] = System.currentTimeMillis();
    }

    private void makeSlaveOnline(int index){
        slaveEvent(index, 0);
        slaveStates[index] = 0;
    }

    private void slaveEvent(int idx, int newState){
        if(slaveStates[idx] != newState){
            EventManager.publish(Event.SLAVE_CONNECTION_EVENT, this, idx, slaves.get(idx), newState >= 0);
        }
    }
}
