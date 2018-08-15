package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import gaia.cu9.ari.gaiaorbit.render.ILineRenderable;
import gaia.cu9.ari.gaiaorbit.render.system.LineRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

/**
 * Represents the outline of a country
 * 
 * @author tsagrista
 *
 */
public class Area extends AbstractPositionEntity implements ILineRenderable {

    private float[][][] loc2d, loc3d;

    private Vector3 aux3;

    private int population;
    private float gdp;
    private int censusYear;

    /** Max latitud/longitude and min latitude/longitude **/
    private Vector2 maxlonlat, minlonlat;
    /** Cartesian points correspoding to maximum lonlat and minimum lonlat **/
    private Vector3 cart0;

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
        int n = loc3d.length;
        for (int lineidx = 0; lineidx < n; lineidx++) {
            float[][] linepoints = loc3d[lineidx];
            int m = linepoints.length;
            for (int pointidx = 1; pointidx < m; pointidx++) {
                renderer.addLine(this, loc3d[lineidx][pointidx - 1][0], loc3d[lineidx][pointidx - 1][1], loc3d[lineidx][pointidx - 1][2], loc3d[lineidx][pointidx][0], loc3d[lineidx][pointidx][1], loc3d[lineidx][pointidx][2], cc[0], cc[1], cc[2], alpha * opacity);
            }
            // Close line
            renderer.addLine(this, loc3d[lineidx][m - 1][0], loc3d[lineidx][m - 1][1], loc3d[lineidx][m - 1][2], loc3d[lineidx][0][0], loc3d[lineidx][0][1], loc3d[lineidx][0][2], cc[0], cc[1], cc[2], alpha * opacity);
        }

    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (isVisibilityOn()) {
            addToRender(this, RenderGroup.LINE);
        }

    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {

        float angleLow = (float) ((ModelBody) parent).THRESHOLD_QUAD() * camera.getFovFactor() * 100f;
        float angleHigh = (float) ((ModelBody) parent).THRESHOLD_QUAD() * camera.getFovFactor() * 200f;

        if (isVisibilityOn() && ((ModelBody) parent).viewAngleApparent > angleLow) {
            //ModelBody papa = (ModelBody) parent;
            localTransform.idt();
            toCartesian(loc2d[0][0][0], loc2d[0][0][1], cart0, localTransform);

            //            Vector3d auxi = aux3d1.get();
            //            transform.getTranslation(auxi).scl(-1);
            //            double cosalpha = auxi.add(cart0.x, cart0.y, cart0.z).nor().dot(GaiaSky.instance.cam.getDirection().nor());
            //
            //            if (cosalpha < -0.1) {

            updateLocalValues(time, camera);
            this.translation.add(pos);

            this.opacity = (float) MathUtilsd.lint(((ModelBody) parent).viewAngleApparent, angleLow, angleHigh, 0, 1);

            this.distToCamera = (float) translation.len();
            this.viewAngle = (float) FastMath.atan(size / distToCamera) / camera.getFovFactor();
            this.viewAngleApparent = this.viewAngle;
            if (!copy) {
                addToRenderLists(camera);
            }
            //            }
        }
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
        ModelBody papa = (ModelBody) parent;
        papa.setToLocalTransform(1f, localTransform, false);
        int lineidx = 0;
        for (float[][] line : loc2d) {
            int pointidx = 0;
            for (float[] point : line) {
                toCartesian(point[0], point[1], aux3, localTransform);

                loc3d[lineidx][pointidx][0] = aux3.x;
                loc3d[lineidx][pointidx][1] = aux3.y;
                loc3d[lineidx][pointidx][2] = aux3.z;

                pointidx++;
            }

            lineidx++;
        }

    }

    private Vector3 toCartesian(float lon, float lat, Vector3 res, Matrix4 localTransform) {
        res.set(0, 0, -0.501f);
        // Latitude [-90..90]
        res.rotate(lat, 1, 0, 0);
        // Longitude [0..360]
        res.rotate(lon + 90, 0, 1, 0);

        res.mul(localTransform);

        return res;

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

    public void setPopulation(Long population) {
        this.population = population.intValue();
    }

    public void setGdp(Double gdp) {
        this.gdp = gdp.floatValue();
    }

    public void setCensusYear(Long census) {
        this.censusYear = census.intValue();
    }

    @Override
    public float getLineWidth() {
        return 1;
    }

}
