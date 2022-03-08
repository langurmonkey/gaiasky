package gaiasky.render;

import gaiasky.util.tree.LoadStatus;

public interface IStatusObject {
    public LoadStatus getStatus();
    public void setStatus(LoadStatus status);
}
