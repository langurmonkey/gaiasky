package gaiasky.scene.system.render.draw.line;

import com.badlogic.gdx.math.MathUtils;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.system.render.draw.LinePrimitiveRenderer;
import gaiasky.scene.view.LineView;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.Settings;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.tree.IPosition;

import java.time.Instant;

/**
 * Implements line rendering for the different families of entities.
 */
public class LineEntityRenderSystem {

    /**
     * The line view object, used to send into the
     * {@link LinePrimitiveRenderer}.
     **/
    private LineView lineView;

    /** Auxiliary color array. **/
    private final float[] rgba = new float[4];

    private Vector3d D31 = new Vector3d();
    private Vector3d D32 = new Vector3d();
    private Vector3d D33 = new Vector3d();
    private Vector3d D34 = new Vector3d();
    protected Vector3d prev = new Vector3d(), curr = new Vector3d();

    public LineEntityRenderSystem() {
        this.lineView = new LineView();
    }

    public void render(final Render render, LinePrimitiveRenderer renderer, ICamera camera, float alpha) {
        final var entity = render.entity;
        var base = Mapper.base.get(entity);
        var trajectory = Mapper.trajectory.get(entity);
        var set = Mapper.starSet.get(render.entity);
        var constel = Mapper.constel.get(render.entity);

        if (trajectory != null) {
            // Orbits.
            var verts = Mapper.verts.get(entity);
            var graph = Mapper.graph.get(entity);
            var body = Mapper.body.get(entity);
            renderTrajectory(render, base, body, graph, trajectory, verts, renderer, alpha);
        } else if (set != null) {
            // Star sets.
            renderStarSet(render, base, set, renderer, camera, alpha);
        } else if (constel != null) {
            // Constellations
            var body = Mapper.body.get(entity);
            renderConstellation(render, base, body, constel, renderer, camera, alpha);
        }
    }

    private void renderConstellation(Render render, Base base, Body body, Constel constel, LinePrimitiveRenderer renderer, ICamera camera, float alpha) {
        alpha *= constel.alpha * base.opacity;

        Vector3d p1 = D31;
        Vector3d p2 = D32;
        Vector3b campos = camera.getPos();

        lineView.setEntity(render.entity);

        for (IPosition[] pair : constel.lines) {
            if (pair != null) {
                getPosition(pair[0], campos, p1, constel);
                getPosition(pair[1], campos, p2, constel);

                renderer.addLine(lineView, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, body.color[0], body.color[1], body.color[2], alpha);
            }
        }
    }

    private void getPosition(IPosition posBean, Vector3b camPos, Vector3d out, Constel constel) {
        Vector3d vel = D33.setZero();
        if (posBean.getVelocity() != null && !posBean.getVelocity().hasNaN()) {
            vel.set(posBean.getVelocity()).scl(constel.deltaYears);
        }
        out.set(posBean.getPosition()).sub(camPos).add(vel);
    }

    private void renderTrajectory(Render render, Base base, Body body, GraphNode graph, Trajectory trajectory, Verts verts, LinePrimitiveRenderer renderer, float alpha) {
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
                // For large periods, fade orbit to 0 a bit before.
                float bottom = trajectory.params != null && (trajectory.params.orbitalPeriod > 40000 || base.ct.isEnabled(ComponentType.Moons)) ? -0.2f : -0.1f;
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

            lineView.setEntity(render.entity);

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

    /**
     * Renders the proper motions of a star set.
     */
    public void renderStarSet(Render render, Base base, StarSet set, LinePrimitiveRenderer renderer, ICamera camera, float alpha) {
        lineView.setEntity(render.entity);

        alpha *= GaiaSky.instance.sgr.alphas[ComponentTypes.ComponentType.VelocityVectors.ordinal()];
        float thPointTimesFovFactor = (float) Settings.settings.scene.star.threshold.point * camera.getFovFactor();
        int n = (int) getMaxProperMotionLines(set);
        for (int i = n - 1; i >= 0; i--) {
            IParticleRecord star = set.pointData.get(set.active[i]);
            float radius = (float) (set.getSize(set.active[i]) * Constants.STAR_SIZE_FACTOR);
            // Position
            Vector3d lPos = set.fetchPosition(star, set.cPosD, D31, set.currDeltaYears);
            // Proper motion
            Vector3d pm = D32.set(star.pmx(), star.pmy(), star.pmz()).scl(set.currDeltaYears);
            // Rest of attributes
            float distToCamera = (float) lPos.len();
            float viewAngle = ((radius / distToCamera) / camera.getFovFactor()) * Settings.settings.scene.star.brightness;
            if (viewAngle >= thPointTimesFovFactor / Settings.settings.scene.properMotion.number && (star.pmx() != 0 || star.pmy() != 0 || star.pmz() != 0)) {
                Vector3d p1 = D31.set(star.x() + pm.x, star.y() + pm.y, star.z() + pm.z).sub(camera.getPos());
                Vector3d ppm = D32.set(star.pmx(), star.pmy(), star.pmz()).scl(Settings.settings.scene.properMotion.length);
                double p1p2len = ppm.len();
                Vector3d p2 = D33.set(ppm).add(p1);

                // Maximum speed in km/s, to normalize
                float maxSpeedKms = 100;
                float r, g, b;
                switch (Settings.settings.scene.properMotion.colorMode) {
                case 0:
                default:
                    // DIRECTION
                    // Normalize, each component is in [-1:1], map to [0:1] and to a color channel
                    ppm.nor();
                    r = (float) (ppm.x + 1d) / 2f;
                    g = (float) (ppm.y + 1d) / 2f;
                    b = (float) (ppm.z + 1d) / 2f;
                    break;
                case 1:
                    // LENGTH
                    ppm.set(star.pmx(), star.pmy(), star.pmz());
                    // Units/year to Km/s
                    ppm.scl(Constants.U_TO_KM / Nature.Y_TO_S);
                    double len = MathUtilsd.clamp(ppm.len(), 0d, maxSpeedKms) / maxSpeedKms;
                    ColorUtils.colormap_long_rainbow((float) (1 - len), rgba);
                    r = rgba[0];
                    g = rgba[1];
                    b = rgba[2];
                    break;
                case 2:
                    // HAS RADIAL VELOCITY - blue: stars with RV, red: stars without RV
                    if (star.radvel() != 0) {
                        r = ColorUtils.gBlue[0] + 0.2f;
                        g = ColorUtils.gBlue[1] + 0.4f;
                        b = ColorUtils.gBlue[2] + 0.4f;
                    } else {
                        r = ColorUtils.gRed[0] + 0.4f;
                        g = ColorUtils.gRed[1] + 0.2f;
                        b = ColorUtils.gRed[2] + 0.2f;
                    }
                    break;
                case 3:
                    // REDSHIFT from Sun - blue: -100 Km/s, red: 100 Km/s
                    float rav = star.radvel();
                    if (rav != 0) {
                        // rv in [0:1]
                        float rv = ((MathUtilsd.clamp(rav, -maxSpeedKms, maxSpeedKms) / maxSpeedKms) + 1) / 2;
                        ColorUtils.colormap_blue_white_red(rv, rgba);
                        r = rgba[0];
                        g = rgba[1];
                        b = rgba[2];
                    } else {
                        r = g = b = 1;
                    }
                    break;
                case 4:
                    // REDSHIFT from Camera - blue: -100 Km/s, red: 100 Km/s
                    if (ppm.len2() != 0) {
                        ppm.set(star.pmx(), star.pmy(), star.pmz());
                        // Units/year to Km/s
                        ppm.scl(Constants.U_TO_KM / Nature.Y_TO_S);
                        Vector3d camStar = D34.set(p1);
                        double pr = ppm.dot(camStar.nor());
                        double projection = ((MathUtilsd.clamp(pr, -(double) maxSpeedKms, maxSpeedKms) / (double) maxSpeedKms) + 1) / 2;
                        ColorUtils.colormap_blue_white_red((float) projection, rgba);
                        r = rgba[0];
                        g = rgba[1];
                        b = rgba[2];
                    } else {
                        r = g = b = 1;
                    }
                    break;
                case 5:
                    // SINGLE COLOR
                    r = ColorUtils.gBlue[0] + 0.2f;
                    g = ColorUtils.gBlue[1] + 0.4f;
                    b = ColorUtils.gBlue[2] + 0.4f;
                    break;
                }

                // Clamp
                r = MathUtilsd.clamp(r, 0, 1);
                g = MathUtilsd.clamp(g, 0, 1);
                b = MathUtilsd.clamp(b, 0, 1);

                renderer.addLine(lineView, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, r, g, b, alpha * base.opacity);
                if (Settings.settings.scene.properMotion.arrowHeads) {
                    // Add Arrow cap.
                    Vector3d p3 = D32.set(ppm).nor().scl(p1p2len * .86).add(p1);
                    p3.rotate(p2, 30);
                    renderer.addLine(lineView, p3.x, p3.y, p3.z, p2.x, p2.y, p2.z, r, g, b, alpha * base.opacity);
                    p3.rotate(p2, -60);
                    renderer.addLine(lineView, p3.x, p3.y, p3.z, p2.x, p2.y, p2.z, r, g, b, alpha * base.opacity);
                }

            }
        }
    }

    private long getMaxProperMotionLines(StarSet set) {
        return Math.min(set.pointData.size(), Settings.settings.scene.star.group.numVelocityVector);
    }

    private int wrap(int idx, int n) {
        return (((idx % n) + n) % n);
    }
}
