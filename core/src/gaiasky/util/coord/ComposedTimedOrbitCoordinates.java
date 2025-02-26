package gaiasky.util.coord;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.tag.TagNoProcess;
import gaiasky.util.Logger;
import gaiasky.util.math.Vector3b;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;

/**
 * Implementation of coordinates that contains more than one timed coordinates object.
 */
public class ComposedTimedOrbitCoordinates implements IBodyCoordinates {
    protected static final Logger.Log logger = Logger.getLogger(ComposedTimedOrbitCoordinates.class);

    public Array<TimedOrbitCoordinates> coordinates;
    /**
     * This flag controls whether only the active orbit is processed (updated, rendered) or
     * all of them are.
     */
    public boolean processOnlyActive = true;
    /** Scene reference **/
    private Scene scene;

    public void setProcessOnlyActive(Boolean b) {
        this.processOnlyActive = b;
    }

    public void setSequence(Object[] seq) {
        coordinates = new Array<>();
        for (var obj : seq) {
            if (obj instanceof TimedOrbitCoordinates toc) {
                coordinates.add(toc);
            } else {
                logger.error(this.getClass().getSimpleName() + " can only hold objects of type " + TimedOrbitCoordinates.class.getSimpleName());
            }
        }
    }

    @Override
    public void doneLoading(Object... params) {
        for (var c : coordinates) {
            c.doneLoading(params);
        }

        // Add scene if needed.
        // When we process only active orbits, we need to add the {@link TagNoProcess} component
        // to inactive orbits.
        if (processOnlyActive) {
            if (params.length == 0) {
                logger.error(new RuntimeException("No parameters found, can't initialize scene."));
            } else if (params[0] instanceof Scene) {
                scene = (Scene) params[0];
            }
        }
    }

    private Vector3b getGenericCoordinates(Instant instant, Vector3b out, Method method) {
        var found = false;
        for (var c : coordinates) {
            if (c.isValid(instant)) {
                var entity = c.coordinates.entity;
                // Processing.
                if (Mapper.tagNoProcess.has(entity) && GaiaSky.instance.isInitialised()) {
                    entity.remove(TagNoProcess.class);
                }
                // Parent.
                var owner = c.coordinates.owner;
                if (c.parent != null && Mapper.graph.get(owner).parent != c.parent) {
                    var parentGraph = Mapper.graph.get(c.parent);
                    parentGraph.addChild(c.parent, owner, true, 1);
                }
                if (!found) {
                    try {
                        method.invoke(c, instant, out);
                        found = true;
                    } catch (Exception e) {
                        logger.error(e, "Error invoking method: " + method);
                    }
                }
            } else if (processOnlyActive) {
                if (!Mapper.tagNoProcess.has(c.coordinates.entity) && GaiaSky.instance.isInitialised()) {
                    c.coordinates.entity.add(scene.engine.createComponent(TagNoProcess.class));
                }
            }
        }
        return out;
    }

    @Override
    public Vector3b getEclipticSphericalCoordinates(Instant instant, Vector3b out) {
        String method = "getEclipticSphericalCoordinates";
        try {
            return getGenericCoordinates(instant, out, IBodyCoordinates.class.getMethod(method, Instant.class, Vector3b.class));
        } catch (NoSuchMethodException nsme) {
            logger.error(nsme, "Method does not exist: " + method);
            return out;
        }
    }

    @Override
    public Vector3b getEclipticCartesianCoordinates(Instant instant, Vector3b out) {
        String method = "getEclipticCartesianCoordinates";
        try {
            return getGenericCoordinates(instant, out, IBodyCoordinates.class.getMethod(method, Instant.class, Vector3b.class));
        } catch (NoSuchMethodException nsme) {
            logger.error(nsme, "Method does not exist: " + method);
            return out;
        }
    }

    @Override
    public Vector3b getEquatorialCartesianCoordinates(Instant instant, Vector3b out) {
        String method = "getEquatorialCartesianCoordinates";
        try {
            return getGenericCoordinates(instant, out, IBodyCoordinates.class.getMethod(method, Instant.class, Vector3b.class));
        } catch (NoSuchMethodException nsme) {
            logger.error(nsme, "Method does not exist: " + method);
            return out;
        }
    }

    @Override
    public void updateReferences(Map<String, Entity> index) {
        for (var c : this.coordinates) {
            c.updateReferences(index);
        }
    }

    @Override
    public IBodyCoordinates getCopy() {
        var copy = new ComposedTimedOrbitCoordinates();
        copy.processOnlyActive = this.processOnlyActive;
        copy.scene = this.scene;
        copy.coordinates = new Array<>(this.coordinates.size);
        for (var c : this.coordinates) {
            copy.coordinates.add((TimedOrbitCoordinates) c.getCopy());
        }
        return copy;
    }
}
