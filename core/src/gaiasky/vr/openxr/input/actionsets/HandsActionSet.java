package gaiasky.vr.openxr.input.actionsets;

import gaiasky.vr.openxr.input.actions.Action;
import gaiasky.vr.openxr.input.actions.HapticsAction;
import gaiasky.vr.openxr.input.actions.PoseAction;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static gaiasky.vr.openxr.input.actions.Action.DeviceType.Left;
import static gaiasky.vr.openxr.input.actions.Action.DeviceType.Right;

public class HandsActionSet extends ActionSet {

    public PoseAction poseLeft = new PoseAction("left-pose", "Left grip pose", Left);
    public PoseAction poseRight = new PoseAction("right-pose", "Right grip pose", Right);
    public HapticsAction hapticLeft = new HapticsAction("haptics-left", "Haptic left", Left);
    public HapticsAction hapticRight = new HapticsAction("haptics-right", "Haptic right", Right);

    public HandsActionSet() {
        super("hands", "Hand poses", 0);
    }

    @Override
    public List<Action> actions() {
        return List.of(poseLeft, poseRight, hapticLeft, hapticRight
        );
    }

    @Override
    public void getDefaultBindings(HashMap<String, List<Pair<Action, String>>> map) {

        // OCULUS TOUCH
        map.computeIfAbsent("/interaction_profiles/oculus/touch_controller", aLong -> new ArrayList<>()).addAll(
                List.of(
                        new Pair<>(poseLeft, "/user/hand/left/input/grip/pose"),
                        new Pair<>(poseRight, "/user/hand/right/input/grip/pose"),
                        new Pair<>(hapticLeft, "/user/hand/left/output/haptic"),
                        new Pair<>(hapticRight, "/user/hand/right/output/haptic")
                )
        );

        // VALVE INDEX
        map.computeIfAbsent("/interaction_profiles/valve/index_controller", aLong -> new ArrayList<>()).addAll(
                List.of(
                        new Pair<>(poseLeft, "/user/hand/left/input/grip/pose"),
                        new Pair<>(poseRight, "/user/hand/right/input/grip/pose"),
                        new Pair<>(hapticLeft, "/user/hand/left/output/haptic"),
                        new Pair<>(hapticRight, "/user/hand/right/output/haptic")
                )
        );

    }
}
