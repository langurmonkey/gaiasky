package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.GL20;
import gaiasky.gui.beans.PrimitiveComboBoxBean.Primitive;
import gaiasky.scene.view.FocusView;

import java.util.Map;

public class Shape implements Component {

    public FocusView track;
    public String trackName;

}
