package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.OpenXRDriver;
import gaiasky.vr.openxr.XrException;

public interface SessionAwareAction {

    void createHandleSession(OpenXRDriver driver) throws XrException;

    void destroyHandleSession();
}
