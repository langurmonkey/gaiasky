package gaiasky.scene.component;

import com.artemis.Component;
import gaiasky.util.filter.attrib.IAttribute;

public class Highlight extends Component {
    /**
     * Is it highlighted?
     */
    public boolean highlighted = false;
    // Plain color for highlighting
    public boolean hlplain = false;
    // Highlight color
    public float[] hlc;
    // Highlight all visible
    public boolean hlallvisible = true;
    // Highlight colormap index
    public int hlcmi;
    // Highlight colormap attribute
    public IAttribute hlcma;
    // Highlight colormap min
    public double hlcmmin;
    // Highlight colormap max
    public double hlcmmax;
    // Point size scaling
    public float pointscaling = 1;
}
