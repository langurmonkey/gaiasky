package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.scene.view.IsFocusActive;
import gaiasky.util.Consumers.Consumer3;

public class Focus implements Component {

    public Consumer3<IsFocusActive, Entity, Base> activeConsumer;
}
