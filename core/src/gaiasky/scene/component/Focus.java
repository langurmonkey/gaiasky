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
import gaiasky.util.math.Vector3d;

public class Focus implements Component {

    /** Consumer that returns whether the focus is active or not. **/
    public Consumer3<FocusActive, Entity, Base> activeConsumer;

    /** Consumer that computes whether the focus is hit by a ray. **/
    public Consumer6<FocusHit, FocusView, Vector3d, Vector3d, NaturalCamera, Array<IFocus>> hitRayConsumer;

    /** Consumer that computes whether the focus is hit by the given screen coordinates. **/
    public Consumer9<FocusHit, FocusView, Integer, Integer, Integer, Integer, Integer, NaturalCamera, Array<IFocus>> hitCoordinatesConsumer;
}
