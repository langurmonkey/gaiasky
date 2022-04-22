package gaiasky.scene.component;

import com.artemis.Component;

public class Arrow extends Component {
    /**
     * Paint arrow caps
     */
    private boolean arrowCap = true;

    public void setArrowCap(boolean arrowCap) {
        this.arrowCap = arrowCap;
    }
}
