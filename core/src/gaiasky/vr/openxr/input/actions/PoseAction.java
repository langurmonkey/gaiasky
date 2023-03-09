package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.OpenXRDriver;
import org.lwjgl.PointerBuffer;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class PoseAction extends Action implements SpaceAwareAction, InputAction {

    private static final XrActionStateGetInfo getInfo = XrActionStateGetInfo.calloc().type(XR_TYPE_ACTION_STATE_GET_INFO);
    private static final XrActionStatePose state = XrActionStatePose.calloc().type(XR_TYPE_ACTION_STATE_POSE);
    private static final XrSpaceLocation location = XrSpaceLocation.calloc();
    private static final XrPosef xrPose = XrPosef.calloc();

    public XrSpace space;
    public PoseBean pose;

    public PoseAction(String name, String localizedName) {
        super(name, localizedName, XR_ACTION_TYPE_POSE_INPUT);
        pose = new PoseBean();
    }

    @Override
    public void createHandle(XrActionSet actionSet, OpenXRDriver driver) {
        super.createHandle(actionSet, driver);
        createActionSpace(driver);
    }

    @Override
    public void createActionSpace(OpenXRDriver driver) {
        try (MemoryStack stack = stackPush()) {
            XrActionSpaceCreateInfo createInfo = XrActionSpaceCreateInfo.malloc(stack)
                    .type$Default()
                    .next(NULL)
                    .poseInActionSpace(XrPosef.malloc(stack)
                            .position$(XrVector3f.calloc(stack).set(0, 0, 0))
                            .orientation(XrQuaternionf.malloc(stack)
                                    .x(0)
                                    .y(0)
                                    .z(0)
                                    .w(1)))
                    .action(handle);

            PointerBuffer pp = stack.mallocPointer(1);
            driver.check(xrCreateActionSpace(driver.xrSession, createInfo, pp), "xrCreateActionSpace");
            space = new XrSpace(pp.get(0), driver.xrSession);
        }

    }

    @Override
    public void sync(OpenXRDriver driver) {
        getInfo.action(handle);
        driver.check(XR10.xrGetActionStatePose(driver.xrSession, getInfo, state), "xrGetActionStatePose");
        pose.active = state.isActive();
        if (pose.active) {
            location.set(XR_TYPE_SPACE_LOCATION, NULL, 0, xrPose);
            driver.check(xrLocateSpace(space, driver.xrAppSpace, driver.currentFrameTime, location));
            if ((location.locationFlags() & XR_SPACE_LOCATION_POSITION_VALID_BIT) != 0 &&
                    (location.locationFlags() & XR_SPACE_LOCATION_ORIENTATION_VALID_BIT) != 0) {
                // Ok!
                var pos = location.pose().position$();
                var orientation = location.pose().orientation();
                pose.position.set(pos.x(), pos.y(), pos.z());
                pose.orientation.set(orientation.x(), orientation.y(), orientation.z(), orientation.w());
            }
        }
    }

    @Override
    public void destroyActionSpace() {

    }
}
