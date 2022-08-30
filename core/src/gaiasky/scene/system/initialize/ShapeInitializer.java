package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.*;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.BlendingAttribute;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.gdx.shader.attribute.FloatAttribute;

import java.util.Map;

public class ShapeInitializer extends AbstractInitSystem {
    public ShapeInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {

    }

    @Override
    public void setUpEntity(Entity entity) {
        var line = Mapper.line.get(entity);
        var label = Mapper.label.get(entity);

        label.label = true;
        label.textScale = 0.2f;
        label.labelMax = 1f;
        label.labelFactor = (float) (0.5e-3f * Constants.DISTANCE_SCALE_FACTOR);
        label.renderConsumer = LabelEntityRenderSystem::renderShape;
        label.renderFunction = LabelView::renderTextEssential;

        line.lineWidth = 1.5f;

        initModel(entity);
    }

    public void initModel(Entity entity) {
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var modelComp = Mapper.model.get(entity);
        var shape = Mapper.shape.get(entity);

        modelComp.renderConsumer = ModelEntityRenderSystem::renderShape;

        graph.localTransform = new Matrix4();
        Pair<IntModel, Map<String, Material>> m = ModelCache.cache.getModel(shape.modelShape, shape.modelParams, Bits.indexes(Usage.Position), shape.primitiveType);
        IntModel model = m.getFirst();
        for (Map.Entry<String, Material> material : m.getSecond().entrySet()) {
            material.getValue().set(new BlendingAttribute(GL20.GL_ONE, GL20.GL_ONE));
            material.getValue().set(new ColorAttribute(ColorAttribute.Diffuse, body.color[0], body.color[1], body.color[2], body.color[3]));
        }

        modelComp.model = new ModelComponent(false);
        var mc = modelComp.model;
        mc.initialize(null);
        DirectionalLight dLight = new DirectionalLight();
        dLight.set(1, 1, 1, 1, 1, 1);
        mc.env = new Environment();
        mc.env.add(dLight);
        mc.env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1.0f, 1.0f, 1.0f, 1f));
        mc.env.set(new FloatAttribute(FloatAttribute.Shininess, 0.2f));
        mc.instance = new IntModelInstance(model, new Matrix4());

        // Relativistic effects
        if (Settings.settings.runtime.relativisticAberration)
            mc.rec.setUpRelativisticEffectsMaterial(mc.instance.materials);
        // Gravitational waves
        if (Settings.settings.runtime.gravitationalWaves)
            mc.rec.setUpGravitationalWavesMaterial(mc.instance.materials);
    }
}
