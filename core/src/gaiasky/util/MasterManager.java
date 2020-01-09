/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.net.HttpParametersUtils;
import com.badlogic.gdx.net.HttpStatus;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.util.Logger.Log;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages a master instance which makes available state information to others
 * in order to synchronize a session.
 *
 * @author tsagrista
 */
public class MasterManager implements IObserver {
    private static final Log logger = Logger.getLogger(MasterManager.class);

    /** Will attempt reconnection to offline slaves with this frequency **/
    private static final long RECONNECT_TIME_MS = 10000;

    // Singleton instance
    public static MasterManager instance;

    public static void initialize() {
        if (GlobalConf.program.NET_MASTER)
            MasterManager.instance = new MasterManager();
    }

    public static boolean hasSlaves() {
        return instance != null && instance.slaves != null && !instance.slaves.isEmpty();
    }

    // Slave list
    private List<String> slaves;

    public boolean isSlaveConnected(String slaveName) {
        return isSlaveConnected(getSlaveIndex(slaveName));

    }

    public int getSlaveIndex(String slaveName){
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
    /** Last ping times for each slave **/
    private long[] slavePingTimes;

    // Parameters maps
    private Map<String, String> camStateTimeParams, camStateParams, params;

    // HTTP request objects
    private HttpRequest request, evtrequest;

    // Response object
    private MasterResponseListener[] responseListeners;

    private MasterManager() {
        super();

        // Slave objects
        slaves = GlobalConf.program.NET_MASTER_SLAVES;
        if (slaves != null && slaves.size() > 0) {
            slaveStates = new byte[slaves.size()];
            slavePingTimes = new long[slaves.size()];
            for (int i = 0; i < slaveStates.length; i++) {
                slaveStates[i] = 0;
                slavePingTimes[i] = 0l;
            }
        }

        // Create parameter maps
        camStateTimeParams = new HashMap<>();
        camStateParams = new HashMap<>();
        params = new HashMap<>();

        // Create request and response objects
        request = new HttpRequest(HttpMethods.POST);
        evtrequest = new HttpRequest(HttpMethods.POST);
        if (slaves != null && slaves.size() > 0) {
            responseListeners = new MasterResponseListener[slaves.size()];
            for (int i = 0; i < slaveStates.length; i++)
                responseListeners[i] = new MasterResponseListener(i);
        }

        // Subscribe to events that need to be broadcasted
        EventManager.instance.subscribe(this, Events.FOV_CHANGED_CMD, Events.TOGGLE_VISIBILITY_CMD, Events.STAR_BRIGHTNESS_CMD, Events.STAR_MIN_OPACITY_CMD, Events.STAR_POINT_SIZE_CMD, Events.DISPOSE);
    }

    /**
     * Broadcasts the given camera state and time to all the slaves
     *
     * @param pos  Camera position
     * @param dir  Camera direction
     * @param up   Camera up
     * @param time Current time
     */
    public void boardcastCameraAndTime(Vector3d pos, Vector3d dir, Vector3d up, ITimeFrameProvider time) {
        camStateTimeParams.put("arg0", Arrays.toString(pos.values()));
        camStateTimeParams.put("arg1", Arrays.toString(dir.values()));
        camStateTimeParams.put("arg2", Arrays.toString(up.values()));
        camStateTimeParams.put("arg3", Long.toString(time.getTime().toEpochMilli()));
        String paramString = HttpParametersUtils.convertHttpParameters(camStateTimeParams);

        boolean slaveOffline = false;
        int i = 0;
        for (String slave : slaves) {
            if (slaveStates[i] == 0) {
                request.setUrl(slave + "setCameraStateAndTime");
                request.setContent(paramString);
                Gdx.net.sendHttpRequest(request, responseListeners[i]);
                i++;
            } else {
                slaveOffline = true;
            }
        }

        // Retry connections after RECONNECT_TIME_MS milliseconds
        if (slaveOffline) {
            long now = System.currentTimeMillis();
            for (i = 0; i < slaveStates.length; i++) {
                if (slaveStates[i] < 0 && now - slavePingTimes[i] > RECONNECT_TIME_MS) {
                    slaveStates[i] = 0;
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
        camStateParams.put("arg0", Arrays.toString(pos.values()));
        camStateParams.put("arg1", Arrays.toString(dir.values()));
        camStateParams.put("arg2", Arrays.toString(up.values()));
        String paramString = HttpParametersUtils.convertHttpParameters(camStateParams);

        int i = 0;
        for (String slave : slaves) {
            if (slaveStates[i] == 0) {
                request.setUrl(slave + "setCameraState");
                request.setContent(paramString);
                Gdx.net.sendHttpRequest(request, responseListeners[i++]);
            }
        }
    }

    public void setSlaveYaw(String slave, float yaw){
        synchronized(params) {
            params.clear();
            params.put("arg0", Float.toString(yaw));
            String paramString = HttpParametersUtils.convertHttpParameters(params);
            if(isSlaveConnected(slave)){
                evtrequest.setUrl(slave + "setProjectionYaw");
                evtrequest.setContent(paramString);
                Gdx.net.sendHttpRequest(evtrequest, responseListeners[0]);
            }
        }
    }
    public void setSlavePitch(String slave, float pitch){
        synchronized(params) {
            params.clear();
            params.put("arg0", Float.toString(pitch));
            String paramString = HttpParametersUtils.convertHttpParameters(params);
            if(isSlaveConnected(slave)){
                evtrequest.setUrl(slave + "setProjectionPitch");
                evtrequest.setContent(paramString);
                Gdx.net.sendHttpRequest(evtrequest, responseListeners[0]);
            }
        }
    }
    public void setSlaveRoll(String slave, float roll){
        synchronized(params) {
            params.clear();
            params.put("arg0", Float.toString(roll));
            String paramString = HttpParametersUtils.convertHttpParameters(params);
            if(isSlaveConnected(slave)){
                evtrequest.setUrl(slave + "setProjectionRoll");
                evtrequest.setContent(paramString);
                Gdx.net.sendHttpRequest(evtrequest, responseListeners[0]);
            }
        }
    }
    public void setSlaveFov(String slave, float fov){
        synchronized(params) {
            params.clear();
            params.put("arg0", Float.toString(fov));
            String paramString = HttpParametersUtils.convertHttpParameters(params);
            if(isSlaveConnected(slave)){
                evtrequest.setUrl(slave + "setProjectionFov");
                evtrequest.setContent(paramString);
                Gdx.net.sendHttpRequest(evtrequest, responseListeners[0]);
            }
        }
    }

    public List<String> getSlaves() {
        return slaves;
    }

    @Override
    public void notify(Events event, Object... data) {
        synchronized(params) {
            params.clear();
            String paramString;
            int i;

            switch (event) {
            case FOV_CHANGED_CMD:
                if(false) { // Each slave has its own fov configured via file/mpcdi
                    params.put("arg0", Float.toString((float) data[0]));
                    paramString = HttpParametersUtils.convertHttpParameters(params);
                    i = 0;
                    for (String slave : slaves) {
                        if (slaveStates[i] == 0) {
                            evtrequest.setUrl(slave + "setFov");
                            evtrequest.setContent(paramString);
                            Gdx.net.sendHttpRequest(evtrequest, responseListeners[i++]);
                        }
                    }
                }
                break;
            case TOGGLE_VISIBILITY_CMD:
                String key = (String) data[0];
                Boolean state = null;
                if (data.length > 2) {
                    state = (Boolean) data[2];
                } else {
                    ComponentType ct = ComponentType.getFromKey(key);
                    state = GlobalConf.scene.VISIBILITY[ct.ordinal()];
                }

                params.put("arg0", key);
                params.put("arg1", state.toString());
                paramString = HttpParametersUtils.convertHttpParameters(params);
                i = 0;
                for (String slave : slaves) {
                    if (slaveStates[i] == 0) {
                        evtrequest.setUrl(slave + "setVisibility");
                        evtrequest.setContent(paramString);
                        Gdx.net.sendHttpRequest(evtrequest, responseListeners[i++]);
                    }
                }
                break;
            case STAR_BRIGHTNESS_CMD:
                float brightness = MathUtilsd.lint((float) data[0], Constants.MIN_STAR_BRIGHT, Constants.MAX_STAR_BRIGHT, Constants.MIN_SLIDER, Constants.MAX_SLIDER);
                params.put("arg0", Float.toString(brightness));
                paramString = HttpParametersUtils.convertHttpParameters(params);
                i = 0;
                for (String slave : slaves) {
                    if (slaveStates[i] == 0) {
                        evtrequest.setUrl(slave + "setStarBrightness");
                        evtrequest.setContent(paramString);
                        Gdx.net.sendHttpRequest(evtrequest, responseListeners[i++]);
                    }
                }
                break;
            case STAR_POINT_SIZE_CMD:
                float size = MathUtilsd.lint((float) data[0], Constants.MIN_STAR_POINT_SIZE, Constants.MAX_STAR_POINT_SIZE, Constants.MIN_SLIDER, Constants.MAX_SLIDER);
                params.put("arg0", Float.toString(size));
                paramString = HttpParametersUtils.convertHttpParameters(params);
                i = 0;
                for (String slave : slaves) {
                    if (slaveStates[i] == 0) {
                        evtrequest.setUrl(slave + "setStarSize");
                        evtrequest.setContent(paramString);
                        Gdx.net.sendHttpRequest(evtrequest, responseListeners[i++]);
                    }
                }
                break;
            case STAR_MIN_OPACITY_CMD:
                float opacity = MathUtilsd.lint((float) data[0], Constants.MIN_STAR_MIN_OPACITY, Constants.MAX_STAR_MIN_OPACITY, Constants.MIN_SLIDER, Constants.MAX_SLIDER);
                params.put("arg0", Float.toString(opacity));
                paramString = HttpParametersUtils.convertHttpParameters(params);
                i = 0;
                for (String slave : slaves) {
                    if (slaveStates[i] == 0) {
                        evtrequest.setUrl(slave + "setMinStarOpacity");
                        evtrequest.setContent(paramString);
                        Gdx.net.sendHttpRequest(evtrequest, responseListeners[i++]);
                    }
                }
                break;
            case DISPOSE:
                i = 0;
                for (String slave : slaves) {
                    if (slaveStates[i] == 0) {
                        evtrequest.setUrl(slave + "quit");
                        Gdx.net.sendHttpRequest(evtrequest, responseListeners[i++]);
                    }
                }
                break;
            default:
                break;
            }
        }
    }

    private class MasterResponseListener implements HttpResponseListener {
        private int index;

        public MasterResponseListener(int index) {
            super();
            this.index = index;
        }

        @Override
        public void handleHttpResponse(HttpResponse httpResponse) {
            if (httpResponse.getStatus().getStatusCode() == HttpStatus.SC_OK) {
            } else {
                logger.error("HTTP status not ok for slave " + index);
                markSlaveOffline(index);
            }
        }

        @Override
        public void failed(Throwable t) {
            logger.error(t);
            markSlaveOffline(index);
            logger.error("Connection failed for slave " + index + " (" + slaves.get(index) + ")");
        }

        @Override
        public void cancelled() {
            markSlaveOffline(index);
            logger.info("Cancelled request for slave " + 0);
        }
    }

    private void markSlaveOffline(int index) {
        slaveStates[index] = -1;
        slavePingTimes[index] = System.currentTimeMillis();
    }
}
