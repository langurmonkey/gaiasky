package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scene.view.FocusView;

import java.util.Map;

public class Shape implements Component {

    public String modelShape;
    public Map<String, Object> modelParams;
    public int primitiveType;
    public FocusView track;
    public String trackName;
}
