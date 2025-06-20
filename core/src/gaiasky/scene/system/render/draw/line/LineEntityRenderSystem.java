/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw.line;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.Constel;
import gaiasky.scene.component.StarSet;
import gaiasky.scene.component.Trajectory;
import gaiasky.scene.system.render.draw.LinePrimitiveRenderer;
import gaiasky.scene.view.LineView;
import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.Settings;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.math.Vector3D;
import gaiasky.util.tree.IPosition;
import net.jafama.FastMath;

import java.time.Instant;
import java.util.List;

public class LineEntityRenderSystem {

    /**
     * Auxiliary color array.
     **/
    private final float[] rgba = new float[4];
    protected Vector3D prev = new Vector3D(), curr = new Vector3D();
    /**
     * The line view object, used to send into the
     * {@link LinePrimitiveRenderer}.
     **/
    private final LineView lineView;
    private final Vector3Q B31 = new Vector3Q();
    private final Vector3D D31 = new Vector3D();
    private final Vector3D D32 = new Vector3D();
    private final Vector3D D33 = new Vector3D();
    private final Vector3D D34 = new Vector3D();

    public LineEntityRenderSystem() {
        this.lineView = new LineView();
    }

    public LineEntityRenderSystem(LineView view) {
        this.lineView = view;
    }

    public void renderVRDevice(Entity entity, LineRenderSystem renderer, ICamera camera, float alpha) {
        var vr = Mapper.vr.get(entity);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        var p0c = vr.colorP0;
        var p1c = vr.colorP1;
        var p2c = vr.colorP2;
        renderer.addLine(lineView, vr.beamP0.x, vr.beamP0.y, vr.beamP0.z, vr.beamP1.x, vr.beamP1.y, vr.beamP1.z, p0c[0], p0c[1], p0c[2], p0c[3], p1c[0], p1c[1], p1c[2], p1c[3]);
        renderer.addLine(lineView, vr.beamP1.x, vr.beamP1.y, vr.beamP1.z, vr.beamP2.x, vr.beamP2.y, vr.beamP2.z, p1c[0], p1c[1], p1c[2], p1c[3], p2c[0], p2c[1], p2c[2], p2c[3]);
    }

    public void renderPolyline(Entity entity, LinePrimitiveRenderer renderer, ICamera camera, float alpha) {
        // Render line CPU
        float[] cc = lineView.body.color;
        alpha *= cc[3];
        var verts = lineView.verts;
        if (verts.pointCloudData != null && verts.pointCloudData.getNumPoints() > 1) {
            var graph = Mapper.graph.get(entity);

            Vector3D prev = D31;

            for (int i = 0; i < verts.pointCloudData.getNumPoints(); i++) {
                verts.pointCloudData.loadPoint(prev, i);
                prev.add(graph.translation);
                renderer.addPoint(lineView, (float) prev.x, (float) prev.y, (float) prev.z, cc[0], cc[1], cc[2], alpha);

            }
            renderer.breakLine();

            // Render cap if needed
            var arrow = Mapper.arrow.get(entity);
            if (arrow != null && arrow.arrowCap) {
                // Get two last points of line
                Vector3D p1 = D32.set(verts.pointCloudData.getX(0), verts.pointCloudData.getY(0), verts.pointCloudData.getZ(0));
                Vector3D p2 = D33.set(verts.pointCloudData.getX(1), verts.pointCloudData.getY(1), verts.pointCloudData.getZ(1));
                Vector3D ppm = D34.set(p1).sub(p2);
                double p1p2len = ppm.len();
                p1.sub(camera.getPos());
                p2.sub(camera.getPos());

                // Add arrow head.
                Vector3D p3 = ppm.nor().scl(p1p2len * 0.7).add(p2);
                p3.rotate(p1, 30);
                renderer.addPoint(lineView, p1.x, p1.y, p1.z, cc[0], cc[1], cc[2], alpha);
                renderer.addPoint(lineView, p3.x, p3.y, p3.z, cc[0], cc[1], cc[2], alpha);
                renderer.breakLine();
                p3.rotate(p1, -60);
                renderer.addPoint(lineView, p1.x, p1.y, p1.z, cc[0], cc[1], cc[2], alpha);
                renderer.addPoint(lineView, p3.x, p3.y, p3.z, cc[0], cc[1], cc[2], alpha);
                renderer.breakLine();

            }
        }
    }

    public void renderPerimeter(Entity entity, LinePrimitiveRenderer renderer, ICamera camera, float alpha) {
        var perimeter = Mapper.perimeter.get(entity);
        var cc = lineView.body.color;

        for (float[][] linePoints : perimeter.loc3d) {
            int m = linePoints.length;
            for (int pointIndex = 1; pointIndex < m; pointIndex++) {
                renderer.addLine(lineView, linePoints[pointIndex - 1][0], linePoints[pointIndex - 1][1], linePoints[pointIndex - 1][2], linePoints[pointIndex][0], linePoints[pointIndex][1], linePoints[pointIndex][2], cc[0], cc[1], cc[2], alpha * lineView.base.opacity);
            }
            // Close line
            renderer.addLine(lineView, linePoints[m - 1][0], linePoints[m - 1][1], linePoints[m - 1][2], linePoints[0][0], linePoints[0][1], linePoints[0][2], cc[0], cc[1], cc[2], alpha * lineView.base.opacity);
        }
    }

    public void renderAxes(Entity entity, LinePrimitiveRenderer renderer, ICamera camera, float alpha) {
        if (alpha > 0) {
            var axis = Mapper.axis.get(entity);

            Vector3D o = axis.o;
            Vector3D x = axis.x;
            Vector3D y = axis.y;
            Vector3D z = axis.z;
            float[][] axesColors = axis.axesColors;
            // X
            renderer.addLine(lineView, o.x, o.y, o.z, x.x, x.y, x.z, axesColors[0][0], axesColors[0][1], axesColors[0][2], alpha);
            // Y
            renderer.addLine(lineView, o.x, o.y, o.z, y.x, y.y, y.z, axesColors[1][0], axesColors[1][1], axesColors[1][2], alpha);
            // Z
            renderer.addLine(lineView, o.x, o.y, o.z, z.x, z.y, z.z, axesColors[2][0], axesColors[2][1], axesColors[2][2], alpha);
        }
    }

    /**
     * Renders the ruler line with caps.
     */
    public void renderRuler(Entity entity, LinePrimitiveRenderer renderer, ICamera camera, float alpha) {
        var body = lineView.body;
        var ruler = Mapper.ruler.get(entity);

        double va = 0.01 * camera.getFovFactor();

        // Main line.
        renderer.addLine(lineView, ruler.p0.x, ruler.p0.y, ruler.p0.z, ruler.p1.x, ruler.p1.y, ruler.p1.z, body.color[0], body.color[1], body.color[2], body.color[3] * alpha);
        // Cap 1.
        addCap(body, ruler.p0, ruler.p1, va, renderer, alpha);
        // Cap 2.
        addCap(body, ruler.p1, ruler.p0, va, renderer, alpha);
    }

    private void addCap(Body body, Vector3D p0, Vector3D p1, double va, LinePrimitiveRenderer renderer, float alpha) {
        // cpos-p0
        Vector3D cp = D32.set(p0);
        // cross(cpos-p0, p0-p1)
        Vector3D crs = D31.set(p1).sub(p0).crs(cp);

        double d = p0.len();
        double lengthCap = FastMath.tan(va) * d;
        crs.setLength(lengthCap);
        Vector3D aux0 = D32.set(p0).add(crs);
        Vector3D aux1 = D33.set(p0).sub(crs);
        renderer.addLine(lineView, aux0.x, aux0.y, aux0.z, aux1.x, aux1.y, aux1.z, body.color[0], body.color[1], body.color[2], alpha);
    }

    /**
     * Renders focus projection lines on the recursive grid.
     */
    public void renderGridRec(Entity entity, LinePrimitiveRenderer renderer, ICamera camera, float alpha) {
        var base = lineView.base;
        var gr = Mapper.gridRec.get(entity);

        // A focus object is needed in order to render the projection lines.
        if (camera.hasFocus()) {
            // Line in ZX.
            renderer.addLine(lineView, gr.a.x, gr.a.y, gr.a.z, gr.b.x, gr.b.y, gr.b.z, gr.ccL[0], gr.ccL[1], gr.ccL[2], gr.ccL[3] * alpha * base.opacity);
            // Line in Y.
            renderer.addLine(lineView, gr.c.x, gr.c.y, gr.c.z, gr.d.x, gr.d.y, gr.d.z, gr.ccL[0], gr.ccL[1], gr.ccL[2], gr.ccL[3] * alpha * base.opacity);
        }
    }

    public void renderConstellationBoundaries(Entity entity, LinePrimitiveRenderer renderer, ICamera camera, float alpha) {
        var base = lineView.base;
        var body = lineView.body;
        var bound = Mapper.bound.get(entity);

        alpha *= 0.3f;
        lineView.setEntity(entity);

        // This is so that the shape renderer does not mess up the z-buffer.
        for (List<Vector3D> points : bound.boundaries) {
            Vector3D previous = null;
            for (Vector3D point : points) {
                if (previous != null) {
                    renderer.addLine(lineView, (float) previous.x, (float) previous.y, (float) previous.z, (float) point.x, (float) point.y, (float) point.z, body.color[0], body.color[1], body.color[2], alpha * base.opacity);
                }
                previous = point;
            }
            // Join last to first.
            Vector3D first = points.getFirst();
            renderer.addLine(lineView, (float) first.x, (float) first.y, (float) first.z, (float) previous.x, (float) previous.y, (float) previous.z, body.color[0], body.color[1], body.color[2], alpha * base.opacity);
        }
    }

    public void renderConstellation(Entity entity, LinePrimitiveRenderer renderer, ICamera camera, float alpha) {
        var base = lineView.base;
        var body = lineView.body;
        var constel = Mapper.constel.get(entity);

        alpha *= constel.alpha * base.opacity;

        Vector3D p1 = D31;
        Vector3D p2 = D32;
        Vector3Q campos = camera.getPos();

        lineView.setEntity(entity);

        for (IPosition[] pair : constel.lines) {
            if (pair != null) {
                getPosition(pair[0], campos, p1, constel);
                getPosition(pair[1], campos, p2, constel);

                renderer.addLine(lineView, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, body.color[0], body.color[1], body.color[2], alpha);
            }
        }
    }

    private void getPosition(IPosition posBean, Vector3Q camPos, Vector3D out, Constel constel) {
        Vector3D vel = D33.setZero();
        if (posBean.getVelocity() != null && !posBean.getVelocity().hasNaN()) {
            vel.set(posBean.getVelocity()).scl(constel.deltaYears);
        }
        out.set(posBean.getPosition()).sub(camPos).add(vel);
    }

    private float getCurrentAlpha(float topAlpha, Trajectory trajectory) {
        float alpha = topAlpha;
        if (trajectory.trailMap > 0) {
            alpha = (1f / (1f - trajectory.trailMap)) * (alpha - trajectory.trailMap);
        }
        return MathUtils.clamp(alpha, 0f, 1f);
    }

    public void renderTrajectory(Entity entity, LinePrimitiveRenderer renderer, ICamera camera, float alpha) {
        var trajectory = Mapper.trajectory.get(entity);
        if (trajectory.bodyRepresentation.isOrbit()) {
            var base = lineView.base;
            var body = lineView.body;
            var verts = lineView.verts;
            var graph = Mapper.graph.get(entity);

            var localTransformD = trajectory.localTransformD;
            var oc = trajectory.oc;
            var orbitTrail = trajectory.orbitTrail;
            var cc = body.color;
            var pointCloudData = verts.pointCloudData;

            float baseOpacity = (float) (alpha * trajectory.alpha * base.opacity);

            Vector3D parentPos;
            parentPos = Mapper.orientation.has(graph.parent) ? Mapper.orientation.get(graph.parent).getNonRotatedPos() : null;
            int last = parentPos != null ? 2 : 1;

            float topAlpha = 1;
            float dAlpha = 0f;
            int stIdx = 0;
            int nPoints = verts.pointCloudData.getNumPoints();

            boolean hasTime = verts.pointCloudData.hasTime();

            Vector3D bodyPos = D31.setZero();
            if (orbitTrail && hasTime) {
                // For large periods, fade orbit to 0 a bit before.
                float bottomAlpha = trajectory.params != null && (trajectory.params.orbitalPeriod > 40000 || base.ct.isEnabled(ComponentType.Moons)) ? -0.2f : -0.1f;
                dAlpha = (topAlpha - bottomAlpha) / nPoints;
                Instant currentTime = GaiaSky.instance.time.getTime();
                long wrapTime = verts.pointCloudData.getWrapTimeMs(currentTime);
                stIdx = verts.pointCloudData.getIndex(wrapTime);

                if (trajectory.body != null) {
                    bodyPos.set(Mapper.graph.get(trajectory.body).translation);
                } else if (oc != null) {
                    oc.loadDataPoint(bodyPos, currentTime);
                    bodyPos.mul(localTransformD);
                }

                topAlpha = bottomAlpha;
                dAlpha = -dAlpha;
            }

            lineView.setEntity(entity);

            // This is so that the shape renderer does not mess up the z-buffer
            int n = 0;
            if (!trajectory.orbitTrail) {
                // No orbit trail, just draw the whole orbit.
                for (int i = 1; i < nPoints; i++) {
                    pointCloudData.loadPoint(prev, i - 1);
                    pointCloudData.loadPoint(curr, i);
                    if (parentPos != null) {
                        prev.sub(parentPos);
                        curr.sub(parentPos);
                    }
                    prev.mul(localTransformD);
                    curr.mul(localTransformD);
                    renderer.addLine(lineView, (float) prev.x, (float) prev.y, (float) prev.z, (float) curr.x, (float) curr.y, (float) curr.z, cc[0], cc[1], cc[2], alpha * cc[3] * baseOpacity);
                }

            } else if (oc != null && oc.period > 0) {
                // Periodic orbits.
                int i = wrap(stIdx + 2, nPoints);
                float p0Alpha;
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

                    p0Alpha = getCurrentAlpha(topAlpha, trajectory);
                    float p1Alpha = getCurrentAlpha(topAlpha - dAlpha, trajectory);

                    // Only render visible chunks.
                    if (p0Alpha > 0.0 || p1Alpha > 0.0) {
                        if (orbitTrail && n == nPoints - 2) {
                            renderer.addLine(lineView, (float) curr.x, (float) curr.y, (float) curr.z, (float) bodyPos.x, (float) bodyPos.y, (float) bodyPos.z, cc[0], cc[1], cc[2], p1Alpha * cc[3] * baseOpacity, cc[0], cc[1], cc[2], p0Alpha * cc[3] * baseOpacity);
                        } else {
                            renderer.addLine(lineView, (float) prev.x, (float) prev.y, (float) prev.z, (float) curr.x, (float) curr.y, (float) curr.z, cc[0], cc[1], cc[2], p0Alpha * cc[3] * baseOpacity, cc[0], cc[1], cc[2], p1Alpha * cc[3] * baseOpacity);
                        }
                    }

                    topAlpha -= dAlpha;

                    // advance
                    i = wrap(i + 1, nPoints);
                    n++;
                }

            } else if (hasTime) {
                // Non-periodic orbits with trail and times.
                dAlpha = -1 / (float) stIdx * (1 - trajectory.trailMap);
                float p0Alpha;
                for (int i = 1; i < stIdx; i++) {
                    pointCloudData.loadPoint(prev, i - 1);
                    pointCloudData.loadPoint(curr, i);
                    if (parentPos != null) {
                        prev.sub(parentPos);
                        curr.sub(parentPos);
                    }
                    prev.mul(localTransformD);
                    curr.mul(localTransformD);

                    p0Alpha = getCurrentAlpha(topAlpha, trajectory);
                    float p1Alpha = getCurrentAlpha(topAlpha - dAlpha, trajectory);

                    if (p0Alpha > 0.0 || p1Alpha > 0.0) {
                        renderer.addLine(lineView, (float) prev.x, (float) prev.y, (float) prev.z, (float) curr.x, (float) curr.y, (float) curr.z, cc[0], cc[1], cc[2], baseOpacity * p0Alpha * cc[3], cc[0], cc[1], cc[2], baseOpacity * p1Alpha * cc[3]);
                    }
                    topAlpha -= dAlpha;
                }
                renderer.addLine(lineView, (float) curr.x, (float) curr.y, (float) curr.z, (float) bodyPos.x, (float) bodyPos.y, (float) bodyPos.z, cc[0], cc[1], cc[2], baseOpacity * topAlpha * cc[3]);

            } else {
                // Non-periodic orbits with trail and no time.
                dAlpha = 1 / ((nPoints - 1) * (1 - trajectory.trailMap));
                float p0Alpha;
                for (int i = nPoints - 1; i > 0; i--) {
                    pointCloudData.loadPoint(prev, i - 1);
                    pointCloudData.loadPoint(curr, i);
                    if (parentPos != null) {
                        prev.sub(parentPos);
                        curr.sub(parentPos);
                    }
                    prev.mul(localTransformD);
                    curr.mul(localTransformD);

                    p0Alpha = MathUtils.clamp(topAlpha, 0f, 1f);
                    float p1Alpha = MathUtils.clamp(topAlpha - dAlpha, 0f, 1f);

                    if (p0Alpha > 0.001) {
                        renderer.addLine(lineView, (float) prev.x, (float) prev.y, (float) prev.z, (float) curr.x, (float) curr.y, (float) curr.z, cc[0], cc[1], cc[2], alpha * cc[3] * p0Alpha * baseOpacity, cc[0], cc[1], cc[2], alpha * cc[3] * p1Alpha * baseOpacity);
                    }
                    topAlpha -= dAlpha;
                }
            }
        }
    }

    /**
     * Renders the velocity vectors of a star set.
     */
    public void renderStarSet(Entity entity, LinePrimitiveRenderer renderer, ICamera camera, float alpha) {
        var base = lineView.base;
        var set = Mapper.starSet.get(entity);

        lineView.setEntity(entity);

        alpha *= GaiaSky.instance.sceneRenderer.alphas[ComponentTypes.ComponentType.VelocityVectors.ordinal()];
        float thPointTimesFovFactor = (float) Settings.settings.scene.star.threshold.point * camera.getFovFactor();
        int n = (int) (getMaxVelocityVectors(entity, set) * (Settings.settings.scene.properMotion.number * 0.1));
        for (int i = n - 1; i >= 0; i--) {
            if (set.indices[i] < 0) {
                // We are done.
                break;
            }
            IParticleRecord star = set.pointData.get(set.indices[i]);
            float radius = (float) (set.getSize(set.indices[i]) * Constants.STAR_SIZE_FACTOR);
            // Position
            Vector3Q lPos = set.fetchPosition(star, set.cPosD, B31, set.currDeltaYears);
            // Proper motion
            Vector3D pm = D32.set(star.vx(), star.vy(), star.vz()).scl(set.currDeltaYears);
            // Rest of attributes
            float distToCamera = (float) lPos.lenDouble();
            float viewAngle = ((radius / distToCamera) / camera.getFovFactor()) * Settings.settings.scene.star.brightness;
            if (viewAngle >= thPointTimesFovFactor && (star.vx() != 0 || star.vy() != 0 || star.vz() != 0)) {
                Vector3D p1 = D31.set(star.x() + pm.x, star.y() + pm.y, star.z() + pm.z).sub(camera.getPos());
                Vector3D ppm = D32.set(star.vx(), star.vy(), star.vz()).scl(Settings.settings.scene.properMotion.length);
                double p1p2len = ppm.len();
                Vector3D p2 = D33.set(ppm).add(p1);

                // Maximum speed in km/s, to normalize
                float maxSpeedKms = 100;
                float r, g, b;
                switch (Settings.settings.scene.properMotion.colorMode) {
                    case 1 -> {
                        // LENGTH
                        ppm.set(star.vx(), star.vy(), star.vz());
                        // Units/year to Km/s
                        ppm.scl(Constants.U_TO_KM / Nature.Y_TO_S);
                        double len = MathUtilsDouble.clamp(ppm.len(), 0d, maxSpeedKms) / maxSpeedKms;
                        ColorUtils.colormap_long_rainbow((float) (1 - len), rgba);
                        r = rgba[0];
                        g = rgba[1];
                        b = rgba[2];
                    }
                    case 2 -> {
                        // HAS RADIAL VELOCITY - blue: stars with RV, red: stars without RV
                        float radialVelocity = star.radVel();
                        if (Float.isFinite(radialVelocity) && radialVelocity != 0) {
                            r = ColorUtils.gBlue[0] + 0.2f;
                            g = ColorUtils.gBlue[1] + 0.4f;
                            b = ColorUtils.gBlue[2] + 0.4f;
                        } else {
                            r = ColorUtils.gRed[0] + 0.4f;
                            g = ColorUtils.gRed[1] + 0.2f;
                            b = ColorUtils.gRed[2] + 0.2f;
                        }
                    }
                    case 3 -> {
                        // REDSHIFT from Sun - blue: -100 Km/s, red: 100 Km/s
                        float radialVelocity = star.radVel();
                        if (Float.isFinite(radialVelocity) && radialVelocity != 0) {
                            // rv in [0:1]
                            float rv = ((MathUtilsDouble.clamp(radialVelocity, -maxSpeedKms, maxSpeedKms) / maxSpeedKms) + 1) / 2;
                            ColorUtils.colormap_blue_white_red(rv, rgba);
                            r = rgba[0];
                            g = rgba[1];
                            b = rgba[2];
                        } else {
                            r = g = b = 1;
                        }
                    }
                    case 4 -> {
                        // REDSHIFT from Camera - blue: -100 Km/s, red: 100 Km/s
                        if (ppm.len2() != 0) {
                            ppm.set(star.vx(), star.vy(), star.vz());
                            // Units/year to Km/s
                            ppm.scl(Constants.U_TO_KM / Nature.Y_TO_S);
                            Vector3D camStar = D34.set(p1);
                            double pr = ppm.dot(camStar.nor());
                            double projection = ((MathUtilsDouble.clamp(pr, -(double) maxSpeedKms, maxSpeedKms) / (double) maxSpeedKms) + 1) / 2;
                            ColorUtils.colormap_blue_white_red((float) projection, rgba);
                            r = rgba[0];
                            g = rgba[1];
                            b = rgba[2];
                        } else {
                            r = g = b = 1;
                        }
                    }
                    case 5 -> {
                        // SINGLE COLOR
                        r = ColorUtils.gBlue[0] + 0.2f;
                        g = ColorUtils.gBlue[1] + 0.4f;
                        b = ColorUtils.gBlue[2] + 0.4f;
                    }
                    default -> {
                        // DIRECTION
                        // Normalize, each component is in [-1:1], map to [0:1] and to a color channel
                        ppm.nor();
                        r = (float) (ppm.x + 1d) / 2f;
                        g = (float) (ppm.y + 1d) / 2f;
                        b = (float) (ppm.z + 1d) / 2f;
                    }
                }

                // Clamp
                r = MathUtilsDouble.clamp(r, 0, 1);
                g = MathUtilsDouble.clamp(g, 0, 1);
                b = MathUtilsDouble.clamp(b, 0, 1);

                renderer.addLine(lineView, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, r, g, b, alpha * base.opacity);
                if (Settings.settings.scene.properMotion.arrowHeads) {
                    // Add the arrow cap.
                    Vector3D p3 = D32.set(ppm).nor().scl(p1p2len * .86).add(p1);
                    p3.rotate(p2, 30);
                    renderer.addLine(lineView, p3.x, p3.y, p3.z, p2.x, p2.y, p2.z, r, g, b, alpha * base.opacity);
                    p3.rotate(p2, -60);
                    renderer.addLine(lineView, p3.x, p3.y, p3.z, p2.x, p2.y, p2.z, r, g, b, alpha * base.opacity);
                }

            }
        }
    }

    private long getMaxVelocityVectors(Entity entity, StarSet set) {
        // Star sets in octrees get their number tuned down, because usually there's many of them in view.
        return FastMath.min(set.indices.length,
                            FastMath.max(Math.round(Settings.settings.scene.star.group.numVelocityVector / (Mapper.octant.has(entity) ? 5f : 1f)),
                                         Constants.MIN_VELOCITY_VECTORS_STAR_GROUP));
    }

    private int wrap(int idx, int n) {
        return (((idx % n) + n) % n);
    }
}
