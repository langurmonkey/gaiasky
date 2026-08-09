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
import gaiasky.data.util.PointCloudData;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;

import java.time.Instant;
import java.util.Date;

public class TrajectoryUtils {

    /**
     * The trajectory refresher daemon.
     **/
    public static OrbitRefresher orbitRefresher;
    private final Vector3Q B31, B32;
    private final Vector3D D31, D32, D33;

    public TrajectoryUtils() {
        B31 = new Vector3Q();
        B32 = new Vector3Q();
        D31 = new Vector3D();
        D32 = new Vector3D();
        D33 = new Vector3D();
    }

    /**
     * Initialize the trajectory refresher daemon.
     **/
    public static void initRefresher(TrajectoryUtils utils) {
        if (orbitRefresher == null) {
            orbitRefresher = new OrbitRefresher("gaiasky-worker-trajectoryupdate", utils);
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
            if (!trajectory.isOnlyBody() && pointCloudData.getNumPoints() > 0) {
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
        if (graph.parent == null) {
            return;
        }

        if (trajectory.model == Trajectory.OrbitOrientationModel.EXTRASOLAR_SYSTEM && transform.matrix == null) {
            computeExtrasolarSystemTransformMatrix(graph, transform);
        } else if (trajectory.model == Trajectory.OrbitOrientationModel.INHERIT && transform.matrix == null) {
            computeInheritedTransformMatrix(graph, transform);
        }
    }

    /**
     * Copies the extrasolar reference frame from the nearest ancestor that has
     * an extrasolar or inherited orientation model.
     */
    public void computeInheritedTransformMatrix(GraphNode graph, RefSysTransform transform) {
        Entity ancestor = graph.parent;
        while (ancestor != null) {
            Trajectory ancestorTrajectory = Mapper.trajectory.get(ancestor);
            RefSysTransform ancestorTransform = Mapper.transform.get(ancestor);
            GraphNode ancestorGraph = Mapper.graph.get(ancestor);

            if (ancestorTrajectory != null
                    && ancestorTransform != null
                    && ancestorGraph != null
                    && ancestorTrajectory.model == Trajectory.OrbitOrientationModel.EXTRASOLAR_SYSTEM
                    && ancestorTransform.matrix == null) {
                computeExtrasolarSystemTransformMatrix(ancestorGraph, ancestorTransform);
            }

            if (ancestorTrajectory != null
                    && (ancestorTrajectory.model == Trajectory.OrbitOrientationModel.EXTRASOLAR_SYSTEM
                    || ancestorTrajectory.model == Trajectory.OrbitOrientationModel.INHERIT)
                    && ancestorTransform != null
                    && ancestorTransform.matrix != null) {
                transform.setTransformMatrix(ancestorTransform.matrix);
                return;
            }

            ancestor = ancestorGraph != null ? ancestorGraph.parent : null;
        }
    }

    public void computeExtrasolarSystemTransformMatrix(GraphNode graph, RefSysTransform transform) {
        Entity parent = graph.parent;
        if (parent == null)
            return;
        Coordinates coord = Mapper.coordinates.get(parent);
        // Compute new transform function from the orbit's parent position
        Vector3Q barycenter = B31;
        if (coord != null && coord.coordinates != null) {
            coord.coordinates.getEquatorialCartesianCoordinates(GaiaSky.instance.time.getTime(), barycenter);
        } else {
            EntityUtils.getAbsolutePosition(parent, barycenter);
        }

        // The reference-plane normal is the line of sight from the Sun to the
        // object. Use the parent's position as that direction.
        Vector3D yd = B32.set(barycenter).nor().put(D31);

        // Project the direction from the object to the north celestial pole
        // onto the reference plane. The pole is the origin's +Y direction in
        // the equatorial frame, so this is the pole direction minus its
        // component along the reference-plane normal.
        Vector3D zd;
        if (Math.abs(yd.y) < 0.9) {
            zd = D32.set(0, 1, 0);
        } else {
            // Near the celestial poles, use the equatorial X direction to
            // avoid amplifying numerical noise in the projection.
            zd = D32.set(1, 0, 0);
        }
        zd.mulAdd(yd, -zd.dot(yd)).nor();

        // Complete the right-handed orthonormal basis.
        Vector3D xd = D33.set(yd).crs(zd).nor();

        transform.matrix = Matrix4D.changeOfBasis(zd, yd, xd);
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
                trajectory.params.setForce(force);

                // Add to queue
                if (!trajectory.refreshing) {
                    orbitRefresher.queue(trajectory.params);
                }
            }
        }
    }
}
