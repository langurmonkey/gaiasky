package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Model;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.IsFocusActive;
import gaiasky.scene.view.LabelView;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.Bits;
import gaiasky.util.Constants;
import gaiasky.util.ModelCache;
import gaiasky.util.Settings;
import gaiasky.util.gdx.IntMeshPartBuilder;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.IntModelBuilder;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.BlendingAttribute;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.gdx.shader.attribute.FloatAttribute;

/**
 * Initializes star cluster entities.
 */
public class ClusterInitializer extends AbstractInitSystem {

    public ClusterInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var cluster = Mapper.cluster.get(entity);
        var label = Mapper.label.get(entity);
        var focus = Mapper.focus.get(entity);

        // Focus active
        focus.activeConsumer = (IsFocusActive i, Entity e, Base b) -> i.isFocusActiveCtOpacity(e, b);

        base.ct = new ComponentTypes(ComponentType.Clusters.ordinal());
        // Compute size from distance and radius, convert to units
        body.size = (float) (Math.tan(Math.toRadians(cluster.raddeg)) * cluster.dist * 2);

        label.textScale = 0.2f;
        label.labelMax = (float) (.5e-3 / Constants.DISTANCE_SCALE_FACTOR);
        label.labelFactor = 1;
        label.renderConsumer = (LabelEntityRenderSystem rs, LabelView l, ExtSpriteBatch b, ExtShaderProgram s, FontRenderSystem f, RenderingContext r, ICamera c)
                -> rs.renderCluster(l, b, s, f, r, c);

    }

    @Override
    public void setUpEntity(Entity entity) {
        initModel(entity);
    }

    private void initModel(Entity entity) {
        var body = Mapper.body.get(entity);
        var model = Mapper.model.get(entity);
        var cluster = Mapper.cluster.get(entity);

        if (cluster.clusterTex == null) {
            cluster.clusterTex = new Texture(Settings.settings.data.dataFileHandle("data/tex/base/cluster-tex.png"), true);
            cluster.clusterTex.setFilter(TextureFilter.MipMapLinearNearest, TextureFilter.Linear);
        }
        if (cluster.model == null) {
            Material mat = new Material(new BlendingAttribute(GL20.GL_ONE, GL20.GL_ONE), new ColorAttribute(ColorAttribute.Diffuse, body.color[0], body.color[1], body.color[2], body.color[3]));
            IntModelBuilder modelBuilder = ModelCache.cache.mb;
            modelBuilder.begin();
            // create part
            IntMeshPartBuilder bPartBuilder = modelBuilder.part("sph", GL20.GL_LINES, Bits.indexes(Usage.Position), mat);
            bPartBuilder.icosphere(1, 3, false, true);

            cluster.model = (modelBuilder.end());
            cluster.modelTransform = new Matrix4();
        }

        model.renderConsumer = (ModelEntityRenderSystem mer, Entity e, Model m, IntModelBatch b, Float a, Double t, RenderingContext r, RenderGroup rg, Boolean s, Boolean rel) ->
                mer.renderStarClusterModel(e, m ,b, a, t, r, rg, s, rel);

        model.model = new ModelComponent(false);
        model.model.initialize(null);
        DirectionalLight dLight = new DirectionalLight();
        dLight.set(1, 1, 1, 1, 1, 1);
        model.model.env = new Environment();
        model.model.env.add(dLight);
        model.model.env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1.0f, 1.0f, 1.0f, 1f));
        model.model.env.set(new FloatAttribute(FloatAttribute.Shininess, 0.2f));
        model.model.instance = new IntModelInstance(cluster.model, cluster.modelTransform);

        // Relativistic effects
        if (Settings.settings.runtime.relativisticAberration)
            model.model.rec.setUpRelativisticEffectsMaterial(model.model.instance.materials);
        // Gravitational waves
        if (Settings.settings.runtime.gravitationalWaves)
            model.model.rec.setUpGravitationalWavesMaterial(model.model.instance.materials);

    }
}
