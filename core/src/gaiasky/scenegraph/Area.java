/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.ILineRenderable;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

/**
 * Represents the outline of a country
 */
public class Area extends SceneGraphNode implements ILineRenderable {

    private float[][][] loc2d, loc3d;

    private Vector3 aux3;

    /** Max latitude/longitude and min latitude/longitude **/
    private final Vector2 maxlonlat;
    private final Vector2 minlonlat;
    /** Cartesian points corresponding to maximum lonlat and minimum lonlat **/
    private final Vector3 cart0;

    public Area() {
        cc = new float[] { 0.8f, 0.8f, 0.f, 1f };
        localTransform = new Matrix4();

        maxlonlat = new Vector2(-1000, -1000);
        minlonlat = new Vector2(1000, 1000);

        cart0 = new Vector3();
    }

    public void initialize() {
        loc3d = new float[loc2d.length][][];
        for (int line = 0; line < loc2d.length; line++) {
            loc3d[line] = new float[loc2d[line].length][3];
        }

        aux3 = new Vector3();
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        for (float[][] linePoints : loc3d) {
            int m = linePoints.length;
            for (int pointIndex = 1; pointIndex < m; pointIndex++) {
                renderer.addLine(this, linePoints[pointIndex - 1][0], linePoints[pointIndex - 1][1], linePoints[pointIndex - 1][2], linePoints[pointIndex][0], linePoints[pointIndex][1], linePoints[pointIndex][2], cc[0], cc[1], cc[2], alpha * opacity);
            }
            // Close line
            renderer.addLine(this, linePoints[m - 1][0], linePoints[m - 1][1], linePoints[m - 1][2], linePoints[0][0], linePoints[0][1], linePoints[0][2], cc[0], cc[1], cc[2], alpha * opacity);
        }
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender()) {
            addToRender(this, RenderGroup.LINE);
        }

    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        float angleLow = (float) ((ModelBody) parent).thresholdQuad * camera.getFovFactor() * 100f;
        float angleHigh = (float) ((ModelBody) parent).thresholdQuad * camera.getFovFactor() * 200f;

        if (isVisibilityOn() && parent.viewAngleApparent > angleLow) {
            localTransform.idt();
            toCartesian(loc2d[0][0][0], loc2d[0][0][1], cart0, localTransform);

            updateLocalValues(time, camera);
            this.translation.add(pos);

            this.opacity = (float) MathUtilsd.lint(parent.viewAngleApparent, angleLow, angleHigh, 0, 1);
            this.opacity *= this.getVisibilityOpacityFactor();

            this.distToCamera = (float) translation.lend();
            this.viewAngle = (float) FastMath.atan(size / distToCamera);
            this.viewAngleApparent = this.viewAngle / camera.getFovFactor();
            if (!copy) {
                addToRenderLists(camera);
            }
        }
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
        ModelBody papa = (ModelBody) parent;
        papa.setToLocalTransform(1f, localTransform, false);
        int lineIndex = 0;
        for (float[][] line : loc2d) {
            int pointIndex = 0;
            for (float[] point : line) {
                toCartesian(point[0], point[1], aux3, localTransform);

                loc3d[lineIndex][pointIndex][0] = aux3.x;
                loc3d[lineIndex][pointIndex][1] = aux3.y;
                loc3d[lineIndex][pointIndex][2] = aux3.z;

                pointIndex++;
            }

            lineIndex++;
        }

    }

    private void toCartesian(float lon, float lat, Vector3 res, Matrix4 localTransform) {
        res.set(0, 0, -0.5015f);
        // Latitude [-90..90]
        res.rotate(lat, 1, 0, 0);
        // Longitude [0..360]
        res.rotate(lon + 90, 0, 1, 0);

        res.mul(localTransform);
    }

    public void setPerimeter(double[][][] perimeter) {
        this.loc2d = new float[perimeter.length][][];
        for (int i = 0; i < perimeter.length; i++) {
            float[][] arr = new float[perimeter[i].length][];
            for (int j = 0; j < perimeter[i].length; j++) {
                arr[j] = new float[2];
                arr[j][0] = (float) perimeter[i][j][0];
                arr[j][1] = (float) perimeter[i][j][1];

                // Longitude
                if (arr[j][0] > maxlonlat.x) {
                    maxlonlat.x = arr[j][0];
                }

                if (arr[j][0] < minlonlat.x) {
                    minlonlat.x = arr[j][0];
                }

                // Latitude
                if (arr[j][1] > maxlonlat.y) {
                    maxlonlat.y = arr[j][1];
                }

                if (arr[j][1] < minlonlat.y) {
                    minlonlat.y = arr[j][1];
                }

            }
            this.loc2d[i] = arr;
        }
    }

    @Override
    public float getLineWidth() {
        return 0.5f;
    }

    @Override
    public int getGlPrimitive() {
        return GL20.GL_LINES;
    }

    @Override
    public boolean mustAddToIndex() {
        return false;
    }

}
