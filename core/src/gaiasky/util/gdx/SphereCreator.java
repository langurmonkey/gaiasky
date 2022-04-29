/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import gaiasky.util.gdx.IntMeshPartBuilder.VertexInfo;

public class SphereCreator extends ModelCreator {

    private static final Vector3 axisY = new Vector3();
    private static final Vector3 axisZ = new Vector3();
    private static final VertexInfo vertTmp3 = new VertexInfo();
    private static IntArray tmpIndices = new IntArray();

    public static void create(IntIntMeshBuilder builder, final Matrix4 transform, float width, float height, float depth, int divisionsU, int divisionsV, boolean flipNormals, float angleUFrom, float angleUTo, float angleVFrom, float angleVTo) {
        axisY.set(0, 1, 0);
        axisZ.set(0, 0, 1);
        final float hw = width * 0.5f;
        final float hh = height * 0.5f;
        final float hd = depth * 0.5f;
        final float auo = MathUtils.degreesToRadians * angleUFrom;
        final float stepU = (MathUtils.degreesToRadians * (angleUTo - angleUFrom)) / divisionsU;
        final float avo = MathUtils.degreesToRadians * angleVFrom;
        final float stepV = (MathUtils.degreesToRadians * (angleVTo - angleVFrom)) / divisionsV;
        final float us = 1f / divisionsU;
        final float vs = 1f / divisionsV;
        float u;
        float v;
        float angleU;
        float angleV;
        VertexInfo curr1 = vertTmp3.set(null, null, null, null);
        curr1.hasUV = curr1.hasPosition = curr1.hasNormal = curr1.hasTangent = curr1.hasBinormal = true;

        if (tmpIndices == null)
            tmpIndices = new IntArray(divisionsU * 2);
        final int s = divisionsU + 3;
        tmpIndices.ensureCapacity(s);
        while (tmpIndices.size > s)
            tmpIndices.pop();
        while (tmpIndices.size < s)
            tmpIndices.add(-1);
        int tempOffset = 0;

        builder.ensureRectangles((divisionsV + 1) * (divisionsU + 1), divisionsV * divisionsU);
        for (int iv = 0; iv <= divisionsV; iv++) {
            angleV = avo + stepV * iv;
            v = vs * iv;
            final float t = MathUtils.sin(angleV);
            final float h = MathUtils.cos(angleV) * hh;
            for (int iu = 0; iu <= divisionsU; iu++) {
                angleU = auo + stepU * iu;
                u = 1f - us * iu;
                curr1.position.set(MathUtils.cos(angleU) * hw * t, h, MathUtils.sin(angleU) * hd * t).mul(transform);
                // Normal is just the position
                curr1.normal.set(curr1.position).nor();

                // ##### Due to our internal refsys, we invert binormal and tangent
                // Binormal, aligned with V
                curr1.binormal.set(1f, 0f, 0f).rotateRad(axisZ, -angleV).rotateRad(axisY, -angleU).nor();
                // Tangent, aligned with U
                curr1.tangent.set(curr1.normal).crs(curr1.binormal).nor();

                curr1.uv.set(u, v);
                tmpIndices.set(tempOffset, builder.vertex(curr1));
                final int o = tempOffset + s;
                if ((iv > 0) && (iu > 0))
                    if (!flipNormals) {
                        builder.rect(tmpIndices.get(tempOffset), tmpIndices.get((o - 1) % s), tmpIndices.get((o - (divisionsU + 2)) % s), tmpIndices.get((o - (divisionsU + 1)) % s));
                    } else {
                        builder.rect(tmpIndices.get(tempOffset), tmpIndices.get((o - (divisionsU + 1)) % s), tmpIndices.get((o - (divisionsU + 2)) % s), tmpIndices.get((o - 1) % s));
                    }
                tempOffset = (tempOffset + 1) % tmpIndices.size;
            }
        }
    }
}
