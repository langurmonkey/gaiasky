/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.data.OrbitRefresher;
import gaiasky.data.orbit.OrbitBodyDataProvider;
import gaiasky.data.orbit.OrbitFileDataProvider;
import gaiasky.data.orbit.OrbitalParametersProvider;
import gaiasky.data.util.PointCloudData;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.util.math.IntersectorDouble;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.time.Instant;
import java.util.Date;

public class TrajectoryUtils {

    /** The trajectory refresher daemon. **/
    public static OrbitRefresher orbitRefresher;
    private final Vector3b B31, B32;
    private final Vector3d D31, D32, D33;

    public TrajectoryUtils() {
        B31 = new Vector3b();
        B32 = new Vector3b();
        D31 = new Vector3d();
        D32 = new Vector3d();
        D33 = new Vector3d();
    }

    /** Initialize the trajectory refresher daemon. **/
    public static void initRefresher() {
        if (orbitRefresher == null) {
            orbitRefresher = new OrbitRefresher("gaiasky-worker-trajectoryupdate");
        }
    }

    public void initOrbitMetadata(Body body, Trajectory trajectory, Verts verts) {
        PointCloudData pointCloudData = verts.pointCloudData;
        if (pointCloudData != null) {
            if (pointCloudData.hasTime()) {
                trajectory.orbitStartMs = pointCloudData.getDate(0).toEpochMilli();
                trajectory.orbitEndMs = pointCloudData.getDate(pointCloudData.getNumPoints() - 1).toEpochMilli();
            }
        }
        updateSize(body, trajectory, verts);
        trajectory.mustRefresh = trajectory.providerClass != null
                && (trajectory.providerClass.equals(OrbitBodyDataProvider.class) || trajectory.providerClass.equals(OrbitFileDataProvider.class))
                && trajectory.body != null
                // body instanceof Planet
                && Mapper.atmosphere.has(trajectory.body)
                && trajectory.oc.period > 0;
    }

    public void updateSize(Body body, Trajectory trajectory, Verts verts) {
        PointCloudData pointCloudData = verts.pointCloudData;
        if (pointCloudData != null) {
            if (!trajectory.onlyBody && pointCloudData.getNumPoints() > 0) {
                pointCloudData.loadPoint(D31, 0);
                int n = pointCloudData.getNumPoints();
                double len = 0;
                for (int i = 1; i < n; i++) {
                    pointCloudData.loadPoint(D32, i);
                    double newLen = D32.sub(D31).len();
                    if (newLen > len) {
                        len = newLen;
                    }
                }
                body.size = (float) (len * 2.5);
            }
        }
    }

    public void initializeTransformMatrix(Trajectory trajectory, GraphNode graph, RefSysTransform transform) {
        if (trajectory.model == Trajectory.OrbitOrientationModel.EXTRASOLAR_SYSTEM && transform.matrix == null && graph.parent != null) {
            computeExtrasolarSystemTransformMatrix(graph, transform);
        }
    }

    public void computeExtrasolarSystemTransformMatrix(GraphNode graph, RefSysTransform transform) {
        Entity parent = graph.parent;
        Coordinates coord = Mapper.coordinates.get(parent);
        // Compute new transform function from the orbit's parent position
        Vector3b barycenter = B31;
        if (coord != null && coord.coordinates != null) {
            coord.coordinates.getEquatorialCartesianCoordinates(GaiaSky.instance.time.getTime(), barycenter);
        } else {
            EntityUtils.getAbsolutePosition(parent, barycenter);
        }

        // Up
        Vector3b y = B32.set(barycenter).nor();
        Vector3d yd = y.put(D31);
        // Towards north - intersect y with plane
        Vector3d zd = D32;
        IntersectorDouble.lineIntersection(barycenter.put(new Vector3d()), (new Vector3d(yd)), new Vector3d(0, 0, 0), new Vector3d(0, 1, 0), zd);
        zd.sub(barycenter).nor();
        //zd.set(yd).crs(0, 1, 0).nor();

        // Orthogonal to ZY, right-hand system
        Vector3d xd = D33.set(yd).crs(zd);

        transform.matrix = Matrix4d.changeOfBasis(zd, yd, xd);
    }

    /**
     * Queues a trajectory refresh task with the refresher for this trajectory.
     *
     * @param verts The verts object containing the data.
     * @param force Whether to force the refresh.
     */
    public void refreshOrbit(Trajectory trajectory, Verts verts, boolean force) {
        if ((force && trajectory.params != null) || (trajectory.mustRefresh && !EntityUtils.isCoordinatesTimeOverflow(trajectory.body))) {
            Instant currentTime = GaiaSky.instance.time.getTime();
            long currentMs = currentTime.toEpochMilli();
            if (force || verts.pointCloudData == null || currentMs < trajectory.orbitStartMs || currentMs > trajectory.orbitEndMs) {
                // Schedule for refresh
                // Work out sample initial date
                Date iniTime;
                if (GaiaSky.instance.time.getWarpFactor() < 0) {
                    // From (now - period) forward (reverse)
                    iniTime = Date.from(Instant.from(currentTime).minusMillis((long) (trajectory.oc.period * 80000000L)));
                } else {
                    // From now forward
                    iniTime = Date.from(currentTime);
                }
                trajectory.params.setIni(iniTime);

                // Add to queue
                if (!trajectory.refreshing) {
                    orbitRefresher.queue(trajectory.params);
                }
            }
        }
    }
}
