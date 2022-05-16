package gaiasky.scene.render.draw.line;

import com.badlogic.gdx.math.MathUtils;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Render;
import gaiasky.scene.render.draw.LinePrimitiveRenderer;
import gaiasky.scene.view.LineView;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.math.Vector3d;

import java.time.Instant;

/**
 * Implements line rendering for the different families of entities.
 */
public class LineEntityRenderSystem {

    /** The line view object, used to send into the
     * {@link LinePrimitiveRenderer}.**/
    private LineView lineView;
    private Vector3d D31 = new Vector3d();
    protected Vector3d prev = new Vector3d(), curr = new Vector3d();

    public LineEntityRenderSystem() {
        this.lineView = new LineView();
    }

    public void render(final Render render, LinePrimitiveRenderer renderer, ICamera camera, float alpha) {
        final var entity = render.entity;
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var trajectory = Mapper.trajectory.get(entity);
        var verts = Mapper.verts.get(entity);
        if (!trajectory.onlyBody) {
            var localTransformD = trajectory.localTransformD;
            var oc = trajectory.oc;
            var orbitTrail = trajectory.orbitTrail;
            var cc = body.color;
            var pointCloudData = verts.pointCloudData;

            alpha *= trajectory.alpha * base.opacity;

            Vector3d parentPos;
            parentPos = Mapper.attitude.has(graph.parent) ? Mapper.attitude.get(graph.parent).nonRotatedPos : null;
            int last = parentPos != null ? 2 : 1;

            float dAlpha = 0f;
            int stIdx = 0;
            int nPoints = verts.pointCloudData.getNumPoints();

            boolean reverse = GaiaSky.instance.time.getWarpFactor() < 0;

            Vector3d bodyPos = D31.setZero();
            if (orbitTrail) {
                float top = alpha;
                float bottom = 0f;
                dAlpha = (top - bottom) / nPoints;
                Instant currentTime = GaiaSky.instance.time.getTime();
                long wrapTime = verts.pointCloudData.getWrapTimeMs(currentTime);
                stIdx = verts.pointCloudData.getIndex(wrapTime);

                if (trajectory.body != null) {
                    bodyPos.set(Mapper.graph.get(trajectory.body).translation);
                } else if (oc != null) {
                    oc.loadDataPoint(bodyPos, currentTime);
                    bodyPos.mul(localTransformD);
                }

                if (!reverse) {
                    alpha = bottom;
                    dAlpha = -dAlpha;
                }
            }

            lineView.setEntity(entity);

            // This is so that the shape renderer does not mess up the z-buffer
            int n = 0;
            if (oc.period > 0) {
                // Periodic orbits
                int i = wrap(stIdx + 2, nPoints);
                float cAlpha;
                while (n < nPoints - last) {
                    // i minus one
                    int im = wrap(i - 1, nPoints);

                    verts.pointCloudData.loadPoint(prev, im);
                    verts.pointCloudData.loadPoint(curr, i);

                    if (parentPos != null) {
                        prev.sub(parentPos);
                        curr.sub(parentPos);
                    }

                    prev.mul(localTransformD);
                    curr.mul(localTransformD);

                    cAlpha = MathUtils.clamp(alpha, 0f, 1f);
                    if (orbitTrail && !reverse && n == nPoints - 2) {
                        renderer.addLine(lineView, (float) curr.x, (float) curr.y, (float) curr.z, (float) bodyPos.x, (float) bodyPos.y, (float) bodyPos.z, cc[0], cc[1], cc[2], cAlpha * cc[3]);
                    } else if (orbitTrail && reverse && n == 0) {
                        renderer.addLine(lineView, (float) curr.x, (float) curr.y, (float) curr.z, (float) bodyPos.x, (float) bodyPos.y, (float) bodyPos.z, cc[0], cc[1], cc[2], cAlpha * cc[3]);
                    }
                    renderer.addLine(lineView, (float) prev.x, (float) prev.y, (float) prev.z, (float) curr.x, (float) curr.y, (float) curr.z, cc[0], cc[1], cc[2], cAlpha * cc[3]);

                    alpha -= dAlpha;

                    // advance
                    i = wrap(i + 1, nPoints);
                    n++;
                }
            } else if (orbitTrail) {
                // Non-periodic orbits with trail
                alpha = (float) (trajectory.alpha * base.opacity);
                dAlpha = 0.8f / (float) stIdx;
                float currentAlpha = 0.4f;
                for (int i = 1; i < stIdx; i++) {
                    pointCloudData.loadPoint(prev, i - 1);
                    pointCloudData.loadPoint(curr, i);
                    if (parentPos != null) {
                        prev.sub(parentPos);
                        curr.sub(parentPos);
                    }
                    prev.mul(localTransformD);
                    curr.mul(localTransformD);
                    renderer.addLine(lineView, (float) prev.x, (float) prev.y, (float) prev.z, (float) curr.x, (float) curr.y, (float) curr.z, cc[0], cc[1], cc[2], alpha * currentAlpha * cc[3]);
                    currentAlpha = MathUtils.clamp(currentAlpha + dAlpha, 0f, 1f);
                }
                renderer.addLine(lineView, (float) curr.x, (float) curr.y, (float) curr.z, (float) bodyPos.x, (float) bodyPos.y, (float) bodyPos.z, cc[0], cc[1], cc[2], alpha * currentAlpha * cc[3]);
            } else {
                // Rest, the whole orbit
                for (int i = 1; i < nPoints; i++) {
                    pointCloudData.loadPoint(prev, i - 1);
                    pointCloudData.loadPoint(curr, i);
                    if (parentPos != null) {
                        prev.sub(parentPos);
                        curr.sub(parentPos);
                    }
                    prev.mul(localTransformD);
                    curr.mul(localTransformD);
                    renderer.addLine(lineView, (float) prev.x, (float) prev.y, (float) prev.z, (float) curr.x, (float) curr.y, (float) curr.z, cc[0], cc[1], cc[2], alpha * cc[3]);
                }
            }
        }
    }

    private int wrap(int idx, int n) {
        return (((idx % n) + n) % n);
    }
}
