package gaiasky.vr.openxr.input.actionsets;

import gaiasky.vr.openxr.input.actions.*;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static gaiasky.vr.openxr.input.actions.Action.DeviceType.Left;
import static gaiasky.vr.openxr.input.actions.Action.DeviceType.Right;

public class GaiaSkyActionSet extends ActionSet {

    // Buttons.
    public final BoolAction showUiLeft = new BoolAction("show-ui-left", "Show UI (left)", Left);
    public final BoolAction showUiRight = new BoolAction("show-ui-right", "Show UI (right)", Right);
    public final BoolAction acceptLeft = new BoolAction("accept-left", "Accept (left)", Left);
    public final BoolAction acceptRight = new BoolAction("accept-right", "Accept (right)", Right);
    public final BoolAction cameraModeLeft = new BoolAction("camera-mode-left", "Camera mode (left)", Left);
    public final BoolAction cameraModeRight = new BoolAction("camera-mode-right", "Camera mode (right)", Right);

    // Axes.
    public final FloatAction selectLeft = new FloatAction("select-object-left", "Select object/UI (left)", Left);
    public final FloatAction selectRight = new FloatAction("select-object-right", "Select object/UI (right)", Right);
    public final Vec2fAction moveLeft = new Vec2fAction("move-left", "Move (left)", Left);
    public final Vec2fAction moveRight = new Vec2fAction("move-right", "Move (right)", Right);

    // Poses.
    public PoseAction poseLeft = new PoseAction("left-pose", "Left grip pose", Left);
    public PoseAction poseRight = new PoseAction("right-pose", "Right grip pose", Right);

    // Haptics.
    public HapticsAction hapticLeft = new HapticsAction("haptics-left", "Haptic left", Left);
    public HapticsAction hapticRight = new HapticsAction("haptics-right", "Haptic right", Right);

    public final List<Action> actions = List.of(
            poseLeft,
            poseRight,
            hapticLeft,
            hapticRight,
            showUiLeft,
            showUiRight,
            acceptLeft,
            acceptRight,
            cameraModeLeft,
            cameraModeRight,
            selectLeft,
            selectRight,
            moveLeft,
            moveRight
    );

    public GaiaSkyActionSet() {
        super("gaiasky", "Gaia Sky actions", 0);
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
                        new Pair<>(poseLeft, "/user/hand/left/input/aim/pose"),
                        new Pair<>(poseRight, "/user/hand/right/input/aim/pose"),

                        new Pair<>(hapticLeft, "/user/hand/left/output/haptic"),
                        new Pair<>(hapticRight, "/user/hand/right/output/haptic"),

                        new Pair<>(showUiLeft, "/user/hand/left/input/x/click"),
                        new Pair<>(showUiRight, "/user/hand/right/input/a/click"),

                        new Pair<>(acceptLeft, "/user/hand/left/input/x/click"),
                        new Pair<>(acceptRight, "/user/hand/right/input/a/click"),

                        new Pair<>(cameraModeLeft, "/user/hand/left/input/y/click"),
                        new Pair<>(cameraModeRight, "/user/hand/right/input/b/click"),

                        new Pair<>(selectLeft, "/user/hand/left/input/trigger/value"),
                        new Pair<>(selectRight, "/user/hand/right/input/trigger/value"),

                        new Pair<>(moveLeft, "/user/hand/left/input/thumbstick"),
                        new Pair<>(moveRight, "/user/hand/right/input/thumbstick")
                ));

        // Valve index
        map.computeIfAbsent("/interaction_profiles/valve/index_controller", aLong -> new ArrayList<>()).addAll(
                List.of(
                        new Pair<>(poseLeft, "/user/hand/left/input/aim/pose"),
                        new Pair<>(poseRight, "/user/hand/right/input/aim/pose"),

                        new Pair<>(hapticLeft, "/user/hand/left/output/haptic"),
                        new Pair<>(hapticRight, "/user/hand/right/output/haptic"),

                        new Pair<>(showUiLeft, "/user/hand/left/input/b/click"),
                        new Pair<>(showUiRight, "/user/hand/right/input/b/click"),

                        new Pair<>(acceptLeft, "/user/hand/left/input/b/click"),
                        new Pair<>(acceptRight, "/user/hand/right/input/b/click"),

                        new Pair<>(cameraModeLeft, "/user/hand/left/input/a/click"),
                        new Pair<>(cameraModeRight, "/user/hand/right/input/a/click"),

                        new Pair<>(selectLeft, "/user/hand/left/input/trigger/value"),
                        new Pair<>(selectRight, "/user/hand/right/input/trigger/value"),

                        new Pair<>(moveLeft, "/user/hand/left/input/thumbstick"),
                        new Pair<>(moveRight, "/user/hand/right/input/thumbstick")
                )
        );
    }
}
