package gaiasky.vr.openxr.input.actionsets;

import gaiasky.vr.openxr.input.actions.Action;
import gaiasky.vr.openxr.input.actions.HapticsAction;
import gaiasky.vr.openxr.input.actions.PoseAction;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HandsActionSet extends ActionSet {

    public PoseAction leftPose = new PoseAction("left-pose", "Left grip pose");
    public PoseAction rightPose = new PoseAction("right-pose", "Right grip pose");
    public HapticsAction leftHaptic = new HapticsAction("haptics-left", "Haptic left");
    public HapticsAction rightHaptic = new HapticsAction("haptics-right", "Haptic right");

    public HandsActionSet() {
        super("hands", "Hand poses", 0);
    }

    @Override
    public List<Action> actions() {
        return List.of(
                leftPose,
                rightPose,
                leftHaptic,
                rightHaptic
        );
    }

    @Override
    public void getDefaultBindings(HashMap<String, List<Pair<Action, String>>> map) {

        // OCULUS TOUCH
        map.computeIfAbsent("/interaction_profiles/oculus/touch_controller", aLong -> new ArrayList<>()).addAll(
                List.of(
                        new Pair<>(leftPose, "/user/hand/left/input/grip/pose"),
                        new Pair<>(rightPose, "/user/hand/right/input/grip/pose"),
                        new Pair<>(leftHaptic, "/user/hand/left/output/haptic"),
                        new Pair<>(rightHaptic, "/user/hand/right/output/haptic")
                )
        );

        // VALVE INDEX
        map.computeIfAbsent("/interaction_profiles/valve/index_controller", aLong -> new ArrayList<>()).addAll(
                List.of(
                        new Pair<>(leftPose, "/user/hand/left/input/grip/pose"),
                        new Pair<>(rightPose, "/user/hand/right/input/grip/pose"),
                        new Pair<>(leftHaptic, "/user/hand/left/output/haptic"),
                        new Pair<>(rightHaptic, "/user/hand/right/output/haptic")
                )
        );

    }
}
