package gaiasky.vr.openxr.input.actionsets;

import gaiasky.vr.openxr.input.XrControllerDevice;
import gaiasky.vr.openxr.input.XrInputListener;
import gaiasky.vr.openxr.input.actions.*;
import gaiasky.vr.openxr.input.actions.PoseAction.PoseType;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GaiaSkyActionSet extends ActionSet {
    public final XrControllerDevice deviceLeft, deviceRight;
    public final List<Action> actions;

    public GaiaSkyActionSet(XrControllerDevice deviceLeft, XrControllerDevice deviceRight) {
        super("gaiasky", "Gaia Sky actions", 0);
        this.deviceLeft = deviceLeft;
        this.deviceRight = deviceRight;

        // Buttons.
        deviceLeft.showUi = new BoolAction("show-ui-left", "Show UI (left)", deviceLeft);
        deviceRight.showUi = new BoolAction("show-ui-right", "Show UI (right)", deviceRight);
        deviceLeft.accept = new BoolAction("accept-left", "Accept (left)", deviceLeft);
        deviceRight.accept = new BoolAction("accept-right", "Accept (right)", deviceRight);
        deviceLeft.cameraMode = new BoolAction("camera-mode-left", "Camera mode (left)", deviceLeft);
        deviceRight.cameraMode = new BoolAction("camera-mode-right", "Camera mode (right)", deviceRight);

        deviceLeft.select = new FloatAction("select-object-left", "Select object/UI (left)", deviceLeft);
        deviceRight.select = new FloatAction("select-object-right", "Select object/UI (right)", deviceRight);
        deviceLeft.move = new Vec2fAction("move-left", "Move (left)", deviceLeft);
        deviceRight.move = new Vec2fAction("move-right", "Move (right)", deviceRight);

        // Poses.
        deviceLeft.gripPose = new PoseAction("left-grip", "Left grip pose", PoseType.GRIP, deviceLeft);
        deviceRight.gripPose = new PoseAction("right-grip", "Right grip pose", PoseType.GRIP, deviceRight);
        deviceLeft.aimPose = new PoseAction("left-aim", "Left aim pose", PoseType.AIM, deviceLeft);
        deviceRight.aimPose = new PoseAction("right-aim", "Right aim pose", PoseType.AIM, deviceRight);

        // Haptics.
        deviceLeft.haptics = new HapticsAction("haptics-left", "Haptic left", deviceLeft);
        deviceRight.haptics = new HapticsAction("haptics-right", "Haptic right", deviceRight);

        actions = List.of(
                deviceLeft.gripPose,
                deviceRight.gripPose,
                deviceLeft.aimPose,
                deviceRight.aimPose,
                deviceLeft.haptics,
                deviceRight.haptics,
                deviceLeft.showUi,
                deviceRight.showUi,
                deviceLeft.accept,
                deviceRight.accept,
                deviceLeft.cameraMode,
                deviceRight.cameraMode,
                deviceLeft.select,
                deviceRight.select,
                deviceLeft.move,
                deviceRight.move
        );
    }

    public void processListener(XrInputListener listener) {
        deviceLeft.processListener(listener);
        deviceRight.processListener(listener);
    }

    @Override
    public List<Action> actions() {
        return actions;
    }

    @Override
    public boolean shouldSync() {
        return true;
    }

    public void getDefaultBindings(HashMap<String, List<Pair<Action, String>>> map) {

        // Oculus touch.
        map.computeIfAbsent("/interaction_profiles/oculus/touch_controller", aLong -> new ArrayList<>()).addAll(
                List.of(
                        new Pair<>(deviceLeft.aimPose, "/user/hand/left/input/aim/pose"),
                        new Pair<>(deviceRight.aimPose, "/user/hand/right/input/aim/pose"),

                        new Pair<>(deviceLeft.gripPose, "/user/hand/left/input/grip/pose"),
                        new Pair<>(deviceRight.gripPose, "/user/hand/right/input/grip/pose"),

                        new Pair<>(deviceLeft.haptics, "/user/hand/left/output/haptic"),
                        new Pair<>(deviceRight.haptics, "/user/hand/right/output/haptic"),

                        new Pair<>(deviceLeft.showUi, "/user/hand/left/input/y/click"),
                        new Pair<>(deviceRight.showUi, "/user/hand/right/input/b/click"),

                        new Pair<>(deviceLeft.accept, "/user/hand/left/input/y/click"),
                        new Pair<>(deviceRight.accept, "/user/hand/right/input/b/click"),

                        new Pair<>(deviceLeft.cameraMode, "/user/hand/left/input/x/click"),
                        new Pair<>(deviceRight.cameraMode, "/user/hand/right/input/a/click"),

                        new Pair<>(deviceLeft.select, "/user/hand/left/input/trigger/value"),
                        new Pair<>(deviceRight.select, "/user/hand/right/input/trigger/value"),

                        new Pair<>(deviceLeft.move, "/user/hand/left/input/thumbstick"),
                        new Pair<>(deviceRight.move, "/user/hand/right/input/thumbstick")
                ));

        // Valve index
        map.computeIfAbsent("/interaction_profiles/valve/index_controller", aLong -> new ArrayList<>()).addAll(
                List.of(
                        new Pair<>(deviceLeft.aimPose, "/user/hand/left/input/aim/pose"),
                        new Pair<>(deviceRight.aimPose, "/user/hand/right/input/aim/pose"),

                        new Pair<>(deviceLeft.gripPose, "/user/hand/left/input/grip/pose"),
                        new Pair<>(deviceRight.gripPose, "/user/hand/right/input/grip/pose"),

                        new Pair<>(deviceLeft.haptics, "/user/hand/left/output/haptic"),
                        new Pair<>(deviceRight.haptics, "/user/hand/right/output/haptic"),

                        new Pair<>(deviceLeft.showUi, "/user/hand/left/input/b/click"),
                        new Pair<>(deviceRight.showUi, "/user/hand/right/input/b/click"),

                        new Pair<>(deviceLeft.accept, "/user/hand/left/input/b/click"),
                        new Pair<>(deviceRight.accept, "/user/hand/right/input/b/click"),

                        new Pair<>(deviceLeft.cameraMode, "/user/hand/left/input/a/click"),
                        new Pair<>(deviceRight.cameraMode, "/user/hand/right/input/a/click"),

                        new Pair<>(deviceLeft.select, "/user/hand/left/input/trigger/value"),
                        new Pair<>(deviceRight.select, "/user/hand/right/input/trigger/value"),

                        new Pair<>(deviceLeft.move, "/user/hand/left/input/thumbstick"),
                        new Pair<>(deviceRight.move, "/user/hand/right/input/thumbstick")
                ));

        // HTC vive controller
        map.computeIfAbsent("/interaction_profiles/htc/vive_controller", aLong -> new ArrayList<>()).addAll(
                List.of(
                        new Pair<>(deviceLeft.aimPose, "/user/hand/left/input/aim/pose"),
                        new Pair<>(deviceRight.aimPose, "/user/hand/right/input/aim/pose"),

                        new Pair<>(deviceLeft.gripPose, "/user/hand/left/input/grip/pose"),
                        new Pair<>(deviceRight.gripPose, "/user/hand/right/input/grip/pose"),

                        new Pair<>(deviceLeft.haptics, "/user/hand/left/output/haptic"),
                        new Pair<>(deviceRight.haptics, "/user/hand/right/output/haptic"),

                        new Pair<>(deviceLeft.showUi, "/user/hand/left/input/menu/click"),
                        new Pair<>(deviceRight.showUi, "/user/hand/right/input/menu/click"),

                        new Pair<>(deviceLeft.accept, "/user/hand/left/input/menu/click"),
                        new Pair<>(deviceRight.accept, "/user/hand/right/input/menu/click"),

                        new Pair<>(deviceLeft.cameraMode, "/user/hand/left/input/trackpad/click"),
                        new Pair<>(deviceRight.cameraMode, "/user/hand/right/input/trackpad/click"),

                        new Pair<>(deviceLeft.select, "/user/hand/left/input/trigger/value"),
                        new Pair<>(deviceRight.select, "/user/hand/right/input/trigger/value"),

                        new Pair<>(deviceLeft.move, "/user/hand/left/input/trackpad"),
                        new Pair<>(deviceRight.move, "/user/hand/right/input/trackpad")
                ));

        // Microsoft motion controller
        map.computeIfAbsent("/interaction_profiles/microsoft/motion_controller", aLong -> new ArrayList<>()).addAll(
                List.of(
                        new Pair<>(deviceLeft.aimPose, "/user/hand/left/input/aim/pose"),
                        new Pair<>(deviceRight.aimPose, "/user/hand/right/input/aim/pose"),

                        new Pair<>(deviceLeft.gripPose, "/user/hand/left/input/grip/pose"),
                        new Pair<>(deviceRight.gripPose, "/user/hand/right/input/grip/pose"),

                        new Pair<>(deviceLeft.haptics, "/user/hand/left/output/haptic"),
                        new Pair<>(deviceRight.haptics, "/user/hand/right/output/haptic"),

                        new Pair<>(deviceLeft.showUi, "/user/hand/left/input/trackpad/click"),
                        new Pair<>(deviceRight.showUi, "/user/hand/right/input/trackpad/click"),

                        new Pair<>(deviceLeft.accept, "/user/hand/left/input/trackpad/click"),
                        new Pair<>(deviceRight.accept, "/user/hand/right/input/trackpad/click"),

                        new Pair<>(deviceLeft.cameraMode, "/user/hand/left/input/thumbstick/click"),
                        new Pair<>(deviceRight.cameraMode, "/user/hand/right/input/thumbstick/click"),

                        new Pair<>(deviceLeft.select, "/user/hand/left/input/trigger/value"),
                        new Pair<>(deviceRight.select, "/user/hand/right/input/trigger/value"),

                        new Pair<>(deviceLeft.move, "/user/hand/left/input/thumbstick"),
                        new Pair<>(deviceRight.move, "/user/hand/right/input/thumbstick")
                ));
    }
}
