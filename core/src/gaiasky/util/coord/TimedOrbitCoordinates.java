package gaiasky.util.coord;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Pool;
import gaiasky.scene.Scene;
import gaiasky.util.Logger;
import gaiasky.util.math.Vector3b;

import java.time.Instant;
import java.util.Map;

/**
 * Composition of {@link IBodyCoordinates} that contains a start and end time of validity.
 */
public class TimedOrbitCoordinates implements IBodyCoordinates {

    static final Pool<TimedOrbitCoordinates> pool = new Pool<>() {
        protected TimedOrbitCoordinates newObject() {
            return new TimedOrbitCoordinates();
        }
    };
    protected static final Logger.Log logger = Logger.getLogger(TimedOrbitCoordinates.class);

    public AbstractOrbitCoordinates coordinates;
    /** Start and end times of these coordinates. **/
    public Instant start, end;
    /** Parent object for this orbit. This is useful when the new orbit changes the reference system. **/
    public String parentName;
    /** Parent entity for this orbit, if any. **/
    public Entity parent;

    public void setParent(String parent) {
        this.parentName = parent;
    }

    public void setStart(String start) {
        this.start = Instant.parse(start);
    }

    public void setEnd(String end) {
        this.end = Instant.parse(end);
    }

    public void setCoordinates(IBodyCoordinates c) {
        if (c instanceof AbstractOrbitCoordinates aoc)
            this.coordinates = aoc;
        else
            logger.error(this.getClass().getSimpleName() + " can only hold (sub)objects of type " + AbstractOrbitCoordinates.class.getSimpleName());
    }

    /**
     * Is this timed orbit coordinates instance in its validity period with regard to the
     * given time?
     * @param t The time to check.
     * @return True if this timed coordinates object is valid, false otherwise.
     */
    public boolean isValid(Instant t) {
        return t.equals(start) || t.equals(end) || (t.isAfter(start) && t.isBefore(end));
    }

    @Override
    public void doneLoading(Object... params) {
        coordinates.doneLoading(params);
        if (parentName != null) {
            if (params.length > 0) {
                if (params[0] instanceof Scene scene) {
                    parent = scene.getEntity(parentName);
                }
            }
        }
    }

    @Override
    public Vector3b getEclipticSphericalCoordinates(Instant instant, Vector3b out) {
        return coordinates.getEclipticSphericalCoordinates(instant, out);
    }

    @Override
    public Vector3b getEclipticCartesianCoordinates(Instant instant, Vector3b out) {
        return coordinates.getEclipticCartesianCoordinates(instant, out);
    }

    @Override
    public Vector3b getEquatorialCartesianCoordinates(Instant instant, Vector3b out) {
        return coordinates.getEquatorialCartesianCoordinates(instant, out);
    }

    @Override
    public void updateReferences(Map<String, Entity> index) {
        coordinates.updateReferences(index);

        // Update parent.
        if (parentName != null) {
            var key = parentName.toLowerCase();
            if (index.containsKey(key)) {
                parent = index.get(key);
            }
        }
    }

    @Override
    public IBodyCoordinates getCopy() {
        var copy = pool.obtain();
        copy.start = this.start;
        copy.end = this.end;
        copy.parentName = this.parentName;
        copy.coordinates = (AbstractOrbitCoordinates) this.coordinates.getCopy();
        return copy;
    }
}
