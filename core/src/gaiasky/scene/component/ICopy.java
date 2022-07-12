package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;

public interface ICopy {

    Component getCopy(Engine engine);

}
