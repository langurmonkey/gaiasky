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
import gaiasky.scene.entity.FocusHit;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.system.render.draw.billboard.BillboardEntityRenderSystem;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.entity.FocusActive;
import gaiasky.util.*;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.Vector3b;

/**
 * Initializes the old Particle and Star objects.
 */
public class ParticleInitializer extends AbstractInitSystem implements IObserver {

    private final ParticleUtils utils;
    private Vector3b B31;

    private final double discFactor = Constants.PARTICLE_DISC_FACTOR;

    public ParticleInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);

        this.utils = new ParticleUtils();
        this.B31 = new Vector3b();

        EventManager.instance.subscribe(this, Event.STAR_POINT_SIZE_CMD);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var celestial = Mapper.celestial.get(entity);
        var mag = Mapper.magnitude.get(entity);
        var pm = Mapper.pm.get(entity);
        var extra = Mapper.extra.get(entity);
        var sa = Mapper.sa.get(entity);
        var label = Mapper.label.get(entity);
        var render = Mapper.renderType.get(entity);
        var hip = Mapper.hip.get(entity);
        var dist = Mapper.distance.get(entity);
        var focus = Mapper.focus.get(entity);
        var bb = Mapper.billboard.get(entity);

        // Focus active.
        focus.activeFunction = FocusActive::isFocusActiveTrue;

        // Billboard renderer.
        bb.renderConsumer = BillboardEntityRenderSystem::renderBillboardCelestial;

        if (hip != null) {
            // Initialize star.
            initializeStar(base, body, celestial, mag, pm, extra, sa, label, render, dist, focus);
        } else {
            // Initialize particle.
            initializeParticle(base, body, celestial, mag, pm, extra, sa, label, render, focus);
        }
    }

    @Override
    public void setUpEntity(Entity entity) {
        // Set up stars.
        if (Mapper.hip.has(entity)) {
            var model = Mapper.model.get(entity);
            model.renderConsumer = ModelEntityRenderSystem::renderParticleStarModel;
            utils.initModel(AssetBean.manager(), model);

            var mag = Mapper.magnitude.get(entity);
            var coordinates = Mapper.coordinates.get(entity);
            if (!Float.isFinite(mag.absmag)) {
                double distPc;
                if (coordinates.coordinates != null) {
                    distPc = coordinates.coordinates.getEquatorialCartesianCoordinates(GaiaSky.instance.time.getTime(), B31).lend() * Constants.U_TO_PC;
                } else {
                    distPc = EntityUtils.getAbsolutePosition(entity, B31).lend() * Constants.U_TO_PC;
                }
                mag.absmag = (float) AstroUtils.apparentToAbsoluteMagnitude(distPc, mag.appmag);
            }
        }

    }

    private void baseInitialization(ProperMotion pm, ParticleExtra extra, Celestial celestial, SolidAngle sa, RenderType render) {
        if (pm.pm == null) {
            pm.pm = new Vector3();
            pm.pmSph = new Vector3();
            pm.hasPm = false;
        } else {
            pm.hasPm = pm.pm.len2() != 0;
        }

        // Defaults.
        sa.thresholdNone = Settings.settings.scene.star.threshold.none;
        sa.thresholdPoint = Settings.settings.scene.star.threshold.point;
        sa.thresholdQuad = Settings.settings.scene.star.threshold.quad;

        if(render.renderGroup == null) {
            render.renderGroup = RenderGroup.BILLBOARD_STAR;
        }

        if(extra.primitiveRenderScale <= 0) {
            extra.primitiveRenderScale = 1;
        }
        float pSize = Settings.settings.scene.star.pointSize < 0 ? 8 : Settings.settings.scene.star.pointSize;
        celestial.innerRad = (0.004f * discFactor + pSize * 0.008f) * 1.5f;


    }

    private void initializeParticle(Base base, Body body, Celestial celestial, Magnitude mag, ProperMotion pm, ParticleExtra extra, SolidAngle sa, Label label, RenderType render, Focus focus) {
        baseInitialization(pm, extra, celestial, sa, render);

        sa.thresholdLabel = sa.thresholdPoint * 1e-2f / Settings.settings.scene.label.number;
        label.textScale = 0.1f;
        label.labelFactor = 1.3e-1f;
        label.labelMax = 0.005f;

        // Actual initialization.
        setDerivedAttributes(body, celestial, mag, extra, false);

        if (base.ct == null)
            base.ct = new ComponentTypes(ComponentType.Galaxies);

        // Relation between the particle size and actual star size (normalized for
        // the Sun, 695700 Km of radius).
        extra.radius = body.size * Constants.STAR_SIZE_FACTOR;

        // Focus hits.
        focus.hitCoordinatesConsumer = FocusHit::addHitCoordinateCelestial;
        focus.hitRayConsumer = FocusHit::addHitRayCelestial;
    }

    private void initializeStar(Base base, Body body, Celestial celestial, Magnitude mag, ProperMotion pm, ParticleExtra extra, SolidAngle sa, Label label, RenderType render, Distance dist, Focus focus) {
        baseInitialization(pm, extra, celestial, sa, render);

        sa.thresholdLabel = sa.thresholdPoint / Settings.settings.scene.label.number;
        label.textScale = 0.2f;
        label.labelFactor = 1.3e-1f;
        label.labelMax = 0.01f;
        label.renderConsumer = LabelEntityRenderSystem::renderCelestial;

        setDerivedAttributes(body, celestial, mag, extra, true);

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

    private void setDerivedAttributes(Body body, Celestial celestial, Magnitude mag, ParticleExtra extra, boolean isStar) {
        if (!Float.isFinite(mag.absmag)) {
            // Default
            mag.absmag = isStar ? 15.0f : -5.0f;
        }

        double flux = Math.pow(10, -mag.absmag / 2.5f);
        setRGB(body, celestial);

        // Calculate size - This contains arbitrary boundary values to make
        // things nice on the render side
        if (!isStar) {
            body.size = (float) (Math.log(Math.pow(flux, 10.0)) * Constants.PC_TO_U);
        } else {
            body.size = (float) (Math.min((Math.pow(flux, 0.5f) * Constants.PC_TO_U * 0.16f), 1e9f) / discFactor);
        }
        extra.computedSize = 0;
    }

    /**
     * Sets the RGB color.
     */
    private void setRGB(Body body, Celestial celestial) {
        if (body.color == null)
            body.color = ColorUtils.BVtoRGB(celestial.colorbv);
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
