/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.data.AssetBean;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.entity.FocusActive;
import gaiasky.scene.entity.FocusHit;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.system.render.draw.billboard.BillboardEntityRenderSystem;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.FocusView;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.Settings;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

public class ParticleInitializer extends AbstractInitSystem implements IObserver {

    private final ParticleUtils utils;
    private final double discFactor = Constants.PARTICLE_DISC_FACTOR;
    private final FocusView view;

    private final Vector3b B31;
    private final Vector3d D31;

    public ParticleInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);

        this.utils = new ParticleUtils();
        this.B31 = new Vector3b();
        this.D31 = new Vector3d();
        this.view = new FocusView();

        EventManager.instance.subscribe(this, Event.STAR_POINT_SIZE_CMD);
    }

    @Override
    public void initializeEntity(Entity entity) {
        view.setEntity(entity);
        var base = view.base;
        var body = view.body;
        var celestial = Mapper.celestial.get(entity);
        var mag = view.getMag();
        var pm = Mapper.pm.get(entity);
        var extra = Mapper.extra.get(entity);
        var sa = Mapper.sa.get(entity);
        var label = Mapper.label.get(entity);
        var render = Mapper.renderType.get(entity);
        var hip = Mapper.hip.get(entity);
        var dist = Mapper.distance.get(entity);
        var focus = Mapper.focus.get(entity);
        var coordinates = Mapper.coordinates.get(entity);
        var bb = Mapper.billboard.get(entity);

        // Focus active.
        focus.activeFunction = FocusActive::isFocusActiveTrue;

        // Billboard renderer.
        bb.renderConsumer = BillboardEntityRenderSystem::renderBillboardCelestial;

        if (hip != null) {
            // Initialize star.
            initializeStar(entity, base, body, celestial, mag, pm, extra, sa, label, render, dist, focus, coordinates);
        } else {
            // Initialize particle.
            initializeParticle(entity, base, body, celestial, mag, pm, extra, sa, label, render, focus, coordinates);
        }
    }

    @Override
    public void setUpEntity(Entity entity) {
        // Set up stars.
        if (Mapper.hip.has(entity)) {
            var model = Mapper.model.get(entity);
            model.renderConsumer = ModelEntityRenderSystem::renderParticleStarModel;
            utils.initModel(AssetBean.manager(), model);
        }
    }

    private void baseInitialization(ProperMotion pm, ParticleExtra extra, Celestial celestial, SolidAngle sa, RenderType render) {
        if (pm.pm == null) {
            pm.pm = new Vector3();
            pm.pmSph = new Vector3();
            pm.hasPm = false;
        } else {
            // Init proper motion.
            if (pm.pm.len2() == 0 && pm.pmSph.len2() != 0) {
                // Init cartesian from spherical.
                gaiasky.util.coord.Coordinates.cartesianToSpherical(view.body.pos, D31);
                if (view.body.posSph == null) {
                    view.body.posSph = new Vector2d();
                }
                view.body.posSph.set((float) (Nature.TO_DEG * D31.x), (float) (Nature.TO_DEG * D31.y));
                var distPc = view.getPos().lenDouble() * Constants.U_TO_PC;

                Vector3d pmv = AstroUtils.properMotionsToCartesian(pm.pmSph.x, pm.pmSph.y, pm.pmSph.z, Math.toRadians(view.getAlpha()), Math.toRadians(view.getDelta()), distPc, new Vector3d());
                pmv.put(pm.pm);
            }
            pm.hasPm = pm.pm.len2() != 0;
        }

        // Defaults.
        sa.thresholdNone = Settings.settings.scene.star.threshold.none;
        sa.thresholdPoint = Settings.settings.scene.star.threshold.point;
        sa.thresholdQuad = Settings.settings.scene.star.threshold.quad;

        if (render.renderGroup == null) {
            render.renderGroup = RenderGroup.BILLBOARD_STAR;
        }

        if (extra.primitiveRenderScale <= 0) {
            extra.primitiveRenderScale = 1;
        }
        float pSize = Settings.settings.scene.star.pointSize < 0 ? 8 : Settings.settings.scene.star.pointSize;
        celestial.innerRad = (0.004f * discFactor + pSize * 0.008f) * 1.5f;

    }

    private void initializeParticle(Entity entity, Base base, Body body, Celestial celestial, Magnitude mag, ProperMotion pm, ParticleExtra extra, SolidAngle sa, Label label, RenderType render, Focus focus, Coordinates coordinates) {
        baseInitialization(pm, extra, celestial, sa, render);

        sa.thresholdLabel = sa.thresholdPoint * 1e-2f / Settings.settings.scene.label.number;
        label.textScale = 0.1f;
        label.labelFactor = 1.3e-1f;
        label.labelMax = 0.005f;
        label.renderConsumer = LabelEntityRenderSystem::renderCelestial;
        label.renderFunction = LabelView::renderTextParticle;

        // Actual initialization.
        setDerivedAttributes(entity, body, celestial, mag, coordinates, extra, false);

        if (base.ct == null)
            base.ct = new ComponentTypes(ComponentType.Galaxies);

        // Relation between the particle size and actual star size (normalized for
        // the Sun, 695700 Km of radius).
        extra.radius = body.size * Constants.STAR_SIZE_FACTOR;

        // Focus hits.
        focus.hitCoordinatesConsumer = FocusHit::addHitCoordinateCelestial;
        focus.hitRayConsumer = FocusHit::addHitRayCelestial;
    }

    private void initializeStar(Entity entity, Base base, Body body, Celestial celestial, Magnitude mag, ProperMotion pm, ParticleExtra extra, SolidAngle sa, Label label, RenderType render, Distance dist, Focus focus, Coordinates coordinates) {
        baseInitialization(pm, extra, celestial, sa, render);

        sa.thresholdLabel = sa.thresholdPoint / Settings.settings.scene.label.number;

        label.label = true;
        label.textScale = 0.2f;
        label.labelFactor = 1.3e-1f;
        label.labelMax = 0.01f;
        label.renderConsumer = LabelEntityRenderSystem::renderCelestial;
        label.renderFunction = LabelView::renderTextParticle;

        setDerivedAttributes(entity, body, celestial, mag, coordinates, extra, true);

        if (base.ct == null)
            base.ct = new ComponentTypes(ComponentType.Stars);

        // Relation between the particle size and actual star size (normalized for
        // the Sun, 695700 Km of radius)
        extra.radius = body.size * Constants.STAR_SIZE_FACTOR;
        dist.distance = 172.4643429 * extra.radius;

        // Focus hits.
        focus.hitCoordinatesConsumer = FocusHit::addHitCoordinateStar;
        focus.hitRayConsumer = FocusHit::addHitRayCelestial;
    }

    private void setDerivedAttributes(Entity entity, Body body, Celestial celestial, Magnitude mag, Coordinates coordinates, ParticleExtra extra, boolean isStar) {
        double distPc;
        if (coordinates.coordinates != null) {
            distPc = coordinates.coordinates.getEquatorialCartesianCoordinates(GaiaSky.instance.time.getTime(), B31).lenDouble() * Constants.U_TO_PC;
        } else {
            distPc = EntityUtils.getAbsolutePosition(entity, B31).lenDouble() * Constants.U_TO_PC;
        }

        boolean finiteApparent = Float.isFinite(mag.appMag);
        boolean finiteAbsolute = Float.isFinite(mag.absMag);

        if (!finiteApparent && !finiteAbsolute) {
            // We have no magnitudes.
            // Set default apparent magnitude to 15.
            mag.appMag = isStar ? 10.0f : -5.0f;
            mag.absMag = (float) AstroUtils.apparentToAbsoluteMagnitude(distPc, mag.appMag);
        } else if (!finiteApparent) {
            // We only have absolute magnitude. Compute apparent from absolute.
            mag.appMag = (float) AstroUtils.absoluteToApparentMagnitude(distPc, mag.absMag);
        } else if (!finiteAbsolute) {
            // We only have apparent magnitude. Compute absolute from apparent.
            mag.absMag = (float) AstroUtils.apparentToAbsoluteMagnitude(distPc, mag.appMag);
        }

        // Color.
        setRGB(body, celestial);

        // Calculate size - This contains arbitrary boundary values to make
        // things nice on the render side
        double flux = Math.pow(10, -mag.absMag / 2.5f);
        if (isStar) {
            body.size = (float) (Math.min((Math.pow(flux, 0.5f) * Constants.PC_TO_U * 0.16f), 1e9f) / discFactor);
        } else {
            body.size = (float) (Math.log(Math.pow(flux, 10.0)) * Constants.PC_TO_U);
        }
        extra.computedSize = 0;
    }

    /**
     * Sets the RGB color.
     */
    private void setRGB(Body body, Celestial celestial) {
        if (body.color == null)
            body.color = ColorUtils.BVtoRGB(celestial.colorBv);
        EntityUtils.setColor2Data(body, celestial, 0.1f);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        GaiaSky.postRunnable(() -> {
            // First, get all current entities for the family
            if (this.engineBackup != null) {
                ImmutableArray<Entity> entities = this.engineBackup.getEntitiesFor(getFamily());
                if (event == Event.STAR_POINT_SIZE_CMD) {
                    for (Entity entity : entities) {
                        Mapper.celestial.get(entity).innerRad = (float) ((0.004f * Constants.PARTICLE_DISC_FACTOR + (Float) data[0] * 0.008f) * 1.5f);
                    }
                }
            }
        });
    }
}
