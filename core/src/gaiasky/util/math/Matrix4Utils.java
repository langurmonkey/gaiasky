/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import java.nio.FloatBuffer;

public class Matrix4Utils {

    public static void setScaling(Matrix4 m, float scl) {
        // Normalize the basis vectors to remove existing scale, then apply new scale
        float lenX = (float) Math.sqrt(m.val[Matrix4.M00] * m.val[Matrix4.M00] +
                                               m.val[Matrix4.M01] * m.val[Matrix4.M01] +
                                               m.val[Matrix4.M02] * m.val[Matrix4.M02]);
        float lenY = (float) Math.sqrt(m.val[Matrix4.M10] * m.val[Matrix4.M10] +
                                               m.val[Matrix4.M11] * m.val[Matrix4.M11] +
                                               m.val[Matrix4.M12] * m.val[Matrix4.M12]);
        float lenZ = (float) Math.sqrt(m.val[Matrix4.M20] * m.val[Matrix4.M20] +
                                               m.val[Matrix4.M21] * m.val[Matrix4.M21] +
                                               m.val[Matrix4.M22] * m.val[Matrix4.M22]);

        if (lenX > 0) {
            float invLenX = 1.0f / lenX;
            m.val[Matrix4.M00] *= invLenX * scl;
            m.val[Matrix4.M01] *= invLenX * scl;
            m.val[Matrix4.M02] *= invLenX * scl;
        }

        if (lenY > 0) {
            float invLenY = 1.0f / lenY;
            m.val[Matrix4.M10] *= invLenY * scl;
            m.val[Matrix4.M11] *= invLenY * scl;
            m.val[Matrix4.M12] *= invLenY * scl;
        }

        if (lenZ > 0) {
            float invLenZ = 1.0f / lenZ;
            m.val[Matrix4.M20] *= invLenZ * scl;
            m.val[Matrix4.M21] *= invLenZ * scl;
            m.val[Matrix4.M22] *= invLenZ * scl;
        }
    }

    public static void setScaling(Matrix4 m, Vector3 scl) {
        // Normalize the basis vectors to remove existing scale, then apply new scale
        float lenX = (float) Math.sqrt(m.val[Matrix4.M00] * m.val[Matrix4.M00] +
                                               m.val[Matrix4.M01] * m.val[Matrix4.M01] +
                                               m.val[Matrix4.M02] * m.val[Matrix4.M02]);
        float lenY = (float) Math.sqrt(m.val[Matrix4.M10] * m.val[Matrix4.M10] +
                                               m.val[Matrix4.M11] * m.val[Matrix4.M11] +
                                               m.val[Matrix4.M12] * m.val[Matrix4.M12]);
        float lenZ = (float) Math.sqrt(m.val[Matrix4.M20] * m.val[Matrix4.M20] +
                                               m.val[Matrix4.M21] * m.val[Matrix4.M21] +
                                               m.val[Matrix4.M22] * m.val[Matrix4.M22]);

        if (lenX > 0) {
            float invLenX = 1.0f / lenX;
            m.val[Matrix4.M00] *= invLenX * scl.x;
            m.val[Matrix4.M01] *= invLenX * scl.x;
            m.val[Matrix4.M02] *= invLenX * scl.x;
        }

        if (lenY > 0) {
            float invLenY = 1.0f / lenY;
            m.val[Matrix4.M10] *= invLenY * scl.y;
            m.val[Matrix4.M11] *= invLenY * scl.y;
            m.val[Matrix4.M12] *= invLenY * scl.y;
        }

        if (lenZ > 0) {
            float invLenZ = 1.0f / lenZ;
            m.val[Matrix4.M20] *= invLenZ * scl.z;
            m.val[Matrix4.M21] *= invLenZ * scl.z;
            m.val[Matrix4.M22] *= invLenZ * scl.z;
        }
    }

    /**
     * Recovers the Euler angles (yaw, pitch, and roll) from the given 4x4 transformation matrix, in degrees.
     * <p>
     * This method extracts the yaw, pitch, and roll values from a matrix representing a 3D rotation. The angles are calculated
     * based on the matrix elements. The result is a 3-element array where:
     * <ul>
     *   <li>Element 0 is the yaw angle (rotation around the Y-axis),</li>
     *   <li>Element 1 is the pitch angle (rotation around the X-axis),</li>
     *   <li>Element 2 is the roll angle (rotation around the Z-axis).</li>
     * </ul>
     * <p>
     * In the case of gimbal lock (when the cosine of the pitch is zero), yaw is set to 0 and roll is computed from the matrix elements.
     *
     * @param m the 4x4 transformation matrix representing the rotation
     * @return an array of floats containing the Euler angles in the order: [yaw, pitch, roll]
     */
    public static float[] recoverEulerAngles(Matrix4 m) {
        float[] eulerAngles = new float[3];

        // Pitch (rotation around X-axis)
        float pitch = (float) Math.asin(-m.val[Matrix4.M22]);

        // Yaw (rotation around Y-axis) and Roll (rotation around Z-axis)
        float yaw, roll;

        if (Math.cos(pitch) != 0) {
            yaw = (float) Math.atan2(m.val[Matrix4.M21], m.val[Matrix4.M22]); // Yaw = atan2(M21, M22)
            roll = (float) Math.atan2(m.val[Matrix4.M10], m.val[Matrix4.M00]); // Roll = atan2(M10, M00)
        } else {
            // Gimbal lock case, we can set yaw and roll to zero (or any default values)
            yaw = 0;
            roll = (float) Math.atan2(m.val[Matrix4.M01], m.val[Matrix4.M11]); // Roll = atan2(M01, M11)
        }

        // Return the Euler angles as an array: [yaw, pitch, roll]
        eulerAngles[0] = (float) Math.toDegrees(yaw);
        eulerAngles[1] = (float) Math.toDegrees(pitch);
        eulerAngles[2] = (float) Math.toDegrees(roll);

        return eulerAngles;
    }

    public static void put(Matrix4 m, FloatBuffer buffer) {
        get(m, buffer.position(), buffer);
    }

    public static void get(Matrix4 m, int offset, FloatBuffer src) {
        m.val[0] = src.get(offset);
        m.val[1] = src.get(offset + 1);
        m.val[2] = src.get(offset + 2);
        m.val[3] = src.get(offset + 3);

        m.val[4] = src.get(offset + 4);
        m.val[5] = src.get(offset + 5);
        m.val[6] = src.get(offset + 6);
        m.val[7] = src.get(offset + 7);

        m.val[8] = src.get(offset + 8);
        m.val[9] = src.get(offset + 9);
        m.val[10] = src.get(offset + 10);
        m.val[11] = src.get(offset + 11);

        m.val[12] = src.get(offset + 12);
        m.val[13] = src.get(offset + 13);
        m.val[14] = src.get(offset + 14);
        m.val[15] = src.get(offset + 15);
    }
}
