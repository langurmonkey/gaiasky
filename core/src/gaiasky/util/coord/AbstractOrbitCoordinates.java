/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import com.badlogic.ashley.core.Entity;
import gaiasky.data.util.PointCloudData;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.math.Vector3d;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOrbitCoordinates implements IBodyCoordinates {
    protected static final Log logger = Logger.getLogger(AbstractOrbitCoordinates.class);
    // Holds all instances
    protected static final List<AbstractOrbitCoordinates> instances = new ArrayList<>();

    protected String orbitname;
    protected Vector3d center;
    protected Entity entity;
    protected double scaling = 1d;

    public AbstractOrbitCoordinates() {
        super();
        instances.add(this);
    }

    public static List<AbstractOrbitCoordinates> getInstances() {
        return instances;
    }

    public static <T extends AbstractOrbitCoordinates> T getInstance(Class<T> clazz) {
        for (AbstractOrbitCoordinates o : instances) {
            if(clazz.isInstance(o)) {
                return (T) o;
            }
        }

        T obj = null;
        try {
            obj = clazz.getDeclaredConstructor().newInstance();
        }catch(Exception e){
            logger.error(e);
        }
        return obj;
    }

    @Override
    public void doneLoading(Object... params) {
        if (params.length == 0) {
            logger.error(new RuntimeException("No parameters found, can't initialize coordinates from orbit: " + orbitname));
        } else {
            if (orbitname != null && !orbitname.isEmpty()) {
                if (params[0] instanceof Scene) {
                    entity = ((Scene) params[0]).index().getEntity(orbitname);
                }
                if (params[1] instanceof Entity) {
                    var trajectory = Mapper.trajectory.get(entity);
                    var orbitObject = (Entity) params[1];
                    var body = Mapper.body.get(orbitObject);
                    trajectory.setBody(orbitObject, body.size / 2);
                }
            }
        }
    }

    public String getOrbitName() {
        return orbitname;
    }

    public void setOrbitname(String orbitName) {
        setOrbitName(orbitName);
    }

    public void setOrbitName(String orbitName) {
        this.orbitname = orbitName;
    }

    public void setScaling(double scaling) {
        this.scaling = scaling;
    }

    @Override
    public String toString() {
        return "{" + "name='" + orbitname + '\'' + ", orbit=" + entity + ", scaling=" + scaling + '}';
    }

    public void setCentre(double[] center) {
        setCenter(center);
    }

    public void setCenter(double[] center) {
        setCenterkm(center);
    }

    public void setCenterkm(double[] center) {
        this.center = new Vector3d(center[0] * Constants.KM_TO_U, center[1] * Constants.KM_TO_U, center[2] * Constants.KM_TO_U);
    }

    public void setCenterpc(double[] center) {
        this.center = new Vector3d(center[0] * Constants.PC_TO_U, center[1] * Constants.PC_TO_U, center[2] * Constants.PC_TO_U);
    }

    protected PointCloudData getData() {
        if (entity != null) {
            return Mapper.verts.get(entity).pointCloudData;
        }
        return null;
    }
}
