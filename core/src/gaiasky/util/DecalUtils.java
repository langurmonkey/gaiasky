/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import gaiasky.render.RenderingContext;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.g2d.Sprite;
import gaiasky.util.math.Vector3d;

public class DecalUtils {

    static Vector3 tmp, tmp2, tmp3;
    static Matrix4 idt, aux1, aux2;

    static {
        tmp = new Vector3();
        tmp2 = new Vector3();
        tmp3 = new Vector3();
        idt = new Matrix4();
        aux1 = new Matrix4();
        aux2 = new Matrix4();
    }

    /**
     * Draws the given sprite using the given font in the given 3D position using
     * the 3D coordinate space. If faceCamera is true, the sprite is rendered
     * always facing the camera. It assumes that {@link ExtSpriteBatch#begin()} has
     * been called. This enables 3D techniques such as z-buffering to be applied
     * to the text textures.
     *
     * @param sprite         The sprite.
     * @param batch          The sprite batch to use.
     * @param x              The x coordinate.
     * @param y              The y coordinate.
     * @param z              The z coordinate.
     * @param size           The scale of the sprite.
     * @param rotationCenter Angles to rotate around center.
     * @param cam            The camera.
     * @param faceCamera     Whether to apply bill-boarding.
     * @param minSizeDegrees Minimum visual size of the text in degrees. Zero or negative to disable.
     * @param maxSizeDegrees Maximum visual size of the text in degrees. Zero or negative to disable.
     */
    public static void drawSprite(Sprite sprite, SpriteBatch batch, float x, float y, float z, double size, float rotationCenter, ICamera cam, boolean faceCamera, float minSizeDegrees, float maxSizeDegrees) {
        sprite.setPosition(0, 0);
        sprite.setCenter(0, 0);
        // Store batch matrices
        aux1.set(batch.getTransformMatrix());
        aux2.set(batch.getProjectionMatrix());

        Camera camera = cam.getCamera();
        Quaternion rotation = getBillboardRotation(faceCamera ? camera.direction : tmp3.set(x, y, z).nor(), camera.up);

        if (minSizeDegrees > 0 || maxSizeDegrees > 0) {
            double dist = camera.position.dst(tmp3.set(x, y, z));
            double minsize = minSizeDegrees > 0 ? Math.tan(Math.toRadians(minSizeDegrees)) * dist : 0d;
            double maxsize = maxSizeDegrees > 0 ? Math.tan(Math.toRadians(maxSizeDegrees)) * dist : 1e20d;
            size = MathUtils.clamp(size, (float) minsize, (float) maxsize);
        }

        float sizeF = (float) size;
        batch.getTransformMatrix().set(camera.combined).translate(x, y, z).rotate(rotation).rotate(0, 1, 0, 180).rotate(0, 0, 1, rotationCenter).scale(sizeF, sizeF, sizeF);
        // Force matrices to be set to shader
        batch.setProjectionMatrix(idt);

        sprite.draw(batch);

        // Restore batch matrices
        batch.setTransformMatrix(aux1);
        batch.setProjectionMatrix(aux2);
    }

    /**
     * Draws the given text using the given font in the given 3D position using
     * the 3D coordinate space. If faceCamera is true, the text is rendered
     * always facing the camera. It assumes that {@link ExtSpriteBatch#begin()} has
     * been called. This enables 3D techniques such as z-buffering to be applied
     * to the text textures.
     *
     * @param font     The font.
     * @param batch    The sprite batch to use.
     * @param text     The text to write.
     * @param position The 3D position.
     * @param camera   The camera.
     */
    public static void drawFont3D(BitmapFont font, ExtSpriteBatch batch, String text, Vector3 position, ICamera camera, boolean faceCamera) {
        drawFont3D(font, batch, text, position, 1f, camera, faceCamera);
    }

    public static void drawFont3D(BitmapFont font, ExtSpriteBatch batch, String text, float x, float y, float z, float size, float rotationCenter, ICamera camera, boolean faceCamera) {
        drawFont3D(font, batch, text, x, y, z, size, rotationCenter, camera, faceCamera, -1, -1);
    }

    /**
     * Draws the given text using the given font in the given 3D position using
     * the 3D coordinate space. If faceCamera is true, the text is rendered
     * always facing the camera. It assumes that {@link ExtSpriteBatch#begin()} has
     * been called. This enables 3D techniques such as z-buffering to be applied
     * to the text textures.
     *
     * @param font           The font.
     * @param batch          The sprite batch to use.
     * @param text           The text to write.
     * @param x              The x coordinate.
     * @param y              The y coordinate.
     * @param z              The z coordinate.
     * @param size           The scale of the font.
     * @param rotationCenter Angles to rotate around center.
     * @param cam            The camera.
     * @param faceCamera     Whether to apply bill-boarding.
     * @param minSizeDegrees Minimum visual size of the text in degrees. Zero or negative to disable.
     * @param maxSizeDegrees Maximum visual size of the text in degrees. Zero or negative to disable.
     */
    public static void drawFont3D(BitmapFont font, ExtSpriteBatch batch, String text, float x, float y, float z, double size, float rotationCenter, ICamera cam, boolean faceCamera, float minSizeDegrees, float maxSizeDegrees) {
        // Store batch matrices
        aux1.set(batch.getTransformMatrix());
        aux2.set(batch.getProjectionMatrix());

        Camera camera = cam.getCamera();
        Quaternion rotation = getBillboardRotation(faceCamera ? camera.direction : tmp3.set(x, y, z).nor(), camera.up);

        if (minSizeDegrees > 0 || maxSizeDegrees > 0) {
            double dist = camera.position.dst(tmp3.set(x, y, z));
            double minsize = minSizeDegrees > 0 ? Math.tan(Math.toRadians(minSizeDegrees)) * dist : 0d;
            double maxsize = maxSizeDegrees > 0 ? Math.tan(Math.toRadians(maxSizeDegrees)) * dist : 1e20d;
            size = MathUtils.clamp(size, (float) minsize, (float) maxsize);
        }

        float sizeF = (float) size;
        batch.getTransformMatrix().set(camera.combined).translate(x, y, z).rotate(rotation).rotate(0, 1, 0, 180).rotate(0, 0, 1, rotationCenter).scale(sizeF, sizeF, sizeF);
        // Force matrices to be set to shader
        batch.setProjectionMatrix(idt);

        font.draw(batch, text, 0, 0);

        // Restore batch matrices
        batch.setTransformMatrix(aux1);
        batch.setProjectionMatrix(aux2);
    }

    /**
     * Draws the given text using the given font in the given 3D position using
     * the 3D coordinate space. If faceCamera is true, the text is rendered
     * always facing the camera. It assumes that {@link ExtSpriteBatch#begin()} has
     * been called. This enables 3D techniques such as z-buffering to be applied
     * to the text textures.
     *
     * @param font     The font.
     * @param batch    The sprite batch to use.
     * @param text     The text to write.
     * @param position The 3D position.
     * @param camera   The camera.
     * @param scale    The scale of the font.
     */
    public static void drawFont3D(BitmapFont font, ExtSpriteBatch batch, String text, Vector3 position, float scale, ICamera camera, boolean faceCamera) {
        drawFont3D(font, batch, text, position.x, position.y, position.z, scale, 0, camera, faceCamera);
    }

    public static void drawFont2D(BitmapFont font, ExtSpriteBatch batch, String text, Vector3 position) {
        font.draw(batch, text, position.x, position.y);
    }

    public static void drawFont2D(BitmapFont font, ExtSpriteBatch batch, String text, float x, float y) {
        font.draw(batch, text, x, y);
    }

    public static void drawFont2D(BitmapFont font, ExtSpriteBatch batch, RenderingContext rc, String text, float x, float y, float scale, float width) {
        drawFont2D(font, batch, rc, text, x, y, scale, -1);
    }

    public static void drawFont2D(BitmapFont font, ExtSpriteBatch batch, RenderingContext rc, String text, float x, float y, float scale, int align) {
        // Backup font scale and matrix.
        float scaleXBak = font.getData().scaleX;
        float scaleYBak = font.getData().scaleY;
        aux1.set(batch.getProjectionMatrix());

        batch.getProjectionMatrix().setToOrtho2D(0, 0, rc.w(), rc.h());
        font.getData().setScale(scale);

        if (align > 0) {
            font.draw(batch, text, x, y, rc.w(), align, false);
        } else {
            font.draw(batch, text, x, y);
        }

        // Restore font scale and matrix.
        batch.setProjectionMatrix(aux1);
        font.getData().setScale(scaleXBak, scaleYBak);

    }

    private static void lookAtRotation(Quaternion quaternion, Vector3 direction, Vector3 up) {
        // Orthonormalize
        direction.nor();
        up = up.sub(direction.scl(up.dot(direction)));
        up.nor();

        Vector3 right = up.crs(direction);

        quaternion.w = (float) Math.sqrt(1.0f + right.x + up.y + direction.z) * 0.5f;

        double w4Recip = 1.0 / (4.0 * quaternion.w);
        quaternion.x = (float) ((up.z - direction.y) * w4Recip);
        quaternion.y = (float) ((direction.x - right.z) * w4Recip);
        quaternion.z = (float) ((right.y - up.x) * w4Recip);
    }

    /**
     * Gets the billboard rotation using the parameters of the given camera
     *
     * @param camera The camera
     *
     * @return The quaternion with the rotation
     */
    public static Quaternion getBillboardRotation(Camera camera) {
        return getBillboardRotation(camera.direction, camera.up);
    }

    /**
     * Returns a Quaternion representing the billboard rotation to be applied to
     * a decal that is always to face the given direction and up vector
     *
     * @param direction The direction vector
     * @param up        The up vector
     *
     * @return The quaternion with the rotation
     */
    public static Quaternion getBillboardRotation(Vector3 direction, Vector3 up) {
        Quaternion rotation = new Quaternion();
        setBillboardRotation(rotation, direction, up);
        return rotation;
    }

    /**
     * Sets the rotation of this decal based on the (normalized) direction and
     * up vector.
     *
     * @param rotation  out-parameter, quaternion where the result is set
     * @param direction the direction vector
     * @param up        the up vector
     */
    public static void setBillboardRotation(Quaternion rotation, final Vector3 direction, final Vector3 up) {
        tmp.set(up).crs(direction).nor();
        tmp2.set(direction).crs(tmp).nor();
        rotation.setFromAxes(tmp.x, tmp2.x, direction.x, tmp.y, tmp2.y, direction.y, tmp.z, tmp2.z, direction.z);
        //tmp.set(direction);
        //tmp2.set(up);
        //lookAtRotation(rotation, tmp, tmp2);
    }

    /**
     * Sets the rotation of this decal based on the (normalized) direction and
     * up vector.
     *
     * @param direction the direction vector
     * @param up        the up vector
     */
    public static void setBillboardRotation(Quaternion rotation, final Vector3d direction, final Vector3d up) {
        tmp.set((float) up.x, (float) up.y, (float) up.z).crs((float) direction.x, (float) direction.y, (float) direction.z).nor();
        tmp2.set((float) direction.x, (float) direction.y, (float) direction.z).crs(tmp).nor();
        rotation.setFromAxes(tmp.x, tmp2.x, (float) direction.x, tmp.y, tmp2.y, (float) direction.y, tmp.z, tmp2.z, (float) direction.z);
        //direction.put(tmp);
        //up.put(tmp2);
        //lookAtRotation(rotation, tmp, tmp2);
    }

}
