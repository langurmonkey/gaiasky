package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector3;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.color.ColorUtils;

/**
 * Initializes the old Particle and Star objects.
 */
public class ParticleInitializationSystem extends IteratingSystem {

    private final double discFactor = Constants.PARTICLE_DISC_FACTOR;

    public ParticleInitializationSystem(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Base base = Mapper.base.get(entity);
        Body body = Mapper.body.get(entity);
        Celestial celestial = Mapper.celestial.get(entity);
        Magnitude mag = Mapper.magnitude.get(entity);
        ProperMotion pm = Mapper.pm.get(entity);
        ParticleExtra extra = Mapper.extra.get(entity);
        SolidAngle sa = Mapper.sa.get(entity);
        Text text = Mapper.text.get(entity);
        RenderType render = Mapper.render.get(entity);
        Hip hip = Mapper.hip.get(entity);
        Distance dist = Mapper.distance.get(entity);


        if (hip != null) {
            // Initialize star
            initializeStar(base, body, celestial, mag, pm, extra, sa, text, render, dist);
        } else {
            // Initialize particle
            initializeParticle(base, body, celestial, mag, pm, extra, sa, text, render);
        }


    }

    private void baseInitialization(Base base, Body body, Celestial celestial, Magnitude mag, ProperMotion pm, ParticleExtra extra, SolidAngle sa, Text text, RenderType render) {
        pm.pm = new Vector3();
        pm.pmSph = new Vector3();

        // Defaults
        sa.thresholdNone = Settings.settings.scene.star.threshold.none;
        sa.thresholdPoint = Settings.settings.scene.star.threshold.point;
        sa.thresholdQuad = Settings.settings.scene.star.threshold.quad;

        text.textScale = 0.2f;
        text.labelFactor = 1.3e-1f;
        text.labelMax = 0.01f;

        extra.primitiveRenderScale = 1;
        render.renderGroup = RenderGroup.BILLBOARD_STAR;

        sa.thresholdFactor = (float) (sa.thresholdPoint / Settings.settings.scene.label.number);
    }

    private void initializeParticle(Base base, Body body, Celestial celestial, Magnitude mag, ProperMotion pm, ParticleExtra extra, SolidAngle sa, Text text, RenderType render) {
        baseInitialization(base, body, celestial, mag, pm, extra, sa, text, render);

        // Actual initialization
        setDerivedAttributes(body, celestial, mag, extra, false);

        if (base.ct == null)
            base.ct = new ComponentTypes(ComponentType.Galaxies);

        // Relation between the particle size and actual star size (normalized for
        // the Sun, 695700 Km of radius)
        extra.radius = body.size * Constants.STAR_SIZE_FACTOR;
    }

    private void initializeStar(Base base, Body body, Celestial celestial, Magnitude mag, ProperMotion pm, ParticleExtra extra, SolidAngle sa, Text text, RenderType render, Distance dist) {
        baseInitialization(base, body, celestial, mag, pm, extra, sa, text, render);

        setDerivedAttributes(body, celestial, mag, extra, true);

        if (base.ct == null)
            base.ct = new ComponentTypes(ComponentType.Stars);

        // Relation between the particle size and actual star size (normalized for
        // the Sun, 695700 Km of radius)
        extra.radius = body.size * Constants.STAR_SIZE_FACTOR;
        dist.distance = 172.4643429 * extra.radius;
    }

    private void setDerivedAttributes(Body body, Celestial celestial, Magnitude mag, ParticleExtra extra, boolean isStar) {
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
        if (body.cc == null)
            body.cc = ColorUtils.BVtoRGB(celestial.colorbv);
        setColor2Data(body, celestial);
    }

    private void setColor2Data(Body body, Celestial celestial) {
        final float plus = .1f;
        celestial.ccPale = new float[] { Math.min(1, body.cc[0] + plus), Math.min(1, body.cc[1] + plus), Math.min(1, body.cc[2] + plus) };
    }
}
