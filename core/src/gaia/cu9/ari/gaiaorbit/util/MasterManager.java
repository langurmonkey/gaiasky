package gaia.cu9.ari.gaiaorbit.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.net.HttpParametersUtils;
import com.badlogic.gdx.net.HttpStatus;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

/**
 * Manages a master instance which makes available state information to others
 * in order to synchronize a session.
 * 
 * @author tsagrista
 *
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

    // Slave list
    private List<String> slaves;
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
        camStateTimeParams = new HashMap<String, String>();
        camStateParams = new HashMap<String, String>();
        params = new HashMap<String, String>();

        // Create request and response objects
        request = new HttpRequest(HttpMethods.POST);
        evtrequest = new HttpRequest(HttpMethods.POST);
        if (slaves != null && slaves.size() > 0) {
            responseListeners = new MasterResponseListener[slaves.size()];
            for (int i = 0; i < slaveStates.length; i++)
                responseListeners[i] = new MasterResponseListener(i);
        }

        // Subscribe to events that need to be broadcasted
        EventManager.instance.subscribe(this, Events.FOV_CHANGED_CMD, Events.TOGGLE_VISIBILITY_CMD, Events.STAR_BRIGHTNESS_CMD, Events.STAR_MIN_OPACITY_CMD, Events.STAR_POINT_SIZE_CMD);
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

    @Override
    public void notify(Events event, Object... data) {
        params.clear();
        switch (event) {
        case FOV_CHANGED_CMD:
            params.put("arg0", Float.toString((float) data[0]));
            String paramString = HttpParametersUtils.convertHttpParameters(params);
            int i = 0;
            for (String slave : slaves) {
                if (slaveStates[i] == 0) {
                    evtrequest.setUrl(slave + "setFov");
                    evtrequest.setContent(paramString);
                    Gdx.net.sendHttpRequest(evtrequest, responseListeners[i++]);
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
                    evtrequest.setUrl(slave + "setStarMinOpacity");
                    evtrequest.setContent(paramString);
                    Gdx.net.sendHttpRequest(evtrequest, responseListeners[i++]);
                }
            }
            break;
        default:
            break;
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
