/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script;

import com.badlogic.ashley.core.Entity;
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
import gaiasky.data.NewStarClusterLoader;
import gaiasky.data.group.DatasetOptions;
import gaiasky.data.group.DatasetOptions.DatasetLoadType;
import gaiasky.data.group.STILDataProvider;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.EventManager.TimeFrame;
import gaiasky.event.IObserver;
import gaiasky.gui.ColormapPicker;
import gaiasky.gui.IGui;
import gaiasky.gui.beans.PrimitiveComboBoxBean.Primitive;
import gaiasky.gui.beans.ShapeComboBoxBean.Shape;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Archetype;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.entity.TrajectoryUtils;
import gaiasky.scene.view.FocusView;
import gaiasky.scene.view.VertsView;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.*;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings.ScreenshotSettings;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.coord.AbstractOrbitCoordinates;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.filter.attrib.AttributeUCD;
import gaiasky.util.filter.attrib.IAttribute;
import gaiasky.util.gdx.contrib.postprocess.effects.CubemapProjections;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.*;
import gaiasky.util.screenshot.ImageRenderer;
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
 * Implementation of the scripting interface using the event system
 */
@SuppressWarnings({ "unused", "WeakerAccess", "SwitchStatementWithTooFewBranches", "SingleStatementInBlock", "SameParameterValue" })
public class EventScriptingInterface implements IScriptingInterface, IObserver {
    private static final Log logger = Logger.getLogger(EventScriptingInterface.class);

    // Reference to the event manager
    private final EventManager em;
    // Reference to asset manager
    private final AssetManager manager;
    // Reference to the catalog manager
    private final CatalogManager catalogManager;
    private LruCache<String, Texture> textures;

    // Auxiliary vectors
    private final Vector3d aux3d1, aux3d2, aux3d3, aux3d4, aux3d5, aux3d6;
    private final Vector3b aux3b1, aux3b2, aux3b3;
    private final Vector2d aux2d1;

    private final Set<AtomicBoolean> stops;

    private TrajectoryUtils trajectoryUtils;
    private final FocusView focusView;
    private final VertsView vertsView;

    private Scene scene;

    public EventScriptingInterface(final AssetManager manager, final CatalogManager catalogManager) {
        this.em = EventManager.instance;
        this.manager = manager;
        this.catalogManager = catalogManager;
        this.focusView = new FocusView();
        this.vertsView = new VertsView();
        new FocusView();

        stops = new HashSet<>();

        // Auxiliary vectors
        aux3d1 = new Vector3d();
        aux3d2 = new Vector3d();
        aux3d3 = new Vector3d();
        aux3d4 = new Vector3d();
        aux3d5 = new Vector3d();
        aux3d6 = new Vector3d();
        aux3b1 = new Vector3b();
        aux3b2 = new Vector3b();
        aux3b3 = new Vector3b();
        aux2d1 = new Vector2d();

        em.subscribe(this, Event.INPUT_EVENT, Event.DISPOSE, Event.SCENE_LOADED);
    }

    private void initializeTextures() {
        if (textures == null) {
            textures = new LruCache<>(100);
        }
    }

    private double[] dArray(List<?> l) {
        if (l == null)
            return null;
        double[] res = new double[l.size()];
        int i = 0;
        for (Object o : l) {
            res[i++] = (Double) o;
        }
        return res;
    }

    private int[] iArray(List<?> l) {
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
        postRunnable(() -> em.post(Event.EVENT_TIME_FRAME_CMD, this, TimeFrame.REAL_TIME));
    }

    @Override
    public void activateSimulationTimeFrame() {
        postRunnable(() -> em.post(Event.EVENT_TIME_FRAME_CMD, this, TimeFrame.SIMULATION_TIME));
    }

    @Override
    public void displayPopupNotification(String message) {
        if (checkString(message, "message")) {
            em.post(Event.POST_POPUP_NOTIFICATION, this, message);
        }
    }

    @Override
    public void displayPopupNotification(String message, float duration) {
        if (checkString(message, "message")) {
            em.post(Event.POST_POPUP_NOTIFICATION, this, message, duration);
        }
    }

    public void displayPopupNotification(String message, Double duration) {
        displayPopupNotification(message, duration.floatValue());
    }

    @Override
    public void setHeadlineMessage(final String headline) {
        postRunnable(() -> em.post(Event.POST_HEADLINE_MESSAGE, this, headline));
    }

    @Override
    public void setSubheadMessage(final String subhead) {
        postRunnable(() -> em.post(Event.POST_SUBHEAD_MESSAGE, this, subhead));
    }

    @Override
    public void clearHeadlineMessage() {
        postRunnable(() -> em.post(Event.CLEAR_HEADLINE_MESSAGE, this));
    }

    @Override
    public void clearSubheadMessage() {
        postRunnable(() -> em.post(Event.CLEAR_SUBHEAD_MESSAGE, this));
    }

    @Override
    public void clearAllMessages() {
        postRunnable(() -> em.post(Event.CLEAR_MESSAGES, this));
    }

    @Override
    public void disableInput() {
        postRunnable(() -> em.post(Event.INPUT_ENABLED_CMD, this, false));
    }

    @Override
    public void enableInput() {
        postRunnable(() -> em.post(Event.INPUT_ENABLED_CMD, this, true));
    }

    @Override
    public void setCinematicCamera(boolean cinematic) {
        postRunnable(() -> em.post(Event.CAMERA_CINEMATIC_CMD, this, cinematic));
    }

    @Override
    public void setCameraFocus(final String focusName) {
        setCameraFocus(focusName, 0.0f);
    }

    @Override
    public void setCameraFocus(final String focusName, final float waitTimeSeconds) {
        if (checkString(focusName, "focusName") && checkFocusName(focusName)) {
            Entity entity = getEntity(focusName);
            if (Mapper.focus.has(entity)) {
                synchronized (focusView) {
                    focusView.setEntity(entity);
                    focusView.getFocus(focusName);
                    NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;
                    changeFocus(focusView, cam, waitTimeSeconds);
                }
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
            Entity entity = getEntity(focusName);
            if (Mapper.focus.has(entity)) {
                synchronized (focusView) {
                    focusView.setEntity(entity);
                    focusView.getFocus(focusName);
                    em.post(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE);
                    em.post(Event.FOCUS_CHANGE_CMD, this, focusView.getEntity());
                }

                postRunnable(() -> {
                    // Instantly set the camera direction to look towards the focus
                    Vector3b camPos = GaiaSky.instance.cameraManager.getPos();
                    Vector3b dir = new Vector3b();
                    synchronized (focusView) {
                        focusView.setEntity(entity);
                        focusView.getAbsolutePosition(dir).sub(camPos);
                    }
                    em.post(Event.CAMERA_DIR_CMD, this, (Object) dir.nor().valuesd());
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
        setCameraFocusInstantAndGo(focusName, true);
    }

    public void setCameraFocusInstantAndGo(final String focusName, final boolean sleep) {
        if (checkString(focusName, "focusName")) {
            Entity entity = getEntity(focusName);
            if (Mapper.focus.has(entity)) {
                synchronized (focusView) {
                    focusView.setEntity(entity);
                    focusView.getFocus(focusName);
                    em.post(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE);
                    em.post(Event.FOCUS_CHANGE_CMD, this, focusView.getEntity(), true);
                    em.post(Event.GO_TO_OBJECT_CMD, this);
                }
                // Make sure the last action is flushed
                if (sleep)
                    sleepFrames(2);
            }
        }
    }

    @Override
    public void setCameraLock(final boolean lock) {
        postRunnable(() -> em.post(Event.FOCUS_LOCK_CMD, this, I18n.msg("gui.camera.lock"), lock));
    }

    @Override
    public void setCameraCenterFocus(boolean centerFocus) {
        postRunnable(() -> em.post(Event.CAMERA_CENTER_FOCUS_CMD, this, centerFocus));
    }

    @Override
    public void setCameraFree() {
        postRunnable(() -> em.post(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE));
    }

    @Override
    public void setCameraFov1() {
        postRunnable(() -> em.post(Event.CAMERA_MODE_CMD, this, CameraMode.GAIA_FOV1_MODE));
    }

    @Override
    public void setCameraFov2() {
        postRunnable(() -> em.post(Event.CAMERA_MODE_CMD, this, CameraMode.GAIA_FOV2_MODE));
    }

    @Override
    public void setCameraFov1and2() {
        postRunnable(() -> em.post(Event.CAMERA_MODE_CMD, this, CameraMode.GAIA_FOVS_MODE));
    }

    @Override
    public void setCameraPostion(final double[] vec) {
        setCameraPosition(vec);
    }

    @Override
    public void setCameraPosition(final double[] position) {
        setCameraPosition(position, false);
    }

    @Override
    public void setCameraPosition(double x, double y, double z) {
        setCameraPosition(new double[] { x, y, z });
    }

    @Override
    public void setCameraPosition(double x, double y, double z, boolean immediate) {
        setCameraPosition(new double[] { x, y, z }, immediate);
    }

    public void setCameraPosition(final List<?> vec) {
        setCameraPosition(dArray(vec));
    }

    public void setCameraPosition(final List<?> vec, boolean immediate) {
        setCameraPosition(dArray(vec), immediate);
    }

    @Override
    public void setCameraPosition(double[] position, boolean immediate) {
        if (checkLength(position, 3, "position")) {
            if (immediate) {
                cameraPositionEvent(position);
            } else {
                postRunnable(() -> cameraPositionEvent(position));
            }
        }
    }

    private void cameraPositionEvent(double[] position) {
        // Convert to km
        position[0] = position[0] * Constants.KM_TO_U;
        position[1] = position[1] * Constants.KM_TO_U;
        position[2] = position[2] * Constants.KM_TO_U;
        // Send event
        em.post(Event.CAMERA_POS_CMD, this, (Object) position);
    }

    @Override
    public double[] getCameraPosition() {
        Vector3d campos = GaiaSky.instance.cameraManager.getPos().tov3d(aux3d1);
        return new double[] { campos.x * Constants.U_TO_KM, campos.y * Constants.U_TO_KM, campos.z * Constants.U_TO_KM };
    }

    @Override
    public void setCameraDirection(final double[] direction) {
        setCameraDirection(direction, false);
    }

    public void setCameraDirection(final List<?> dir) {
        setCameraDirection(dArray(dir));
    }

    public void setCameraDirection(final List<?> dir, final boolean immediate) {
        setCameraDirection(dArray(dir), immediate);
    }

    @Override
    public void setCameraDirection(double[] direction, boolean immediate) {
        if (checkLength(direction, 3, "direction")) {
            if (immediate) {
                cameraDirectionEvent(direction);
            } else {
                postRunnable(() -> cameraDirectionEvent(direction));
            }
        }
    }

    private void cameraDirectionEvent(final double[] direction) {
        em.post(Event.CAMERA_DIR_CMD, this, (Object) direction);
    }

    @Override
    public double[] getCameraDirection() {
        Vector3d camdir = GaiaSky.instance.cameraManager.getDirection();
        return new double[] { camdir.x, camdir.y, camdir.z };
    }

    @Override
    public void setCameraUp(final double[] up) {
        setCameraUp(up, false);
    }

    public void setCameraUp(final List<?> up) {
        setCameraUp(dArray(up));
    }

    public void setCameraUp(final List<?> up, final boolean immediate) {
        setCameraUp(dArray(up), immediate);
    }

    @Override
    public void setCameraUp(final double[] up, final boolean immediate) {
        if (checkLength(up, 3, "up")) {
            if (immediate) {
                cameraUpEvent(up);
            } else {
                postRunnable(() -> cameraUpEvent(up));
            }
        }
    }

    private void cameraUpEvent(final double[] up) {
        em.post(Event.CAMERA_UP_CMD, this, (Object) up);
    }

    @Override
    public double[] getCameraUp() {
        Vector3d camUp = GaiaSky.instance.cameraManager.getUp();
        return new double[] { camUp.x, camUp.y, camUp.z };
    }

    @Override
    public void setCameraPositionAndFocus(String focus, String other, double rotation, double viewAngle) {
        if (checkNum(viewAngle, 1e-50d, Double.MAX_VALUE, "viewAngle") && checkNotNull(focus, "focus") && checkNotNull(other, "other")) {

            if (scene.index().containsEntity(focus) && scene.index().containsEntity(other)) {
                Entity focusObj, otherObj;
                synchronized (focusView) {
                    focusObj = scene.findFocus(focus);
                    focusView.setEntity(focusObj);
                    focusView.getFocus(focus);

                    otherObj = scene.findFocus(other);
                    focusView.setEntity(otherObj);
                    focusView.getFocus(other);
                }
                setCameraPositionAndFocus(focusObj, otherObj, rotation, viewAngle);
            }
        }
    }

    public void setCameraPositionAndFocus(String focus, String other, long rotation, long viewAngle) {
        setCameraPositionAndFocus(focus, other, (double) rotation, (double) viewAngle);
    }

    public void pointAtSkyCoordinate(double ra, double dec) {
        em.post(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);
        em.post(Event.FREE_MODE_COORD_CMD, this, ra, dec);
    }

    public void pointAtSkyCoordinate(long ra, long dec) {
        pointAtSkyCoordinate((double) ra, (double) dec);
    }

    private void setCameraPositionAndFocus(Entity focus, Entity other, double rotation, double viewAngle) {
        if (checkNum(viewAngle, 1e-50d, Double.MAX_VALUE, "viewAngle") && checkNotNull(focus, "focus") && checkNotNull(other, "other")) {

            em.post(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE);
            em.post(Event.FOCUS_CHANGE_CMD, this, focus);

            synchronized (focusView) {
                focusView.setEntity(focus);
                double radius = focusView.getRadius();
                double dist = radius / Math.tan(Math.toRadians(viewAngle / 2)) + radius;

                // Up to ecliptic north pole
                Vector3d up = new Vector3d(0, 1, 0).mul(Coordinates.eclToEq());

                Vector3b focusPos = aux3b1;
                focusView.getAbsolutePosition(focusPos);

                focusView.setEntity(other);
                Vector3b otherPos = aux3b2;
                focusView.getAbsolutePosition(otherPos);
                focusView.clearEntity();

                Vector3b otherToFocus = aux3b3;
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
    }

    @Override
    public void setCameraSpeed(final float speed) {
        if (checkNum(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "speed"))
            postRunnable(() -> em.post(Event.CAMERA_SPEED_CMD, this, speed / 10f, false));
    }

    public void setCameraSpeed(final int speed) {
        setCameraSpeed((float) speed);
    }

    @Override
    public double getCameraSpeed() {
        return GaiaSky.instance.cameraManager.getSpeed();
    }

    @Override
    public void setCameraRotationSpeed(float speed) {
        if (checkNum(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "speed"))
            postRunnable(() -> em.post(Event.ROTATION_SPEED_CMD, this, MathUtilsd.lint(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED)));
    }

    public void setCameraRotationSpeed(final int speed) {
        setRotationCameraSpeed((float) speed);
    }

    @Override
    public void setRotationCameraSpeed(final float speed) {
        setCameraRotationSpeed(speed);
    }

    public void setRotationCameraSpeed(final int speed) {
        setRotationCameraSpeed((float) speed);
    }

    @Override
    public void setCameraTurningSpeed(float speed) {
        if (checkNum(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "speed"))
            postRunnable(() -> em.post(Event.TURNING_SPEED_CMD, this, MathUtilsd.lint(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED), false));
    }

    public void setCameraTurningSpeed(final int speed) {
        setTurningCameraSpeed((float) speed);
    }

    @Override
    public void setTurningCameraSpeed(final float speed) {
        setCameraTurningSpeed(speed);

    }

    public void setTurningCameraSpeed(final int speed) {
        setTurningCameraSpeed((float) speed);
    }

    @Override
    public void setCameraSpeedLimit(int index) {
        if (checkNum(index, 0, 18, "index"))
            postRunnable(() -> em.post(Event.SPEED_LIMIT_CMD, this, index));
    }

    @Override
    public void setCameraTrackingObject(String objectName) {
        if (objectName == null) {
            removeCameraTrackingObject();
        } else if (checkFocusName(objectName)) {
            synchronized (focusView) {
                Entity trackingObject = getFocus(objectName);
                em.post(Event.CAMERA_TRACKING_OBJECT_CMD, this, trackingObject, objectName);
            }
        } else {
            removeCameraTrackingObject();
        }
    }

    @Override
    public void removeCameraTrackingObject() {
        em.post(Event.CAMERA_TRACKING_OBJECT_CMD, this, null, null);
    }

    @Override
    public void setCameraOrientationLock(boolean lock) {
        postRunnable(() -> em.post(Event.ORIENTATION_LOCK_CMD, this, I18n.msg("gui.camera.lock.orientation"), lock));
    }

    @Override
    public void cameraForward(final double cameraForward) {
        if (checkNum(cameraForward, -100d, 100d, "cameraForward"))
            postRunnable(() -> em.post(Event.CAMERA_FWD, this, cameraForward));
    }

    public void cameraForward(final long value) {
        cameraForward((double) value);
    }

    @Override
    public void cameraRotate(final double deltaX, final double deltaY) {
        if (checkNum(deltaX, -100d, 100d, "deltaX") && checkNum(deltaY, -100d, 100d, "deltaY"))
            postRunnable(() -> em.post(Event.CAMERA_ROTATE, this, deltaX, deltaY));
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
            postRunnable(() -> em.post(Event.CAMERA_ROLL, this, roll));
    }

    public void cameraRoll(final long roll) {
        cameraRoll((double) roll);
    }

    @Override
    public void cameraTurn(final double deltaX, final double deltaY) {
        if (checkNum(deltaX, -100d, 100d, "deltaX") && checkNum(deltaY, -100d, 100d, "deltaY")) {
            postRunnable(() -> em.post(Event.CAMERA_TURN, this, deltaX, deltaY));
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
        postRunnable(() -> em.post(Event.CAMERA_STOP, this));

    }

    @Override
    public void cameraCenter() {
        postRunnable(() -> em.post(Event.CAMERA_CENTER, this));
    }

    @Override
    public IFocus getClosestObjectToCamera() {
        return GaiaSky.instance.cameraManager.getClosestBody();
    }

    @Override
    public void setFov(final float newFov) {
        if (!SlaveManager.projectionActive()) {
            if (checkNum(newFov, Constants.MIN_FOV, Constants.MAX_FOV, "newFov"))
                postRunnable(() -> em.post(Event.FOV_CHANGED_CMD, this, newFov));
        }
    }

    public void setFov(final int newFov) {
        setFov((float) newFov);
    }

    @Override
    public void setVisibility(final String key, final boolean visible) {
        setComponentTypeVisibility(key, visible);
    }

    @Override
    public void setComponentTypeVisibility(String key, boolean visible) {
        if (checkCtKeyNull(key)) {
            logger.error("Element '" + key + "' does not exist. Possible values are:");
            ComponentType[] cts = ComponentType.values();
            for (ComponentType ct : cts)
                logger.error(ct.key);
        } else {
            postRunnable(() -> em.post(Event.TOGGLE_VISIBILITY_CMD, this, key, visible));
        }
    }

    @Override
    public boolean getComponentTypeVisibility(String key) {
        if (checkCtKeyNull(key)) {
            logger.error("Element '" + key + "' does not exist. Possible values are:");
            ComponentType[] cts = ComponentType.values();
            for (ComponentType ct : cts)
                logger.error(ct.key);
            return false;
        } else {
            ComponentType ct = ComponentType.getFromKey(key);
            return Settings.settings.scene.visibility.get(ct);
        }
    }

    @Override
    public boolean setObjectVisibility(String name, boolean visible) {
        Entity obj = getEntity(name);
        if (obj == null) {
            logger.error("No object found with name '" + name + "'");
            return false;
        }

        postRunnable(() -> EventManager.publish(Event.PER_OBJECT_VISIBILITY_CMD, this, obj, name, visible));
        return true;
    }

    @Override
    public boolean getObjectVisibility(String name) {
        Entity obj = getEntity(name);
        if (obj == null) {
            logger.error("No object found with name '" + name + "'");
            return false;
        }

        boolean visible;
        synchronized (focusView) {
            focusView.setEntity(obj);
            visible = focusView.isVisible(true);
        }
        return visible;
    }

    @Override
    public void setLabelSizeFactor(float factor) {
        if (checkNum(factor, Constants.MIN_LABEL_SIZE, Constants.MAX_LABEL_SIZE, "labelSizeFactor")) {
            postRunnable(() -> em.post(Event.LABEL_SIZE_CMD, this, factor));
        }
    }

    @Override
    public void setForceDisplayLabel(String name, boolean forceLabel) {
        if (checkObjectName(name)) {
            Entity obj = getEntity(name);
            em.post(Event.FORCE_OBJECT_LABEL_CMD, this, obj, name, forceLabel);
        }
    }

    @Override
    public void setLabelColor(String name, double[] color) {
        if (checkObjectName(name)) {
            Entity obj = getEntity(name);
            em.post(Event.LABEL_COLOR_CMD, this, obj, name, GlobalResources.toFloatArray(color));
        }
    }

    public void setLabelColor(String name, final List<?> color) {
        setLabelColor(name, dArray(color));
    }

    @Override
    public boolean getForceDisplayLabel(String name) {
        Entity obj = getEntity(name);
        if (obj == null) {
            logger.error("No object found with name '" + name + "'");
            return false;
        }

        boolean ret;
        synchronized (focusView) {
            focusView.setEntity(obj);
            ret = focusView.isForceLabel(name);
        }
        return ret;
    }

    public void setLabelSizeFactor(int factor) {
        setLabelSizeFactor((float) factor);
    }

    @Override
    public void setLineWidthFactor(final float factor) {
        if (checkNum(factor, Constants.MIN_LINE_WIDTH, Constants.MAX_LINE_WIDTH, "lineWidthFactor")) {
            postRunnable(() -> em.post(Event.LINE_WIDTH_CMD, this, factor));
        }
    }

    public void setLineWidthFactor(int factor) {
        setLineWidthFactor((float) factor);
    }

    private boolean checkCtKeyNull(String key) {
        ComponentType ct = ComponentType.getFromKey(key);
        return ct == null;
    }

    @Override
    public void setProperMotionsNumberFactor(float factor) {
        postRunnable(() -> EventManager.publish(Event.PM_NUM_FACTOR_CMD, this, MathUtilsd.lint(factor, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_PM_NUM_FACTOR, Constants.MAX_PM_NUM_FACTOR)));
    }

    @Override
    public void setProperMotionsColorMode(int mode) {
        postRunnable(() -> EventManager.publish(Event.PM_COLOR_MODE_CMD, this, mode % 6));
    }

    @Override
    public void setProperMotionsArrowheads(boolean arrowheadsEnabled) {
        postRunnable(() -> EventManager.publish(Event.PM_ARROWHEADS_CMD, this, arrowheadsEnabled));
    }

    public void setProperMotionsNumberFactor(int factor) {
        setProperMotionsNumberFactor((float) factor);
    }

    public void setUnfilteredProperMotionsNumberFactor(float factor) {
        Settings.settings.scene.properMotion.number = factor;
    }

    @Override
    public void setProperMotionsLengthFactor(float factor) {
        postRunnable(() -> EventManager.publish(Event.PM_LEN_FACTOR_CMD, this, factor));
    }

    public void setProperMotionsLengthFactor(int factor) {
        setProperMotionsLengthFactor((float) factor);
    }

    @Override
    public void setProperMotionsMaxNumber(long maxNumber) {
        Settings.settings.scene.star.group.numVelocityVector = (int) maxNumber;
    }

    @Override
    public long getProperMotionsMaxNumber() {
        return Settings.settings.scene.star.group.numVelocityVector;
    }

    @Override
    public void setCrosshairVisibility(boolean visible) {
        setFocusCrosshairVisibility(visible);
        setClosestCrosshairVisibility(visible);
        setHomeCrosshairVisibility(visible);
    }

    @Override
    public void setFocusCrosshairVisibility(boolean visible) {
        postRunnable(() -> em.post(Event.CROSSHAIR_FOCUS_CMD, this, visible));
    }

    @Override
    public void setClosestCrosshairVisibility(boolean visible) {
        postRunnable(() -> em.post(Event.CROSSHAIR_CLOSEST_CMD, this, visible));
    }

    @Override
    public void setHomeCrosshairVisibility(boolean visible) {
        postRunnable(() -> em.post(Event.CROSSHAIR_HOME_CMD, this, visible));
    }

    @Override
    public void setMinimapVisibility(boolean visible) {
        postRunnable(() -> em.post(Event.SHOW_MINIMAP_ACTION, this, visible));
    }

    @Override
    public void setAmbientLight(final float ambientLight) {
        if (checkNum(ambientLight, Constants.MIN_AMBIENT_LIGHT, Constants.MAX_AMBIENT_LIGHT, "ambientLight"))
            postRunnable(() -> em.post(Event.AMBIENT_LIGHT_CMD, this, ambientLight));
    }

    public void setAmbientLight(final int value) {
        setAmbientLight((float) value);
    }

    @Override
    public void setSimulationTime(int year, int month, int day, int hour, int min, int sec, int millisec) {
        LocalDateTime date = LocalDateTime.of(year, month, day, hour, min, sec, millisec);
        em.post(Event.TIME_CHANGE_CMD, this, date.toInstant(ZoneOffset.UTC));
    }

    @Override
    public void setSimulationTime(final long time) {
        if (checkNum(time, 1, Long.MAX_VALUE, "time"))
            em.post(Event.TIME_CHANGE_CMD, this, Instant.ofEpochMilli(time));
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
        em.post(Event.TIME_STATE_CMD, this, true);
    }

    @Override
    public void stopSimulationTime() {
        em.post(Event.TIME_STATE_CMD, this, false);
    }

    @Override
    public boolean isSimulationTimeOn() {
        return GaiaSky.instance.time.isTimeOn();
    }

    @Override
    public void setSimulationPace(final double warp) {
        setTimeWarp(warp);
    }

    public void setSimulationPace(final long warp) {
        setSimulationPace((double) warp);
    }

    @Override
    public void setTimeWarp(final double warp) {
        em.post(Event.TIME_WARP_CMD, this, warp);
    }

    public void setTimeWarp(final long warp) {
        setTimeWarp((double) warp);
    }

    @Override
    public void setTargetTime(long ms) {
        em.post(Event.TARGET_TIME_CMD, this, Instant.ofEpochMilli(ms));
    }

    @Override
    public void setTargetTime(int year, int month, int day, int hour, int min, int sec, int millisec) {
        em.post(Event.TARGET_TIME_CMD, this, LocalDateTime.of(year, month, day, hour, min, sec, millisec).toInstant(ZoneOffset.UTC));
    }

    @Override
    public void unsetTargetTime() {
        em.post(Event.TARGET_TIME_CMD, this);
    }

    @Override
    public void setStarBrightness(final float brightness) {
        if (checkNum(brightness, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "brightness"))
            em.post(Event.STAR_BRIGHTNESS_CMD, this, MathUtilsd.lint(brightness, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_STAR_BRIGHTNESS, Constants.MAX_STAR_BRIGHTNESS));
    }

    @Override
    public void setStarBrightnessPower(float power) {
        if (checkFinite(power, "brightness-pow")) {
            em.post(Event.STAR_BRIGHTNESS_POW_CMD, this, power);
        }
    }

    public void setStarBrightness(final int brightness) {
        setStarBrightness((float) brightness);
    }

    @Override
    public float getStarBrightness() {
        return (float) MathUtilsd.lint(Settings.settings.scene.star.brightness, Constants.MIN_STAR_BRIGHTNESS, Constants.MAX_STAR_BRIGHTNESS, Constants.MIN_SLIDER, Constants.MAX_SLIDER);
    }

    @Override
    public void setStarSize(final float size) {
        if (checkNum(size, Constants.MIN_STAR_POINT_SIZE, Constants.MAX_STAR_POINT_SIZE, "size"))
            em.post(Event.STAR_POINT_SIZE_CMD, this, size);
    }

    public void setStarSize(final int size) {
        setStarSize((float) size);
    }

    @Override
    public float getStarSize() {
        return MathUtilsd.lint(Settings.settings.scene.star.pointSize, Constants.MIN_STAR_POINT_SIZE, Constants.MAX_STAR_POINT_SIZE, Constants.MIN_SLIDER, Constants.MAX_SLIDER);
    }

    @Override
    public float getStarMinOpacity() {
        return MathUtilsd.lint(Settings.settings.scene.star.opacity[0], Constants.MIN_STAR_MIN_OPACITY, Constants.MAX_STAR_MIN_OPACITY, Constants.MIN_SLIDER, Constants.MAX_SLIDER);
    }

    public float getMinStarOpacity() {
        return getStarMinOpacity();
    }

    @Override
    public void setStarMinOpacity(float minOpacity) {
        if (checkNum(minOpacity, Constants.MIN_STAR_MIN_OPACITY, Constants.MAX_STAR_MIN_OPACITY, "min-opacity"))
            EventManager.publish(Event.STAR_MIN_OPACITY_CMD, this, minOpacity);
    }

    public void setMinStarOpacity(float minOpacity) {
        setStarMinOpacity(minOpacity);
    }

    @Override
    public void setStarTextureIndex(int index) {
        if (checkNum(index, 1, 4, "index")) {
            EventManager.publish(Event.BILLBOARD_TEXTURE_IDX_CMD, this, index);
        }
    }

    @Override
    public void setStarGroupNearestNumber(int n) {
        if (checkNum(n, 1, 1000000, "nNearest")) {
            EventManager.publish(Event.STAR_GROUP_NEAREST_CMD, this, n);
        }
    }

    @Override
    public void setStarGroupBillboard(boolean flag) {
        EventManager.publish(Event.STAR_GROUP_BILLBOARD_CMD, this, flag);
    }

    @Override
    public void setOrbitSolidAngleThreshold(float angleDeg) {
        if (checkNum(angleDeg, 0.0f, 180f, "solid-angle")) {
            postRunnable(() -> EventManager.publish(Event.ORBIT_SOLID_ANGLE_TH_CMD, this, (double) angleDeg));
        }
    }

    @Override
    public void setProjectionYaw(float yaw) {
        if (SlaveManager.projectionActive()) {
            postRunnable(() -> {
                Settings.settings.program.net.slave.yaw = yaw;
                SlaveManager.instance.yaw = yaw;
            });
        }
    }

    @Override
    public void setProjectionPitch(float pitch) {
        if (SlaveManager.projectionActive()) {
            postRunnable(() -> {
                Settings.settings.program.net.slave.pitch = pitch;
                SlaveManager.instance.pitch = pitch;
            });
        }
    }

    @Override
    public void setProjectionRoll(float roll) {
        if (SlaveManager.projectionActive()) {
            postRunnable(() -> {
                Settings.settings.program.net.slave.roll = roll;
                SlaveManager.instance.roll = roll;
            });
        }
    }

    @Override
    public void setProjectionFov(float newFov) {
        if (checkNum(newFov, Constants.MIN_FOV, 170f, "newFov"))
            postRunnable(() -> {
                SlaveManager.instance.cameraFov = newFov;
                em.post(Event.FOV_CHANGED_CMD, this, newFov);
            });
    }

    @Override
    public void setLimitFps(double limitFps) {
        if (checkNum(limitFps, -Double.MAX_VALUE, Constants.MAX_FPS, "limitFps")) {
            em.post(Event.LIMIT_FPS_CMD, this, limitFps);
        }
    }

    @Override
    public void setLimitFps(int limitFps) {
        setLimitFps((double) limitFps);
    }

    @Override
    public void configureScreenshots(int width, int height, String directory, String namePrefix) {
        if (checkNum(width, 1, Integer.MAX_VALUE, "width") && checkNum(height, 1, Integer.MAX_VALUE, "height") && checkString(directory, "directory") && checkDirectoryExists(directory, "directory") && checkString(namePrefix, "namePrefix")) {
            em.post(Event.SCREENSHOT_CMD, this, width, height, directory);
        }
    }

    @Override
    public void setScreenshotsMode(String screenshotMode) {
        // Hack to keep compatibility with old scripts
        if (screenshotMode != null && screenshotMode.equalsIgnoreCase("redraw")) {
            screenshotMode = "ADVANCED";
        }
        if (checkStringEnum(screenshotMode, Settings.ScreenshotMode.class, "screenshotMode")) {
            em.post(Event.SCREENSHOT_MODE_CMD, this, screenshotMode);
            postRunnable(() -> em.post(Event.SCREENSHOT_SIZE_UPDATE, this, Settings.settings.screenshot.resolution[0], Settings.settings.screenshot.resolution[1]));
        }
    }

    @Override
    public void saveScreenshot() {
        ScreenshotSettings ss = Settings.settings.screenshot;
        em.post(Event.SCREENSHOT_CMD, this, ss.resolution[0], ss.resolution[1], ss.location);
    }

    @Override
    public void takeScreenshot() {
        saveScreenshot();
    }

    public void setMinStarOpacity(int opacity) {
        setMinStarOpacity((float) opacity);
    }

    @Override
    public void configureFrameOutput(int width, int height, int fps, String directory, String namePrefix) {
        configureFrameOutput(width, height, (double) fps, directory, namePrefix);
    }

    @Override
    public void configureFrameOutput(int width, int height, double fps, String directory, String namePrefix) {
        if (checkNum(width, 1, Integer.MAX_VALUE, "width") && checkNum(height, 1, Integer.MAX_VALUE, "height") && checkNum(fps, Constants.MIN_FPS, Constants.MAX_FPS, "FPS") && checkString(directory, "directory") && checkDirectoryExists(directory, "directory") && checkString(namePrefix, "namePrefix")) {
            em.post(Event.FRAME_OUTPUT_MODE_CMD, this, Settings.ScreenshotMode.ADVANCED);
            em.post(Event.CONFIG_FRAME_OUTPUT_CMD, this, width, height, fps, directory, namePrefix);
        }
    }

    @Override
    public void configureRenderOutput(int width, int height, int fps, String directory, String namePrefix) {
        configureFrameOutput(width, height, fps, directory, namePrefix);
    }

    @Override
    public void setFrameOutputMode(String screenshotMode) {
        // Hack to keep compatibility with old scripts
        if (screenshotMode != null && screenshotMode.equalsIgnoreCase("redraw")) {
            screenshotMode = "ADVANCED";
        }
        if (checkStringEnum(screenshotMode, Settings.ScreenshotMode.class, "screenshotMode"))
            em.post(Event.FRAME_OUTPUT_MODE_CMD, this, screenshotMode);
    }

    @Override
    public boolean isFrameOutputActive() {
        return Settings.settings.frame.active;
    }

    @Override
    public boolean isRenderOutputActive() {
        return isFrameOutputActive();
    }

    @Override
    public double getFrameOutputFps() {
        return Settings.settings.frame.targetFps;
    }

    @Override
    public double getRenderOutputFps() {
        return getFrameOutputFps();
    }

    @Override
    public void setFrameOutput(boolean active) {
        em.post(Event.FRAME_OUTPUT_CMD, this, active);
    }

    @Override
    public FocusView getObject(String name) {
        return getObject(name, 0);
    }

    @Override
    public FocusView getObject(String name, double timeoutSeconds) {
        Entity object = getEntity(name, timeoutSeconds);
        return new FocusView(object);
    }

    @Override
    public VertsView getLineObject(String name) {
        return getLineObject(name, 0);
    }

    @Override
    public VertsView getLineObject(String name, double timeoutSeconds) {
        Entity object = getEntity(name, timeoutSeconds);
        if (Mapper.verts.has(object)) {
            return new VertsView(object);
        } else {
            logger.error(name + " is not a verts object.");
            return null;
        }
    }

    public Entity getEntity(String name) {
        return getEntity(name, 0);
    }

    public Entity getEntity(String name, double timeOutSeconds) {
        Entity obj = scene.getEntity(name);
        if (obj == null) {
            if (name.matches("[0-9]+")) {
                // Check with 'HIP '
                obj = scene.getEntity("hip " + name);
            } else if (name.matches("hip [0-9]+")) {
                obj = scene.getEntity(name.substring(4));
            }
        }

        // If negative, no limit in waiting
        if (timeOutSeconds < 0)
            timeOutSeconds = Double.MAX_VALUE;

        double startMs = System.currentTimeMillis();
        double elapsedSeconds = 0;
        while (obj == null && elapsedSeconds < timeOutSeconds) {
            sleepFrames(1);
            obj = scene.getEntity(name);
            elapsedSeconds = (System.currentTimeMillis() - startMs) / 1000d;
        }
        return obj;
    }

    private Entity getFocus(String name) {
        return scene.findFocus(name);
    }

    private Entity getFocusEntity(String name) {
        return scene.findFocus(name);
    }

    @Override
    public void setObjectSizeScaling(String name, double scalingFactor) {
        Entity object = getEntity(name);
        if (object == null) {
            logger.error("Object '" + name + "' does not exist");
            return;
        }
        if (Mapper.modelScaffolding.has(object)) {
            var scaffolding = Mapper.modelScaffolding.get(object);
            scaffolding.setSizeScaleFactor(scalingFactor);
        } else {
            logger.error("Object '" + name + "' is not a model object");
        }
    }

    @Override
    public void setOrbitCoordinatesScaling(String name, double scalingFactor) {
        int modified = 0;
        String className, objectName;
        if (name.contains(":")) {
            int idx = name.indexOf(":");
            className = name.substring(0, idx);
            objectName = name.substring(idx + 1, name.length());
        } else {
            className = name;
            objectName = null;
        }
        List<AbstractOrbitCoordinates> aocs = AbstractOrbitCoordinates.getInstances();
        for (AbstractOrbitCoordinates aoc : aocs) {
            if (aoc.getClass().getSimpleName().equalsIgnoreCase(className)) {
                if (objectName != null) {
                    if (aoc.getOrbitName() != null && aoc.getOrbitName().contains(objectName)) {
                        aoc.setScaling(scalingFactor);
                        modified++;
                    }
                } else {
                    aoc.setScaling(scalingFactor);
                    modified++;
                }
            }
        }
        logger.info(name + ": modified scaling of " + modified + " orbits");
    }

    private void initializeTrajectoryUtils() {
        if (trajectoryUtils == null) {
            trajectoryUtils = new TrajectoryUtils();
        }
    }

    @Override
    public void refreshAllOrbits() {
        initializeTrajectoryUtils();
        postRunnable(() -> {
            var orbits = scene.findEntitiesByFamily(scene.getFamilies().orbits);
            for (Entity orbit : orbits) {
                var trajectory = Mapper.trajectory.get(orbit);
                var verts = Mapper.verts.get(orbit);
                trajectoryUtils.refreshOrbit(trajectory, verts, true);
            }
        });
    }

    @Override
    public void forceUpdateScene() {
        postRunnable(() -> em.post(Event.SCENE_FORCE_UPDATE, this));
    }

    public void refreshObjectOrbit(String name) {
        String orbitName = name + " orbit";
        if (checkObjectName(orbitName)) {
            FocusView view = getObject(orbitName);
            var trajectory = Mapper.trajectory.get(view.getEntity());
            var verts = Mapper.verts.get(view.getEntity());
            if (trajectory != null && verts != null) {
                initializeTrajectoryUtils();
                postRunnable(() -> {
                    trajectoryUtils.refreshOrbit(trajectory, verts, true);
                });
            }
        }
    }

    @Override
    public double getObjectRadius(String name) {
        Entity object = scene.findFocus(name);
        if (object == null)
            return -1;

        focusView.setEntity(object);
        focusView.getFocus(name);
        return focusView.getRadius() * Constants.U_TO_KM;
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
            if (scene.index().containsEntity(name)) {
                Entity focus = scene.findFocus(name);
                focusView.setEntity(focus);
                focusView.getFocus(name);
                goToObject(focus, viewAngle, waitTimeSeconds, stop);
            } else {
                logger.info("FOCUS_MODE object does not exist: " + name);
            }
        }
    }

    public void goToObject(String name, double viewAngle, int waitTimeSeconds, AtomicBoolean stop) {
        goToObject(name, viewAngle, (float) waitTimeSeconds, stop);
    }

    void goToObject(Entity object, double viewAngle, float waitTimeSeconds, AtomicBoolean stop) {
        if (checkNotNull(object, "object") && checkNum(viewAngle, -Double.MAX_VALUE, Double.MAX_VALUE, "viewAngle")) {
            stops.add(stop);
            NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;

            focusView.setEntity(object);
            changeFocus(focusView, cam, waitTimeSeconds);

            /* target angle */
            double target = Math.toRadians(viewAngle);
            if (target < 0)
                target = Math.toRadians(20d);

            long prevTime = TimeUtils.millis();
            if (focusView.getSolidAngle() < target) {
                // Add forward movement while distance > target distance
                while (focusView.getSolidAngle() < target && (stop == null || !stop.get())) {
                    // dt in ms
                    long dt = TimeUtils.timeSinceMillis(prevTime);
                    prevTime = TimeUtils.millis();

                    em.post(Event.CAMERA_FWD, this, (double) dt);
                    try {
                        sleep(0.1f);
                    } catch (Exception e) {
                        logger.error(e);
                    }
                }
            } else {
                // Add backward movement while distance > target distance
                while (focusView.getSolidAngleApparent() > target && (stop == null || !stop.get())) {
                    // dt in ms
                    long dt = TimeUtils.timeSinceMillis(prevTime);
                    prevTime = TimeUtils.millis();

                    em.post(Event.CAMERA_FWD, this, (double) -dt);
                    try {
                        sleep(0.1f);
                    } catch (Exception e) {
                        logger.error(e);
                    }
                }
            }

            // We can stop now
            em.post(Event.CAMERA_STOP, this);
        }
    }

    public void goToObject(Entity object, double viewAngle, int waitTimeSeconds, AtomicBoolean stop) {
        goToObject(object, viewAngle, (float) waitTimeSeconds, stop);
    }

    @Override
    public void goToObjectInstant(String name) {
        setCameraFocusInstantAndGo(name);
    }

    @Override
    public void landOnObject(String name) {
        if (checkString(name, "name")) {
            Entity target = getEntity(name);
            if (Mapper.focus.has(target)) {
                synchronized (focusView) {
                    focusView.setEntity(target);
                    focusView.getFocus(name);
                }
                landOnObject(target, null);
            }
        }
    }

    void landOnObject(Entity object, AtomicBoolean stop) {
        if (checkNotNull(object, "object")) {

            stops.add(stop);

            synchronized (focusView) {
                if (Mapper.atmosphere.has(object)) {
                    focusView.setEntity(object);
                    // Planets.
                    NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;
                    // FOCUS_MODE wait - 2 seconds
                    float waitTimeSeconds = -1;

                    /*
                     * SAVE
                     */

                    // Save speed, set it to 50
                    double speed = Settings.settings.scene.camera.speed;
                    em.post(Event.CAMERA_SPEED_CMD, this, 25f / 10f, false);

                    // Save turn speed, set it to 50
                    double turnSpeedBak = Settings.settings.scene.camera.turn;
                    em.post(Event.TURNING_SPEED_CMD, this, (float) MathUtilsd.lint(20d, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED), false);

                    // Save cinematic
                    boolean cinematic = Settings.settings.scene.camera.cinematic;
                    Settings.settings.scene.camera.cinematic = true;

                    /*
                     * FOCUS
                     */
                    changeFocus(focusView, cam, waitTimeSeconds);

                    /* target distance */
                    double target = 100 * Constants.M_TO_U;

                    Vector3b camObj = aux3b1;
                    focusView.getAbsolutePosition(camObj).add(cam.posinv).nor();
                    Vector3d dir = cam.direction;

                    // Add forward movement while distance > target distance
                    boolean distanceNotMet = (focusView.getDistToCamera() - focusView.getRadius()) > target;
                    boolean viewNotMet = Math.abs(dir.angle(camObj)) < 90;

                    long prevTime = TimeUtils.millis();
                    while ((distanceNotMet || viewNotMet) && (stop == null || !stop.get())) {
                        // dt in ms
                        long dt = TimeUtils.timeSinceMillis(prevTime);
                        prevTime = TimeUtils.millis();

                        if (distanceNotMet)
                            em.post(Event.CAMERA_FWD, this, 0.1d * dt);
                        else
                            cam.stopForwardMovement();

                        if (viewNotMet) {
                            if (focusView.getDistToCamera() - focusView.getRadius() < focusView.getRadius() * 5)
                                // Start turning where we are at n times the radius
                                em.post(Event.CAMERA_TURN, this, 0d, dt / 500d);
                        } else {
                            cam.stopRotateMovement();
                        }

                        try {
                            sleepFrames(1);
                        } catch (Exception e) {
                            logger.error(e);
                        }

                        // focus.transform.getTranslation(aux);
                        viewNotMet = Math.abs(dir.angle(camObj)) < 90;
                        distanceNotMet = (focusView.getDistToCamera() - focusView.getRadius()) > target;
                    }

                    // STOP
                    em.post(Event.CAMERA_STOP, this);

                    // Roll till done
                    Vector3d up = cam.up;
                    // aux1 <- camera-object
                    camObj = focusView.getAbsolutePosition(aux3b1).sub(cam.pos);
                    double ang1 = up.angle(camObj);
                    double ang2 = up.cpy().rotate(cam.direction, 1).angle(camObj);
                    double rollSign = ang1 < ang2 ? -1d : 1d;

                    if (ang1 < 170) {
                        rollAndWait(rollSign * 0.02d, 170d, 50L, cam, camObj, stop);

                        // STOP
                        cam.stopMovement();

                        rollAndWait(rollSign * 0.006d, 176d, 50L, cam, camObj, stop);
                        // STOP
                        cam.stopMovement();

                        rollAndWait(rollSign * 0.003d, 178d, 50L, cam, camObj, stop);
                    }

                    /*
                     * RESTORE
                     */

                    // We can stop now
                    em.post(Event.CAMERA_STOP, this);

                    // Restore cinematic
                    Settings.settings.scene.camera.cinematic = cinematic;

                    // Restore speed
                    em.post(Event.CAMERA_SPEED_CMD, this, (float) speed, false);

                    // Restore turning speed
                    em.post(Event.TURNING_SPEED_CMD, this, (float) turnSpeedBak, false);
                }
            }
        }
    }

    @Override
    public void landAtObjectLocation(String name, String locationName) {
        landAtObjectLocation(name, locationName, null);
    }

    public void landAtObjectLocation(String name, String locationName, AtomicBoolean stop) {
        if (checkString(name, "name")) {
            stops.add(stop);
            Entity entity = getEntity(name);
            if (Mapper.focus.has(entity)) {
                synchronized (focusView) {
                    focusView.setEntity(entity);
                    focusView.getFocus(name);
                }
                landAtObjectLocation(entity, locationName, stop);
            }
        }
    }

    public void landAtObjectLocation(Entity object, String locationName, AtomicBoolean stop) {
        if (checkNotNull(object, "object") && checkString(locationName, "locationName")) {

            stops.add(stop);
            if (Mapper.atmosphere.has(object)) {
                synchronized (focusView) {
                    focusView.setEntity(object);
                    Entity loc = focusView.getChildByNameAndArchetype(locationName, scene.archetypes().get("gaiasky.scenegraph.Loc"));
                    if (loc != null) {
                        var locMark = Mapper.loc.get(loc);
                        landAtObjectLocation(object, locMark.location.x, locMark.location.y, stop);
                        return;
                    }
                    logger.info("Location '" + locationName + "' not found on object '" + focusView.getCandidateName() + "'");
                }
            }
        }
    }

    @Override
    public void landAtObjectLocation(String name, double longitude, double latitude) {
        if (checkString(name, "name")) {
            Entity entity = getEntity(name);
            if (Mapper.focus.has(entity)) {
                synchronized (focusView) {
                    focusView.setEntity(entity);
                    focusView.getFocus(name);
                }
                landAtObjectLocation(entity, longitude, latitude, null);
            }
        }
    }

    void landAtObjectLocation(Entity entity, double longitude, double latitude, AtomicBoolean stop) {
        if (checkNotNull(entity, "object") && checkNum(latitude, -90d, 90d, "latitude") && checkNum(longitude, 0d, 360d, "longitude")) {
            synchronized (focusView) {
                focusView.setEntity(entity);
                stops.add(stop);
                String nameStub = focusView.getCandidateName() + " [loc]";

                if (!scene.index().containsEntity(nameStub)) {
                    var archetype = scene.archetypes().get("gaiasky.scenegraph.Invisible");
                    Entity invisible = archetype.createEntity();
                    var base = Mapper.base.get(invisible);
                    base.setName(nameStub);
                    base.setCt("Others");
                    var body = Mapper.body.get(invisible);
                    body.setSizeM(500.0);
                    var graph = Mapper.graph.get(invisible);
                    graph.translation = new Vector3b();
                    graph.setParent(Scene.ROOT_NAME);
                    scene.initializeEntity(invisible);
                    scene.setUpEntity(invisible);
                    EventManager.publish(Event.SCENE_ADD_OBJECT_NO_POST_CMD, this, invisible, true);
                }
                Entity invisible = scene.getEntity(nameStub);

                if (Mapper.atmosphere.has(entity)) {
                    NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;

                    double targetAngle = 35 * MathUtilsd.degRad;
                    if (focusView.getSolidAngle() > targetAngle) {
                        // Zoom out
                        while (focusView.getSolidAngle() > targetAngle && (stop == null || !stop.get())) {
                            cam.addForwardForce(-5d);
                            sleepFrames(1);
                        }
                        // STOP
                        cam.stopMovement();
                    }

                    // Go to object
                    goToObject(focusView.getEntity(), 20, -1, stop);

                    // Save speed, set it to 50
                    double speed = Settings.settings.scene.camera.speed;
                    em.post(Event.CAMERA_SPEED_CMD, this, 25f / 10f);

                    // Save turn speed, set it to 50
                    double turnSpeedBak = Settings.settings.scene.camera.turn;
                    em.post(Event.TURNING_SPEED_CMD, this, (float) MathUtilsd.lint(50d, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED));

                    // Save rotation speed, set it to 20
                    double rotationSpeedBak = Settings.settings.scene.camera.rotate;
                    em.post(Event.ROTATION_SPEED_CMD, this, (float) MathUtilsd.lint(20d, Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED));

                    // Save cinematic
                    boolean cinematic = Settings.settings.scene.camera.cinematic;
                    Settings.settings.scene.camera.cinematic = true;

                    // Save crosshair
                    boolean crosshair = Settings.settings.scene.crosshair.focus;
                    Settings.settings.scene.crosshair.focus = false;

                    // Get target position
                    Vector3b target = aux3b1;
                    focusView.getPositionAboveSurface(longitude, latitude, 50, target);

                    // Get object position
                    Vector3b objectPosition = focusView.getAbsolutePosition(aux3b2);

                    // Check intersection with object
                    boolean intersects = Intersectord.checkIntersectSegmentSphere(cam.pos.tov3d(aux3d3), target.tov3d(aux3d1), objectPosition.tov3d(aux3d2), focusView.getRadius());

                    if (intersects) {
                        cameraRotate(5d, 5d);
                    }

                    while (intersects && (stop == null || !stop.get())) {
                        sleep(0.1f);

                        objectPosition = focusView.getAbsolutePosition(aux3b2);
                        intersects = Intersectord.checkIntersectSegmentSphere(cam.pos.tov3d(aux3d3), target.tov3d(aux3d1), objectPosition.tov3d(aux3d2), focusView.getRadius());
                    }

                    cameraStop();

                    Mapper.base.get(invisible).ct = focusView.getCt();
                    Mapper.body.get(invisible).pos.set(target);

                    // Go to object
                    goToObject(nameStub, 20, 0, stop);

                    // Restore cinematic
                    Settings.settings.scene.camera.cinematic = cinematic;

                    // Restore speed
                    em.post(Event.CAMERA_SPEED_CMD, this, (float) speed);

                    // Restore turning speed
                    em.post(Event.TURNING_SPEED_CMD, this, (float) turnSpeedBak);

                    // Restore rotation speed
                    em.post(Event.ROTATION_SPEED_CMD, this, (float) rotationSpeedBak);

                    // Restore crosshair
                    Settings.settings.scene.crosshair.focus = crosshair;

                    // Land
                    landOnObject(focusView.getEntity(), stop);
                }
                EventManager.publish(Event.SCENE_REMOVE_OBJECT_CMD, this, invisible, true);
            }
        }
    }

    private void rollAndWait(double roll, double target, long sleep, NaturalCamera cam, Vector3b camobj, AtomicBoolean stop) {
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
        Entity entity = getEntity(name);
        if (Mapper.focus.has(entity)) {
            focusView.setEntity(entity);
            focusView.getFocus(name);
            if (focusView.getSet() != null) {
                var pos = focusView.getAbsolutePosition(name, aux3b1);
                return pos.sub(GaiaSky.instance.getICamera().getPos()).lend() * Constants.U_TO_KM;
            } else {
                return (focusView.getDistToCamera() - focusView.getRadius()) * Constants.U_TO_KM;
            }
        }

        return -1;
    }

    @Override
    public double[] getStarParameters(String id) {
        Entity entity = getEntity(id);
        if (Mapper.starSet.has(entity)) {
            var set = Mapper.starSet.get(entity);
            // This star group contains the star
            IParticleRecord sb = set.getCandidateBean();
            if (sb != null) {
                double[] rgb = sb.rgb();
                return new double[] { sb.ra(), sb.dec(), sb.parallax(), sb.mualpha(), sb.mudelta(), sb.radvel(), sb.appmag(), rgb[0], rgb[1], rgb[2] };
            }
        }

        return null;
    }

    @Override
    public double[] getObjectPosition(String name) {
        Entity entity = getEntity(name);
        if (Mapper.focus.has(entity)) {
            focusView.setEntity(entity);
            focusView.getFocus(name);
            focusView.getAbsolutePosition(name, aux3b1);
            return new double[] { aux3b1.x.doubleValue(), aux3b1.y.doubleValue(), aux3b1.z.doubleValue() };
        }
        return null;
    }

    @Override
    public double[] getObjectPredictedPosition(String name) {
        Entity entity = getEntity(name);
        if (Mapper.focus.has(entity)) {
            focusView.setEntity(entity);
            focusView.getFocus(name);
            focusView.getPredictedPosition(aux3b1, GaiaSky.instance.time, GaiaSky.instance.getICamera(), false);
            return new double[] { aux3b1.x.doubleValue(), aux3b1.y.doubleValue(), aux3b1.z.doubleValue() };
        }
        return null;
    }

    @Override
    public void setGuiScrollPosition(final float pixelY) {
        postRunnable(() -> em.post(Event.GUI_SCROLL_POSITION_CMD, this, pixelY));

    }

    public void setGuiScrollPosition(final int pixelY) {
        setGuiScrollPosition((float) pixelY);
    }

    @Override
    public void enableGui() {
        postRunnable(() -> em.post(Event.DISPLAY_GUI_CMD, this, true, I18n.msg("notif.cleanmode")));
    }

    @Override
    public void disableGui() {
        postRunnable(() -> em.post(Event.DISPLAY_GUI_CMD, this, false, I18n.msg("notif.cleanmode")));
    }

    @Override
    public float getGuiScaleFactor() {
        return Settings.settings.program.ui.scale;
    }

    @Override
    public void displayMessageObject(final int id, final String message, final float x, final float y, final float r, final float g, final float b, final float a, final float fontSize) {
        postRunnable(() -> em.post(Event.ADD_CUSTOM_MESSAGE, this, id, message, x, y, r, g, b, a, fontSize));
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
        postRunnable(() -> em.post(Event.ADD_CUSTOM_TEXT, this, id, text, x, y, maxWidth, maxHeight, r, g, b, a, fontSize));
    }

    public void displayTextObject(final int id, final String text, final float x, final float y, final float maxWidth, final float maxHeight, final float r, final float g, final float b, final float a, final int fontSize) {
        displayTextObject(id, text, x, y, maxWidth, maxHeight, r, g, b, a, (float) fontSize);
    }

    @Override
    public void displayImageObject(final int id, final String path, final float x, final float y, final float r, final float g, final float b, final float a) {
        postRunnable(() -> {
            Texture tex = getTexture(path);
            em.post(Event.ADD_CUSTOM_IMAGE, this, id, tex, x, y, r, g, b, a);
        });
    }

    @Override
    public void displayImageObject(final int id, final String path, final double x, final double y, final double[] color) {
        if (checkNotNull(color, "color") && checkLengths(color, 3, 4, "color")) {
            float a = color.length > 3 ? (float) color[3] : 1f;
            displayImageObject(id, path, (float) x, (float) y, (float) color[0], (float) color[1], (float) color[2], a);
        }
    }

    public void displayImageObject(final int id, final String path, final double x, final double y, final List<?> color) {
        displayImageObject(id, path, x, y, dArray(color));
    }

    @Override
    public void displayImageObject(final int id, final String path, final float x, final float y) {
        postRunnable(() -> {
            Texture tex = getTexture(path);
            em.post(Event.ADD_CUSTOM_IMAGE, this, id, tex, x, y);
        });
    }

    @Override
    public void removeAllObjects() {
        postRunnable(() -> em.post(Event.REMOVE_ALL_OBJECTS, this));
    }

    @Override
    public void removeObject(final int id) {
        postRunnable(() -> em.post(Event.REMOVE_OBJECTS, this, (Object) new int[] { id }));
    }

    @Override
    public void removeObjects(final int[] ids) {
        postRunnable(() -> em.post(Event.REMOVE_OBJECTS, this, (Object) ids));
    }

    public void removeObjects(final List<?> ids) {
        removeObjects(iArray(ids));
    }

    @Override
    public void maximizeInterfaceWindow() {
        postRunnable(() -> em.post(Event.GUI_FOLD_CMD, this, false));
    }

    @Override
    public void minimizeInterfaceWindow() {
        postRunnable(() -> em.post(Event.GUI_FOLD_CMD, this, true));
    }

    @Override
    public void setGuiPosition(final float x, final float y) {
        postRunnable(() -> em.post(Event.GUI_MOVE_CMD, this, x, y));
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
            return new float[] { x, y, actor.getWidth(), actor.getHeight() };
        } else {
            return null;
        }

    }

    @Override
    public void expandGuiComponent(String name) {
        em.post(Event.EXPAND_PANE_CMD, this, name);
    }

    @Override
    public void collapseGuiComponent(String name) {
        em.post(Event.COLLAPSE_PANE_CMD, this, name);
    }

    @Override
    public String getVersionNumber() {
        return Settings.settings.version.version;
    }

    @Override
    public boolean waitFocus(String name, long timeoutMs) {
        long iniTime = TimeUtils.millis();
        NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;
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

    @Override
    public void setCameraRecorderFps(double targetFps) {
        if (checkNum(targetFps, Constants.MIN_FPS, Constants.MAX_FPS, "targetFps")) {
            em.post(Event.CAMRECORDER_FPS_CMD, this, targetFps);
        }
    }

    private Texture getTexture(String path) {
        if (textures == null || !textures.containsKey(path)) {
            preloadTexture(path);
        }
        return textures.get(path);
    }

    @Override
    public void preloadTexture(String path) {
        preloadTextures(new String[] { path });
    }

    @Override
    public String getAssetsLocation() {
        return Settings.settings.ASSETS_LOC;
    }

    @Override
    public void preloadTextures(String[] paths) {
        initializeTextures();
        for (final String path : paths) {
            // This only works in async mode!
            postRunnable(() -> manager.load(path, Texture.class));
            while (!manager.isLoaded(path)) {
                sleepFrames(1);
            }
            Texture tex = manager.get(path, Texture.class);
            textures.put(path, tex);
        }
    }

    @Override
    public void startRecordingCameraPath() {
        em.post(Event.RECORD_CAMERA_CMD, this, true, null);
    }

    @Override
    public void startRecordingCameraPath(String fileName) {
        em.post(Event.RECORD_CAMERA_CMD, this, true, Path.of(fileName).getFileName().toString());
    }

    @Override
    public void stopRecordingCameraPath() {
        em.post(Event.RECORD_CAMERA_CMD, this, false, null);
    }

    @Override
    public void playCameraPath(String file, boolean sync) {
        runCameraPath(file, sync);
    }

    @Override
    public void runCameraPath(String file, boolean sync) {
        em.post(Event.PLAY_CAMERA_CMD, this, file);

        // Wait if needed
        if (sync) {
            Object monitor = new Object();
            IObserver watcher = (event, source, data) -> {
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
            em.subscribe(watcher, Event.CAMERA_PLAY_INFO);
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
    public void playCameraPath(String file) {
        runCameraPath(file);
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

        final Vector3d D31, D32, D33;

        /**
         * A runnable that interpolates the camera state (position, direction, up) to the new given state
         * in the specified number of seconds.
         *
         * @param cam     The camera to use.
         * @param pos     The final position.
         * @param dir     The final direction.
         * @param up      The final up vector.
         * @param seconds The number of seconds to complete the transition.
         * @param end     An optional runnable that is executed when the transition has completed.
         */
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

            // Set up interpolation.
            posl = getPathd(cam.getPos().tov3d(aux3d3), pos);
            dirl = getPathd(cam.getDirection(), dir);
            upl = getPathd(cam.getUp(), up);

            // Aux
            D31 = new Vector3d();
            D32 = new Vector3d();
            D33 = new Vector3d();
        }

        private Pathd<Vector3d> getPathd(Vector3d p0, double[] p1) {
            Vector3d[] points = new Vector3d[] { new Vector3d(p0), new Vector3d(p1[0], p1[1], p1[2]) };
            return new Lineard<>(points);
        }

        @Override
        public void run() {
            // Update elapsed time
            elapsed = GaiaSky.instance.getT() - start;

            // Interpolation variable
            double alpha = MathUtilsd.clamp(elapsed / seconds, 0.0, 0.99999999999999);

            // Set camera state
            cam.setPos(posl.valueAt(D31, alpha));
            cam.setDirection(dirl.valueAt(D31, alpha).nor());
            Vector3d up = upl.valueAt(D31, alpha);
            Vector3d right = D32.set(cam.direction).crs(up);
            cam.setUp(right.crs(cam.direction).nor());

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
    public void cameraTransitionKm(double[] camPos, double[] camDir, double[] camUp, double seconds) {
        cameraTransition(internalUnitsToKilometres(camPos), camDir, camUp, seconds, true);
    }

    public void cameraTransitionKm(List<?> camPos, List<?> camDir, List<?> camUp, double seconds) {
        cameraTransitionKm(dArray(camPos), dArray(camDir), dArray(camUp), seconds);
    }

    public void cameraTransitionKm(List<?> camPos, List<?> camDir, List<?> camUp, long seconds) {
        cameraTransitionKm(camPos, camDir, camUp, (double) seconds);
    }

    @Override
    public void cameraTransition(double[] camPos, double[] camDir, double[] camUp, double seconds) {
        cameraTransition(camPos, camDir, camUp, seconds, true);
    }

    public void cameraTransition(double[] camPos, double[] camDir, double[] camUp, long seconds) {
        cameraTransition(camPos, camDir, camUp, (double) seconds);
    }

    public void cameraTransition(List<?> camPos, List<?> camDir, List<?> camUp, double seconds) {
        cameraTransition(dArray(camPos), dArray(camDir), dArray(camUp), seconds);
    }

    public void cameraTransition(List<?> camPos, List<?> camDir, List<?> camUp, long seconds) {
        cameraTransition(camPos, camDir, camUp, (double) seconds);
    }

    private int cTransSeq = 0;

    @Override
    public void cameraTransition(double[] camPos, double[] camDir, double[] camUp, double seconds, boolean sync) {
        NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;

        // Put camera in free mode.
        em.post(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);

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

            // Remove and return
            unparkRunnable(name);
        }
    }

    public void cameraTransition(List<?> camPos, List<?> camDir, List<?> camUp, double seconds, boolean sync) {
        cameraTransition(dArray(camPos), dArray(camDir), dArray(camUp), seconds, sync);
    }

    public void cameraTransition(List<?> camPos, List<?> camDir, List<?> camUp, long seconds, boolean sync) {
        cameraTransition(camPos, camDir, camUp, (double) seconds, sync);
    }

    @Override
    public void sleep(float seconds) {
        if (checkNum(seconds, 0f, Float.MAX_VALUE, "seconds")) {
            if (seconds == 0f)
                return;

            if (this.isFrameOutputActive()) {
                this.sleepFrames(Math.max(1, Math.round(this.getFrameOutputFps() * seconds)));
            } else {
                try {
                    Thread.sleep(Math.round(seconds * 1000f));
                } catch (InterruptedException e) {
                    logger.error(e);
                }
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
                synchronized (GaiaSky.instance.frameMonitor) {
                    GaiaSky.instance.frameMonitor.wait();
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
    private void changeFocus(FocusView object, NaturalCamera cam, double waitTimeSeconds) {
        // Post focus change and wait, if needed
        FocusView currentFocus = (FocusView) cam.getFocus();
        if (currentFocus == null ||
                Mapper.particleSet.has(currentFocus.getEntity()) ||
                currentFocus.getEntity() != object.getEntity()) {
            em.post(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE);
            em.post(Event.FOCUS_CHANGE_CMD, this, object.getEntity());

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
        return new double[] { pos.x, pos.y, pos.z };
    }

    public double[] galacticToInternalCartesian(int l, int b, int r) {
        return galacticToInternalCartesian((double) l, (double) b, (double) r);
    }

    @Override
    public double[] eclipticToInternalCartesian(double l, double b, double r) {
        Vector3d pos = Coordinates.sphericalToCartesian(l * Nature.TO_RAD, b * Nature.TO_RAD, r * Constants.KM_TO_U, new Vector3d());
        pos.mul(Coordinates.eclipticToEquatorial());
        return new double[] { pos.x, pos.y, pos.z };
    }

    public double[] eclipticToInternalCartesian(int l, int b, int r) {
        return eclipticToInternalCartesian((double) l, (double) b, (double) r);
    }

    @Override
    public double[] equatorialToInternalCartesian(double ra, double dec, double r) {
        Vector3d pos = Coordinates.sphericalToCartesian(ra * Nature.TO_RAD, dec * Nature.TO_RAD, r * Constants.KM_TO_U, new Vector3d());
        return new double[] { pos.x, pos.y, pos.z };
    }

    public double[] equatorialToInternalCartesian(int ra, int dec, int r) {
        return equatorialToInternalCartesian((double) ra, (double) dec, (double) r);
    }

    public double[] internalCartesianToEquatorial(double x, double y, double z) {
        Vector3b in = aux3b1.set(x, y, z);
        Vector3d out = aux3d6;
        Coordinates.cartesianToSpherical(in, out);
        return new double[] { out.x * Nature.TO_DEG, out.y * Nature.TO_DEG, in.lend() };
    }

    public double[] internalCartesianToEquatorial(int x, int y, int z) {
        return internalCartesianToEquatorial((double) x, (double) y, (double) z);
    }

    @Override
    public double[] equatorialCartesianToInternalCartesian(double[] eq, double kmFactor) {
        aux3d1.set(eq).scl(kmFactor).scl(Constants.KM_TO_U);
        return new double[] { aux3d1.y, aux3d1.z, aux3d1.x };
    }

    public double[] equatorialCartesianToInternalCartesian(final List<?> eq, double kmFactor) {
        return equatorialCartesianToInternalCartesian(dArray(eq), kmFactor);
    }

    @Override
    public double[] equatorialToGalactic(double[] eq) {
        aux3d1.set(eq).mul(Coordinates.eqToGal());
        return aux3d1.values();
    }

    public double[] equatorialToGalactic(List<?> eq) {
        return equatorialToGalactic(dArray(eq));
    }

    @Override
    public double[] equatorialToEcliptic(double[] eq) {
        aux3d1.set(eq).mul(Coordinates.eqToEcl());
        return aux3d1.values();
    }

    public double[] equatorialToEcliptic(List<?> eq) {
        return equatorialToEcliptic(dArray(eq));
    }

    @Override
    public double[] galacticToEquatorial(double[] gal) {
        aux3d1.set(gal).mul(Coordinates.galToEq());
        return aux3d1.values();
    }

    public double[] galacticToEquatorial(List<?> gal) {
        return galacticToEquatorial(dArray(gal));
    }

    @Override
    public double[] eclipticToEquatorial(double[] ecl) {
        aux3d1.set(ecl).mul(Coordinates.eclToEq());
        return aux3d1.values();
    }

    public double[] eclipticToEquatorial(List<?> ecl) {
        return eclipticToEquatorial(dArray(ecl));
    }

    @Override
    public void setBrightnessLevel(double level) {
        if (checkNum(level, -1d, 1d, "brightness"))
            postRunnable(() -> em.post(Event.BRIGHTNESS_CMD, this, (float) level));
    }

    public void setBrightnessLevel(long level) {
        setBrightnessLevel((double) level);
    }

    @Override
    public void setContrastLevel(double level) {
        if (checkNum(level, 0d, 2d, "contrast"))
            postRunnable(() -> em.post(Event.CONTRAST_CMD, this, (float) level));
    }

    public void setContrastLevel(long level) {
        setContrastLevel((double) level);
    }

    @Override
    public void setHueLevel(double level) {
        if (checkNum(level, 0d, 2d, "hue"))
            postRunnable(() -> em.post(Event.HUE_CMD, this, (float) level));
    }

    public void setHueLevel(long level) {
        setHueLevel((double) level);
    }

    @Override
    public void setSaturationLevel(double level) {
        if (checkNum(level, 0d, 2d, "saturation"))
            postRunnable(() -> em.post(Event.SATURATION_CMD, this, (float) level));
    }

    public void setSaturationLevel(long level) {
        setSaturationLevel((double) level);
    }

    @Override
    public void setGammaCorrectionLevel(double level) {
        if (checkNum(level, 0d, 3d, "gamma correction"))
            postRunnable(() -> em.post(Event.GAMMA_CMD, this, (float) level));
    }

    public void setGammaCorrectionLevel(long level) {
        setGammaCorrectionLevel((double) level);
    }

    @Override
    public void setHDRToneMappingType(String type) {
        if (checkString(type, new String[] { "auto", "AUTO", "exposure", "EXPOSURE", "none", "NONE" }, "tone mapping type"))
            postRunnable(() -> em.post(Event.TONEMAPPING_TYPE_CMD, this, Settings.ToneMapping.valueOf(type.toUpperCase())));
    }

    @Override
    public void setExposureToneMappingLevel(double level) {
        if (checkNum(level, 0d, 20d, "exposure"))
            postRunnable(() -> em.post(Event.EXPOSURE_CMD, this, (float) level));
    }

    public void setExposureToneMappingLevel(long level) {
        setExposureToneMappingLevel((double) level);
    }

    @Override
    public void setCubemapMode(boolean state, String projection) {
        CubemapProjections.CubemapProjection newProj = CubemapProjections.CubemapProjection.valueOf(projection.toUpperCase());
        postRunnable(() -> em.post(Event.CUBEMAP_CMD, this, state, newProj));
    }

    @Override
    public void setPanoramaMode(boolean state) {
        postRunnable(() -> em.post(Event.CUBEMAP_CMD, this, state, CubemapProjections.CubemapProjection.EQUIRECTANGULAR));
    }

    @Override
    public void setPlanetariumMode(boolean state) {
        postRunnable(() -> em.post(Event.CUBEMAP_CMD, this, state, CubemapProjections.CubemapProjection.FISHEYE));
    }

    @Override
    public void setCubemapResolution(int resolution) {
        if (checkNum(resolution, 20, 15000, "resolution")) {
            postRunnable(() -> em.post(Event.CUBEMAP_RESOLUTION_CMD, this, resolution));
        }
    }

    @Override
    public void setCubemapProjection(String projection) {
        if (checkStringEnum(projection, CubemapProjections.CubemapProjection.class, "projection")) {
            CubemapProjections.CubemapProjection newProj = CubemapProjections.CubemapProjection.valueOf(projection.toUpperCase());
            em.post(Event.CUBEMAP_PROJECTION_CMD, this, newProj);
        }
    }

    @Override
    public void setStereoscopicMode(boolean state) {
        postRunnable(() -> em.post(Event.STEREOSCOPIC_CMD, this, state));
    }

    @Override
    public void setStereoscopicProfile(int index) {
        postRunnable(() -> em.post(Event.STEREO_PROFILE_CMD, this, index));
    }

    @Override
    public long getCurrentFrameNumber() {
        return GaiaSky.instance.frames;
    }

    @Override
    public void setLensFlare(boolean state) {
        postRunnable(() -> em.post(Event.LENS_FLARE_CMD, this, state));
    }

    @Override
    public void setMotionBlur(boolean state) {
        postRunnable(() -> em.post(Event.MOTION_BLUR_CMD, this, state));
    }

    @Override
    public void setStarGlow(boolean state) {
        postRunnable(() -> em.post(Event.LIGHT_GLOW_CMD, this, state));
    }

    @Override
    public void setBloom(float value) {
        if (checkNum(value, 0f, 1f, "bloom"))
            postRunnable(() -> em.post(Event.BLOOM_CMD, this, value));
    }

    public void setBloom(int level) {
        setBloom((float) level);
    }

    @Override
    public void setSmoothLodTransitions(boolean value) {
        postRunnable(() -> em.post(Event.OCTREE_PARTICLE_FADE_CMD, this, I18n.msg("element.octreeparticlefade"), value));
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

    public double[] rotate3(List<?> vector, List<?> axis, double angle) {
        return rotate3(dArray(vector), dArray(axis), angle);
    }

    public double[] rotate3(List<?> vector, List<?> axis, long angle) {
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

    public double[] rotate2(List<?> vector, double angle) {
        return rotate2(dArray(vector), angle);
    }

    public double[] rotate2(List<?> vector, long angle) {
        return rotate2(vector, (double) angle);
    }

    @Override
    public double[] cross3(double[] vec1, double[] vec2) {
        return aux3d1.set(vec1).crs(aux3d2.set(vec2)).values();
    }

    public double[] cross3(List<?> vec1, List<?> vec2) {
        return cross3(dArray(vec1), dArray(vec2));
    }

    @Override
    public double dot3(double[] vec1, double[] vec2) {
        return aux3d1.set(vec1).dot(aux3d2.set(vec2));
    }

    public double dot3(List<?> vec1, List<?> vec2) {
        return dot3(dArray(vec1), dArray(vec2));
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color) {
        addPolyline(name, points, color, 1f);
    }

    public void addPolyline(String name, List<?> points, List<?> color) {
        addPolyline(name, points, color, 1f);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color, double lineWidth) {
        addPolyline(name, points, color, lineWidth, false);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color, double lineWidth, boolean arrowCaps) {
        addPolyline(name, points, color, lineWidth, GL20.GL_LINE_STRIP, arrowCaps);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color, double lineWidth, int primitive) {
        addPolyline(name, points, color, lineWidth, primitive, false);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color, double lineWidth, int primitive, boolean arrowCaps) {
        if (checkString(name, "name") && checkNum(lineWidth, 0.1f, 50f, "lineWidth") && checkNum(primitive, 1, 3, "primitive")) {
            // New
            {
                var archetype = scene.archetypes().get("gaiasky.scenegraph.Polyline");
                var entity = archetype.createEntity();

                var base = Mapper.base.get(entity);
                base.setName(name);
                base.setComponentType(ComponentType.Others);

                var body = Mapper.body.get(entity);
                body.setColor(color);
                body.setLabelColor(color);

                var line = Mapper.line.get(entity);
                line.lineWidth = (float) lineWidth;

                var arrow = Mapper.arrow.get(entity);
                arrow.arrowCap = arrowCaps;

                var verts = Mapper.verts.get(entity);
                synchronized (vertsView) {
                    vertsView.setEntity(entity);
                    vertsView.setPrimitiveSize((float) lineWidth);
                    vertsView.setPoints(points);
                    vertsView.setRenderGroup(arrowCaps ? RenderGroup.LINE : RenderGroup.LINE_GPU);
                    vertsView.setClosedLoop(false);
                    vertsView.setGlPrimitive(primitive);
                }

                var graph = Mapper.graph.get(entity);
                graph.setParent(Scene.ROOT_NAME);

                scene.initializeEntity(entity);
                scene.setUpEntity(entity);

                em.post(Event.SCENE_ADD_OBJECT_CMD, this, entity, true);
            }
        }

    }

    public void addPolyline(String name, double[] points, double[] color, int lineWidth) {
        addPolyline(name, points, color, (float) lineWidth);
    }

    public void addPolyline(String name, double[] points, double[] color, int lineWidth, int primitive) {
        addPolyline(name, points, color, (float) lineWidth, primitive);
    }

    public void addPolyline(String name, List<?> points, List<?> color, float lineWidth) {
        addPolyline(name, dArray(points), dArray(color), lineWidth);
    }

    public void addPolyline(String name, List<?> points, List<?> color, float lineWidth, boolean arrowCaps) {
        addPolyline(name, dArray(points), dArray(color), lineWidth, arrowCaps);
    }

    public void addPolyline(String name, List<?> points, List<?> color, float lineWidth, int primitive) {
        addPolyline(name, dArray(points), dArray(color), lineWidth, primitive);
    }

    public void addPolyline(String name, List<?> points, List<?> color, float lineWidth, int primitive, boolean arrowCaps) {
        addPolyline(name, dArray(points), dArray(color), lineWidth, primitive, arrowCaps);
    }

    public void addPolyline(String name, List<?> points, List<?> color, int lineWidth) {
        addPolyline(name, points, color, (float) lineWidth);
    }

    public void addPolyline(String name, List<?> points, List<?> color, int lineWidth, boolean arrowCaps) {
        addPolyline(name, points, color, (float) lineWidth, arrowCaps);
    }

    public void addPolyline(String name, List<?> points, List<?> color, int lineWidth, int primitive) {
        addPolyline(name, points, color, (float) lineWidth, primitive);
    }

    public void addPolyline(String name, List<?> points, List<?> color, int lineWidth, int primitive, boolean arrowCaps) {
        addPolyline(name, points, color, (float) lineWidth, primitive, arrowCaps);
    }

    @Override
    public void removeModelObject(String name) {
        if (checkString(name, "name")) {
            em.post(Event.SCENE_REMOVE_OBJECT_CMD, this, name, true);
        }
    }

    @Override
    public void postRunnable(Runnable runnable) {
        GaiaSky.postRunnable(runnable);
    }

    @Override
    public void parkRunnable(String id, Runnable runnable) {
        parkSceneRunnable(id, runnable);
    }

    @Override
    public void parkSceneRunnable(String id, Runnable runnable) {
        if (checkString(id, "id")) {
            em.post(Event.PARK_RUNNABLE, this, id, runnable);
        }
    }

    @Override
    public void parkCameraRunnable(String id, Runnable runnable) {
        if (checkString(id, "id")) {
            em.post(Event.PARK_CAMERA_RUNNABLE, this, id, runnable);
        }
    }

    @Override
    public void removeRunnable(String id) {
        if (checkString(id, "id"))
            em.post(Event.UNPARK_RUNNABLE, this, id);
    }

    @Override
    public void unparkRunnable(String id) {
        removeRunnable(id);
    }

    @Override
    public void setCameraState(double[] pos, double[] dir, double[] up) {
        postRunnable(() -> {
            em.post(Event.CAMERA_POS_CMD, this, (Object) pos);
            em.post(Event.CAMERA_DIR_CMD, this, (Object) dir);
            em.post(Event.CAMERA_UP_CMD, this, (Object) up);
        });
    }

    public void setCameraState(List<?> pos, List<?> dir, List<?> up) {
        setCameraState(dArray(pos), dArray(dir), dArray(up));
    }

    @Override
    public void setCameraStateAndTime(double[] pos, double[] dir, double[] up, long time) {
        postRunnable(() -> {
            em.post(Event.CAMERA_PROJECTION_CMD, this, pos, dir, up);
            em.post(Event.TIME_CHANGE_CMD, this, Instant.ofEpochMilli(time));
        });
    }

    public void setCameraStateAndTime(List<?> pos, List<?> dir, List<?> up, long time) {
        setCameraStateAndTime(dArray(pos), dArray(dir), dArray(up), time);
    }

    @Override
    public void resetImageSequenceNumber() {
        ImageRenderer.resetSequenceNumber();
    }

    @Override
    public boolean loadDataset(String dsName, String absolutePath) {
        return loadDataset(dsName, absolutePath, CatalogInfoSource.SCRIPT, true);
    }

    @Override
    public boolean loadDataset(String dsName, String path, boolean sync) {
        return loadDataset(dsName, path, CatalogInfoSource.SCRIPT, sync);
    }

    public boolean loadDataset(String dsName, String path, CatalogInfoSource type, boolean sync) {
        if (sync) {
            return loadDatasetImmediate(dsName, path, type, true);
        } else {
            Thread t = new Thread(() -> loadDatasetImmediate(dsName, path, type, false));
            t.start();
            return true;
        }
    }

    public boolean loadDataset(String dsName, String path, CatalogInfoSource type, DatasetOptions datasetOptions, boolean sync) {
        if (sync) {
            return loadDatasetImmediate(dsName, path, type, datasetOptions, true);
        } else {
            Thread t = new Thread(() -> loadDatasetImmediate(dsName, path, type, datasetOptions, false));
            t.start();
            return true;
        }
    }

    public boolean loadDataset(String dsName, DataSource ds, CatalogInfoSource type, DatasetOptions datasetOptions, boolean sync) {
        if (sync) {
            return loadDatasetImmediate(dsName, ds, type, datasetOptions, true);
        } else {
            Thread t = new Thread(() -> loadDatasetImmediate(dsName, ds, type, datasetOptions, false));
            t.start();
            return true;
        }
    }

    @Override
    public boolean loadStarDataset(String dsName, String path, boolean sync) {
        return loadStarDataset(dsName, path, CatalogInfoSource.SCRIPT, 1, new double[] { 0, 0, 0, 0 }, null, null, sync);
    }

    @Override
    public boolean loadStarDataset(String dsName, String path, double magnitudeScale, boolean sync) {
        return loadStarDataset(dsName, path, CatalogInfoSource.SCRIPT, magnitudeScale, new double[] { 0, 0, 0, 0 }, null, null, sync);
    }

    @Override
    public boolean loadStarDataset(String dsName, String path, double magnitudeScale, double[] labelColor, boolean sync) {
        return loadStarDataset(dsName, path, CatalogInfoSource.SCRIPT, magnitudeScale, labelColor, null, null, sync);
    }

    public boolean loadStarDataset(String dsName, String path, double magnitudeScale, final List<?> labelColor, boolean sync) {
        return loadStarDataset(dsName, path, magnitudeScale, dArray(labelColor), sync);
    }

    @Override
    public boolean loadStarDataset(String dsName, String path, double magnitudeScale, double[] labelColor, double[] fadeIn, double[] fadeOut, boolean sync) {
        return loadStarDataset(dsName, path, CatalogInfoSource.SCRIPT, magnitudeScale, labelColor, fadeIn, fadeOut, sync);
    }

    public boolean loadStarDataset(String dsName, String path, double magnitudeScale, final List<?> labelColor, final List<?> fadeIn, final List<?> fadeOut, boolean sync) {
        return loadStarDataset(dsName, path, magnitudeScale, dArray(labelColor), dArray(fadeIn), dArray(fadeOut), sync);
    }

    public boolean loadStarDataset(String dsName, String path, CatalogInfoSource type, double magnitudeScale, double[] labelColor, double[] fadeIn, double[] fadeOut, boolean sync) {
        DatasetOptions dops = DatasetOptions.getStarDatasetOptions(dsName, magnitudeScale, labelColor, fadeIn, fadeOut);
        return loadDataset(dsName, path, type, dops, sync);
    }

    @Override
    public boolean loadParticleDataset(String dsName, String path, double profileDecay, double[] particleColor, double colorNoise, double[] labelColor, double particleSize, String ct, boolean sync) {
        return loadParticleDataset(dsName, path, profileDecay, particleColor, colorNoise, labelColor, particleSize, new double[] { 1.5d, 100d }, ct, null, null, sync);
    }

    public boolean loadParticleDataset(String dsName, String path, double profileDecay, List<?> particleColor, double colorNoise, List<?> labelColor, double particleSize, String ct, boolean sync) {
        return loadParticleDataset(dsName, path, profileDecay, dArray(particleColor), colorNoise, dArray(labelColor), particleSize, ct, null, null, sync);
    }

    @Override
    public boolean loadParticleDataset(String dsName, String path, double profileDecay, double[] particleColor, double colorNoise, double[] labelColor, double particleSize, String ct, double[] fadeIn, double[] fadeOut, boolean sync) {
        return loadParticleDataset(dsName, path, profileDecay, particleColor, colorNoise, labelColor, particleSize, new double[] { Math.tan(Math.toRadians(0.1)), Math.tan(Math.toRadians(6.0)) }, ct, fadeIn, fadeOut, sync);
    }

    public boolean loadParticleDataset(String dsName, String path, double profileDecay, final List<?> particleColor, double colorNoise, final List<?> labelColor, double particleSize, String ct, final List<?> fadeIn, final List<?> fadeOut, boolean sync) {
        return loadParticleDataset(dsName, path, profileDecay, dArray(particleColor), colorNoise, dArray(labelColor), particleSize, ct, dArray(fadeIn), dArray(fadeOut), sync);
    }

    @Override
    public boolean loadParticleDataset(String dsName, String path, double profileDecay, double[] particleColor, double colorNoise, double[] labelColor, double particleSize, double[] sizeLimits, String ct, double[] fadeIn, double[] fadeOut, boolean sync) {
        ComponentType compType = ComponentType.valueOf(ct);
        return loadParticleDataset(dsName, path, profileDecay, particleColor, colorNoise, labelColor, particleSize, sizeLimits, compType, fadeIn, fadeOut, sync);
    }

    public boolean loadParticleDataset(String dsName, String path, double profileDecay, final List<?> particleColor, double colorNoise, final List<?> labelColor, double particleSize, List<?> sizeLimits, String ct, final List<?> fadeIn, final List<?> fadeOut, boolean sync) {
        return loadParticleDataset(dsName, path, profileDecay, dArray(particleColor), colorNoise, dArray(labelColor), particleSize, dArray(sizeLimits), ct, dArray(fadeIn), dArray(fadeOut), sync);
    }

    public boolean loadParticleDataset(String dsName, String path, double profileDecay, double[] particleColor, double colorNoise, double[] labelColor, double particleSize, double[] sizeLimits, ComponentType ct, double[] fadeIn, double[] fadeOut, boolean sync) {
        return loadParticleDataset(dsName, path, CatalogInfoSource.SCRIPT, profileDecay, particleColor, colorNoise, labelColor, particleSize, sizeLimits, ct, fadeIn, fadeOut, sync);
    }

    public boolean loadParticleDataset(String dsName, String path, CatalogInfoSource type, double profileDecay, double[] particleColor, double colorNoise, double[] labelColor, double particleSize, double[] sizeLimits, ComponentType ct, double[] fadeIn, double[] fadeOut, boolean sync) {
        DatasetOptions dops = DatasetOptions.getParticleDatasetOptions(dsName, profileDecay, particleColor, colorNoise, labelColor, particleSize, sizeLimits, ct, fadeIn, fadeOut);
        return loadDataset(dsName, path, type, dops, sync);
    }

    @Override
    public boolean loadStarClusterDataset(String dsName, String path, double[] particleColor, double[] fadeIn, double[] fadeOut, boolean sync) {
        return loadStarClusterDataset(dsName, path, particleColor, ComponentType.Clusters.toString(), fadeIn, fadeOut, sync);
    }

    public boolean loadStarClusterDataset(String dsName, String path, List<?> particleColor, List<?> fadeIn, List<?> fadeOut, boolean sync) {
        return loadStarClusterDataset(dsName, path, dArray(particleColor), dArray(fadeIn), dArray(fadeOut), sync);
    }

    @Override
    public boolean loadStarClusterDataset(String dsName, String path, double[] particleColor, double[] labelColor, double[] fadeIn, double[] fadeOut, boolean sync) {
        return loadStarClusterDataset(dsName, path, particleColor, labelColor, ComponentType.Clusters.toString(), fadeIn, fadeOut, sync);
    }

    public boolean loadStarClusterDataset(String dsName, String path, List<?> particleColor, List<?> labelColor, List<?> fadeIn, List<?> fadeOut, boolean sync) {
        return loadStarClusterDataset(dsName, path, dArray(particleColor), dArray(labelColor), dArray(fadeIn), dArray(fadeOut), sync);
    }

    @Override
    public boolean loadStarClusterDataset(String dsName, String path, double[] particleColor, String ct, double[] fadeIn, double[] fadeOut, boolean sync) {
        ComponentType compType = ComponentType.valueOf(ct);
        DatasetOptions dops = DatasetOptions.getStarClusterDatasetOptions(dsName, particleColor, particleColor.clone(), compType, fadeIn, fadeOut);
        return loadDataset(dsName, path, CatalogInfoSource.SCRIPT, dops, sync);
    }

    public boolean loadStarClusterDataset(String dsName, String path, List<?> particleColor, String ct, List<?> fadeIn, List<?> fadeOut, boolean sync) {
        return loadStarClusterDataset(dsName, path, dArray(particleColor), ct, dArray(fadeIn), dArray(fadeOut), sync);
    }

    @Override
    public boolean loadStarClusterDataset(String dsName, String path, double[] particleColor, double[] labelColor, String ct, double[] fadeIn, double[] fadeOut, boolean sync) {
        ComponentType compType = ComponentType.valueOf(ct);
        DatasetOptions datasetOptions = DatasetOptions.getStarClusterDatasetOptions(dsName, particleColor, labelColor, compType, fadeIn, fadeOut);
        return loadDataset(dsName, path, CatalogInfoSource.SCRIPT, datasetOptions, sync);
    }

    @Override
    public boolean loadVariableStarDataset(String dsName, String path, double magnitudeScale, double[] labelColor, double[] fadeIn, double[] fadeOut, boolean sync) {
        return loadVariableStarDataset(dsName, path, CatalogInfoSource.SCRIPT, magnitudeScale, labelColor, fadeIn, fadeOut, sync);
    }

    public boolean loadVariableStarDataset(String dsName, String path, CatalogInfoSource type, double magnitudeScale, double[] labelColor, double[] fadeIn, double[] fadeOut, boolean sync) {
        DatasetOptions dops = DatasetOptions.getVariableStarDatasetOptions(dsName, magnitudeScale, labelColor, ComponentType.Stars, fadeIn, fadeOut);
        return loadDataset(dsName, path, type, dops, sync);
    }

    public boolean loadStarClusterDataset(String dsName, String path, List<?> particleColor, List<?> labelColor, String ct, List<?> fadeIn, List<?> fadeOut, boolean sync) {
        return loadStarClusterDataset(dsName, path, dArray(particleColor), dArray(labelColor), ct, dArray(fadeIn), dArray(fadeOut), sync);
    }

    private boolean loadDatasetImmediate(String dsName, String path, CatalogInfoSource type, boolean sync) {
        return loadDatasetImmediate(dsName, path, type, null, sync);
    }

    private boolean loadDatasetImmediate(String dsName, String path, CatalogInfoSource type, DatasetOptions datasetOptions, boolean sync) {
        Path p = Paths.get(path);
        if (Files.exists(p) && Files.isReadable(p)) {
            try {
                return loadDatasetImmediate(dsName, new FileDataSource(p.toFile()), type, datasetOptions, sync);
            } catch (Exception e) {
                logger.error("Error loading file: " + p, e);
            }
        } else {
            logger.error("Can't read file: " + path);
        }
        return false;
    }

    private List<IParticleRecord> loadParticleBeans(DataSource ds, DatasetOptions datasetOptions) {
        STILDataProvider provider = new STILDataProvider();
        provider.setDatasetOptions(datasetOptions);
        String catalogName = datasetOptions != null && datasetOptions.catalogName != null ? datasetOptions.catalogName : ds.getName();
        return provider.loadData(ds, 1.0f, () -> {
            // Create
            EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, catalogName, 0.01f);
        }, (current, count) -> {
            EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, catalogName, (float) current / (float) count);
            if (current % 250000 == 0) {
                logger.info(current + " objects loaded...");
            }
        }, () -> {
            // Force remove
            EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, catalogName, 2f);
        });
    }

    private boolean loadDatasetImmediate(String dsName, DataSource ds, CatalogInfoSource type, DatasetOptions datasetOptions, boolean sync) {
        try {
            logger.info(I18n.msg("notif.catalog.loading", dsName));

            // Create star/particle group or star clusters
            if (checkString(dsName, "datasetName")) {
                if (datasetOptions == null || datasetOptions.type == DatasetLoadType.STARS || datasetOptions.type == DatasetLoadType.VARIABLES) {
                    List<IParticleRecord> data = loadParticleBeans(ds, datasetOptions);
                    if (data != null && !data.isEmpty()) {
                        // STAR GROUP
                        AtomicReference<Entity> starGroup = new AtomicReference<>();
                        postRunnable(() -> {
                            if (datasetOptions != null)
                                datasetOptions.initializeCatalogInfo = false;
                            starGroup.set(EntityUtils.getStarSet(scene, dsName, ds.getName(), data, datasetOptions, false));

                            // Catalog info.
                            CatalogInfo ci = new CatalogInfo(dsName, ds.getName(), null, type, 1.5f, starGroup.get());
                            // Add to scene.
                            EventManager.publish(Event.SCENE_ADD_OBJECT_CMD, this, starGroup.get(), true);
                            // Add to catalog manager -> setUp.
                            scene.setUpEntity(starGroup.get());

                            String typeStr = datasetOptions == null || datasetOptions.type == DatasetLoadType.STARS ? I18n.msg("gui.dsload.stars.name") : I18n.msg("gui.dsload.variablestars.name");
                            logger.info(I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, dsName + ": " + I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                        });
                        // Sync waiting until the node is in the scene graph
                        while (sync && (starGroup.get() == null || Mapper.graph.get(starGroup.get()).parent != null)) {
                            sleepFrames(1);
                        }
                    }
                } else if (datasetOptions.type == DatasetLoadType.PARTICLES) {
                    // PARTICLE GROUP
                    List<IParticleRecord> data = loadParticleBeans(ds, datasetOptions);
                    if (data != null && !data.isEmpty()) {
                        AtomicReference<Entity> particleGroup = new AtomicReference<>();
                        postRunnable(() -> {
                            datasetOptions.initializeCatalogInfo = false;
                            particleGroup.set(EntityUtils.getParticleSet(scene, dsName, ds.getName(), data, datasetOptions, false));

                            // Catalog info
                            CatalogInfo ci = new CatalogInfo(dsName, ds.getName(), ds.getURL().toString(), type, 1.5f, particleGroup.get());
                            // Add to scene.
                            EventManager.publish(Event.SCENE_ADD_OBJECT_CMD, this, ci.entity, true);
                            // Add to catalog manager -> setUp
                            scene.setUpEntity(particleGroup.get());

                            String typeStr = I18n.msg("gui.dsload.objects.name");
                            logger.info(I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, dsName + ": " + I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                        });
                        // Sync waiting until the node is in the scene graph
                        while (sync && (particleGroup.get() == null || Mapper.graph.get(particleGroup.get()).parent != null)) {
                            sleepFrames(1);
                        }
                    }
                } else if (datasetOptions.type == DatasetLoadType.CLUSTERS) {
                    // STAR CLUSTERS
                    var archetype = scene.archetypes().get("gaiasky.scenegraph.GenericCatalog");
                    var entity = archetype.createEntity();

                    var base = Mapper.base.get(entity);
                    base.setName(dsName);
                    base.setCt(datasetOptions.ct.toString());

                    var body = Mapper.body.get(entity);
                    body.setColor(datasetOptions.particleColor);
                    body.setLabelColor(datasetOptions.labelColor);
                    body.setPosition(new double[] { 0, 0, 0 });

                    var fade = Mapper.fade.get(entity);
                    fade.setFadeIn(datasetOptions.fadeIn);
                    fade.setFadeOut(datasetOptions.fadeOut);

                    var graph = Mapper.graph.get(entity);
                    graph.setParent(Scene.ROOT_NAME);

                    postRunnable(() -> {
                        // Load data
                        NewStarClusterLoader scl = new NewStarClusterLoader();
                        scl.initialize(ds, scene);
                        scl.setParentName(dsName);
                        scl.loadData();
                        Array<Entity> clusters = scl.getClusters();

                        // Initialize
                        scene.initializeEntity(entity);
                        for (Entity cluster : clusters) {
                            scene.initializeEntity(cluster);
                            var cBody = Mapper.body.get(cluster);
                            cBody.setColor(datasetOptions.particleColor);
                            cBody.setLabelColor(datasetOptions.labelColor);
                        }

                        // Insert
                        scene.insert(entity, true);
                        for (Entity cluster : clusters) {
                            scene.insert(cluster, true);
                        }

                        // Finalize
                        scene.setUpEntity(entity);
                        for (Entity cluster : clusters) {
                            scene.setUpEntity(cluster);
                        }

                        String typeStr = I18n.msg("gui.dsload.clusters.name");
                        logger.info(I18n.msg("notif.catalog.loaded", graph.children.size, typeStr));
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, dsName + ": " + I18n.msg("notif.catalog.loaded", graph.children.size, typeStr));
                    });
                    // Sync waiting until the node is in the scene graph
                    while (sync && graph.parent == null) {
                        sleepFrames(1);
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
            return this.catalogManager.contains(dsName);
        }
        return false;
    }

    @Override
    public boolean removeDataset(String dsName) {
        if (checkString(dsName, "datasetName")) {
            boolean exists = this.catalogManager.contains(dsName);
            if (exists)
                postRunnable(() -> EventManager.publish(Event.CATALOG_REMOVE, this, dsName));
            else
                logger.warn("Dataset with name " + dsName + " does not exist");
            return exists;
        }
        return false;
    }

    @Override
    public boolean hideDataset(String dsName) {
        if (checkString(dsName, "datasetName")) {
            boolean exists = this.catalogManager.contains(dsName);
            if (exists) {
                postRunnable(() -> EventManager.publish(Event.CATALOG_VISIBLE, this, dsName, false));
            } else {
                logger.warn("Dataset with name " + dsName + " does not exist");
            }
            return exists;
        }
        return false;
    }

    @Override
    public boolean showDataset(String dsName) {
        if (checkString(dsName, "datasetName")) {
            boolean exists = this.catalogManager.contains(dsName);
            if (exists) {
                postRunnable(() -> EventManager.publish(Event.CATALOG_VISIBLE, this, dsName, true));
            } else {
                logger.warn("Dataset with name " + dsName + " does not exist");
            }
            return exists;
        }
        return false;
    }

    @Override
    public boolean highlightDataset(String dsName, boolean highlight) {
        if (checkString(dsName, "datasetName")) {
            boolean exists = this.catalogManager.contains(dsName);
            if (exists) {
                CatalogInfo ci = this.catalogManager.get(dsName);
                postRunnable(() -> EventManager.publish(Event.CATALOG_HIGHLIGHT, this, ci, highlight));
            } else {
                logger.warn("Dataset with name " + dsName + " does not exist");
            }
            return exists;
        }
        return false;
    }

    @Override
    public boolean highlightDataset(String dsName, int colorIndex, boolean highlight) {
        float[] color = ColorUtils.getColorFromIndex(colorIndex);
        return highlightDataset(dsName, color[0], color[1], color[2], color[3], highlight);
    }

    @Override
    public boolean highlightDataset(String dsName, float r, float g, float b, float a, boolean highlight) {
        if (checkString(dsName, "datasetName")) {
            boolean exists = this.catalogManager.contains(dsName);
            if (exists) {
                CatalogInfo ci = this.catalogManager.get(dsName);
                ci.plainColor = true;
                ci.hlColor[0] = r;
                ci.hlColor[1] = g;
                ci.hlColor[2] = b;
                ci.hlColor[3] = a;
                postRunnable(() -> EventManager.publish(Event.CATALOG_HIGHLIGHT, this, ci, highlight));
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
            boolean exists = this.catalogManager.contains(dsName);
            if (exists) {
                CatalogInfo ci = this.catalogManager.get(dsName);
                IAttribute attribute = getAttributeByName(attributeName, ci);
                int cmapIndex = getCmapIndexByName(colorMap);
                if (attribute != null && cmapIndex >= 0) {
                    ci.plainColor = false;
                    ci.hlCmapIndex = cmapIndex;
                    ci.hlCmapMin = minMap;
                    ci.hlCmapMax = maxMap;
                    ci.hlCmapAttribute = attribute;
                    postRunnable(() -> EventManager.publish(Event.CATALOG_HIGHLIGHT, this, ci, highlight));
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

            // New
            {
                if (ci.entity != null) {
                    var entity = ci.entity;
                    synchronized (focusView) {
                        focusView.setEntity(entity);
                        if (focusView.isSet()) {
                            ObjectDoubleMap.Keys<UCD> ucds = focusView.getSet().data().get(0).extraKeys();
                            for (UCD ucd : ucds)
                                if (ucd.colname.equalsIgnoreCase(name))
                                    return new AttributeUCD(ucd);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean setDatasetHighlightSizeFactor(String dsName, float sizeFactor) {
        if (checkString(dsName, "datasetName") && checkNum(sizeFactor, Constants.MIN_DATASET_SIZE_FACTOR, Constants.MAX_DATASET_SIZE_FACTOR, "sizeFactor")) {

            boolean exists = this.catalogManager.contains(dsName);
            if (exists) {
                CatalogInfo ci = this.catalogManager.get(dsName);
                ci.setHlSizeFactor(sizeFactor);
            } else {
                logger.warn("Dataset with name " + dsName + " does not exist");
            }
            return exists;
        }
        return false;
    }

    @Override
    public boolean setDatasetHighlightAllVisible(String dsName, boolean allVisible) {
        if (checkString(dsName, "datasetName")) {

            boolean exists = this.catalogManager.contains(dsName);
            if (exists) {
                CatalogInfo ci = this.catalogManager.get(dsName);
                ci.setHlAllVisible(allVisible);
            } else {
                logger.warn("Dataset with name " + dsName + " does not exist");
            }
            return exists;
        }
        return false;
    }

    @Override
    public void setDatasetPointSizeMultiplier(String dsName, double multiplier) {
        if (checkString(dsName, "datasetName")) {
            boolean exists = this.catalogManager.contains(dsName);
            if (exists) {
                em.post(Event.CATALOG_POINT_SIZE_SCALING_CMD, this, dsName, multiplier);
            } else {
                logger.warn("Catalog does not exist: " + dsName);
            }
        }
    }

    @Override
    public void addShapeAroundObject(String shapeName, String shapeType, String primitive, double size, String objectName, float r, float g, float b, float a, boolean showLabel, boolean trackObject) {
        if (checkString(shapeName, "shapeName") && checkStringEnum(shapeType, Shape.class, "shape") && checkStringEnum(primitive, Primitive.class, "primitive") && checkNum(size, 0, Double.MAX_VALUE, "size") && checkObjectName(objectName)) {
            final var shapeLc = shapeType.toLowerCase();
            postRunnable(() -> {
                // NEW
                {
                    Entity trackingObject = getFocusEntity(objectName);
                    float[] color = new float[] { r, g, b, a };
                    int primitiveInt = Primitive.valueOf(primitive.toUpperCase()).equals(Primitive.LINES) ? GL20.GL_LINES : GL20.GL_TRIANGLES;
                    // Create shape
                    Archetype at = scene.archetypes().get("gaiasky.scenegraph.ShapeObject");
                    Entity newShape = at.createEntity();

                    var base = Mapper.base.get(newShape);
                    base.setName(shapeName.trim());
                    base.ct = new ComponentTypes(ComponentType.Others.ordinal());

                    var body = Mapper.body.get(newShape);
                    body.setColor(color);
                    body.setLabelColor(new float[] { r, g, b, a });
                    body.size = (float) (size * Constants.KM_TO_U);

                    var graph = Mapper.graph.get(newShape);
                    graph.setParent(Scene.ROOT_NAME);

                    var shape = Mapper.shape.get(newShape);
                    if (trackObject) {
                        shape.track = new FocusView(trackingObject);
                    } else {
                        body.pos = EntityUtils.getAbsolutePosition(trackingObject, objectName, new Vector3b());
                    }
                    shape.trackName = objectName;

                    Map<String, Object> params = new HashMap<>();
                    params.put("quality", 25L);
                    params.put("divisions", shapeLc.equalsIgnoreCase(Shape.OCTAHEDRONSPHERE.toString()) ? 3L : 15L);
                    params.put("recursion", 3L);
                    params.put("diameter", 1.0);
                    params.put("width", 1.0);
                    params.put("height", 1.0);
                    params.put("depth", 1.0);
                    params.put("innerradius", 0.6);
                    params.put("outerradius", 1.0);
                    params.put("sphere-in-ring", false);
                    params.put("flip", false);

                    var model = Mapper.model.get(newShape);
                    model.model = new ModelComponent();
                    model.model.type = shapeLc;
                    model.model.setPrimitiveType(primitiveInt);
                    model.model.setParams(params);

                    // Initialize shape.
                    scene.initializeEntity(newShape);
                    scene.setUpEntity(newShape);

                    // Add to scene.
                    EventManager.publish(Event.SCENE_ADD_OBJECT_NO_POST_CMD, this, newShape, false);
                }
            });
        }
    }

    @Override
    public void setMaximumSimulationTime(long years) {
        Settings.settings.runtime.setMaxTime(Math.abs(years));
    }

    public void setMaximumSimulationTime(double years) {
        if (Double.isFinite(years))
            setMaximumSimulationTime((long) years);
        else
            logger.error("The number of years is not a finite number: " + years);
    }

    public void setMaximumSimulationTime(Long years) {
        setMaximumSimulationTime(years.longValue());
    }

    public void setMaximumSimulationTime(Double years) {
        if (Double.isFinite(years))
            setMaximumSimulationTime(years.longValue());
        else
            logger.error("The number of years is not a finite number: " + years);
    }

    public void setMaximumSimulationTime(Integer years) {
        setMaximumSimulationTime(years.longValue());
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
    public double[] internalUnitsToKilometres(double[] internalUnits) {
        double[] result = new double[internalUnits.length];
        for (int i = 0; i < internalUnits.length; i++) {
            result[i] = internalUnitsToKilometres(internalUnits[i]);
        }
        return result;
    }

    public double[] internalUnitsToKilometres(List<?> internalUnits) {
        double[] result = new double[internalUnits.size()];
        for (int i = 0; i < internalUnits.size(); i++) {
            result[i] = internalUnitsToKilometres((double) internalUnits.get(i));
        }
        return result;
    }

    @Override
    public double metresToInternalUnits(double metres) {
        return metres * Constants.M_TO_U;
    }

    @Override
    public double kilometresToInternalUnits(double kilometres) {
        return kilometres * Constants.KM_TO_U;
    }

    public double kilometersToInternalUnits(double kilometres) {
        return kilometres * Constants.KM_TO_U;
    }

    @Override
    public List<String> listDatasets() {
        Set<String> names = this.catalogManager.getDatasetNames();
        if (names != null)
            return new ArrayList<>(names);
        else
            return new ArrayList<>();
    }

    @Override
    public long getFrameNumber() {
        return GaiaSky.instance.frames;
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
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case INPUT_EVENT:
            inputCode = (Integer) data[0];
            break;
        case DISPOSE:
            // Stop all
            for (AtomicBoolean stop : stops) {
                if (stop != null)
                    stop.set(true);
            }
            break;
        case SCENE_LOADED:
            this.scene = (Scene) data[0];
            this.focusView.setScene(this.scene);
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

    private boolean checkFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            logger.error(name + " must be finite: " + value);
            return false;
        }
        return true;
    }

    private boolean checkFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            logger.error(name + " must be finite: " + value);
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

    private boolean checkDirectoryExists(String location, String name) {
        if (location == null) {
            logger.error(name + ": location can't be null");
            return false;
        }
        Path p = Path.of(location);
        if (Files.notExists(p)) {
            logger.error(name + ": path does not exist");
            return false;
        }
        return true;
    }

    private boolean checkObjectName(String name) {
        return getEntity(name) != null;
    }

    private boolean checkFocusName(String name) {
        Entity entity = getFocus(name);
        if (entity == null) {
            logger.error(name + ": entity does not exist");
        }
        return entity != null;
    }

    private void logPossibleValues(String value, String[] possibleValues, String name) {
        logger.error(name + " value not valid: " + value + ". Possible values are:");
        for (String v : possibleValues)
            logger.error(v);
    }

    private <T extends Enum<T>> boolean checkStringEnum(String value, Class<T> clazz, String name) {
        if (checkString(value, name)) {
            for (Enum<T> en : EnumSet.allOf(clazz)) {
                if (value.equalsIgnoreCase(en.toString())) {
                    return true;
                }
            }
            logger.error(name + " value not valid: " + value + ". Must be a value in the enum " + clazz.getSimpleName() + ":");
            for (Enum<T> en : EnumSet.allOf(clazz)) {
                logger.error(en.toString());
            }
        }
        return false;
    }

    private boolean checkNotNull(Object o, String name) {
        if (o == null) {
            logger.error(name + " can't be null");
            return false;
        }
        return true;
    }
}
