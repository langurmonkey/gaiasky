package gaiasky.scene.component;

import com.artemis.Component;
import gaiasky.render.ComponentTypes;

public class Base extends Component {
    /**
     * The internal identifier
     **/
    public long id = -1;

    /**
     * The name(s) of the node, if any.
     */
    public String[] names;

    /**
     * The index of the localized name in the {@link #names} array.
     */
    public int localizedNameIndex = 0;

    /**
     * The first name of the parent object.
     */
    public String parentName = null;

    /**
     * Time of last visibility change in milliseconds
     */
    protected long lastStateChangeTimeMs = 0;

    /**
     * The ownOpacity value (alpha)
     */
    public float opacity = 1f;

    /**
     * Component types, for managing visibility
     */
    public ComponentTypes ct;
}
