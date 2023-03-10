package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.XrDriver;

public interface SpaceAwareAction {

    void createActionSpace(XrDriver driver);

    void destroyActionSpace();
}
