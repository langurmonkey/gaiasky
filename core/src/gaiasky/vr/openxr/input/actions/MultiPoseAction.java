package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.OpenXRDriver;
import gaiasky.vr.openxr.OpenXRState;
import org.lwjgl.PointerBuffer;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;

import java.util.Arrays;
import java.util.List;

import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.system.MemoryStack.stackCallocPointer;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

public class MultiPoseAction extends Action implements SpaceAwareAction, InputAction {

    private static final XrActionStateGetInfo getInfo = XrActionStateGetInfo.calloc().type(XR_TYPE_ACTION_STATE_GET_INFO);
    private static final XrActionStatePose state = XrActionStatePose.calloc().type(XR_TYPE_ACTION_STATE_POSE);
    private static final XrSpaceLocation location = XrSpaceLocation.calloc();
    private static final XrPosef pose = XrPosef.calloc();

    public final int amount;
    public final List<String> subActionPathsStr;
    public XrSpace[] spaces;
    public VRControllerDevice[] poses;

    public MultiPoseAction(String name, String localizedName, String[] subActionPathsStr) {
        super(name, localizedName, XR10.XR_ACTION_TYPE_POSE_INPUT);
        this.subActionPathsStr = Arrays.asList(subActionPathsStr);
        this.amount = subActionPathsStr.length;
        this.poses = new VRControllerDevice[amount];
        for (int i = 0; i < amount; i++) {
            this.poses[i] = new VRControllerDevice();
        }
    }

    @Override
    public void createHandle(XrActionSet actionSet, OpenXRDriver driver) {
        try (var stack = stackPush()) {

            var subActionPaths = stack.callocLong(amount);

            for (int i = 0; i < amount; i++) {
                var str = subActionPathsStr.get(i);
                subActionPaths.put(i, driver.getPath(str));
            }

            XrActionCreateInfo actionCreateInfo = XrActionCreateInfo.calloc(stack).set(
                    XR10.XR_TYPE_ACTION_CREATE_INFO,
                    NULL,
                    memUTF8(name),
                    type,
                    amount,
                    subActionPaths,
                    memUTF8(localizedName)
            );
            PointerBuffer pp = stackCallocPointer(1);
            driver.check(XR10.xrCreateAction(actionSet, actionCreateInfo, pp));
            handle = new XrAction(pp.get(), actionSet);
        }
        createActionSpace(driver);
    }

    @Override
    public void createActionSpace(OpenXRDriver driver) {
        try (MemoryStack stack = stackPush()) {
            spaces = new XrSpace[amount];
            for (int i = 0; i < amount; i++) {
                XrActionSpaceCreateInfo createInfo = XrActionSpaceCreateInfo.malloc(stack)
                        .type$Default()
                        .poseInActionSpace(XrPosef.malloc(stack)
                                .position$(XrVector3f.calloc(stack).set(0, 0, 0))
                                .orientation(XrQuaternionf.malloc(stack)
                                        .x(0)
                                        .y(0)
                                        .z(0)
                                        .w(1)))
                        .action(handle)
                        .subactionPath(driver.getPath(subActionPathsStr.get(i)));

                PointerBuffer pp = stack.mallocPointer(1);
                driver.check(xrCreateActionSpace(driver.xrSession, createInfo, pp), "xrCreateActionSpace");
                spaces[i] = new XrSpace(pp.get(0), driver.xrSession);
            }
        }
    }

    public void bakActionSpace(OpenXRDriver driver) {
        try (var stack = stackPush()) {
            spaces = new XrSpace[amount];
            for (int i = 0; i < amount; i++) {
                XrActionSpaceCreateInfo createInfo = XrActionSpaceCreateInfo.calloc(stack).set(
                        XR10.XR_TYPE_ACTION_SPACE_CREATE_INFO,
                        NULL,
                        handle,
                        driver.getPath(subActionPathsStr.get(i)),
                        OpenXRState.POSE_IDENTITY
                );
                PointerBuffer pp = stackCallocPointer(1);
                driver.check(XR10.xrCreateActionSpace(driver.xrSession, createInfo, pp), "xrCreateActionSpace");
                spaces[i] = new XrSpace(pp.get(0), driver.xrSession);
            }
        }
    }

    @Override
    public void sync(OpenXRDriver driver) {
        for (int i = 0; i < amount; i++) {
            getInfo.subactionPath(driver.getPath(subActionPathsStr.get(i)));
            getInfo.action(handle);
            driver.check(XR10.xrGetActionStatePose(driver.xrSession, getInfo, state), "xrGetActionStatePose");
            poses[i].active = state.isActive();
            if (poses[i].active) {
                location.set(XR_TYPE_SPACE_LOCATION, NULL, 0, pose);
                driver.check(xrLocateSpace(spaces[i], driver.xrAppSpace, driver.currentFrameTime, location));
                if ((location.locationFlags() & XR_SPACE_LOCATION_POSITION_VALID_BIT) != 0 &&
                        (location.locationFlags() & XR_SPACE_LOCATION_ORIENTATION_VALID_BIT) != 0) {
                    // Ok!
                    var pos = location.pose().position$();
                    var orientation = location.pose().orientation();
                    poses[i].position.set(pos.x(), pos.y(), pos.z());
                    poses[i].orientation.set(orientation.x(), orientation.y(), orientation.z(), orientation.w());
                }
            }
        }
    }

    @Override
    public void destroyActionSpace() {
        if (spaces != null) {
            for (var space : spaces) {
                XR10.xrDestroySpace(space);
            }
        }
    }
}
