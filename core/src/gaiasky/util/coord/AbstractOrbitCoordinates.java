/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import com.badlogic.ashley.core.Entity;
import gaiasky.data.util.PointCloudData;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.math.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractOrbitCoordinates implements IBodyCoordinates {
    protected static final Log logger = Logger.getLogger(AbstractOrbitCoordinates.class);
    // Holds all instances
    protected static final List<AbstractOrbitCoordinates> instances = new ArrayList<>();

    protected String orbitName;
    protected boolean periodic = true;
    protected Vector3d center;
    protected Entity entity;
    protected Entity owner;
    protected double scaling = 1d;

    protected AbstractOrbitCoordinates() {
        super();
        instances.add(this);
    }

    public static List<AbstractOrbitCoordinates> getInstances() {
        return instances;
    }

    public static <T extends AbstractOrbitCoordinates> T getInstance(Class<T> clazz) {
        for (AbstractOrbitCoordinates o : instances) {
            if (clazz.isInstance(o)) {
                return (T) o;
            }
        }

        T obj = null;
        try {
            obj = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.error(e);
        }
        return obj;
    }

    @Override
    public void doneLoading(Object... params) {
        if (params.length == 0) {
            logger.error(new RuntimeException("No parameters found, can't initialize coordinates from orbit: " + orbitName));
        } else {
            if (orbitName != null && !orbitName.isEmpty()) {
                if (params[0] instanceof Scene) {
                    entity = ((Scene) params[0]).index().getEntity(orbitName);
                }
                if (params[1] instanceof Entity orbitObject) {
                    owner = orbitObject;
                    var trajectory = Mapper.trajectory.get(entity);
                    trajectory.setBody(owner, EntityUtils.getRadius(owner), trajectory.distDown, trajectory.distUp);
                }
            }
        }
    }

    public String getOrbitName() {
        return orbitName;
    }

    public void setOrbitName(String orbitName) {
        this.orbitName = orbitName;
    }

    public void setOrbitname(String orbitName) {
        setOrbitName(orbitName);
    }

    public void setPeriodic(Boolean periodic) {
        this.periodic = periodic;
    }

    public void setScaling(double scaling) {
        this.scaling = scaling;
    }

    @Override
    public String toString() {
        return "{" + "name='" + orbitName + '\'' + ", orbit=" + entity + ", scaling=" + scaling + '}';
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

    protected void updateOwner(Map<String, Entity> index) {
       if(owner != null) {
           var base = Mapper.base.get(owner);
           if (base != null) {
               var newOwner = index.get(base.getName().toLowerCase());
               if(newOwner != null) {
                   owner = newOwner;
               }
           }
       }
    }

    protected void copyParameters(AbstractOrbitCoordinates other){
        other.orbitName = this.orbitName;
        other.center = this.center;
        other.periodic = this.periodic;
        other.owner = this.owner;
        other.entity = this.entity;
        other.scaling = this.scaling;
    }

    @Override
    public void updateReferences(Map<String, Entity> index) {
    }

    @Override
    public IBodyCoordinates getCopy() {
        return this;
    }
}
