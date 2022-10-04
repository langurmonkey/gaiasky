package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.scene.record.RotationComponent;

public class ParentOrientation implements Component {

    public boolean parentOrientation = false;
    public Matrix4 orientationf;
    public RotationComponent parentrc;

}
