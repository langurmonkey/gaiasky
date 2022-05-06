package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.*;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.BlendingAttribute;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.gdx.shader.attribute.FloatAttribute;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;

import java.util.Map;
import java.util.TreeMap;

/**
 * Initializes the old Particle and Star objects.
 */
public class ParticleInitializer extends InitSystem {

    private final double discFactor = Constants.PARTICLE_DISC_FACTOR;

    public ParticleInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
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
        var text = Mapper.text.get(entity);
        var render = Mapper.render.get(entity);
        var hip = Mapper.hip.get(entity);
        var dist = Mapper.distance.get(entity);


        if (hip != null) {
            // Initialize star
            initializeStar(base, body, celestial, mag, pm, extra, sa, text, render, dist);
        } else {
            // Initialize particle
            initializeParticle(base, body, celestial, mag, pm, extra, sa, text, render);
        }
    }

    @Override
    public void setUpEntity(Entity entity) {
        var hip = Mapper.hip.get(entity);
        if(hip != null) {

        }

    }

    private void baseInitialization(ProperMotion pm, ParticleExtra extra, SolidAngle sa, Text text, RenderType render) {
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
        baseInitialization(pm, extra, sa, text, render);

        // Actual initialization
        setDerivedAttributes(body, celestial, mag, extra, false);

        if (base.ct == null)
            base.ct = new ComponentTypes(ComponentType.Galaxies);

        // Relation between the particle size and actual star size (normalized for
        // the Sun, 695700 Km of radius)
        extra.radius = body.size * Constants.STAR_SIZE_FACTOR;
    }

    private void initializeStar(Base base, Body body, Celestial celestial, Magnitude mag, ProperMotion pm, ParticleExtra extra, SolidAngle sa, Text text, RenderType render, Distance dist) {
        baseInitialization(pm, extra, sa, text, render);

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

    private void initModel(final AssetManager manager, final Model m) {
        Texture tex = manager.get(Settings.settings.data.dataFile("tex/base/star.jpg"), Texture.class);
        Texture lut = manager.get(Settings.settings.data.dataFile("tex/base/lut.jpg"), Texture.class);
        tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        Map<String, Object> params = new TreeMap<>();
        params.put("quality", 120L);
        params.put("diameter", 1d);
        params.put("flip", false);

        Pair<IntModel, Map<String, gaiasky.util.gdx.shader.Material>> pair = ModelCache.cache.getModel("sphere", params, Bits.indexes(Usage.Position, Usage.Normal, Usage.TextureCoordinates), GL20.GL_TRIANGLES);
        IntModel model = pair.getFirst();
        Material mat = pair.getSecond().get("base");
        mat.clear();
        mat.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
        mat.set(new TextureAttribute(TextureAttribute.Normal, lut));
        // Only to activate view vector (camera position)
        mat.set(new ColorAttribute(ColorAttribute.Specular));
        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
        Matrix4 modelTransform = new Matrix4();
        m.model = new ModelComponent(false);
        m.model.initialize(null);
        m.model.env = new Environment();
        m.model.env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
        m.model.env.set(new FloatAttribute(FloatAttribute.Time, 0f));
        m.model.instance = new IntModelInstance(model, modelTransform);
        // Relativistic effects
        if (Settings.settings.runtime.relativisticAberration)
            m.model.rec.setUpRelativisticEffectsMaterial(m.model.instance.materials);
        m.model.setModelInitialized(true);
    }
}
