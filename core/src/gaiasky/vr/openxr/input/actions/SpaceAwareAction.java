package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.OpenXRDriver;

public interface SpaceAwareAction {

    void createActionSpace(OpenXRDriver driver);

    void destroyActionSpace();
}
