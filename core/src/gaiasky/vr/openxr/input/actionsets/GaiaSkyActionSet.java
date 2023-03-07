package gaiasky.vr.openxr.input.actionsets;

import gaiasky.vr.openxr.OpenXRDriver;
import gaiasky.vr.openxr.input.actions.Action;
import gaiasky.vr.openxr.input.actions.BoolAction;
import gaiasky.vr.openxr.input.actions.FloatAction;
import gaiasky.vr.openxr.input.actions.Vec2fAction;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GaiaSkyActionSet extends ActionSet {

    // Buttons.
    public final BoolAction ui = new BoolAction("ui");
    public final BoolAction ok = new BoolAction("ok");
    public final BoolAction use = new BoolAction("use");
    public final BoolAction cameraMode = new BoolAction("cameraMode");

    // Axes.
    public final FloatAction select = new FloatAction("select");
    public final Vec2fAction move = new Vec2fAction("move");

    public final List<Action> actions = List.of(
            ui,
            ok,
            cameraMode,
            select,
            move
    );

    public GaiaSkyActionSet() {
        super("gaiasky", 0);
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
                        new Pair<>(ui, "/user/hand/right/input/a/click"),
                        new Pair<>(ui, "/user/hand/left/input/x/click"),
                        new Pair<>(ok, "/user/hand/right/input/a/click"),
                        new Pair<>(ok, "/user/hand/left/input/x/click"),
                        new Pair<>(cameraMode, "/user/hand/right/input/b/click"),
                        new Pair<>(cameraMode, "/user/hand/left/input/y/click"),
                        new Pair<>(select, "/user/hand/right/input/trigger/value"),
                        new Pair<>(select, "/user/hand/left/input/trigger/value"),
                        new Pair<>(move, "/user/hand/right/input/thumbstick"),
                        new Pair<>(move, "/user/hand/left/input/thumbstick")
                ));

        // Valve index
        map.computeIfAbsent("/interaction_profiles/valve/index_controller", aLong -> new ArrayList<>()).addAll(
                List.of(
                        new Pair<>(ui, "/user/hand/right/input/b/click"),
                        new Pair<>(ui, "/user/hand/left/input/b/click"),
                        new Pair<>(ok, "/user/hand/right/input/b/click"),
                        new Pair<>(ok, "/user/hand/left/input/b/click"),
                        new Pair<>(cameraMode, "/user/hand/right/input/a/click"),
                        new Pair<>(cameraMode, "/user/hand/left/input/a/click"),
                        new Pair<>(select, "/user/hand/right/input/trigger/value"),
                        new Pair<>(select, "/user/hand/left/input/trigger/value"),
                        new Pair<>(move, "/user/hand/right/input/thumbstick"),
                        new Pair<>(move, "/user/hand/left/input/thumbstick")
                )
        );
    }
}
