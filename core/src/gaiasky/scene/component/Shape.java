package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scene.view.FocusView;

public class Shape implements Component {

    public FocusView track;
    public String trackName;

    public boolean focusable = false;

    public void setFocusable(Boolean focusable) {
        this.focusable = focusable;
    }

}
