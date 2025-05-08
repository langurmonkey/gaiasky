/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import gaiasky.util.math.PlaneDouble.PlaneSide;

public class FrustumDouble {
    protected static final Vector3D[] clipSpacePlanePoints = { new Vector3D(-1, -1, -1), new Vector3D(1, -1, -1), new Vector3D(1, 1, -1), new Vector3D(-1, 1, -1), // near clip
            new Vector3D(-1, -1, 1), new Vector3D(1, -1, 1), new Vector3D(1, 1, 1), new Vector3D(-1, 1, 1) }; // far clip
    protected static final double[] clipSpacePlanePointsArray = new double[8 * 3];
    private final static Vector3D tmpV = new Vector3D();

    static {
        int j = 0;
        for (Vector3D v : clipSpacePlanePoints) {
            clipSpacePlanePointsArray[j++] = v.x;
            clipSpacePlanePointsArray[j++] = v.y;
            clipSpacePlanePointsArray[j++] = v.z;
        }
    }

    /** the six clipping planes, near, far, left, right, top, bottom **/
    public final PlaneDouble[] planes = new PlaneDouble[6];

    /** eight points making up the near and far clipping "rectangles". order is counter clockwise, starting at bottom left **/
    public final Vector3D[] planePoints = { new Vector3D(), new Vector3D(), new Vector3D(), new Vector3D(), new Vector3D(), new Vector3D(), new Vector3D(), new Vector3D() };
    protected final double[] planePointsArray = new double[8 * 3];

    public FrustumDouble() {
        for (int i = 0; i < 6; i++) {
            planes[i] = new PlaneDouble(new Vector3D(), 0);
        }
    }

    /**
     * Updates the clipping plane's based on the given inverse combined projection and view matrix, e.g. from an
     * {@link OrthographicCamera} or {@link PerspectiveCamera}.
     *
     * @param inverseProjectionView the combined projection and view matrices.
     */
    public void update(Matrix4D inverseProjectionView) {
        System.arraycopy(clipSpacePlanePointsArray, 0, planePointsArray, 0, clipSpacePlanePointsArray.length);
        Matrix4D.prj(inverseProjectionView.val, planePointsArray, 0, 8, 3);
        for (int i = 0, j = 0; i < 8; i++) {
            Vector3D v = planePoints[i];
            v.x = planePointsArray[j++];
            v.y = planePointsArray[j++];
            v.z = planePointsArray[j++];
        }

        planes[0].set(planePoints[1], planePoints[0], planePoints[2]);
        planes[1].set(planePoints[4], planePoints[5], planePoints[7]);
        planes[2].set(planePoints[0], planePoints[4], planePoints[3]);
        planes[3].set(planePoints[5], planePoints[1], planePoints[6]);
        planes[4].set(planePoints[2], planePoints[3], planePoints[6]);
        planes[5].set(planePoints[4], planePoints[0], planePoints[1]);
    }

    /**
     * Returns whether the point is in the frustum.
     *
     * @param point The point
     *
     * @return Whether the point is in the frustum.
     */
    public boolean pointInFrustum(Vector3D point) {
        for (int i = 0; i < planes.length; i++) {
            PlaneSide result = planes[i].testPoint(point);
            if (result == PlaneSide.Back)
                return false;
        }
        return true;
    }

    /**
     * Returns whether the point is in the frustum.
     *
     * @param x The X coordinate of the point
     * @param y The Y coordinate of the point
     * @param z The Z coordinate of the point
     *
     * @return Whether the point is in the frustum.
     */
    public boolean pointInFrustum(float x, float y, float z) {
        for (int i = 0; i < planes.length; i++) {
            PlaneSide result = planes[i].testPoint(x, y, z);
            if (result == PlaneSide.Back)
                return false;
        }
        return true;
    }

    /**
     * Returns whether the given sphere is in the frustum.
     *
     * @param center The center of the sphere
     * @param radius The radius of the sphere
     *
     * @return Whether the sphere is in the frustum
     */
    public boolean sphereInFrustum(Vector3D center, float radius) {
        for (int i = 0; i < 6; i++)
            if ((planes[i].normal.x * center.x + planes[i].normal.y * center.y + planes[i].normal.z * center.z) < (-radius - planes[i].d))
                return false;
        return true;
    }

    /**
     * Returns whether the given sphere is in the frustum.
     *
     * @param x      The X coordinate of the center of the sphere
     * @param y      The Y coordinate of the center of the sphere
     * @param z      The Z coordinate of the center of the sphere
     * @param radius The radius of the sphere
     *
     * @return Whether the sphere is in the frustum
     */
    public boolean sphereInFrustum(float x, float y, float z, float radius) {
        for (int i = 0; i < 6; i++)
            if ((planes[i].normal.x * x + planes[i].normal.y * y + planes[i].normal.z * z) < (-radius - planes[i].d))
                return false;
        return true;
    }

    /**
     * Returns whether the given sphere is in the frustum not checking whether it is behind the near and far clipping plane.
     *
     * @param center The center of the sphere
     * @param radius The radius of the sphere
     *
     * @return Whether the sphere is in the frustum
     */
    public boolean sphereInFrustumWithoutNearFar(Vector3D center, float radius) {
        for (int i = 2; i < 6; i++)
            if ((planes[i].normal.x * center.x + planes[i].normal.y * center.y + planes[i].normal.z * center.z) < (-radius - planes[i].d))
                return false;
        return true;
    }

    /**
     * Returns whether the given sphere is in the frustum not checking whether it is behind the near and far clipping plane.
     *
     * @param x      The X coordinate of the center of the sphere
     * @param y      The Y coordinate of the center of the sphere
     * @param z      The Z coordinate of the center of the sphere
     * @param radius The radius of the sphere
     *
     * @return Whether the sphere is in the frustum
     */
    public boolean sphereInFrustumWithoutNearFar(float x, float y, float z, float radius) {
        for (int i = 2; i < 6; i++)
            if ((planes[i].normal.x * x + planes[i].normal.y * y + planes[i].normal.z * z) < (-radius - planes[i].d))
                return false;
        return true;
    }

    /**
     * Returns whether the given {@link BoundingBoxDouble} is in the frustum.
     *
     * @param bounds The bounding box
     *
     * @return Whether the bounding box is in the frustum
     */
    public boolean boundsInFrustum(BoundingBoxDouble bounds) {
        for (int i = 0, len2 = planes.length; i < len2; i++) {
            if (planes[i].testPoint(bounds.getCorner000(tmpV)) != PlaneSide.Back)
                continue;
            if (planes[i].testPoint(bounds.getCorner001(tmpV)) != PlaneSide.Back)
                continue;
            if (planes[i].testPoint(bounds.getCorner010(tmpV)) != PlaneSide.Back)
                continue;
            if (planes[i].testPoint(bounds.getCorner011(tmpV)) != PlaneSide.Back)
                continue;
            if (planes[i].testPoint(bounds.getCorner100(tmpV)) != PlaneSide.Back)
                continue;
            if (planes[i].testPoint(bounds.getCorner101(tmpV)) != PlaneSide.Back)
                continue;
            if (planes[i].testPoint(bounds.getCorner110(tmpV)) != PlaneSide.Back)
                continue;
            if (planes[i].testPoint(bounds.getCorner111(tmpV)) != PlaneSide.Back)
                continue;
            return false;
        }

        return true;
    }

    /**
     * Returns whether the given bounding box is in the frustum.
     *
     * @return Whether the bounding box is in the frustum
     */
    public boolean boundsInFrustum(Vector3D center, Vector3D dimensions) {
        return boundsInFrustum(center.x, center.y, center.z, dimensions.x / 2, dimensions.y / 2, dimensions.z / 2);
    }

    /**
     * Returns whether the given bounding box is in the frustum.
     *
     * @return Whether the bounding box is in the frustum
     */
    public boolean boundsInFrustum(double x, double y, double z, double halfWidth, double halfHeight, double halfDepth) {
        for (PlaneDouble plane : planes) {
            if (plane.testPoint(x + halfWidth, y + halfHeight, z + halfDepth) != PlaneSide.Back)
                continue;
            if (plane.testPoint(x + halfWidth, y + halfHeight, z - halfDepth) != PlaneSide.Back)
                continue;
            if (plane.testPoint(x + halfWidth, y - halfHeight, z + halfDepth) != PlaneSide.Back)
                continue;
            if (plane.testPoint(x + halfWidth, y - halfHeight, z - halfDepth) != PlaneSide.Back)
                continue;
            if (plane.testPoint(x - halfWidth, y + halfHeight, z + halfDepth) != PlaneSide.Back)
                continue;
            if (plane.testPoint(x - halfWidth, y + halfHeight, z - halfDepth) != PlaneSide.Back)
                continue;
            if (plane.testPoint(x - halfWidth, y - halfHeight, z + halfDepth) != PlaneSide.Back)
                continue;
            if (plane.testPoint(x - halfWidth, y - halfHeight, z - halfDepth) != PlaneSide.Back)
                continue;
            return false;
        }

        return true;
    }

}
