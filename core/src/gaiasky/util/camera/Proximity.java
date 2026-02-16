/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.camera;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.scene.record.ParticleKepler;
import gaiasky.scene.record.RotationComponent;
import gaiasky.scene.view.FocusView;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.*;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.OctreeNode;

import java.util.Arrays;

public class Proximity {
    // Default number of proximity entries
    private static final int DEFAULT_SIZE = 4;

    // proximity record type
    private static final byte TYPE_UNDEFINED = -1;
    private static final byte TYPE_STAR = 0;
    private static final byte TYPE_STAR_GROUP = 1;
    private static final byte TYPE_OTHER = 2;

    public NearbyRecord[] updating, effective, array0, array1;

    public Proximity() {
        this(DEFAULT_SIZE);
    }

    public Proximity(int size) {
        this.array0 = new NearbyRecord[size];
        this.array1 = new NearbyRecord[size];
        this.updating = this.array0;
        this.effective = this.array1;
    }

    public void set(int index, int originalIndex, IParticleRecord pr, ICamera camera) {
        this.set(index, originalIndex, pr, camera, 0);
    }

    public void set(int index, int originalIndex, IParticleRecord pr, ICamera camera, double deltaYears) {
        if (this.updating[index] == null) {
            this.updating[index] = new NearbyRecord();
        }
        NearbyRecord c = this.updating[index];
        convert(pr, c, camera, deltaYears);
        c.index = originalIndex;
    }

    public void set(int index, int originalIndex, IFocus focus, ICamera camera) {
        if (this.updating[index] == null) {
            this.updating[index] = new NearbyRecord();
        }
        NearbyRecord c = this.updating[index];
        convert(focus, c, camera);
        c.index = originalIndex;
    }

    /**
     * Sets the given record at the given index, overwriting
     * the current value
     *
     * @param index  The index
     * @param record The nearby record
     */
    public void set(int index, NearbyRecord record) {
        this.updating[index] = record;
    }

    /**
     * Inserts the given record at the given index,
     * moving the rest of the values to the right
     *
     * @param index  The index
     * @param record The nearby record
     */
    public void insert(int index, NearbyRecord record) {
        NearbyRecord oldRecord;
        NearbyRecord newRecord = record;
        for (int i = index; i < this.updating.length; i++) {
            oldRecord = this.updating[i];
            this.updating[i] = newRecord;
            newRecord = oldRecord;
        }
    }

    /**
     * Inserts the given object at the given index,
     * moving the rest of the values to the right
     *
     * @param index  The index
     * @param object The nearby record
     */
    public void insert(int index, IFocus object, ICamera camera) {
        NearbyRecord record = convert(object, new NearbyRecord(), camera);
        insert(index, record);
    }

    /**
     * Updates the list of proximal objects with the given {@link NearbyRecord}.
     *
     * @param object The record to use for updating.
     *
     * @return Whether this proximity array was modified.
     */
    public boolean update(NearbyRecord object) {
        int i = 0;
        for (NearbyRecord record : updating) {
            if (record == null) {
                set(i, object);
                return true;
            } else if (record == object) {
                // Already in
                return false;
            } else if (object.distToCamera < record.distToCamera) {
                insert(i, object);
                return true;
            }
            i++;
        }
        return false;
    }

    /**
     * Updates the list of proximal objects with the given {@link IFocus}.
     *
     * @param object The record to use for updating.
     * @param camera The camera.
     *
     * @return Whether this proximity array was modified.
     */
    public boolean update(IFocus object, ICamera camera) {
        int i = 0;
        for (NearbyRecord record : updating) {
            if (record == null) {
                set(i, -1, object, camera);
                return true;
            } else if (record == object) {
                // Already in
                return false;
            } else if (!object.getName().equalsIgnoreCase(record.name)) {
                if (object.getClosestDistToCamera() < record.distToCamera) {
                    // Insert
                    insert(i, object, camera);
                    return true;
                }
            }
            i++;
        }
        return false;
    }

    private byte getType(IFocus f) {
        if (f instanceof FocusView && Mapper.hip.has(((FocusView) f).getEntity())) {
            return TYPE_STAR;
        } else {
            return TYPE_OTHER;
        }
    }

    /**
     * Swaps the arrays in this double-buffer implementation
     */
    public void swapBuffers() {
        if (updating == array0) {
            // updating <- array1
            // effective <- array0
            updating = array1;
            effective = array0;
            clear(updating);
        } else {
            // updating <- array0
            // effective <- array1
            updating = array0;
            effective = array1;
            clear(updating);
        }
    }

    private void clear(NearbyRecord[] arr) {
        Arrays.fill(arr, null);
    }

    public NearbyRecord convert(IParticleRecord pr, NearbyRecord c, ICamera camera, double deltaYears) {
        return switch (pr.getType()) {
            case PARTICLE, STAR, PARTICLE_EXT, VARIABLE -> {
                c.pm.set(pr.vx(), pr.vy(), pr.vz()).scl(deltaYears);
                c.absolutePos.set(pr.x(), pr.y(), pr.z()).add(c.pm);
                c.pos.set(c.absolutePos).sub(camera.getPos());
                c.size = pr.size();
                c.radius = pr.radius();
                c.distToCamera = c.pos.len() - c.radius;
                c.name = pr.names()[0];
                c.type = TYPE_STAR_GROUP;

                Color col = new Color();
                Color.abgr8888ToColor(col, pr.color());
                c.col[0] = col.r;
                c.col[1] = col.g;
                c.col[2] = col.b;
                c.col[3] = col.a;
                yield c;
            }
            case KEPLER -> {
                var k = (ParticleKepler) pr;

                c.absolutePos.set(pr.x(), pr.y(), pr.z());
                yield c;
            }
            case VECTOR -> {
                c.absolutePos.set(pr.x(), pr.y(), pr.z());
                c.pos.set(c.absolutePos).sub(camera.getPos());
                yield c;
            }
        };
    }

    public NearbyRecord convert(IFocus focus, NearbyRecord c, ICamera camera) {
        c.pm.set(0, 0, 0);
        Vector3Q absPos = new Vector3Q();
        absPos = focus.getAbsolutePosition(absPos);
        c.absolutePos.set(absPos);
        c.pos.set(c.absolutePos).sub(camera.getPos());
        c.size = focus.getSize();
        c.radius = focus.getRadius();
        c.distToCamera = focus.getClosestDistToCamera();
        c.name = focus.getName();
        c.type = getType(focus);

        float[] col = focus.getColor();
        if (col != null) {
            c.col[0] = col[0];
            c.col[1] = col[1];
            c.col[2] = col[2];
            c.col[3] = 1f;
        } else {
            // White by default.
            c.col[0] = 1f;
            c.col[1] = 1f;
            c.col[2] = 1f;
            c.col[3] = 1f;
        }
        return c;
    }

    public static class NearbyRecord implements IFocus {
        public double distToCamera, size, radius;
        public Vector3D pos, pm, absolutePos;
        public float[] col;
        public String name;
        public byte type = TYPE_UNDEFINED;
        // The index in the source list
        public int index;

        public NearbyRecord() {
            pos = new Vector3D();
            pm = new Vector3D();
            absolutePos = new Vector3D();
            col = new float[4];
        }

        public boolean isStar() {
            return type == TYPE_STAR;
        }

        public boolean isStarGroup() {
            return type == TYPE_STAR_GROUP;
        }

        public boolean isOther() {
            return type == TYPE_OTHER;
        }

        public boolean isUndefined() {
            return type == TYPE_UNDEFINED;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public long getId() {
            return -1;
        }

        @Override
        public long getCandidateId() {
            return -1;
        }

        @Override
        public boolean hasId() {
            return false;
        }


        @Override
        public String getLocalizedName() {
            return name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String[] getNames() {
            return new String[]{name};
        }

        @Override
        public boolean hasName(String name) {
            return this.name.equalsIgnoreCase(name);
        }

        @Override
        public boolean hasName(String name, boolean matchCase) {
            return matchCase ? this.name.equals(name) : this.name.equalsIgnoreCase(name);
        }

        @Override
        public String getClosestName() {
            return name;
        }

        @Override
        public String getClosestLocalizedName() {
            return I18n.localize(getClosestName());
        }

        @Override
        public String getCandidateName() {
            return name;
        }

        @Override
        public ComponentTypes getCt() {
            return new ComponentTypes(ComponentTypes.ComponentType.Stars);
        }

        @Override
        public boolean isFocusActive() {
            return true;
        }

        @Override
        public Vector3Q getPos() {
            return null;
        }

        @Override
        public Entity getFirstStarAncestorEntity() {
            return null;
        }

        @Override
        public IFocus getFirstStarAncestor() {
            return null;
        }

        @Override
        public Vector3Q getAbsolutePosition(Vector3Q out) {
            return out.set(absolutePos);
        }

        @Override
        public Vector3Q getAbsolutePosition(String name, Vector3Q out) {
            if (name.equalsIgnoreCase(this.name))
                return out.set(absolutePos);
            else
                return out;
        }

        @Override
        public Vector3Q getClosestAbsolutePos(Vector3Q out) {
            return out.set(absolutePos);
        }

        @Override
        public Vector2D getPosSph() {
            return null;
        }

        @Override
        public Vector3Q getPredictedPosition(Vector3Q aux, ITimeFrameProvider time, ICamera camera, boolean force) {
            return null;
        }

        @Override
        public Vector3Q getPredictedPosition(Vector3Q aux, double deltaTime) {
            return null;
        }

        @Override
        public double getDistToCamera() {
            return this.distToCamera;
        }

        @Override
        public double getClosestDistToCamera() {
            return this.distToCamera;
        }

        @Override
        public double getSolidAngle() {
            return 0;
        }

        @Override
        public double getSolidAngleApparent() {
            return 0;
        }

        @Override
        public double getCandidateSolidAngleApparent() {
            return 0;
        }

        @Override
        public double getAlpha() {
            return 0;
        }

        @Override
        public double getDelta() {
            return 0;
        }

        @Override
        public double getSize() {
            return this.size;
        }

        @Override
        public double getRadius() {
            return this.radius;
        }

        @Override
        public double getTEff() {
            return 0;
        }

        @Override
        public double getElevationAt(Vector3Q camPos) {
            return 0;
        }

        @Override
        public double getElevationAt(Vector3Q camPos, boolean useFuturePosition) {
            return 0;
        }

        @Override
        public double getElevationAt(Vector3Q camPos, Vector3Q nextPos) {
            return 0;
        }

        @Override
        public double getHeightScale() {
            return 0;
        }

        @Override
        public float getAppmag() {
            return 0;
        }

        @Override
        public float getAbsmag() {
            return 0;
        }

        @Override
        public Matrix4D getOrientation() {
            return null;
        }

        @Override
        public RotationComponent getRotationComponent() {
            return null;
        }

        @Override
        public QuaternionDouble getOrientationQuaternion() {
            return null;
        }

        @Override
        public void addEntityHitCoordinate(int screenX, int screenY, int w, int h, int pixelDist, NaturalCamera camera, Array<Entity> hits) {

        }

        @Override
        public void addEntityHitRay(Vector3D p0, Vector3D p1, NaturalCamera camera, Array<Entity> hits) {

        }

        @Override
        public void makeFocus() {

        }

        @Override
        public IFocus getFocus(String name) {
            return null;
        }

        @Override
        public boolean isCoordinatesTimeOverflow() {
            return false;
        }

        @Override
        public int getSceneGraphDepth() {
            return 0;
        }

        @Override
        public OctreeNode getOctant() {
            return null;
        }

        @Override
        public boolean isCopy() {
            return false;
        }

        @Override
        public boolean isFocusable() {
            return false;
        }

        @Override
        public boolean isCameraCollision() {
            return true;
        }

        @Override
        public float[] getColor() {
            return col;
        }

        @Override
        public boolean isForceLabel() {
            return false;
        }

        @Override
        public boolean isForceLabel(String name) {
            return false;
        }

        @Override
        public boolean isRenderLabel() {
            return false;
        }

        @Override
        public boolean isRenderLabel(String name) {
            return false;
        }
    }
}
