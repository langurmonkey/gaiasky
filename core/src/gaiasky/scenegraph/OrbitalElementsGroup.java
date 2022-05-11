package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.api.IRenderable;
import gaiasky.render.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.CatalogInfo;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

/**
 * This class acts as a group of orbital element objects.
 * It compiles the orbit objects which are only-body and those which are not, and
 * updates them accordingly to reduce CPU calls.
 */
public class OrbitalElementsGroup extends GenericCatalog implements IRenderable, IObserver {
    private Array<SceneGraphNode> alwaysUpdate;

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);

        // Check children which need updating every time
        initializeOrbitsWithOrbit();

        // Initialize catalog info if not set
        initializeCatalogInfo();

        EventManager.instance.subscribe(this, Event.GPU_DISPOSE_ORBITAL_ELEMENTS);
    }

    /**
     * Gather the children objects that need to be rendered as an orbit line into a list,
     * for they need to be updated every single frame.
     */
    private void initializeOrbitsWithOrbit() {
        if (alwaysUpdate == null) {
            alwaysUpdate = new Array<>();
        } else {
            alwaysUpdate.clear();
        }
        if (children != null && children.size > 0) {
            for (SceneGraphNode sgn : children) {
                if (sgn instanceof Orbit) {
                    Orbit orbit = (Orbit) sgn;
                    if (!orbit.onlyBody) {
                        alwaysUpdate.add(orbit);
                    }
                } else {
                    // Not an orbit, always add
                    alwaysUpdate.add(sgn);
                }
            }
        }
    }

    private void initializeCatalogInfo() {
        if (this.catalogInfo == null) {
            // Create catalog info and broadcast
            CatalogInfo ci = new CatalogInfo(names[0], names[0], null, CatalogInfoSource.INTERNAL, 1f, this);
            ci.nParticles = this.children != null ? this.children.size : -1;

            // Insert
            EventManager.publish(Event.CATALOG_ADD, this, ci, false);
        }
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender()) {
            addToRender(this, RenderGroup.ORBITAL_ELEMENTS_GROUP);
        }
    }

    public void update(ITimeFrameProvider time, final Vector3b parentTransform, ICamera camera, float opacity) {
        this.opacity = opacity;
        translation.set(parentTransform);
        Vector3d aux = D31.get();

        if (this.positionObject == null) {
            this.currentDistance = aux.set(this.pos).sub(camera.getPos()).len() * camera.getFovFactor();
        } else {
            this.currentDistance = this.positionObject.distToCamera;
        }

        // Update with translation/rotation/etc
        updateLocal(time, camera);

        if (children != null) {
            if (initialUpdate) {
                // Update all
                for (int i = 0; i < children.size; i++) {
                    SceneGraphNode child = children.get(i);
                    child.update(time, translation, camera, this.opacity);
                }
                initialUpdate = false;
            } else {
                // Update needed
                for (int i = 0; i < alwaysUpdate.size; i++) {
                    SceneGraphNode child = alwaysUpdate.get(i);
                    child.update(time, translation, camera, this.opacity);
                }
            }
        }
    }

    public void markForUpdate(){
        EventManager.publish(Event.GPU_DISPOSE_ORBITAL_ELEMENTS, this);
    }

    @Override
    public void highlight(boolean hl, float[] color, boolean allVisible) {
        markForUpdate();
        super.highlight(hl, color, allVisible);
    }

    @Override
    public void dispose(){
        GaiaSky.instance.sceneGraph.remove(this, true);
        // Unsubscribe from all events
        EventManager.instance.removeAllSubscriptions(this);
        EventManager.publish(Event.GPU_DISPOSE_ORBITAL_ELEMENTS, this);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.GPU_DISPOSE_ORBITAL_ELEMENTS) {
            if (source == this) {
                initializeOrbitsWithOrbit();
            }
        }
    }
}
