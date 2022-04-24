package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

public class Arrow implements Component {
    /**
     * Paint arrow caps
     */
    private boolean arrowCap = true;

    public void setArrowCap(boolean arrowCap) {
        this.arrowCap = arrowCap;
    }
}
