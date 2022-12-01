package gaiasky.gui.beans;

import gaiasky.render.ComponentTypes.ComponentType;

public class ComponentTypeBean {

    public ComponentType ct;

    public ComponentTypeBean(ComponentType ct) {
        this.ct = ct;
    }

    public String toString() {
        return ct.getName();
    }
}
