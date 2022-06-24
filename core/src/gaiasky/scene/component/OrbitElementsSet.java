package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.scenegraph.SceneGraphNode;

public class OrbitElementsSet implements Component {
    public Array<Entity> alwaysUpdate;
    public boolean initialUpdate = true;
}
