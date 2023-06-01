/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.scene.view.FilterView;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.Functions.Function2;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.math.IntersectorDouble;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.util.List;

public class FocusHit {

    private final Vector3 F31 = new Vector3();
    private final Vector3d D31 = new Vector3d();
    private final Vector3d D32 = new Vector3d();
    private final Vector3d D33 = new Vector3d();
    private final Vector3b B31 = new Vector3b();
    private final FilterView filter;

    public FocusHit() {
        filter = new FilterView();
    }

    boolean hitConditionOverflow(FocusView view) {
        return hitCondition(view) && !view.isCoordinatesTimeOverflow();
    }

    boolean hitCondition(FocusView view) {
        return GaiaSky.instance.isOn(view.base.ct) && view.getOpacity() > 0;
    }

    protected boolean checkClickDistance(int screenX,
                                         int screenY,
                                         Vector3 pos,
                                         NaturalCamera camera,
                                         PerspectiveCamera pcamera,
                                         double pixelSize) {
        return pos.dst(screenX % pcamera.viewportWidth, screenY, pos.z) <= pixelSize;
    }

    private double computeHitSolidAngleCelestial(FocusView view,
                                                 float fovFactor) {
        return view.getSolidAngle();
    }

    public void addHitCoordinateCelestial(FocusView view,
                                          int screenX,
                                          int screenY,
                                          int w,
                                          int h,
                                          int pixelDist,
                                          NaturalCamera camera,
                                          Array<Entity> hits) {
        addHitCoordinateCelestial(view, screenX, screenY, w, h, pixelDist, 1, this::computeHitSolidAngleCelestial, camera, hits);
    }

    private void addHitCoordinateCelestial(FocusView view,
                                           int screenX,
                                           int screenY,
                                           int w,
                                           int h,
                                           int pixelDist,
                                           float solidAngleFactor,
                                           Function2<FocusView, Float, Double> solidAngleFunction,
                                           NaturalCamera camera,
                                           Array<Entity> hits) {
        if (hitConditionOverflow(view)) {
            var entity = view.getEntity();

            Vector3 pos = F31;
            Vector3b posDouble = EntityUtils.getAbsolutePosition(entity, B31).add(camera.getInversePos());
            pos.set(posDouble.valuesf());

            if (camera.direction.dot(posDouble) > 0) {
                // The object is in front of us, roughly.
                double angle = solidAngleFunction.apply(view, camera.fovFactor) * solidAngleFactor;

                PerspectiveCamera perspectiveCamera;
                if (Settings.settings.program.modeStereo.active) {
                    if (screenX < Gdx.graphics.getWidth() / 2f) {
                        perspectiveCamera = camera.getCameraStereoLeft();
                    } else {
                        perspectiveCamera = camera.getCameraStereoRight();
                    }
                    perspectiveCamera.update();
                } else {
                    perspectiveCamera = camera.camera;
                }

                angle = (float) Math.toDegrees(angle * camera.getFovFactor()) * (40f / perspectiveCamera.fieldOfView);
                double pixelSize = Math.max(pixelDist, ((angle * perspectiveCamera.viewportHeight) / perspectiveCamera.fieldOfView) / 2);
                perspectiveCamera.project(pos);
                pos.y = perspectiveCamera.viewportHeight - pos.y;
                if (Settings.settings.program.modeStereo.active) {
                    pos.x /= 2;
                }
                // Check click distance
                if (checkClickDistance(screenX, screenY, pos, camera, perspectiveCamera, pixelSize)) {
                    //Hit
                    hits.add(entity);
                }
            }
        }
    }

    public void addHitRayCelestial(FocusView view,
                                   Vector3d p0,
                                   Vector3d p1,
                                   NaturalCamera camera,
                                   Array<Entity> hits) {
        if (hitConditionOverflow(view)) {
            var entity = view.getEntity();

            Vector3b posb = EntityUtils.getAbsolutePosition(entity, B31).add(camera.getInversePos());

            if (camera.direction.dot(posb) > 0) {
                // The star is in front of us
                // Diminish the size of the star
                // when we are close by
                double dist = posb.lenDouble();
                double distToLine = IntersectorDouble.distanceLinePoint(p0, p1, posb.put(D31));
                double value = distToLine / dist;

                if (value < 0.01) {
                    hits.add(entity);
                }
            }
        }
    }

    /**
     * If we render the model, we set up a sphere at the object's position with
     * its radius and check for intersections with the ray
     */
    public void addHitCoordinateModel(FocusView view,
                                      int screenX,
                                      int screenY,
                                      int w,
                                      int h,
                                      int pixelDist,
                                      NaturalCamera camera,
                                      Array<Entity> hits) {
        if (hitConditionOverflow(view)) {
            var entity = view.getEntity();
            var sa = Mapper.sa.get(entity);

            if (view.getSolidAngleApparent() < sa.thresholdQuad * camera.getFovFactor()) {
                addHitCoordinateCelestial(view, screenX, screenY, w, h, pixelDist, camera, hits);
            } else {
                var graph = Mapper.graph.get(entity);

                Vector3 auxf = F31;
                Vector3d aux1d = D31;
                Vector3d aux2d = D32;
                Vector3d aux3d = D33;

                // aux1d contains the position of the body in the camera ref sys
                aux1d.set(graph.translation);
                auxf.set(aux1d.valuesf());

                if (camera.direction.dot(aux1d) > 0) {
                    // The object is in front of us
                    auxf.set(screenX, screenY, 2f);
                    camera.camera.unproject(auxf).nor();

                    // aux2d contains the position of the click in the camera ref sys
                    aux2d.set(auxf.x, auxf.y, auxf.z);

                    // aux3d contains the camera position, [0,0,0]
                    aux3d.set(0, 0, 0);

                    boolean intersect = IntersectorDouble.checkIntersectRaySpehre(aux3d, aux2d, aux1d, view.getRadius());
                    if (intersect) {
                        //Hit
                        hits.add(entity);
                    }
                }
            }
        }
    }

    public void addHitRayModel(FocusView view,
                               Vector3d p0,
                               Vector3d p1,
                               NaturalCamera camera,
                               Array<Entity> hits) {
        if (hitConditionOverflow(view)) {
            var entity = view.getEntity();
            var sa = Mapper.sa.get(entity);

            if (view.getSolidAngleApparent() < sa.thresholdQuad * camera.getFovFactor()) {
                addHitRayCelestial(view, p0, p1, camera, hits);
            } else {
                var graph = Mapper.graph.get(entity);

                Vector3d aux1d = D31;

                // aux1d contains the position of the body in the camera ref sys
                aux1d.set(graph.translation);

                boolean intersect = IntersectorDouble.checkIntersectRaySpehre(p0, p1, aux1d, view.getRadius());
                if (intersect) {
                    //Hit
                    hits.add(entity);
                }
            }
        }
    }

    private double computeHitSolidAngleStar(FocusView view,
                                            float fovFactor) {
        double solidAngle = view.getSolidAngle();
        if (solidAngle > Constants.STAR_SOLID_ANGLE_THRESHOLD_BOTTOM / fovFactor && solidAngle < Constants.STAR_SOLID_ANGLE_THRESHOLD_TOP / fovFactor) {
            return 20f * Constants.STAR_SOLID_ANGLE_THRESHOLD_BOTTOM / fovFactor;
        }
        return solidAngle;
    }

    public void addHitCoordinateStar(FocusView view,
                                     int screenX,
                                     int screenY,
                                     int w,
                                     int h,
                                     int pixelDist,
                                     NaturalCamera camera,
                                     Array<Entity> hits) {
        addHitCoordinateCelestial(view, screenX, screenY, w, h, pixelDist, Settings.settings.scene.star.brightness * 1e3f, this::computeHitSolidAngleStar, camera, hits);
    }

    public void addHitCoordinateParticleSet(FocusView view,
                                            int screenX,
                                            int screenY,
                                            int w,
                                            int h,
                                            int pixelDist,
                                            NaturalCamera camera,
                                            Array<Entity> hits) {
        var set = view.getSet();
        List<IParticleRecord> pointData = set.pointData;
        int n = pointData.size();
        if (hitCondition(view)) {
            var entity = view.getEntity();

            filter.setEntity(entity);
            Array<Pair<Integer, Double>> temporalHits = new Array<>();
            for (int i = 0; i < n; i++) {
                if (filter.filter(i)) {
                    IParticleRecord pb = pointData.get(i);
                    Vector3 posFloat = F31;
                    Vector3d pos = set.fetchPositionDouble(pb, camera.getPos(), D31, set.getDeltaYears());
                    posFloat.set(pos.valuesf());

                    if (camera.direction.dot(pos) > 0) {
                        // The particle is in front of us
                        // Diminish the size of the star
                        // when we are close by
                        double dist = pos.len();
                        double angle = set.getRadius(i) / dist / camera.getFovFactor();

                        PerspectiveCamera perspectiveCamera;
                        if (Settings.settings.program.modeStereo.active) {
                            if (screenX < Gdx.graphics.getWidth() / 2f) {
                                perspectiveCamera = camera.getCameraStereoLeft();
                            } else {
                                perspectiveCamera = camera.getCameraStereoRight();
                            }
                            perspectiveCamera.update();
                        } else {
                            perspectiveCamera = camera.camera;
                        }

                        angle = (float) Math.toDegrees(angle * camera.fovFactor) * (40f / perspectiveCamera.fieldOfView);
                        double pixelSize = Math.max(pixelDist, ((angle * perspectiveCamera.viewportHeight) / perspectiveCamera.fieldOfView) / 2);
                        perspectiveCamera.project(posFloat);
                        posFloat.y = perspectiveCamera.viewportHeight - posFloat.y;
                        if (Settings.settings.program.modeStereo.active) {
                            posFloat.x /= 2;
                        }

                        // Check click distance
                        if (posFloat.dst(screenX % perspectiveCamera.viewportWidth, screenY, posFloat.z) <= pixelSize) {
                            //Hit
                            temporalHits.add(new Pair<>(i, angle));
                        }
                    }
                }
            }

            Pair<Integer, Double> best = null;
            for (Pair<Integer, Double> hit : temporalHits) {
                if (best == null)
                    best = hit;
                else if (hit.getSecond() > best.getSecond()) {
                    best = hit;
                }
            }
            if (best != null) {
                // We found the best hit
                set.candidateFocusIndex = best.getFirst();
                set.updateFocusDataPos();
                hits.add(entity);
                return;
            }

        }
        set.candidateFocusIndex = -1;
        set.updateFocusDataPos();
    }

    public void addHitRayParticleSet(FocusView view,
                                     Vector3d p0,
                                     Vector3d p1,
                                     NaturalCamera camera,
                                     Array<Entity> hits) {
        var set = view.getSet();
        List<IParticleRecord> pointData = set.pointData;
        int n = pointData.size();
        if (hitCondition(view)) {
            var entity = view.getEntity();

            Vector3d beamDir = new Vector3d();
            filter.setEntity(entity);
            Array<Pair<Integer, Double>> temporalHits = new Array<>();
            for (int i = 0; i < n; i++) {
                if (filter.filter(i)) {
                    IParticleRecord pb = pointData.get(i);
                    Vector3d posd = set.fetchPositionDouble(pb, set.cPosD, D31, set.getDeltaYears());
                    beamDir.set(p1).sub(p0);
                    if (camera.direction.dot(posd) > 0) {
                        // The star is in front of us
                        // Diminish the size of the star
                        // when we are close by
                        double dist = posd.len();
                        double angle = set.getRadius(i) / dist / camera.getFovFactor();
                        double distToLine = IntersectorDouble.distanceLinePoint(p0, p1, posd.put(D31));
                        double value = distToLine / dist;

                        if (value < 0.01) {
                            temporalHits.add(new Pair<>(i, angle));
                        }
                    }
                }
            }

            Pair<Integer, Double> best = null;
            for (Pair<Integer, Double> hit : temporalHits) {
                if (best == null)
                    best = hit;
                else if (hit.getSecond() > best.getSecond()) {
                    best = hit;
                }
            }
            if (best != null) {
                // We found the best hit
                set.candidateFocusIndex = best.getFirst();
                set.updateFocusDataPos();
                hits.add(entity);
                return;
            }

        }
        set.candidateFocusIndex = -1;
        set.updateFocusDataPos();
    }

    public void addHitCoordinateCluster(FocusView view,
                                        int screenX,
                                        int screenY,
                                        int w,
                                        int h,
                                        int pixelDist,
                                        NaturalCamera camera,
                                        Array<Entity> hits) {
        if (hitCondition(view)) {
            var entity = view.getEntity();

            Vector3 pos = F31;
            Vector3b aux = B31;
            Vector3b posb = EntityUtils.getAbsolutePosition(entity, aux).add(camera.posinv);
            pos.set(posb.valuesf());

            if (camera.direction.dot(posb) > 0) {
                // The star is in front of us
                // Diminish the size of the star
                // when we are close by
                double angle = view.getSolidAngle();

                PerspectiveCamera pcamera;
                if (Settings.settings.program.modeStereo.active) {
                    if (screenX < Gdx.graphics.getWidth() / 2f) {
                        pcamera = camera.getCameraStereoLeft();
                    } else {
                        pcamera = camera.getCameraStereoRight();
                    }
                    pcamera.update();
                } else {
                    pcamera = camera.camera;
                }

                angle = (float) Math.toDegrees(angle * camera.getFovFactor()) * (40f / pcamera.fieldOfView);
                double pixelSize = ((angle * pcamera.viewportHeight) / pcamera.fieldOfView) / 2;
                pcamera.project(pos);
                pos.y = pcamera.viewportHeight - pos.y;
                if (Settings.settings.program.modeStereo.active) {
                    pos.x /= 2;
                }
                // Check click distance
                if (checkClickDistance(screenX, screenY, pos, camera, pcamera, pixelSize)) {
                    //Hit
                    hits.add(entity);
                }
            }
        }
    }

    public void addHitRayCluster(FocusView view,
                                 Vector3d p0,
                                 Vector3d p1,
                                 NaturalCamera camera,
                                 Array<Entity> hits) {
        if (hitCondition(view)) {
            var entity = view.getEntity();

            Vector3b aux = B31;
            Vector3b posb = EntityUtils.getAbsolutePosition(entity, aux).add(camera.getInversePos());

            if (camera.direction.dot(posb) > 0) {
                // The star is in front of us
                // Diminish the size of the star
                // when we are close by
                double dist = posb.lenDouble();
                double distToLine = IntersectorDouble.distanceLinePoint(p0, p1, posb.tov3d(D32));
                double value = distToLine / dist;

                if (value < 0.01) {
                    hits.add(entity);
                }
            }
        }
    }
}
