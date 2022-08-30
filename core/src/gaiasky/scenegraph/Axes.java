/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.ILineRenderable;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Logger;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

/**
 * Representation of axes
 */
public class Axes extends SceneGraphNode implements ILineRenderable {
    private static final double LINE_SIZE_RAD = Math.tan(Math.toRadians(2.9));
    private String transformName;
    private Matrix4d coordinateSystem;
    private final Vector3d o;
    private final Vector3d x;
    private final Vector3d y;
    private final Vector3d z;
    private Vector3d b0, b1, b2;

    // RGBA colors for each of the bases XYZ -> [3][3]
    private float[][] axesColors;

    public Axes() {
        super();
        o = new Vector3d();
        x = new Vector3d();
        y = new Vector3d();
        z = new Vector3d();
    }

    @Override
    public void initialize() {
        // Base
        b0 = new Vector3d(1, 0, 0);
        b1 = new Vector3d(0, 1, 0);
        b2 = new Vector3d(0, 0, 1);

    }

    @Override
    public void doneLoading(AssetManager manager) {
        if (transformName != null) {
            Class<Coordinates> c = Coordinates.class;
            try {
                Method m = ClassReflection.getMethod(c, transformName);
                coordinateSystem = (Matrix4d) m.invoke(null);
            } catch (ReflectionException e) {
                Logger.getLogger(this.getClass()).error("Error getting/invoking method Coordinates." + transformName + "()");
            }
            b0.mul(coordinateSystem);
            b1.mul(coordinateSystem);
            b2.mul(coordinateSystem);
        }

        // Axes colors, RGB default
        if (axesColors == null) {
            axesColors = new float[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };
        }

    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
        distToCamera = (float) camera.getPos().lend();
        size = (float) (LINE_SIZE_RAD * distToCamera) * camera.getFovFactor();

        o.set(camera.getInversePos());
        x.set(b0).scl(size).add(o);
        y.set(b1).scl(size).add(o);
        z.set(b2).scl(size).add(o);
    }

    @Override
    public void addToRenderLists(ICamera camera) {
        if (this.shouldRender())
            addToRender(this, RenderGroup.LINE);
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        if (alpha > 0) {
            // X
            renderer.addLine(this, o.x, o.y, o.z, x.x, x.y, x.z, axesColors[0][0], axesColors[0][1], axesColors[0][2], alpha);
            // Y
            renderer.addLine(this, o.x, o.y, o.z, y.x, y.y, y.z, axesColors[1][0], axesColors[1][1], axesColors[1][2], alpha);
            // Z
            renderer.addLine(this, o.x, o.y, o.z, z.x, z.y, z.z, axesColors[2][0], axesColors[2][1], axesColors[2][2], alpha);
        }
    }

    public float getLineWidth() {
        return 1;
    }

    public int getGlPrimitive() {
        return GL30.GL_LINES;
    }

    public void setTransformName(String transformName) {
        this.transformName = transformName;
    }

    public void setAxesColors(double[][] colors) {
        axesColors = new float[3][3];
        axesColors[0][0] = (float) colors[0][0];
        axesColors[0][1] = (float) colors[0][1];
        axesColors[0][2] = (float) colors[0][2];

        axesColors[1][0] = (float) colors[1][0];
        axesColors[1][1] = (float) colors[1][1];
        axesColors[1][2] = (float) colors[1][2];

        axesColors[2][0] = (float) colors[2][0];
        axesColors[2][1] = (float) colors[2][1];
        axesColors[2][2] = (float) colors[2][2];
    }
}
