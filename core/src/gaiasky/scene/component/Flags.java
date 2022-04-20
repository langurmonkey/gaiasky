package gaiasky.scene.component;

import com.artemis.Component;

public class Flags extends Component {

    /**
     * Flag indicating whether the object has been computed in this step.
     */
    public boolean computed = true;

    /**
     * Is this node visible?
     */
    protected boolean visible = true;

    /**
     * Force to render the label of this entity,
     * bypassing the solid angle check
     */
    protected boolean forceLabel = false;

    /**
     * Is this just a copy?
     */
    public boolean copy = false;
}
