/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.RefSysTransform;
import gaiasky.scene.component.Trajectory;
import gaiasky.scene.entity.TrajectoryUtils;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;
import org.junit.Assert;
import org.junit.Test;

public class TrajectoryUtilsTest {

    @Test
    public void testExtrasolarSystemTransformUsesParentLineOfSight() {
        Entity parent = entityWithBody(1, 0, 0);
        Entity orbit = entityWithParent(parent);
        RefSysTransform transform = Mapper.transform.get(orbit);

        new TrajectoryUtils().computeExtrasolarSystemTransformMatrix(Mapper.graph.get(orbit), transform);

        Assert.assertNotNull(transform.matrix);

        // For a parent on +X, the reference-plane normal is +X. The projected
        // north-pole direction is +Y, and the completed third axis is +Z.
        Vector3D normal = new Vector3D(1, 0, 0).mul(transform.matrix);
        Vector3D northProjection = new Vector3D(0, 1, 0).mul(transform.matrix);
        Assert.assertTrue(normal.epsilonEquals(new Vector3D(0, 1, 0), 1e-12));
        Assert.assertTrue(northProjection.epsilonEquals(new Vector3D(1, 0, 0), 1e-12));

        assertOrthonormal(transform.matrix);
    }

    @Test
    public void testInheritedTransformMatchesNearestExtrasolarAncestor() {
        Entity system = entityWithBody(0, 0, 1);
        Entity parentOrbit = entityWithParent(system);
        Entity childOrbit = entityWithParent(parentOrbit);

        Trajectory parentTrajectory = new Trajectory();
        parentTrajectory.model = Trajectory.OrbitOrientationModel.EXTRASOLAR_SYSTEM;
        parentOrbit.add(parentTrajectory);
        RefSysTransform parentTransform = new RefSysTransform();
        parentOrbit.add(parentTransform);

        Trajectory childTrajectory = new Trajectory();
        childTrajectory.model = Trajectory.OrbitOrientationModel.INHERIT;
        childOrbit.add(childTrajectory);
        RefSysTransform childTransform = new RefSysTransform();
        childOrbit.add(childTransform);

        TrajectoryUtils utils = new TrajectoryUtils();
        utils.computeExtrasolarSystemTransformMatrix(Mapper.graph.get(parentOrbit), parentTransform);
        utils.computeInheritedTransformMatrix(Mapper.graph.get(childOrbit), childTransform);

        Assert.assertNotNull(childTransform.matrix);
        for (int i = 0; i < 16; i++) {
            Assert.assertEquals(parentTransform.matrix.val[i], childTransform.matrix.val[i], 1e-12);
        }

        // INHERIT must use the system frame, not the immediate parent body's
        // position. The immediate parent has no Body position here.
        assertOrthonormal(childTransform.matrix);
    }

    private static Entity entityWithBody(double x, double y, double z) {
        Entity entity = new Entity();
        Body body = new Body();
        body.pos.set(x, y, z);
        entity.add(body);
        entity.add(new GraphNode());
        return entity;
    }

    private static Entity entityWithParent(Entity parent) {
        Entity entity = new Entity();
        GraphNode graph = new GraphNode();
        graph.parent = parent;
        entity.add(graph);
        entity.add(new RefSysTransform());
        return entity;
    }

    private static void assertOrthonormal(Matrix4D matrix) {
        Vector3D x = new Vector3D(1, 0, 0).mul(matrix);
        Vector3D y = new Vector3D(0, 1, 0).mul(matrix);
        Vector3D z = new Vector3D(0, 0, 1).mul(matrix);

        Assert.assertEquals(1.0, x.len(), 1e-12);
        Assert.assertEquals(1.0, y.len(), 1e-12);
        Assert.assertEquals(1.0, z.len(), 1e-12);
        Assert.assertEquals(0.0, x.dot(y), 1e-12);
        Assert.assertEquals(0.0, x.dot(z), 1e-12);
        Assert.assertEquals(0.0, y.dot(z), 1e-12);
    }
}
