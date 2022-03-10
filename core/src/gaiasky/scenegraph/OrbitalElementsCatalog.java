package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

/**
 * This class acts as a catalog of orbital element objects.
 * It compiles the orbit objects which are only-body and those which are not, and
 * updates them accordingly to reduce CPU calls.
 */
public class OrbitalElementsCatalog extends FadeNode implements IObserver {
    private Array<Orbit> orbitsWithOrbit;
    private int rendered = -1;

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);

        // Check children which need updating every time
        orbitsWithOrbit = new Array<>();
        if (children != null && children.size > 0) {
            for (SceneGraphNode sgn : children) {
                if (sgn instanceof Orbit) {
                    Orbit orbit = (Orbit) sgn;
                    if (!orbit.onlyBody) {
                        orbitsWithOrbit.add(orbit);
                    }
                }
            }
        }
        EventManager.instance.subscribe(this, Event.INITIALIZED_INFO);
    }

    public void update(ITimeFrameProvider time, final Vector3b parentTransform, ICamera camera, float opacity) {
        this.opacity = opacity;
        translation.set(parentTransform);
        Vector3d aux = aux3d1.get();

        if (this.position == null) {
            this.currentDistance = aux.set(this.pos).sub(camera.getPos()).len() * camera.getFovFactor();
        } else {
            this.currentDistance = this.position.distToCamera;
        }

        // Update with translation/rotation/etc
        updateLocal(time, camera);

        if (children != null) {
            if (rendered > 1) {
                if (orbitsWithOrbit.isEmpty() || !GaiaSky.instance.isOn(ComponentType.Orbits)) {
                    children.first().update(time, translation, camera, this.opacity);
                } else {
                    for (int i = 0; i < orbitsWithOrbit.size; i++) {
                        SceneGraphNode child = orbitsWithOrbit.get(i);
                        child.update(time, translation, camera, this.opacity);
                    }
                }

            } else {
                if (initialUpdate || GaiaSky.instance.isOn(ct)) {
                    for (int i = 0; i < children.size; i++) {
                        SceneGraphNode child = children.get(i);
                        child.update(time, translation, camera, this.opacity);
                    }
                    initialUpdate = false;
                    if (rendered >= 0)
                        rendered++;
                }
            }
        }
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.INITIALIZED_INFO) {
            rendered = 0;
        }
    }
}
