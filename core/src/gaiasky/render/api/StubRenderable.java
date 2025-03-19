package gaiasky.render.api;

import gaiasky.render.ComponentTypes;

public final class StubRenderable implements IRenderable{
    @Override
    public ComponentTypes getComponentType() {
        return null;
    }

    @Override
    public double getDistToCamera() {
        return 0;
    }

    @Override
    public float getOpacity() {
        return 0;
    }
}
