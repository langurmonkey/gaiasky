package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.scene.entity.FocusActive;
import gaiasky.scene.entity.FocusHit;
import gaiasky.scene.view.FocusView;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.Consumers.*;
import gaiasky.util.Functions.Function3;
import gaiasky.util.math.Vector3d;

public class Focus implements Component {

    /** Consumer that returns whether the focus is active or not. **/
    public Function3<FocusActive, Entity, Base, Boolean> activeFunction;

    /** Consumer that computes whether the focus is hit by a ray. **/
    public Consumer6<FocusHit, FocusView, Vector3d, Vector3d, NaturalCamera, Array<Entity>> hitRayConsumer;

    /** Consumer that computes whether the focus is hit by the given screen coordinates. **/
    public Consumer9<FocusHit, FocusView, Integer, Integer, Integer, Integer, Integer, NaturalCamera, Array<Entity>> hitCoordinatesConsumer;
}
