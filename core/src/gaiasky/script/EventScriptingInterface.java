/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.data.cluster.StarClusterLoader;
import gaiasky.data.group.DatasetOptions;
import gaiasky.data.group.STILDataProvider;
import gaiasky.desktop.util.SysUtils;
import gaiasky.event.EventManager;
import gaiasky.event.EventManager.TimeFrame;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interfce.ColormapPicker;
import gaiasky.interfce.IGui;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scenegraph.*;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.screenshot.ImageRenderer;
import gaiasky.util.*;
import gaiasky.util.CatalogInfo.CatalogInfoType;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColourUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.filter.attrib.AttributeUCD;
import gaiasky.util.filter.attrib.IAttribute;
import gaiasky.util.gdx.contrib.postprocess.effects.CubemapProjections;
import gaiasky.util.math.*;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.ucd.UCD;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of the scripting interface using the event system.
 *
 * @author Toni Sagrista
 */
@SuppressWarnings({"unused", "WeakerAccess", "SwitchStatementWithTooFewBranches", "SingleStatementInBlock", "SameParameterValue"})
public class EventScriptingInterface implements IScriptingInterface, IObserver {
    private static final Log logger = Logger.getLogger(EventScriptingInterface.class);

    private EventManager em;
    private AssetManager manager;
    private LruCache<String, Texture> textures;

    private static EventScriptingInterface instance = null;

    public static EventScriptingInterface instance() {
        if (instance == null) {
            instance = new EventScriptingInterface();
        }
        return instance;
    }

    private Vector3d aux3d1, aux3d2, aux3d3, aux3d4, aux3d5, aux3d6;
    private Vector2d aux2d1;

    private Set<AtomicBoolean> stops;

    /**
     * Used to wait for new frames
     */
    private final Object frameMonitor;

    /**
     * Contains the current frame number. Available right after
     * notify() is called on frameMonitor.
     */
    private long frameNumber;

    private EventScriptingInterface() {
        em = EventManager.instance;
        manager = GaiaSky.instance.manager;

        frameMonitor = new Object();

        stops = new HashSet<>();

        aux3d1 = new Vector3d();
        aux3d2 = new Vector3d();
        aux3d3 = new Vector3d();
        aux3d4 = new Vector3d();
        aux3d5 = new Vector3d();
        aux3d6 = new Vector3d();
        aux2d1 = new Vector2d();

        em.subscribe(this, Events.INPUT_EVENT, Events.DISPOSE, Events.FRAME_TICK);
    }

    private void initializeTextures() {
        if (textures == null) {
            textures = new LruCache<>(100);
        }
    }

    private double[] dArray(List l) {
        if (l == null)
            return null;
        double[] res = new double[l.size()];
        int i = 0;
        for (Object o : l) {
            res[i++] = (Double) o;
        }
        return res;
    }

    private int[] iArray(List l) {
        if (l == null)
            return null;
        int[] res = new int[l.size()];
        int i = 0;
        for (Object o : l) {
            res[i++] = (Integer) o;
        }
        return res;
    }

    @Override
    public void activateRealTimeFrame() {
        GaiaSky.postRunnable(() -> em.post(Events.EVENT_TIME_FRAME_CMD, TimeFrame.REAL_TIME));
    }

    @Override
    public void activateSimulationTimeFrame() {
        GaiaSky.postRunnable(() -> em.post(Events.EVENT_TIME_FRAME_CMD, TimeFrame.SIMULATION_TIME));
    }

    @Override
    public void setHeadlineMessage(final String headline) {
        GaiaSky.postRunnable(() -> em.post(Events.POST_HEADLINE_MESSAGE, headline));
    }

    @Override
    public void setSubheadMessage(final String subhead) {
        GaiaSky.postRunnable(() -> em.post(Events.POST_SUBHEAD_MESSAGE, subhead));
    }

    @Override
    public void clearHeadlineMessage() {
        GaiaSky.postRunnable(() -> em.post(Events.CLEAR_HEADLINE_MESSAGE));
    }

    @Override
    public void clearSubheadMessage() {
        GaiaSky.postRunnable(() -> em.post(Events.CLEAR_SUBHEAD_MESSAGE));
    }

    @Override
    public void clearAllMessages() {
        GaiaSky.postRunnable(() -> em.post(Events.CLEAR_MESSAGES));
    }

    @Override
    public void disableInput() {
        GaiaSky.postRunnable(() -> em.post(Events.INPUT_ENABLED_CMD, false));
    }

    @Override
    public void enableInput() {
        GaiaSky.postRunnable(() -> em.post(Events.INPUT_ENABLED_CMD, true));
    }

    @Override
    public void setCinematicCamera(boolean cinematic) {
        GaiaSky.postRunnable(() -> em.post(Events.CAMERA_CINEMATIC_CMD, cinematic, false));
    }

    @Override
    public void setCameraFocus(final String focusName) {
        setCameraFocus(focusName.toLowerCase(), 0.0f);
    }

    @Override
    public void setCameraFocus(final String focusName, final float waitTimeSeconds) {
        if (checkString(focusName, "focusName")) {
            SceneGraphNode sgn = getObject(focusName);
            if (sgn instanceof IFocus) {
                IFocus focus = (IFocus) sgn;
                NaturalCamera cam = GaiaSky.instance.cam.naturalCamera;
                changeFocus(focus, cam, waitTimeSeconds);
            } else {
                logger.error("FOCUS_MODE object does not exist: " + focusName);
            }
        }
    }

    public void setCameraFocus(final String focusName, final int waitTimeSeconds) {
        setCameraFocus(focusName, (float) waitTimeSeconds);
    }

    @Override
    public void setCameraFocusInstant(final String focusName) {
        if (checkString(focusName, "focusName")) {
            SceneGraphNode sgn = getObject(focusName);
            if (sgn instanceof IFocus) {
                IFocus focus = (IFocus) sgn;
                em.post(Events.CAMERA_MODE_CMD, CameraMode.FOCUS_MODE);
                em.post(Events.FOCUS_CHANGE_CMD, focus);

                GaiaSky.postRunnable(() -> {
                    // Instantly set the camera direction to look towards the focus
                    double[] campos = GaiaSky.instance.cam.getPos().values();
                    Vector3d dir = new Vector3d();
                    focus.getAbsolutePosition(dir).sub(campos[0], campos[1], campos[2]);
                    double[] d = dir.nor().values();
                    em.post(Events.CAMERA_DIR_CMD, d);
                });
                // Make sure the last action is flushed
                sleepFrames(2);
            } else {
                logger.error("FOCUS_MODE object does not exist: " + focusName);
            }
        }
    }

    @Override
    public void setCameraFocusInstantAndGo(final String focusName) {
        if (checkString(focusName, "focusName")) {
            SceneGraphNode sgn = getObject(focusName);
            if (sgn instanceof IFocus) {
                IFocus focus = (IFocus) sgn;
                em.post(Events.CAMERA_MODE_CMD, CameraMode.FOCUS_MODE);
                em.post(Events.FOCUS_CHANGE_CMD, focus, true);
                em.post(Events.GO_TO_OBJECT_CMD);
                // Make sure the last action is flushed
                sleepFrames(2);
            }
        }
    }

    @Override
    public void setCameraLock(final boolean lock) {
        GaiaSky.postRunnable(() -> em.post(Events.FOCUS_LOCK_CMD, I18n.bundle.get("gui.camera.lock"), lock));
    }

    @Override
    public void setCameraCenterFocus(boolean centerFocus) {
        GaiaSky.postRunnable(() -> em.post(Events.CAMERA_CENTER_FOCUS_CMD, centerFocus));
    }

    @Override
    public void setCameraFree() {
        GaiaSky.postRunnable(() -> em.post(Events.CAMERA_MODE_CMD, CameraMode.FREE_MODE));
    }

    @Override
    public void setCameraFov1() {
        GaiaSky.postRunnable(() -> em.post(Events.CAMERA_MODE_CMD, CameraMode.GAIA_FOV1_MODE));
    }

    @Override
    public void setCameraFov2() {
        GaiaSky.postRunnable(() -> em.post(Events.CAMERA_MODE_CMD, CameraMode.GAIA_FOV2_MODE));
    }

    @Override
    public void setCameraFov1and2() {
        GaiaSky.postRunnable(() -> em.post(Events.CAMERA_MODE_CMD, CameraMode.GAIA_FOVS_MODE));
    }

    @Override
    public void setCameraPostion(final double[] vec) {
        setCameraPosition(vec);
    }

    @Override
    public void setCameraPosition(final double[] vec) {
        if (vec.length != 3)
            throw new RuntimeException("vec parameter must have three components");
        GaiaSky.postRunnable(() -> {
            // Convert to km
            vec[0] = vec[0] * Constants.KM_TO_U;
            vec[1] = vec[1] * Constants.KM_TO_U;
            vec[2] = vec[2] * Constants.KM_TO_U;
            // Send event
            em.post(Events.CAMERA_POS_CMD, vec);
        });
    }

    @Override
    public void setCameraPosition(double x, double y, double z) {
        setCameraPosition(new double[]{x, y, z});
    }

    public void setCameraPosition(final List vec) {
        setCameraPosition(dArray(vec));
    }

    @Override
    public double[] getCameraPosition() {
        Vector3d campos = GaiaSky.instance.cam.getPos();
        return new double[]{campos.x * Constants.U_TO_KM, campos.y * Constants.U_TO_KM, campos.z * Constants.U_TO_KM};
    }

    @Override
    public void setCameraDirection(final double[] dir) {
        GaiaSky.postRunnable(() -> em.post(Events.CAMERA_DIR_CMD, dir));
    }

    public void setCameraDirection(final List dir) {
        setCameraDirection(dArray(dir));
    }

    @Override
    public double[] getCameraDirection() {
        Vector3d camdir = GaiaSky.instance.cam.getDirection();
        return new double[]{camdir.x, camdir.y, camdir.z};
    }

    @Override
    public void setCameraUp(final double[] up) {
        GaiaSky.postRunnable(() -> em.post(Events.CAMERA_UP_CMD, up));

    }

    public void setCameraUp(final List up) {
        setCameraUp(dArray(up));
    }

    @Override
    public double[] getCameraUp() {
        Vector3d camup = GaiaSky.instance.cam.getUp();
        return new double[]{camup.x, camup.y, camup.z};
    }

    @Override
    public void setCameraPositionAndFocus(String focus, String other, double rotation, double viewAngle) {
        if (checkNum(viewAngle, 1e-50d, Double.MAX_VALUE, "viewAngle") && checkNotNull(focus, "focus") && checkNotNull(other, "other")) {

            String focuslc = focus.toLowerCase();
            String otherlc = other.toLowerCase();
            ISceneGraph sg = GaiaSky.instance.sg;
            if (sg.containsNode(focuslc) && sg.containsNode(otherlc)) {
                IFocus focusObj = sg.findFocus(focuslc);
                IFocus otherObj = sg.findFocus(otherlc);
                setCameraPositionAndFocus(focusObj, otherObj, rotation, viewAngle);
            }
        }
    }

    public void setCameraPositionAndFocus(String focus, String other, long rotation, long viewAngle) {
        setCameraPositionAndFocus(focus, other, (double) rotation, (double) viewAngle);
    }

    public void pointAtSkyCoordinate(double ra, double dec) {
        em.post(Events.CAMERA_MODE_CMD, CameraMode.FREE_MODE);
        em.post(Events.FREE_MODE_COORD_CMD, (float) ra, (float) dec);
    }

    public void pointAtSkyCoordinate(long ra, long dec) {
        pointAtSkyCoordinate((double) ra, (double) dec);
    }

    private void setCameraPositionAndFocus(IFocus focus, IFocus other, double rotation, double viewAngle) {
        if (checkNum(viewAngle, 1e-50d, Double.MAX_VALUE, "viewAngle") && checkNotNull(focus, "focus") && checkNotNull(other, "other")) {

            em.post(Events.CAMERA_MODE_CMD, CameraMode.FOCUS_MODE);
            em.post(Events.FOCUS_CHANGE_CMD, focus);

            double radius = focus.getRadius();
            double dist = radius / Math.tan(Math.toRadians(viewAngle / 2)) + radius;

            // Up to ecliptic north pole
            Vector3d up = new Vector3d(0, 1, 0).mul(Coordinates.eclToEq());

            Vector3d focusPos = aux3d1;
            focus.getAbsolutePosition(focusPos);
            Vector3d otherPos = aux3d2;
            other.getAbsolutePosition(otherPos);

            Vector3d otherToFocus = aux3d3;
            otherToFocus.set(focusPos).sub(otherPos).nor();
            Vector3d focusToOther = aux3d4.set(otherToFocus);
            focusToOther.scl(-dist).rotate(up, rotation);

            // New camera position
            Vector3d newCamPos = aux3d5.set(focusToOther).add(focusPos).scl(Constants.U_TO_KM);

            // New camera direction
            Vector3d newCamDir = aux3d6.set(focusToOther);
            newCamDir.scl(-1).nor();

            // Finally, set values
            setCameraPosition(newCamPos.values());
            setCameraDirection(newCamDir.values());
            setCameraUp(up.values());
        }
    }

    @Override
    public void setCameraSpeed(final float speed) {
        if (checkNum(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "speed"))
            GaiaSky.postRunnable(() -> em.post(Events.CAMERA_SPEED_CMD, speed / 10f, false));
    }

    public void setCameraSpeed(final int speed) {
        setCameraSpeed((float) speed);
    }

    @Override
    public double getCameraSpeed() {
        return GaiaSky.instance.cam.getSpeed();
    }

    @Override
    public void setRotationCameraSpeed(final float speed) {
        if (checkNum(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "speed"))
            GaiaSky.postRunnable(() -> em.post(Events.ROTATION_SPEED_CMD, MathUtilsd.lint(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED), false));
    }

    public void setRotationCameraSpeed(final int speed) {
        setRotationCameraSpeed((float) speed);
    }

    @Override
    public void setTurningCameraSpeed(final float speed) {
        if (checkNum(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "speed"))
            GaiaSky.postRunnable(() -> em.post(Events.TURNING_SPEED_CMD, MathUtilsd.lint(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED), false));

    }

    public void setTurningCameraSpeed(final int speed) {
        setTurningCameraSpeed((float) speed);
    }

    @Override
    public void setCameraSpeedLimit(int index) {
        if (checkNum(index, 0, 18, "index"))
            GaiaSky.postRunnable(() -> em.post(Events.SPEED_LIMIT_CMD, index, false));
    }

    @Override
    public void setCameraOrientationLock(boolean lock) {
        GaiaSky.postRunnable(() -> em.post(Events.ORIENTATION_LOCK_CMD, I18n.bundle.get("gui.camera.lock.orientation"), lock, false));
    }

    @Override
    public void cameraForward(final double cameraForward) {
        if (checkNum(cameraForward, -100d, 100d, "cameraForward"))
            GaiaSky.postRunnable(() -> em.post(Events.CAMERA_FWD, cameraForward));
    }

    public void cameraForward(final long value) {
        cameraForward((double) value);
    }

    @Override
    public void cameraRotate(final double deltaX, final double deltaY) {
        if (checkNum(deltaX, -100d, 100d, "deltaX") && checkNum(deltaY, -100d, 100d, "deltaY"))
            GaiaSky.postRunnable(() -> em.post(Events.CAMERA_ROTATE, deltaX, deltaY));
    }

    public void cameraRotate(final double deltaX, final long deltaY) {
        cameraRotate(deltaX, (double) deltaY);
    }

    public void cameraRotate(final long deltaX, final double deltaY) {
        cameraRotate((double) deltaX, deltaY);
    }

    @Override
    public void cameraRoll(final double roll) {
        if (checkNum(roll, -100d, 100d, "roll"))
            GaiaSky.postRunnable(() -> em.post(Events.CAMERA_ROLL, roll));
    }

    public void cameraRoll(final long roll) {
        cameraRoll((double) roll);
    }

    @Override
    public void cameraTurn(final double deltaX, final double deltaY) {
        if (checkNum(deltaX, -100d, 100d, "deltaX") && checkNum(deltaY, -100d, 100d, "deltaY")) {
            GaiaSky.postRunnable(() -> em.post(Events.CAMERA_TURN, deltaX, deltaY));
        }
    }

    public void cameraTurn(final double deltaX, final long deltaY) {
        cameraTurn(deltaX, (double) deltaY);
    }

    public void cameraTurn(final long deltaX, final double deltaY) {
        cameraTurn((double) deltaX, deltaY);
    }

    public void cameraTurn(final long deltaX, final long deltaY) {
        cameraTurn((double) deltaX, (double) deltaY);
    }

    @Override
    public void cameraYaw(final double amount) {
        cameraTurn(amount, 0d);
    }

    public void cameraYaw(final long amount) {
        cameraYaw((double) amount);
    }

    @Override
    public void cameraPitch(final double amount) {
        cameraTurn(0d, amount);
    }

    public void cameraPitch(final long amount) {
        cameraPitch((double) amount);
    }

    @Override
    public void cameraStop() {
        GaiaSky.postRunnable(() -> em.post(Events.CAMERA_STOP));

    }

    @Override
    public void cameraCenter() {
        GaiaSky.postRunnable(() -> em.post(Events.CAMERA_CENTER));
    }

    @Override
    public IFocus getClosestObjectToCamera() {
        return GaiaSky.instance.cam.getClosestBody();
    }

    @Override
    public void setFov(final float newFov) {
        if (!SlaveManager.projectionActive()) {
            if (checkNum(newFov, Constants.MIN_FOV, Constants.MAX_FOV, "newFov"))
                GaiaSky.postRunnable(() -> em.post(Events.FOV_CHANGED_CMD, newFov));
        }
    }

    public void setFov(final int newFov) {
        setFov((float) newFov);
    }

    @Override
    public void setVisibility(final String key, final boolean visible) {
        if (!checkComponentTypeKey(key)) {
            logger.error("Element '" + key + "' does not exist. Possible values are:");
            ComponentType[] cts = ComponentType.values();
            for (ComponentType ct : cts)
                logger.error(ct.key);
        } else {
            GaiaSky.postRunnable(() -> em.post(Events.TOGGLE_VISIBILITY_CMD, key, false, visible));
        }
    }

    @Override
    public void setLabelSizeFactor(float factor) {
        if (checkNum(factor, Constants.MIN_LABEL_SIZE, Constants.MAX_LABEL_SIZE, "labelSizeFactor")) {
            GaiaSky.postRunnable(() -> em.post(Events.LABEL_SIZE_CMD, factor, false));
        }
    }

    public void setLabelSizeFactor(int factor) {
        setLabelSizeFactor((float) factor);
    }

    @Override
    public void setLineWidthFactor(final float factor) {
        if (checkNum(factor, Constants.MIN_LINE_WIDTH, Constants.MAX_LINE_WIDTH, "lineWidthFactor")) {
            GaiaSky.postRunnable(() -> em.post(Events.LINE_WIDTH_CMD, factor, false));
        }
    }

    public void setLineWidthFactor(int factor) {
        setLineWidthFactor((float) factor);
    }

    private boolean checkComponentTypeKey(String key) {
        ComponentType[] cts = ComponentType.values();
        boolean keyFound = false;
        for (ComponentType ct : cts)
            keyFound = keyFound || key.equals(ct.key);

        return keyFound;
    }

    @Override
    public void setProperMotionsNumberFactor(float factor) {
        GaiaSky.postRunnable(() -> EventManager.instance.post(Events.PM_NUM_FACTOR_CMD, MathUtilsd.lint(factor, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_PM_NUM_FACTOR, Constants.MAX_PM_NUM_FACTOR), false));
    }

    @Override
    public void setProperMotionsColorMode(int mode) {
        GaiaSky.postRunnable(() -> EventManager.instance.post(Events.PM_COLOR_MODE_CMD, mode % 6, false));
    }

    @Override
    public void setProperMotionsArrowheads(boolean arrowheadsEnabled) {
        GaiaSky.postRunnable(() -> EventManager.instance.post(Events.PM_ARROWHEADS_CMD, arrowheadsEnabled, false));
    }

    public void setProperMotionsNumberFactor(int factor) {
        setProperMotionsNumberFactor((float) factor);
    }

    public void setUnfilteredProperMotionsNumberFactor(float factor) {
        GlobalConf.scene.PM_NUM_FACTOR = factor;
    }

    @Override
    public void setProperMotionsLengthFactor(float factor) {
        GaiaSky.postRunnable(() -> EventManager.instance.post(Events.PM_LEN_FACTOR_CMD, factor, false));
    }

    public void setProperMotionsLengthFactor(int factor) {
        setProperMotionsLengthFactor((float) factor);
    }

    @Override
    public void setProperMotionsMaxNumber(long maxNumber) {
        GlobalConf.scene.N_PM_STARS = maxNumber;
    }

    @Override
    public long getProperMotionsMaxNumber() {
        return GlobalConf.scene.N_PM_STARS;
    }

    @Override
    public void setCrosshairVisibility(boolean visible) {
        setFocusCrosshairVisibility(visible);
        setClosestCrosshairVisibility(visible);
        setHomeCrosshairVisibility(visible);
    }

    @Override
    public void setFocusCrosshairVisibility(boolean visible) {
        GaiaSky.postRunnable(() -> em.post(Events.CROSSHAIR_FOCUS_CMD, visible));
    }

    @Override
    public void setClosestCrosshairVisibility(boolean visible) {
        GaiaSky.postRunnable(() -> em.post(Events.CROSSHAIR_CLOSEST_CMD, visible));
    }

    @Override
    public void setHomeCrosshairVisibility(boolean visible) {
        GaiaSky.postRunnable(() -> em.post(Events.CROSSHAIR_HOME_CMD, visible));
    }

    @Override
    public void setMinimapVisibility(boolean visible) {
        GaiaSky.postRunnable(() -> em.post(Events.SHOW_MINIMAP_ACTION, visible, false));
    }

    @Override
    public void setAmbientLight(final float ambientLight) {
        if (checkNum(ambientLight, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "ambientLight"))
            GaiaSky.postRunnable(() -> em.post(Events.AMBIENT_LIGHT_CMD, ambientLight));
    }

    public void setAmbientLight(final int value) {
        setAmbientLight((float) value);
    }

    @Override
    public void setSimulationTime(int year, int month, int day, int hour, int min, int sec, int millisec) {
        LocalDateTime date = LocalDateTime.of(year, month, day, hour, min, sec, millisec);
        em.post(Events.TIME_CHANGE_CMD, date.toInstant(ZoneOffset.UTC));
    }

    @Override
    public void setSimulationTime(final long time) {
        if (checkNum(time, 1, Long.MAX_VALUE, "time"))
            em.post(Events.TIME_CHANGE_CMD, Instant.ofEpochMilli(time));
    }

    @Override
    public long getSimulationTime() {
        ITimeFrameProvider time = GaiaSky.instance.time;
        return time.getTime().toEpochMilli();
    }

    @Override
    public int[] getSimulationTimeArr() {
        ITimeFrameProvider time = GaiaSky.instance.time;
        Instant instant = time.getTime();
        LocalDateTime c = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        int[] result = new int[7];
        result[0] = c.get(ChronoField.YEAR_OF_ERA);
        result[1] = c.getMonthValue();
        result[2] = c.getDayOfMonth();
        result[3] = c.getHour();
        result[4] = c.getMinute();
        result[5] = c.getSecond();
        result[6] = c.get(ChronoField.MILLI_OF_SECOND);
        return result;
    }

    @Override
    public void startSimulationTime() {
        em.post(Events.TIME_STATE_CMD, true, false);
    }

    @Override
    public void stopSimulationTime() {
        em.post(Events.TIME_STATE_CMD, false, false);
    }

    @Override
    public boolean isSimulationTimeOn() {
        return GaiaSky.instance.time.isTimeOn();
    }

    @Override
    public void setSimulationPace(final double pace) {
        em.post(Events.PACE_CHANGE_CMD, pace);
    }

    public void setSimulationPace(final long pace) {
        setSimulationPace((double) pace);
    }

    @Override
    public void setTargetTime(long ms) {
        em.post(Events.TARGET_TIME_CMD, Instant.ofEpochMilli(ms));
    }

    @Override
    public void setTargetTime(int year, int month, int day, int hour, int min, int sec, int millisec) {
        em.post(Events.TARGET_TIME_CMD, LocalDateTime.of(year, month, day, hour, min, sec, millisec).toInstant(ZoneOffset.UTC));
    }

    @Override
    public void unsetTargetTime() {
        em.post(Events.TARGET_TIME_CMD);
    }

    @Override
    public void setStarBrightness(final float brightness) {
        if (checkNum(brightness, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "brightness"))
            GaiaSky.postRunnable(() -> em.post(Events.STAR_BRIGHTNESS_CMD, MathUtilsd.lint(brightness, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_STAR_BRIGHT, Constants.MAX_STAR_BRIGHT), false));
    }

    public void setStarBrightness(final int brightness) {
        setStarBrightness((float) brightness);
    }

    @Override
    public float getStarBrightness() {
        return (float) MathUtilsd.lint(GlobalConf.scene.STAR_BRIGHTNESS, Constants.MIN_STAR_BRIGHT, Constants.MAX_STAR_BRIGHT, Constants.MIN_SLIDER, Constants.MAX_SLIDER);
    }

    @Override
    public void setStarSize(final float size) {
        if (checkNum(size, Constants.MIN_STAR_POINT_SIZE, Constants.MAX_STAR_POINT_SIZE, "size"))
            GaiaSky.postRunnable(() -> em.post(Events.STAR_POINT_SIZE_CMD, size, false));
    }

    public void setStarSize(final int size) {
        setStarSize((float) size);
    }

    @Override
    public float getStarSize() {
        return MathUtilsd.lint(GlobalConf.scene.STAR_POINT_SIZE, Constants.MIN_STAR_POINT_SIZE, Constants.MAX_STAR_POINT_SIZE, Constants.MIN_SLIDER, Constants.MAX_SLIDER);
    }

    @Override
    public float getMinStarOpacity() {
        return MathUtilsd.lint(GlobalConf.scene.STAR_MIN_OPACITY, Constants.MIN_STAR_MIN_OPACITY, Constants.MAX_STAR_MIN_OPACITY, Constants.MIN_SLIDER, Constants.MAX_SLIDER);
    }

    @Override
    public void setMinStarOpacity(float opacity) {
        if (checkNum(opacity, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "opacity"))
            GaiaSky.postRunnable(() -> EventManager.instance.post(Events.STAR_MIN_OPACITY_CMD, MathUtilsd.lint(opacity, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_STAR_MIN_OPACITY, Constants.MAX_STAR_MIN_OPACITY), false));
    }

    @Override
    public void setProjectionYaw(float yaw) {
        if (SlaveManager.projectionActive()) {
            GaiaSky.postRunnable(() -> {
                GlobalConf.program.NET_SLAVE_YAW = yaw;
                SlaveManager.instance.yaw = yaw;
            });
        }
    }

    @Override
    public void setProjectionPitch(float pitch) {
        if (SlaveManager.projectionActive()) {
            GaiaSky.postRunnable(() -> {
                GlobalConf.program.NET_SLAVE_PITCH = pitch;
                SlaveManager.instance.pitch = pitch;
            });
        }
    }

    @Override
    public void setProjectionRoll(float roll) {
        if (SlaveManager.projectionActive()) {
            GaiaSky.postRunnable(() -> {
                GlobalConf.program.NET_SLAVE_ROLL = roll;
                SlaveManager.instance.roll = roll;
            });
        }
    }

    @Override
    public void setProjectionFov(float newFov) {
        if (checkNum(newFov, Constants.MIN_FOV, 170f, "newFov"))
            GaiaSky.postRunnable(() -> {
                SlaveManager.instance.cameraFov = newFov;
                em.post(Events.FOV_CHANGED_CMD, newFov, false);
            });
    }

    public void setMinStarOpacity(int opacity) {
        setMinStarOpacity((float) opacity);
    }

    @Override
    public void configureFrameOutput(int width, int height, int fps, String folder, String namePrefix) {
        if (checkNum(width, 1, Integer.MAX_VALUE, "width") && checkNum(height, 1, Integer.MAX_VALUE, "height") && checkNum(fps, 1, Integer.MAX_VALUE, "FPS") && checkString(folder, "folder") && checkString(namePrefix, "namePrefix")) {
            em.post(Events.FRAME_OUTPUT_MODE_CMD, GlobalConf.ScreenshotMode.redraw);
            em.post(Events.CONFIG_FRAME_OUTPUT_CMD, width, height, fps, folder, namePrefix);
        }
    }

    @Override
    public void configureRenderOutput(int width, int height, int fps, String folder, String namePrefix) {
        configureFrameOutput(width, height, fps, folder, namePrefix);
    }

    @Override
    public void setFrameOutputMode(String screenshotMode) {
        if (checkStringEnum(screenshotMode, GlobalConf.ScreenshotMode.class, "screenshotMode"))
            em.post(Events.FRAME_OUTPUT_MODE_CMD, screenshotMode);
    }

    @Override
    public boolean isFrameOutputActive() {
        return GlobalConf.frame.RENDER_OUTPUT;
    }

    @Override
    public boolean isRenderOutputActive() {
        return isFrameOutputActive();
    }

    @Override
    public int getFrameOutputFps() {
        return GlobalConf.frame.RENDER_TARGET_FPS;
    }

    @Override
    public int getRenderOutputFps() {
        return getFrameOutputFps();
    }

    @Override
    public void setFrameOutput(boolean active) {
        em.post(Events.FRAME_OUTPUT_CMD, active);
    }

    @Override
    public SceneGraphNode getObject(String name) {
        return getObject(name, 0);
    }

    @Override
    public SceneGraphNode getObject(String name, double timeOutSeconds) {
        ISceneGraph sg = GaiaSky.instance.sg;
        SceneGraphNode obj = sg.getNode(name);
        if (obj == null) {
            if (name.matches("[0-9]+")) {
                // Check with 'HIP '
                obj = sg.getNode("hip " + name);
            } else if (name.matches("hip [0-9]+") || name.matches("HIP [0-9]+")) {
                obj = sg.getNode(name.substring(4));
            }
        }

        // If negative, no limit in waiting
        if (timeOutSeconds < 0)
            timeOutSeconds = Double.MAX_VALUE;

        double startMs = System.currentTimeMillis();
        double elapsedSeconds = 0;
        while (obj == null && elapsedSeconds < timeOutSeconds) {
            sleepFrames(1);
            obj = sg.getNode(name);
            elapsedSeconds = (System.currentTimeMillis() - startMs) / 1000d;
        }
        return obj;
    }

    @Override
    public void setObjectSizeScaling(String name, double scalingFactor) {
        SceneGraphNode sgn = getObject(name);
        if (sgn == null) {
            logger.error("Object '" + name + "' does not exist");
            return;
        }
        if (sgn instanceof ModelBody) {
            ModelBody m = (ModelBody) sgn;
            m.setSizescalefactor(scalingFactor);
        } else {
            logger.error("Object '" + name + "' is not a model object");
        }
    }

    @Override
    public double getObjectRadius(String name) {
        ISceneGraph sg = GaiaSky.instance.sg;
        IFocus obj = sg.findFocus(name.toLowerCase().trim());
        if (obj == null)
            return -1;
        else if (obj instanceof IStarFocus) {
            // TODO Remove this dirty hack
            return obj.getRadius() * 1.4856329941301618 * Constants.U_TO_KM;
        } else {
            return obj.getRadius() * Constants.U_TO_KM;
        }
    }

    @Override
    public void goToObject(String name) {
        goToObject(name, -1);
    }

    @Override
    public void goToObject(String name, double angle) {
        goToObject(name, angle, -1);
    }

    @Override
    public void goToObject(String name, double viewAngle, float waitTimeSeconds) {
        goToObject(name, viewAngle, waitTimeSeconds, null);
    }

    public void goToObject(String name, double viewAngle, int waitTimeSeconds) {
        goToObject(name, viewAngle, (float) waitTimeSeconds);
    }

    public void goToObject(String name, long viewAngle, int waitTimeSeconds) {
        goToObject(name, (double) viewAngle, (float) waitTimeSeconds);
    }

    public void goToObject(String name, long viewAngle, float waitTimeSeconds) {
        goToObject(name, (double) viewAngle, waitTimeSeconds);
    }

    private void goToObject(String name, double viewAngle, float waitTimeSeconds, AtomicBoolean stop) {
        if (checkString(name, "name")) {
            String namelc = name.toLowerCase().trim();
            ISceneGraph sg = GaiaSky.instance.sg;
            if (sg.containsNode(namelc)) {
                IFocus focus = sg.findFocus(namelc);
                goToObject(focus, viewAngle, waitTimeSeconds, stop);
            } else {
                logger.info("FOCUS_MODE object does not exist: " + name);
            }
        }
    }

    public void goToObject(String name, double viewAngle, int waitTimeSeconds, AtomicBoolean stop) {
        goToObject(name, viewAngle, (float) waitTimeSeconds, stop);
    }

    void goToObject(IFocus object, double viewAngle, float waitTimeSeconds, AtomicBoolean stop) {
        if (checkNotNull(object, "object") && checkNum(viewAngle, 0, Double.MAX_VALUE, "viewAngle")) {

            stops.add(stop);
            NaturalCamera cam = GaiaSky.instance.cam.naturalCamera;

            changeFocus(object, cam, waitTimeSeconds);

            /* target angle */
            double target = Math.toRadians(viewAngle);
            if (target < 0)
                target = Math.toRadians(20d);

            long prevtime = TimeUtils.millis();
            if (object.getViewAngleApparent() < target) {
                // Add forward movement while distance > target distance
                while (object.getViewAngleApparent() < target && (stop == null || !stop.get())) {
                    // dt in ms
                    long dt = TimeUtils.timeSinceMillis(prevtime);
                    prevtime = TimeUtils.millis();

                    em.post(Events.CAMERA_FWD, 1d * dt);
                    try {
                        sleepFrames(1);
                    } catch (Exception e) {
                        logger.error(e);
                    }
                }
            } else {
                // Add backward movement while distance > target distance
                while (object.getViewAngleApparent() > target && (stop == null || !stop.get())) {
                    // dt in ms
                    long dt = TimeUtils.timeSinceMillis(prevtime);
                    prevtime = TimeUtils.millis();

                    em.post(Events.CAMERA_FWD, -1d * dt);
                    try {
                        sleepFrames(1);
                    } catch (Exception e) {
                        logger.error(e);
                    }
                }
            }

            // We can stop now
            em.post(Events.CAMERA_STOP);
        }
    }

    public void goToObject(IFocus object, double viewAngle, int waitTimeSeconds, AtomicBoolean stop) {
        goToObject(object, viewAngle, (float) waitTimeSeconds, stop);
    }

    @Override
    public void goToObjectInstant(String name) {
        setCameraFocusInstantAndGo(name);
    }

    @Override
    public void landOnObject(String name) {
        if (checkString(name, "name")) {
            SceneGraphNode sgn = getObject(name);
            if (sgn instanceof IFocus)
                landOnObject((IFocus) sgn, null);
        }
    }

    void landOnObject(IFocus object, AtomicBoolean stop) {
        if (checkNotNull(object, "object")) {

            stops.add(stop);
            if (object instanceof Planet) {
                NaturalCamera cam = GaiaSky.instance.cam.naturalCamera;
                // FOCUS_MODE wait - 2 seconds
                float waitTimeSeconds = -1;

                /*
                 * SAVE
                 */

                // Save speed, set it to 50
                double speed = GlobalConf.scene.CAMERA_SPEED;
                em.post(Events.CAMERA_SPEED_CMD, 25f / 10f, false);

                // Save turn speed, set it to 50
                double turnSpeedBak = GlobalConf.scene.TURNING_SPEED;
                em.post(Events.TURNING_SPEED_CMD, (float) MathUtilsd.lint(20d, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED), false);

                // Save cinematic
                boolean cinematic = GlobalConf.scene.CINEMATIC_CAMERA;
                GlobalConf.scene.CINEMATIC_CAMERA = true;

                /*
                 * FOCUS
                 */

                changeFocus(object, cam, waitTimeSeconds);

                /* target distance */
                double target = 100 * Constants.M_TO_U;

                object.getAbsolutePosition(aux3d1).add(cam.posinv).nor();
                Vector3d dir = cam.direction;

                // Add forward movement while distance > target distance
                boolean distanceNotMet = (object.getDistToCamera() - object.getRadius()) > target;
                boolean viewNotMet = Math.abs(dir.angle(aux3d1)) < 90;

                long prevtime = TimeUtils.millis();
                while ((distanceNotMet || viewNotMet) && (stop == null || !stop.get())) {
                    // dt in ms
                    long dt = TimeUtils.timeSinceMillis(prevtime);
                    prevtime = TimeUtils.millis();

                    if (distanceNotMet)
                        em.post(Events.CAMERA_FWD, 0.1d * dt);
                    else
                        cam.stopForwardMovement();

                    if (viewNotMet) {
                        if (object.getDistToCamera() - object.getRadius() < object.getRadius() * 5)
                            // Start turning where we are at n times the radius
                            em.post(Events.CAMERA_TURN, 0d, dt / 500d);
                    } else {
                        cam.stopRotateMovement();
                    }

                    try {
                        sleepFrames(1);
                    } catch (Exception e) {
                        logger.error(e);
                    }

                    // focus.transform.getTranslation(aux);
                    viewNotMet = Math.abs(dir.angle(aux3d1)) < 90;
                    distanceNotMet = (object.getDistToCamera() - object.getRadius()) > target;
                }

                // STOP
                em.post(Events.CAMERA_STOP);

                // Roll till done
                Vector3d up = cam.up;
                // aux1 <- camera-object
                object.getAbsolutePosition(aux3d1).sub(cam.pos);
                double ang1 = up.angle(aux3d1);
                double ang2 = up.cpy().rotate(cam.direction, 1).angle(aux3d1);
                double rollsign = ang1 < ang2 ? -1d : 1d;

                if (ang1 < 170) {

                    rollAndWait(rollsign * 0.02d, 170d, 50L, cam, aux3d1, stop);
                    // STOP
                    cam.stopMovement();

                    rollAndWait(rollsign * 0.006d, 176d, 50L, cam, aux3d1, stop);
                    // STOP
                    cam.stopMovement();

                    rollAndWait(rollsign * 0.003d, 178d, 50L, cam, aux3d1, stop);
                }
                /*
                 * RESTORE
                 */

                // We can stop now
                em.post(Events.CAMERA_STOP);

                // Restore cinematic
                GlobalConf.scene.CINEMATIC_CAMERA = cinematic;

                // Restore speed
                em.post(Events.CAMERA_SPEED_CMD, (float) speed, false);

                // Restore turning speed
                em.post(Events.TURNING_SPEED_CMD, (float) turnSpeedBak, false);

            }
        }
    }

    @Override
    public void landOnObjectLocation(String name, String locationName) {
        landOnObjectLocation(name, locationName, null);
    }

    public void landOnObjectLocation(String name, String locationName, AtomicBoolean stop) {
        if (checkString(name, "name")) {
            stops.add(stop);
            SceneGraphNode sgn = getObject(name);
            if (sgn instanceof IFocus)
                landOnObjectLocation((IFocus) sgn, locationName, stop);
        }
    }

    public void landOnObjectLocation(IFocus object, String locationName, AtomicBoolean stop) {
        if (checkNotNull(object, "object") && checkString(locationName, "locationName")) {

            stops.add(stop);
            if (object instanceof Planet) {
                Planet planet = (Planet) object;
                SceneGraphNode sgn = planet.getChildByNameAndType(locationName.toLowerCase().trim(), Loc.class);
                if (sgn != null) {
                    Loc location = (Loc) sgn;
                    landOnObjectLocation(object, location.getLocation().x, location.getLocation().y, stop);
                    return;
                }
                logger.info("Location '" + locationName + "' not found on object '" + object.getCandidateName() + "'");
            }
        }
    }

    @Override
    public void landOnObjectLocation(String name, double longitude, double latitude) {
        if (checkString(name, "name")) {
            SceneGraphNode sgn = getObject(name);
            if (sgn instanceof IFocus)
                landOnObjectLocation((IFocus) sgn, longitude, latitude, null);
        }
    }

    void landOnObjectLocation(IFocus object, double longitude, double latitude, AtomicBoolean stop) {
        if (checkNotNull(object, "object") && checkNum(latitude, -90d, 90d, "latitude") && checkNum(longitude, 0d, 360d, "longitude")) {
            stops.add(stop);
            ISceneGraph sg = GaiaSky.instance.sg;
            String nameStub = object.getCandidateName() + " [loc]";

            if (!sg.containsNode(nameStub)) {
                Invisible invisible = new Invisible(nameStub);
                EventManager.instance.post(Events.SCENE_GRAPH_ADD_OBJECT_CMD, invisible, true);
            }
            Invisible invisible = (Invisible) getObject(nameStub, 5);

            if (object instanceof Planet) {
                Planet planet = (Planet) object;
                NaturalCamera cam = GaiaSky.instance.cam.naturalCamera;

                double targetAngle = 35 * MathUtilsd.degRad;
                if (planet.viewAngle > targetAngle) {
                    // Zoom out
                    while (planet.viewAngle > targetAngle && (stop == null || !stop.get())) {
                        cam.addForwardForce(-5d);
                        sleepFrames(1);
                    }
                    // STOP
                    cam.stopMovement();
                }

                // Go to object
                goToObject(object, 20, -1, stop);

                // Save speed, set it to 50
                double speed = GlobalConf.scene.CAMERA_SPEED;
                em.post(Events.CAMERA_SPEED_CMD, 25f / 10f, false);

                // Save turn speed, set it to 50
                double turnSpeedBak = GlobalConf.scene.TURNING_SPEED;
                em.post(Events.TURNING_SPEED_CMD, (float) MathUtilsd.lint(50d, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED), false);

                // Save rotation speed, set it to 20
                double rotationSpeedBak = GlobalConf.scene.ROTATION_SPEED;
                em.post(Events.ROTATION_SPEED_CMD, (float) MathUtilsd.lint(20d, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED), false);

                // Save cinematic
                boolean cinematic = GlobalConf.scene.CINEMATIC_CAMERA;
                GlobalConf.scene.CINEMATIC_CAMERA = true;

                // Save crosshair
                boolean crosshair = GlobalConf.scene.CROSSHAIR_FOCUS;
                GlobalConf.scene.CROSSHAIR_FOCUS = false;

                // Get target position
                Vector3d target = aux3d1;
                planet.getPositionAboveSurface(longitude, latitude, 50, target);

                // Get object position
                Vector3d objectPosition = planet.getAbsolutePosition(aux3d2);

                // Check intersection with object
                boolean intersects = Intersectord.checkIntersectSegmentSphere(cam.pos, target, objectPosition, planet.getRadius());

                if (intersects) {
                    cameraRotate(5d, 5d);
                }

                while (intersects && (stop == null || !stop.get())) {
                    sleep(0.1f);

                    objectPosition = planet.getAbsolutePosition(aux3d2);
                    intersects = Intersectord.checkIntersectSegmentSphere(cam.pos, target, objectPosition, planet.getRadius());
                }

                cameraStop();

                invisible.ct = planet.ct;
                invisible.pos.set(target);

                // Go to object
                goToObject(nameStub, 20, 0, stop);

                // Restore cinematic
                GlobalConf.scene.CINEMATIC_CAMERA = cinematic;

                // Restore speed
                em.post(Events.CAMERA_SPEED_CMD, (float) speed, false);

                // Restore turning speed
                em.post(Events.TURNING_SPEED_CMD, (float) turnSpeedBak, false);

                // Restore rotation speed
                em.post(Events.ROTATION_SPEED_CMD, (float) rotationSpeedBak, false);

                // Restore crosshair
                GlobalConf.scene.CROSSHAIR_FOCUS = crosshair;

                // Land
                landOnObject(object, stop);

            }

            sg.remove(invisible, true);
        }
    }

    private void rollAndWait(double roll, double target, long sleep, NaturalCamera cam, Vector3d camobj, AtomicBoolean stop) {
        // Apply roll and wait
        double ang = cam.up.angle(camobj);

        while (ang < target && (stop == null || !stop.get())) {
            cam.addRoll(roll, false);

            try {
                sleep(sleep);
            } catch (Exception e) {
                logger.error(e);
            }

            ang = cam.up.angle(aux3d1);
        }
    }

    @Override
    public double getDistanceTo(String name) {
        SceneGraphNode sgn = getObject(name);
        if (sgn instanceof IFocus) {
            IFocus obj = (IFocus) sgn;
            return (obj.getDistToCamera() - obj.getRadius()) * Constants.U_TO_KM;
        }

        return -1;
    }

    @Override
    public double[] getObjectPosition(String name) {
        SceneGraphNode sgn = getObject(name);
        if (sgn instanceof IFocus) {
            IFocus obj = (IFocus) sgn;
            obj.getAbsolutePosition(name.toLowerCase(), aux3d1);
            return new double[]{aux3d1.x, aux3d1.y, aux3d1.z};
        }
        return null;
    }

    @Override
    public void setGuiScrollPosition(final float pixelY) {
        GaiaSky.postRunnable(() -> em.post(Events.GUI_SCROLL_POSITION_CMD, pixelY));

    }

    public void setGuiScrollPosition(final int pixelY) {
        setGuiScrollPosition((float) pixelY);
    }

    @Override
    public void enableGui() {
        GaiaSky.postRunnable(() -> em.post(Events.DISPLAY_GUI_CMD, true, I18n.bundle.get("notif.cleanmode")));
    }

    @Override
    public void disableGui() {
        GaiaSky.postRunnable(() -> em.post(Events.DISPLAY_GUI_CMD, false, I18n.bundle.get("notif.cleanmode")));
    }

    @Override
    public void displayMessageObject(final int id, final String message, final float x, final float y, final float r, final float g, final float b, final float a, final float fontSize) {
        GaiaSky.postRunnable(() -> em.post(Events.ADD_CUSTOM_MESSAGE, id, message, x, y, r, g, b, a, fontSize));
    }

    @Override
    public void displayMessageObject(final int id, final String message, final double x, final double y, final double[] color, final double fontSize) {
        if (checkNotNull(color, "color") && checkLengths(color, 3, 4, "color")) {
            float a = color.length > 3 ? (float) color[3] : 1f;
            displayMessageObject(id, message, (float) x, (float) y, (float) color[0], (float) color[1], (float) color[2], a, (float) fontSize);
        }
    }

    public void displayMessageObject(final int id, final String message, final double x, final double y, final List color, final double fontSize) {
        displayMessageObject(id, message, x, y, dArray(color), fontSize);
    }

    public void displayMessageObject(final int id, final String message, final float x, final float y, final float r, final float g, final float b, final float a, final int fontSize) {
        displayMessageObject(id, message, x, y, r, g, b, a, (float) fontSize);
    }

    @Override
    public void displayTextObject(final int id, final String text, final float x, final float y, final float maxWidth, final float maxHeight, final float r, final float g, final float b, final float a, final float fontSize) {
        GaiaSky.postRunnable(() -> em.post(Events.ADD_CUSTOM_TEXT, id, text, x, y, maxWidth, maxHeight, r, g, b, a, fontSize));
    }

    public void displayTextObject(final int id, final String text, final float x, final float y, final float maxWidth, final float maxHeight, final float r, final float g, final float b, final float a, final int fontSize) {
        displayTextObject(id, text, x, y, maxWidth, maxHeight, r, g, b, a, (float) fontSize);
    }

    @Override
    public void displayImageObject(final int id, final String path, final float x, final float y, final float r, final float g, final float b, final float a) {
        GaiaSky.postRunnable(() -> {
            Texture tex = getTexture(path);
            em.post(Events.ADD_CUSTOM_IMAGE, id, tex, x, y, r, g, b, a);
        });
    }

    @Override
    public void displayImageObject(final int id, final String path, final double x, final double y, final double[] color) {
        if (checkNotNull(color, "color") && checkLengths(color, 3, 4, "color")) {
            float a = color.length > 3 ? (float) color[3] : 1f;
            displayImageObject(id, path, (float) x, (float) y, (float) color[0], (float) color[1], (float) color[2], a);
        }
    }

    public void displayImageObject(final int id, final String path, final double x, final double y, final List color) {
        displayImageObject(id, path, x, y, dArray(color));
    }

    @Override
    public void displayImageObject(final int id, final String path, final float x, final float y) {
        GaiaSky.postRunnable(() -> {
            Texture tex = getTexture(path);
            em.post(Events.ADD_CUSTOM_IMAGE, id, tex, x, y);
        });
    }

    @Override
    public void removeAllObjects() {
        GaiaSky.postRunnable(() -> em.post(Events.REMOVE_ALL_OBJECTS));
    }

    @Override
    public void removeObject(final int id) {
        GaiaSky.postRunnable(() -> em.post(Events.REMOVE_OBJECTS, new int[]{id}));
    }

    @Override
    public void removeObjects(final int[] ids) {
        GaiaSky.postRunnable(() -> em.post(Events.REMOVE_OBJECTS, ids));
    }

    public void removeObjects(final List ids) {
        removeObjects(iArray(ids));
    }

    @Override
    public void maximizeInterfaceWindow() {
        GaiaSky.postRunnable(() -> em.post(Events.GUI_FOLD_CMD, false));
    }

    @Override
    public void minimizeInterfaceWindow() {
        GaiaSky.postRunnable(() -> em.post(Events.GUI_FOLD_CMD, true));
    }

    @Override
    public void setGuiPosition(final float x, final float y) {
        GaiaSky.postRunnable(() -> em.post(Events.GUI_MOVE_CMD, x, y));
    }

    public void setGuiPosition(final int x, final int y) {
        setGuiPosition((float) x, (float) y);
    }

    public void setGuiPosition(final float x, final int y) {
        setGuiPosition(x, (float) y);
    }

    public void setGuiPosition(final int x, final float y) {
        setGuiPosition((float) x, y);
    }

    @Override
    public void waitForInput() {
        while (inputCode < 0) {
            sleepFrames(1);
        }
        // Consume
        inputCode = -1;

    }

    @Override
    public void waitForEnter() {
        while (inputCode != Keys.ENTER) {
            sleepFrames(1);
        }
        // Consume
        inputCode = -1;
    }

    @Override
    public void waitForInput(int keyCode) {
        while (inputCode != keyCode) {
            sleepFrames(1);
        }
        // Consume
        inputCode = -1;
    }

    private int inputCode = -1;

    @Override
    public int getScreenWidth() {
        return Gdx.graphics.getWidth();
    }

    @Override
    public int getScreenHeight() {
        return Gdx.graphics.getHeight();
    }

    @Override
    public float[] getPositionAndSizeGui(String name) {
        IGui gui = GaiaSky.instance.mainGui;
        Actor actor = gui.getGuiStage().getRoot().findActor(name);
        if (actor != null) {
            float x = actor.getX();
            float y = actor.getY();
            // x and y relative to parent, so we need to add coordinates of
            // parents up to top
            Group parent = actor.getParent();
            while (parent != null) {
                x += parent.getX();
                y += parent.getY();
                parent = parent.getParent();
            }
            return new float[]{x, y, actor.getWidth(), actor.getHeight()};
        } else {
            return null;
        }

    }

    @Override
    public void expandGuiComponent(String name) {
        em.post(Events.EXPAND_PANE_CMD, name);
    }

    @Override
    public void collapseGuiComponent(String name) {
        em.post(Events.COLLAPSE_PANE_CMD, name);
    }

    @Override
    public String getVersionNumber() {
        return GlobalConf.version.version;
    }

    @Override
    public boolean waitFocus(String name, long timeoutMs) {
        long iniTime = TimeUtils.millis();
        NaturalCamera cam = GaiaSky.instance.cam.naturalCamera;
        while (cam.focus == null || !cam.focus.getName().equalsIgnoreCase(name)) {
            sleepFrames(1);
            long spent = TimeUtils.millis() - iniTime;
            if (timeoutMs > 0 && spent > timeoutMs) {
                // Timeout!
                return true;
            }
        }
        return false;
    }

    private Texture getTexture(String path) {
        if (textures == null || !textures.containsKey(path)) {
            preloadTexture(path);
        }
        return textures.get(path);
    }

    @Override
    public void preloadTexture(String path) {
        preloadTextures(new String[]{path});
    }

    @Override
    public String getAssetsLocation() {
        return GlobalConf.ASSETS_LOC;
    }

    @Override
    public void preloadTextures(String[] paths) {
        initializeTextures();
        for (final String path : paths) {
            // This only works in async mode!
            GaiaSky.postRunnable(() -> manager.load(path, Texture.class));
            while (!manager.isLoaded(path)) {
                sleepFrames(1);
            }
            Texture tex = manager.get(path, Texture.class);
            textures.put(path, tex);
        }
    }

    @Override
    public void startRecordingCameraPath() {
        em.post(Events.RECORD_CAMERA_CMD, true);
    }

    @Override
    public void stopRecordingCameraPath() {
        em.post(Events.RECORD_CAMERA_CMD, false);
    }

    @Override
    public void runCameraPath(String file, boolean sync) {
        em.post(Events.PLAY_CAMERA_CMD, file);

        // Wait if needed
        if (sync) {
            Object monitor = new Object();
            IObserver watcher = (event, data) -> {
                switch (event) {
                    case CAMERA_PLAY_INFO:
                        Boolean status = (Boolean) data[0];
                        if (!status) {
                            synchronized (monitor) {
                                monitor.notify();
                            }
                        }
                        break;
                    default:
                        break;
                }
            };
            em.subscribe(watcher, Events.CAMERA_PLAY_INFO);
            // Wait for camera to finish
            synchronized (monitor) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    logger.error(e, "Error waiting for camera file to finish");
                }
            }
        }
    }

    @Override
    public void runCameraPath(String file) {
        runCameraPath(file, false);
    }

    @Override
    public void runCameraRecording(String file) {
        runCameraPath(file, false);
    }

    class CameraTransitionRunnable implements Runnable {
        NaturalCamera cam;
        double seconds;
        double elapsed, start;
        double[] targetPos, targetDir, targetUp;
        Pathd<Vector3d> posl, dirl, upl;

        Runnable end;
        final Object lock;

        Vector3d aux;

        public CameraTransitionRunnable(NaturalCamera cam, double[] pos, double[] dir, double[] up, double seconds, Runnable end) {
            this.cam = cam;
            this.targetPos = pos;
            this.targetDir = dir;
            this.targetUp = up;
            this.seconds = seconds;
            this.start = GaiaSky.instance.getT();
            this.elapsed = 0;
            this.end = end;
            this.lock = new Object();

            // Set up interpolators
            posl = getPathd(cam.getPos(), pos);
            dirl = getPathd(cam.getDirection(), dir);
            upl = getPathd(cam.getUp(), up);

            // Aux
            aux = new Vector3d();
        }

        private Pathd<Vector3d> getPathd(Vector3d p0, double[] p1) {
            Vector3d[] points = new Vector3d[]{new Vector3d(p0), new Vector3d(p1[0], p1[1], p1[2])};
            return new Lineard<>(points);
        }

        @Override
        public void run() {
            // Update elapsed time
            elapsed = GaiaSky.instance.getT() - start;

            // Interpolation variable
            double alpha = MathUtilsd.clamp(elapsed / seconds, 0.0, 0.99999999999999);

            // Set camera state
            cam.setPos(posl.valueAt(aux, alpha));
            cam.setDirection(dirl.valueAt(aux, alpha));
            cam.setUp(upl.valueAt(aux, alpha));

            // Finish if needed
            if (elapsed >= seconds) {
                // On end, run runnable if present, otherwise notify lock
                if (end != null) {
                    end.run();
                } else {
                    synchronized (lock) {
                        lock.notify();
                    }
                }
            }
        }
    }

    @Override
    public void cameraTransition(double[] camPos, double[] camDir, double[] camUp, double seconds) {
        cameraTransition(camPos, camDir, camUp, seconds, true);
    }

    public void cameraTransition(double[] camPos, double[] camDir, double[] camUp, long seconds) {
        cameraTransition(camPos, camDir, camUp, (double) seconds);
    }

    public void cameraTransition(List camPos, List camDir, List camUp, double seconds) {
        cameraTransition(dArray(camPos), dArray(camDir), dArray(camUp), seconds);
    }

    public void cameraTransition(List camPos, List camDir, List camUp, long seconds) {
        cameraTransition(camPos, camDir, camUp, (double) seconds);
    }

    private int cTransSeq = 0;

    @Override
    public void cameraTransition(double[] camPos, double[] camDir, double[] camUp, double seconds, boolean sync) {
        NaturalCamera cam = GaiaSky.instance.cam.naturalCamera;

        // Put in focus mode
        em.post(Events.CAMERA_MODE_CMD, CameraMode.FREE_MODE);

        // Set up final actions
        String name = "cameraTransition" + (cTransSeq++);
        Runnable end = null;
        if (!sync)
            end = () -> unparkRunnable(name);

        // Create and park runnable
        CameraTransitionRunnable r = new CameraTransitionRunnable(cam, camPos, camDir, camUp, seconds, end);
        parkRunnable(name, r);

        if (sync) {
            // Wait on lock
            synchronized (r.lock) {
                try {
                    r.lock.wait();
                } catch (InterruptedException e) {
                    logger.error(e);
                }
            }

            // Unpark and return
            unparkRunnable(name);
        }
    }

    public void cameraTransition(List camPos, List camDir, List camUp, double seconds, boolean sync) {
        cameraTransition(dArray(camPos), dArray(camDir), dArray(camUp), seconds, sync);
    }

    public void cameraTransition(List camPos, List camDir, List camUp, long seconds, boolean sync) {
        cameraTransition(camPos, camDir, camUp, (double) seconds, sync);
    }

    @Override
    public void sleep(float seconds) {
        if (this.isFrameOutputActive()) {
            this.sleepFrames(Math.round(this.getFrameOutputFps() * seconds));
        } else {
            try {
                Thread.sleep(Math.round(seconds * 1000));
            } catch (InterruptedException e) {
                logger.error(e);
            }
        }

    }

    public void sleep(int seconds) {
        sleep((float) seconds);
    }

    @Override
    public void sleepFrames(long frames) {
        long frameCount = 0;
        while (frameCount < frames) {
            try {
                synchronized (frameMonitor) {
                    frameMonitor.wait();
                }
                frameCount++;
            } catch (InterruptedException e) {
                logger.error("Error while waiting on frameMonitor", e);
            }
        }
    }

    /**
     * Checks if the object is the current focus of the given camera. If it is not,
     * it sets it as focus and waits if necessary.
     *
     * @param object          The new focus object.
     * @param cam             The current camera.
     * @param waitTimeSeconds Max time to wait for the camera to face the focus, in
     *                        seconds. If negative, we wait until the end.
     */
    private void changeFocus(IFocus object, NaturalCamera cam, double waitTimeSeconds) {
        // Post focus change and wait, if needed
        IFocus currentFocus = cam.getFocus();
        if (currentFocus != object) {
            em.post(Events.CAMERA_MODE_CMD, CameraMode.FOCUS_MODE);
            em.post(Events.FOCUS_CHANGE_CMD, object);

            // Wait til camera is facing focus or
            if (waitTimeSeconds < 0) {
                waitTimeSeconds = Double.MAX_VALUE;
            }
            long start = System.currentTimeMillis();
            double elapsedSeconds = 0;
            while (!cam.facingFocus && elapsedSeconds < waitTimeSeconds) {
                // Wait
                try {
                    sleepFrames(1);
                } catch (Exception e) {
                    logger.error(e);
                }
                elapsedSeconds = (System.currentTimeMillis() - start) / 1000d;
            }
        }
    }

    @Override
    public double[] galacticToInternalCartesian(double l, double b, double r) {
        Vector3d pos = Coordinates.sphericalToCartesian(l * Nature.TO_RAD, b * Nature.TO_RAD, r * Constants.KM_TO_U, new Vector3d());
        pos.mul(Coordinates.galacticToEquatorial());
        return new double[]{pos.x, pos.y, pos.z};
    }

    @Override
    public double[] eclipticToInternalCartesian(double l, double b, double r) {
        Vector3d pos = Coordinates.sphericalToCartesian(l * Nature.TO_RAD, b * Nature.TO_RAD, r * Constants.KM_TO_U, new Vector3d());
        pos.mul(Coordinates.eclipticToEquatorial());
        return new double[]{pos.x, pos.y, pos.z};
    }

    @Override
    public double[] equatorialToInternalCartesian(double ra, double dec, double r) {
        Vector3d pos = Coordinates.sphericalToCartesian(ra * Nature.TO_RAD, dec * Nature.TO_RAD, r * Constants.KM_TO_U, new Vector3d());
        return new double[]{pos.x, pos.y, pos.z};
    }

    public double[] internalCartesianToEquatorial(double x, double y, double z) {
        Vector3d in = aux3d1.set(x, y, z);
        Vector3d out = aux3d2;
        Coordinates.cartesianToSpherical(in, out);
        return new double[]{out.x * Nature.TO_DEG, out.y * Nature.TO_DEG, in.len()};
    }

    @Override
    public double[] equatorialCartesianToInternalCartesian(double[] eq, double kmFactor) {
        aux3d1.set(eq).scl(kmFactor).scl(Constants.KM_TO_U);
        return new double[]{aux3d1.y, aux3d1.z, aux3d1.x};
    }

    public double[] equatorialCartesianToInternalCartesian(final List eq, double kmFactor) {
        return equatorialCartesianToInternalCartesian(dArray(eq), kmFactor);
    }

    @Override
    public double[] equatorialToGalactic(double[] eq) {
        aux3d1.set(eq).mul(Coordinates.eqToGal());
        return aux3d1.values();
    }

    public double[] equatorialToGalactic(List eq) {
        return equatorialToGalactic(dArray(eq));
    }

    @Override
    public double[] equatorialToEcliptic(double[] eq) {
        aux3d1.set(eq).mul(Coordinates.eqToEcl());
        return aux3d1.values();
    }

    public double[] equatorialToEcliptic(List eq) {
        return equatorialToEcliptic(dArray(eq));
    }

    @Override
    public double[] galacticToEquatorial(double[] gal) {
        aux3d1.set(gal).mul(Coordinates.galToEq());
        return aux3d1.values();
    }

    public double[] galacticToEquatorial(List gal) {
        return galacticToEquatorial(dArray(gal));
    }

    @Override
    public double[] eclipticToEquatorial(double[] ecl) {
        aux3d1.set(ecl).mul(Coordinates.eclToEq());
        return aux3d1.values();
    }

    public double[] eclipticToEquatorial(List ecl) {
        return eclipticToEquatorial(dArray(ecl));
    }

    @Override
    public void setBrightnessLevel(double level) {
        if (checkNum(level, -1d, 1d, "brightness"))
            GaiaSky.postRunnable(() -> em.post(Events.BRIGHTNESS_CMD, (float) level, false));
    }

    public void setBrightnessLevel(long level) {
        setBrightnessLevel((double) level);
    }

    @Override
    public void setContrastLevel(double level) {
        if (checkNum(level, 0d, 2d, "contrast"))
            GaiaSky.postRunnable(() -> em.post(Events.CONTRAST_CMD, (float) level, false));
    }

    public void setContrastLevel(long level) {
        setContrastLevel((double) level);
    }

    @Override
    public void setHueLevel(double level) {
        if (checkNum(level, 0d, 2d, "hue"))
            GaiaSky.postRunnable(() -> em.post(Events.HUE_CMD, (float) level, false));
    }

    public void setHueLevel(long level) {
        setHueLevel((double) level);
    }

    @Override
    public void setSaturationLevel(double level) {
        if (checkNum(level, 0d, 2d, "saturation"))
            GaiaSky.postRunnable(() -> em.post(Events.SATURATION_CMD, (float) level, false));
    }

    public void setSaturationLevel(long level) {
        setSaturationLevel((double) level);
    }

    @Override
    public void setGammaCorrectionLevel(double level) {
        if (checkNum(level, 0d, 3d, "gamma correction"))
            GaiaSky.postRunnable(() -> em.post(Events.GAMMA_CMD, (float) level, false));
    }

    public void setGammaCorrectionLevel(long level) {
        setGammaCorrectionLevel((double) level);
    }

    @Override
    public void setHDRToneMappingType(String type) {
        if (checkString(type, new String[]{"auto", "AUTO", "exposure", "EXPOSURE", "none", "NONE"}, "tone mapping type"))
            GaiaSky.postRunnable(() -> em.post(Events.TONEMAPPING_TYPE_CMD, GlobalConf.PostprocessConf.ToneMapping.valueOf(type.toUpperCase()), false));
    }

    @Override
    public void setExposureToneMappingLevel(double level) {
        if (checkNum(level, 0d, 20d, "exposure"))
            GaiaSky.postRunnable(() -> em.post(Events.EXPOSURE_CMD, (float) level, false));
    }

    public void setExposureToneMappingLevel(long level) {
        setExposureToneMappingLevel((double) level);
    }

    @Override
    public void setCubemapMode(boolean state, String projection) {
        CubemapProjections.CubemapProjection newProj = CubemapProjections.CubemapProjection.valueOf(projection.toUpperCase());
        GaiaSky.postRunnable(() -> em.post(Events.CUBEMAP_CMD, state, newProj, false));
    }

    @Override
    public void setPanoramaMode(boolean state) {
        GaiaSky.postRunnable(() -> em.post(Events.CUBEMAP_CMD, state, CubemapProjections.CubemapProjection.EQUIRECTANGULAR, false));
    }

    @Override
    public void setPlanetariumMode(boolean state) {
        GaiaSky.postRunnable(() -> em.post(Events.CUBEMAP_CMD, state, CubemapProjections.CubemapProjection.FISHEYE, false));
    }

    @Override
    public void setCubemapResolution(int resolution) {
        if (checkNum(resolution, 20, 15000, "resolution")) {
            GaiaSky.postRunnable(() -> em.post(Events.CUBEMAP_RESOLUTION_CMD, resolution));
        }
    }

    @Override
    public void setCubemapProjection(String projection) {
        if (checkStringEnum(projection, CubemapProjections.CubemapProjection.class, "projection")) {
            CubemapProjections.CubemapProjection newProj = CubemapProjections.CubemapProjection.valueOf(projection.toUpperCase());
            em.post(Events.CUBEMAP_PROJECTION_CMD, newProj);
        }
    }

    @Override
    public void setStereoscopicMode(boolean state) {
        GaiaSky.postRunnable(() -> em.post(Events.STEREOSCOPIC_CMD, state, false));
    }

    @Override
    public void setStereoscopicProfile(int index) {
        GaiaSky.postRunnable(() -> em.post(Events.STEREO_PROFILE_CMD, index));
    }

    @Override
    public long getCurrentFrameNumber() {
        return GaiaSky.instance.frames;
    }

    @Override
    public void setLensFlare(boolean state) {
        GaiaSky.postRunnable(() -> em.post(Events.LENS_FLARE_CMD, state, false));
    }

    @Override
    public void setMotionBlur(boolean state) {
        GaiaSky.postRunnable(() -> em.post(Events.MOTION_BLUR_CMD, state, false));
    }

    @Override
    public void setStarGlow(boolean state) {
        GaiaSky.postRunnable(() -> em.post(Events.LIGHT_SCATTERING_CMD, state, false));
    }

    @Override
    public void setBloom(float value) {
        if (checkNum(value, 0f, 1f, "bloom"))
            GaiaSky.postRunnable(() -> em.post(Events.BLOOM_CMD, value, false));
    }

    public void setBloom(int level) {
        setBloom((float) level);
    }

    @Override
    public void setSmoothLodTransitions(boolean value) {
        GaiaSky.postRunnable(() -> em.post(Events.OCTREE_PARTICLE_FADE_CMD, I18n.bundle.get("element.octreeparticlefade"), value));
    }

    @Override
    public double[] rotate3(double[] vector, double[] axis, double angle) {
        Vector3d v = aux3d1.set(vector);
        Vector3d a = aux3d2.set(axis);
        return v.rotate(a, angle).values();
    }

    public double[] rotate3(double[] vector, double[] axis, long angle) {
        return rotate3(vector, axis, (double) angle);
    }

    public double[] rotate3(List vector, List axis, double angle) {
        return rotate3(dArray(vector), dArray(axis), angle);
    }

    public double[] rotate3(List vector, List axis, long angle) {
        return rotate3(vector, axis, (double) angle);
    }

    @Override
    public double[] rotate2(double[] vector, double angle) {
        Vector2d v = aux2d1.set(vector);
        return v.rotate(angle).values();
    }

    public double[] rotate2(double[] vector, long angle) {
        return rotate2(vector, (double) angle);
    }

    public double[] rotate2(List vector, double angle) {
        return rotate2(dArray(vector), angle);
    }

    public double[] rotate2(List vector, long angle) {
        return rotate2(vector, (double) angle);
    }

    @Override
    public double[] cross3(double[] vec1, double[] vec2) {
        return aux3d1.set(vec1).crs(aux3d2.set(vec2)).values();
    }

    public double[] cross3(List vec1, List vec2) {
        return cross3(dArray(vec1), dArray(vec2));
    }

    @Override
    public double dot3(double[] vec1, double[] vec2) {
        return aux3d1.set(vec1).dot(aux3d2.set(vec2));
    }

    public double dot3(List vec1, List vec2) {
        return dot3(dArray(vec1), dArray(vec2));
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color) {
        addPolyline(name, points, color, 1f);
    }

    public void addPolyline(String name, List points, List color) {
        addPolyline(name, points, color, 1f);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color, double lineWidth) {
        addPolyline(name, points, color, lineWidth, GL20.GL_LINE_STRIP);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color, double lineWidth, int primitive) {
        if (checkString(name, "name") && checkNum(lineWidth, 0.1f, 50f, "lineWidth") && checkNum(primitive, 1, 3, "primitive")) {
            Polyline pl = new Polyline(primitive);
            pl.setCt("Others");
            pl.setColor(color);
            pl.setName(name);
            pl.setPoints(points);
            pl.setPrimitiveSize((float) lineWidth);
            pl.setParent("Universe");
            pl.initialize();

            em.post(Events.SCENE_GRAPH_ADD_OBJECT_CMD, pl, true);
        }

    }

    public void addPolyline(String name, double[] points, double[] color, int lineWidth) {
        addPolyline(name, points, color, (float) lineWidth);
    }

    public void addPolyline(String name, double[] points, double[] color, int lineWidth, int primitive) {
        addPolyline(name, points, color, (float) lineWidth, primitive);
    }

    public void addPolyline(String name, List points, List color, float lineWidth) {
        addPolyline(name, dArray(points), dArray(color), lineWidth);
    }

    public void addPolyline(String name, List points, List color, float lineWidth, int primitive) {
        addPolyline(name, dArray(points), dArray(color), lineWidth, primitive);
    }

    public void addPolyline(String name, List points, List color, int lineWidth) {
        addPolyline(name, points, color, (float) lineWidth);
    }

    public void addPolyline(String name, List points, List color, int lineWidth, int primitive) {
        addPolyline(name, points, color, (float) lineWidth, primitive);
    }

    @Override
    public void removeModelObject(String name) {
        if (checkString(name, "name"))
            em.post(Events.SCENE_GRAPH_REMOVE_OBJECT_CMD, name, true);
    }

    @Override
    public void postRunnable(Runnable runnable) {
        GaiaSky.postRunnable(runnable);
    }

    @Override
    public void parkRunnable(String id, Runnable runnable) {
        if (checkString(id, "id"))
            em.post(Events.PARK_POST_RUNNABLE, id, runnable);
    }

    @Override
    public void unparkRunnable(String id) {
        if (checkString(id, "id"))
            em.post(Events.UNPARK_POST_RUNNABLE, id);
    }

    @Override
    public void setCameraState(double[] pos, double[] dir, double[] up) {
        GaiaSky.postRunnable(() -> {
            em.post(Events.CAMERA_POS_CMD, pos);
            em.post(Events.CAMERA_DIR_CMD, dir);
            em.post(Events.CAMERA_UP_CMD, up);
        });
    }

    public void setCameraState(List pos, List dir, List up) {
        setCameraState(dArray(pos), dArray(dir), dArray(up));
    }

    @Override
    public void setCameraStateAndTime(double[] pos, double[] dir, double[] up, long time) {
        GaiaSky.postRunnable(() -> {
            em.post(Events.CAMERA_PROJECTION_CMD, pos, dir, up);
            em.post(Events.TIME_CHANGE_CMD, Instant.ofEpochMilli(time));
        });
    }

    public void setCameraStateAndTime(List pos, List dir, List up, long time) {
        setCameraStateAndTime(dArray(pos), dArray(dir), dArray(up), time);
    }

    @Override
    public void resetImageSequenceNumber() {
        ImageRenderer.resetSequenceNumber();
    }

    @Override
    public boolean loadDataset(String dsName, String absolutePath) {
        return loadDataset(dsName, absolutePath, CatalogInfoType.SCRIPT, true);
    }

    @Override
    public boolean loadDataset(String dsName, String path, boolean sync) {
        return loadDataset(dsName, path, CatalogInfoType.SCRIPT, sync);
    }

    public boolean loadDataset(String dsName, String path, CatalogInfoType type, boolean sync) {
        if (sync) {
            return loadDatasetImmediate(dsName, path, type, true);
        } else {
            Thread t = new Thread(() -> loadDatasetImmediate(dsName, path, type, false));
            t.start();
            return true;
        }
    }

    public boolean loadDataset(String dsName, String path, CatalogInfoType type, DatasetOptions dops, boolean sync) {
        if (sync) {
            return loadDatasetImmediate(dsName, path, type, dops, true);
        } else {
            Thread t = new Thread(() -> loadDatasetImmediate(dsName, path, type, dops, false));
            t.start();
            return true;
        }
    }

    public boolean loadDataset(String dsName, DataSource ds, CatalogInfoType type, DatasetOptions dops, boolean sync) {
        if (sync) {
            return loadDatasetImmediate(dsName, ds, type, dops, true);
        } else {
            Thread t = new Thread(() -> loadDatasetImmediate(dsName, ds, type, dops, false));
            t.start();
            return true;
        }
    }

    @Override
    public boolean loadStarDataset(String dsName, String path, double magnitudeScale, double[] labelColor, double[] fadeIn, double[] fadeOut, boolean sync) {
        return loadStarDataset(dsName, path, CatalogInfoType.SCRIPT, magnitudeScale, labelColor, fadeIn, fadeOut, sync);
    }

    public boolean loadStarDataset(String dsName, String path, double magnitudeScale, final List labelColor, final List fadeIn, final List fadeOut, boolean sync) {
        return loadStarDataset(dsName, path, magnitudeScale, dArray(labelColor), dArray(fadeIn), dArray(fadeOut), sync);
    }

    public boolean loadStarDataset(String dsName, String path, CatalogInfoType type, double magnitudeScale, double[] labelColor, double[] fadeIn, double[] fadeOut, boolean sync) {
        DatasetOptions dops = DatasetOptions.getStarDatasetOptions(magnitudeScale, labelColor, fadeIn, fadeOut);
        return loadDataset(dsName, path, type, dops, sync);
    }

    @Override
    public boolean loadParticleDataset(String dsName, String path, double profileDecay, double[] particleColor, double colorNoise, double[] labelColor, double particleSize, String ct, double[] fadeIn, double[] fadeOut, boolean sync) {
        return loadParticleDataset(dsName, path, profileDecay, particleColor, colorNoise, labelColor, particleSize, new double[]{1.5d, 100d}, ct, fadeIn, fadeOut, sync);
    }

    public boolean loadParticleDataset(String dsName, String path, double profileDecay, double[] particleColor, double colorNoise, double[] labelColor, double particleSize, double[] sizeLimits, String ct, double[] fadeIn, double[] fadeOut, boolean sync) {
        ComponentType compType = ComponentType.valueOf(ct);
        return loadParticleDataset(dsName, path, profileDecay, particleColor, colorNoise, labelColor, particleSize, sizeLimits, compType, fadeIn, fadeOut, sync);
    }

    public boolean loadParticleDataset(String dsName, String path, double profileDecay, final List particleColor, double colorNoise, final List labelColor, double particleSize, String ct, final List fadeIn, final List fadeOut, boolean sync) {
        return loadParticleDataset(dsName, path, profileDecay, dArray(particleColor), colorNoise, dArray(labelColor), particleSize, ct, dArray(fadeIn), dArray(fadeOut), sync);
    }

    public boolean loadParticleDataset(String dsName, String path, double profileDecay, double[] particleColor, double colorNoise, double[] labelColor, double particleSize, double[] sizeLimits, ComponentType ct, double[] fadeIn, double[] fadeOut, boolean sync) {
        return loadParticleDataset(dsName, path, CatalogInfoType.SCRIPT, profileDecay, particleColor, colorNoise, labelColor, particleSize, sizeLimits, ct, fadeIn, fadeOut, sync);
    }

    public boolean loadParticleDataset(String dsName, String path, CatalogInfoType type, double profileDecay, double[] particleColor, double colorNoise, double[] labelColor, double particleSize, double[] sizeLimits, ComponentType ct, double[] fadeIn, double[] fadeOut, boolean sync) {
        DatasetOptions dops = DatasetOptions.getParticleDatasetOptions(profileDecay, particleColor, colorNoise, labelColor, particleSize, sizeLimits, ct, fadeIn, fadeOut);
        return loadDataset(dsName, path, type, dops, sync);
    }

    @Override
    public boolean loadStarClusterDataset(String dsName, String path, double[] particleColor, double[] fadeIn, double[] fadeOut, boolean sync) {
        return loadStarClusterDataset(dsName, path, particleColor, ComponentType.Clusters.toString(), fadeIn, fadeOut, sync);
    }

    public boolean loadStarClusterDataset(String dsName, String path, List particleColor, List fadeIn, List fadeOut, boolean sync) {
        return loadStarClusterDataset(dsName, path, dArray(particleColor), dArray(fadeIn), dArray(fadeOut), sync);
    }

    @Override
    public boolean loadStarClusterDataset(String dsName, String path, double[] particleColor, double[] labelColor, double[] fadeIn, double[] fadeOut, boolean sync) {
        return loadStarClusterDataset(dsName, path, particleColor, labelColor, ComponentType.Clusters.toString(), fadeIn, fadeOut, sync);
    }

    public boolean loadStarClusterDataset(String dsName, String path, List particleColor, List labelColor, List fadeIn, List fadeOut, boolean sync) {
        return loadStarClusterDataset(dsName, path, dArray(particleColor), dArray(labelColor), dArray(fadeIn), dArray(fadeOut), sync);
    }


    @Override
    public boolean loadStarClusterDataset(String dsName, String path, double[] particleColor, String ct, double[] fadeIn, double[] fadeOut, boolean sync) {
        ComponentType compType = ComponentType.valueOf(ct);
        DatasetOptions dops = DatasetOptions.getStarClusterDatasetOptions(dsName, particleColor, particleColor.clone(), compType, fadeIn, fadeOut);
        return loadDataset(dsName, path, CatalogInfoType.SCRIPT, dops, sync);
    }

    public boolean loadStarClusterDataset(String dsName, String path, List particleColor, String ct, List fadeIn, List fadeOut, boolean sync) {
        return loadStarClusterDataset(dsName, path, dArray(particleColor), ct, dArray(fadeIn), dArray(fadeOut), sync);
    }

    @Override
    public boolean loadStarClusterDataset(String dsName, String path, double[] particleColor, double[] labelColor, String ct, double[] fadeIn, double[] fadeOut, boolean sync) {
        ComponentType compType = ComponentType.valueOf(ct);
        DatasetOptions dops = DatasetOptions.getStarClusterDatasetOptions(dsName, particleColor, labelColor, compType, fadeIn, fadeOut);
        return loadDataset(dsName, path, CatalogInfoType.SCRIPT, dops, sync);
    }

    public boolean loadStarClusterDataset(String dsName, String path, List particleColor, List labelColor, String ct, List fadeIn, List fadeOut, boolean sync) {
        return loadStarClusterDataset(dsName, path, dArray(particleColor), dArray(labelColor), ct, dArray(fadeIn), dArray(fadeOut), sync);
    }

    private boolean loadDatasetImmediate(String dsName, String path, CatalogInfoType type, boolean sync) {
        return loadDatasetImmediate(dsName, path, type, null, sync);
    }

    private boolean loadDatasetImmediate(String dsName, String path, CatalogInfoType type, DatasetOptions dops, boolean sync) {
        Path p = Paths.get(path);
        if (Files.exists(p) && Files.isReadable(p)) {
            try {
                return loadDatasetImmediate(dsName, new FileDataSource(p.toFile()), type, dops, sync);
            } catch (Exception e) {
                logger.error("Error loading file: " + p, e);
            }
        } else {
            logger.error("Can't read file: " + path);
        }
        return false;
    }

    private Array<ParticleBean> loadParticleBeans(DataSource ds, DatasetOptions dops) {
        STILDataProvider provider = new STILDataProvider();
        provider.setDatasetOptions(dops);
        @SuppressWarnings("unchecked") Array<ParticleBean> data = (Array<ParticleBean>) provider.loadData(ds, 1.0f);
        return data;
    }

    private boolean loadDatasetImmediate(String dsName, DataSource ds, CatalogInfoType type, DatasetOptions dops, boolean sync) {
        try {
            logger.info(I18n.txt("notif.catalog.loading", dsName));

            // Create star/particle group or star clusters
            if (checkString(dsName, "datasetName")) {
                if (dops == null || dops.type == DatasetOptions.DatasetLoadType.STARS) {
                    Array<ParticleBean> data = loadParticleBeans(ds, dops);
                    if (data != null && !data.isEmpty()) {
                        // STAR GROUP
                        AtomicReference<StarGroup> starGroup = new AtomicReference<>();
                        GaiaSky.postRunnable(() -> {
                            starGroup.set(StarGroup.getStarGroup(dsName, data, dops));

                            // Catalog info
                            CatalogInfo ci = new CatalogInfo(dsName, ds.getName(), null, type, 1.5f, starGroup.get());
                            EventManager.instance.post(Events.CATALOG_ADD, ci, true);

                            logger.info(data.size + " stars loaded");
                        });
                        // Sync waiting until the node is in the scene graph
                        while (sync && (starGroup.get() == null || !starGroup.get().inSceneGraph)) {
                            sleepFrames(1);
                        }
                    }
                } else if (dops == null || dops.type == DatasetOptions.DatasetLoadType.PARTICLES) {
                    // PARTICLE GROUP
                    Array<ParticleBean> data = loadParticleBeans(ds, dops);
                    if (data != null && !data.isEmpty()) {
                        AtomicReference<ParticleGroup> particleGroup = new AtomicReference<>();
                        GaiaSky.postRunnable(() -> {
                            particleGroup.set(ParticleGroup.getParticleGroup(dsName, data, dops));

                            // Catalog info
                            CatalogInfo ci = new CatalogInfo(dsName, ds.getName(), null, type, 1.5f, particleGroup.get());
                            EventManager.instance.post(Events.CATALOG_ADD, ci, true);

                            logger.info(data.size + " particles loaded");
                        });
                        // Sync waiting until the node is in the scene graph
                        while (sync && (particleGroup.get() == null || !particleGroup.get().inSceneGraph)) {
                            sleepFrames(1);
                        }
                    }
                } else if (dops == null || dops.type == DatasetOptions.DatasetLoadType.CLUSTERS) {
                    // STAR CLUSTERS
                    if (ds instanceof FileDataSource) {
                        FileDataSource fds = (FileDataSource) ds;
                        GenericCatalog scc = new GenericCatalog();
                        scc.setName(dops.catalogName);
                        scc.setDescription(dsName);
                        scc.setParent("Universe");
                        scc.setFadein(dops.fadeIn);
                        scc.setFadeout(dops.fadeOut);
                        scc.setColor(dops.particleColor);
                        scc.setLabelcolor(dops.labelColor);
                        scc.setCt(dops.ct.toString());
                        scc.setPosition(new double[]{0,0,0});

                        scc.setDatafile(fds.getFile().getAbsolutePath());
                        scc.setProvider(StarClusterLoader.class.getName());

                        GaiaSky.postRunnable(() -> {
                            scc.initialize();
                            SceneGraphNode.insert(scc, true);
                            scc.doneLoading(manager);
                            logger.info(scc.children.size + " star clusters loaded");
                        });
                        // Sync waiting until the node is in the scene graph
                        while (sync && (!scc.inSceneGraph)) {
                            sleepFrames(1);
                        }
                    } else {
                        logger.error("Only file data sources supported for star clusters");
                    }
                }
                // One extra flush frame
                sleepFrames(1);
                return true;
            } else {
                // No data has been loaded
                return false;
            }
        } catch (Exception e) {
            logger.error(e);
            return false;
        }
    }

    @Override
    public boolean hasDataset(String dsName) {
        if (checkString(dsName, "datasetName")) {
            return CatalogManager.instance().contains(dsName);
        }
        return false;
    }

    @Override
    public boolean removeDataset(String dsName) {
        if (checkString(dsName, "datasetName")) {
            boolean exists = CatalogManager.instance().contains(dsName);
            if (exists)
                GaiaSky.postRunnable(() -> EventManager.instance.post(Events.CATALOG_REMOVE, dsName));
            else
                logger.warn("Dataset with name " + dsName + " does not exist");
            return exists;
        }
        return false;
    }

    @Override
    public boolean hideDataset(String dsName) {
        if (checkString(dsName, "datasetName")) {
            boolean exists = CatalogManager.instance().contains(dsName);
            if (exists)
                EventManager.instance.post(Events.CATALOG_VISIBLE, dsName, false);
            else
                logger.warn("Dataset with name " + dsName + " does not exist");
            return exists;
        }
        return false;
    }

    @Override
    public boolean showDataset(String dsName) {
        if (checkString(dsName, "datasetName")) {
            boolean exists = CatalogManager.instance().contains(dsName);
            if (exists)
                EventManager.instance.post(Events.CATALOG_VISIBLE, dsName, true);
            else
                logger.warn("Dataset with name " + dsName + " does not exist");
            return exists;
        }
        return false;
    }

    @Override
    public boolean highlightDataset(String dsName, boolean highlight) {
        if (checkString(dsName, "datasetName")) {
            boolean exists = CatalogManager.instance().contains(dsName);
            if (exists) {
                CatalogInfo ci = CatalogManager.instance().get(dsName);
                EventManager.instance.post(Events.CATALOG_HIGHLIGHT, ci, highlight, false);
            } else {
                logger.warn("Dataset with name " + dsName + " does not exist");
            }
            return exists;
        }
        return false;
    }

    @Override
    public boolean highlightDataset(String dsName, int colorIndex, boolean highlight) {
        float[] color = ColourUtils.getColorFromIndex(colorIndex);
        return highlightDataset(dsName, color[0], color[1], color[2], color[3], highlight);
    }

    @Override
    public boolean highlightDataset(String dsName, float r, float g, float b, float a, boolean highlight) {
        if (checkString(dsName, "datasetName")) {
            boolean exists = CatalogManager.instance().contains(dsName);
            if (exists) {
                CatalogInfo ci = CatalogManager.instance().get(dsName);
                ci.plainColor = true;
                ci.hlColor[0] = r;
                ci.hlColor[1] = g;
                ci.hlColor[2] = b;
                ci.hlColor[3] = a;
                EventManager.instance.post(Events.CATALOG_HIGHLIGHT, ci, highlight, false);
            } else {
                logger.warn("Dataset with name " + dsName + " does not exist");
            }
            return exists;
        }
        return false;
    }

    @Override
    public boolean highlightDataset(String dsName, String attributeName, String colorMap, double minMap, double maxMap, boolean highlight) {
        if (checkString(dsName, "datasetName")) {
            boolean exists = CatalogManager.instance().contains(dsName);
            if (exists) {
                CatalogInfo ci = CatalogManager.instance().get(dsName);
                IAttribute attribute = getAttributeByName(attributeName, ci);
                int cmapIndex = getCmapIndexByName(colorMap);
                if (attribute != null && cmapIndex >= 0) {
                    ci.plainColor = false;
                    ci.hlCmapIndex = cmapIndex;
                    ci.hlCmapMin = minMap;
                    ci.hlCmapMax = maxMap;
                    ci.hlCmapAttribute = attribute;
                    EventManager.instance.post(Events.CATALOG_HIGHLIGHT, ci, highlight, false);
                } else {
                    if (attribute == null)
                        logger.error("Could not find attribute with name '" + attributeName + "'");
                    if (cmapIndex < 0)
                        logger.error("Could not find color map with name '" + colorMap + "'");
                }
            } else {
                logger.warn("Dataset with name " + dsName + " does not exist");
            }
            return exists;
        }
        return false;
    }

    private int getCmapIndexByName(String name) {
        for (Pair<String, Integer> cmap : ColormapPicker.cmapList) {
            if (name.equalsIgnoreCase(cmap.getFirst()))
                return cmap.getSecond();
        }
        return -1;
    }

    private IAttribute getAttributeByName(String name, CatalogInfo ci) {
        try {
            // One of the default attributes
            Class<?> clazz = Class.forName("gaiasky.util.filter.attrib.Attribute" + name);
            Constructor<?> ctor = clazz.getConstructor();
            return (IAttribute) ctor.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            // Try extra attributes
            if (ci.object instanceof ParticleGroup) {
                ParticleGroup pg = (ParticleGroup) ci.object;
                Set<UCD> ucds = pg.get(0).extra.keySet();
                for (UCD ucd : ucds)
                    if (ucd.colname.equalsIgnoreCase(name))
                        return new AttributeUCD(ucd);
            }
        }
        return null;
    }

    @Override
    public boolean setDatasetHighlightSizeFactor(String dsName, float sizeFactor) {
        if (checkString(dsName, "datasetName") && checkNum(sizeFactor, Constants.MIN_DATASET_SIZE_FACTOR, Constants.MAX_DATASET_SIZE_FACTOR, "sizeFactor")) {

            boolean exists = CatalogManager.instance().contains(dsName);
            if (exists) {
                CatalogInfo ci = CatalogManager.instance().get(dsName);
                ci.setHlSizeFactor(sizeFactor);
            } else {
                logger.warn("Dataset with name " + dsName + " does not exist");
            }
            return exists;
        }
        return false;
    }

    @Override
    public double getMeterToInternalUnitConversion() {
        return Constants.M_TO_U;
    }

    @Override
    public double getInternalUnitToMeterConversion() {
        return Constants.U_TO_M;
    }

    @Override
    public double internalUnitsToMetres(double internalUnits) {
        return internalUnits * Constants.U_TO_M;
    }

    @Override
    public double internalUnitsToKilometres(double internalUnits) {
        return internalUnits * Constants.U_TO_KM;
    }

    @Override
    public double metresToInternalUnits(double metres) {
        return metres * Constants.M_TO_U;
    }

    @Override
    public double kilometrestointernalunits(double kilometres) {
        return kilometres * Constants.KM_TO_U;
    }

    @Override
    public List<String> listDatasets() {
        Set<String> names = CatalogManager.instance().getDatasetNames();
        if (names != null)
            return new ArrayList<>(names);
        else
            return new ArrayList<>();
    }

    @Override
    public long getFrameNumber() {
        return frameNumber;
    }

    @Override
    public String getDefaultFramesDir() {
        return SysUtils.getDefaultFramesDir().toAbsolutePath().toString();
    }

    @Override
    public String getDefaultScreenshotsDir() {
        return SysUtils.getDefaultScreenshotsDir().toAbsolutePath().toString();
    }

    @Override
    public String getDefaultCameraDir() {
        return SysUtils.getDefaultCameraDir().toAbsolutePath().toString();
    }

    @Override
    public String getDefaultMusicDir() {
        return SysUtils.getDefaultMusicDir().toAbsolutePath().toString();
    }

    @Override
    public String getDefaultMappingsDir() {
        return SysUtils.getDefaultMappingsDir().toAbsolutePath().toString();
    }

    @Override
    public String getDataDir() {
        return SysUtils.getDataDir().toAbsolutePath().toString();
    }

    @Override
    public String getConfigDir() {
        return SysUtils.getConfigDir().toAbsolutePath().toString();
    }

    @Override
    public String getLocalDataDir() {
        return SysUtils.getLocalDataDir().toAbsolutePath().toString();
    }

    @Override
    public void print(String message) {
        logger.info(message);
    }

    @Override
    public void log(String message) {
        logger.info(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void quit() {
        Gdx.app.exit();
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
            case INPUT_EVENT:
                inputCode = (Integer) data[0];
                break;
            case FRAME_TICK:
                // New frame
                frameNumber = (Long) data[0];
                synchronized (frameMonitor) {
                    frameMonitor.notify();
                }
                break;
            case DISPOSE:
                // Stop all
                for (AtomicBoolean stop : stops) {
                    if (stop != null)
                        stop.set(true);
                }
                break;
            default:
                break;
        }

    }

    private boolean checkNum(int value, int min, int max, String name) {
        if (value < min || value > max) {
            logger.error(name + " must be between " + min + " and " + max + ": " + value);
            return false;
        }
        return true;
    }

    private boolean checkNum(long value, long min, long max, String name) {
        if (value < min || value > max) {
            logger.error(name + " must be between " + min + " and " + max + ": " + value);
            return false;
        }
        return true;
    }

    private boolean checkNum(float value, float min, float max, String name) {
        if (value < min || value > max) {
            logger.error(name + " must be between " + min + " and " + max + ": " + value);
            return false;
        }
        return true;
    }

    private boolean checkNum(double value, double min, double max, String name) {
        if (value < min || value > max) {
            logger.error(name + " must be between " + min + " and " + max + ": " + value);
            return false;
        }
        return true;
    }

    private boolean checkLengths(double[] array, int length1, int length2, String name) {
        if (array.length != length1 && array.length != length2) {
            logger.error(name + " must have a length of " + length1 + " or " + length2 + ". Current length is " + array.length);
            return false;
        }
        return true;
    }

    private boolean checkLength(double[] array, int length, String name) {
        if (array.length != length) {
            logger.error(name + " must have a length of " + length + ". Current length is " + array.length);
            return false;
        }
        return true;
    }

    private boolean checkString(String value, String name) {
        if (value == null || value.isEmpty()) {
            logger.error(name + " can't be null nor empty");
            return false;
        }
        return true;
    }

    private boolean checkString(String value, String[] possibleValues, String name) {
        if (checkString(value, name)) {
            for (String v : possibleValues) {
                if (value.equals(v))
                    return true;
            }
            logPossibleValues(value, possibleValues, name);
            return false;
        }
        logPossibleValues(value, possibleValues, name);
        return false;
    }

    private void logPossibleValues(String value, String[] possibleValues, String name) {
        logger.error(name + " value not valid: " + value + ". Possible values are:");
        for (String v : possibleValues)
            logger.error(v);
    }

    private <T extends Enum<T>> boolean checkStringEnum(String value, Class<T> clazz, String name) {
        if (checkString(value, name)) {
            for (Enum en : EnumSet.allOf(clazz)) {
                if (value.equalsIgnoreCase(en.toString())) {
                    return true;
                }
            }
            logger.error(name + " value not valid: " + value + ". Must be a value in the enum " + clazz.getSimpleName() + ":");
            for (Enum en : EnumSet.allOf(clazz)) {
                logger.error(en.toString());
            }
            return false;
        } else {
            return false;
        }
    }

    private boolean checkNotNull(Object o, String name) {
        if (o == null) {
            logger.error(name + " can't be null");
            return false;
        }
        return true;
    }
}
