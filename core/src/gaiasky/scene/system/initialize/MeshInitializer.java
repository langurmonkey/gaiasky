package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.data.AssetBean;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Mesh;
import gaiasky.scene.record.ModelComponent;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;

public class MeshInitializer extends AbstractInitSystem {
    public MeshInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var model = Mapper.model.get(entity);

        model.renderConsumer = ModelEntityRenderSystem::renderMeshModel;

        ModelComponent mc = model.model;
        if (mc != null) {
            mc.initialize(true);
        }

    }

    @Override
    public void setUpEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var model = Mapper.model.get(entity);
        var mesh = Mapper.mesh.get(entity);
        var label = Mapper.label.get(entity);
        ModelComponent mc = model.model;

        label.label = true;
        label.textScale = 0.2f;
        label.labelFactor = 0.8e-3f;
        label.labelMax = 1f;
        label.renderConsumer = LabelEntityRenderSystem::renderMesh;
        label.renderFunction = LabelView::renderTextEssential;

        AssetManager manager = AssetBean.manager();
        if (mc != null) {
            try {
                mc.doneLoading(manager, graph.localTransform, body.color, true);
                if (mesh.shading == Mesh.MeshShading.ADDITIVE) {
                    mc.setDepthTest(0, false);
                }
            } catch (Exception e) {
                mc = null;
            }
        }

        recomputePositioning(entity, mc, mesh);
    }

    private void recomputePositioning(Entity entity, ModelComponent mc, Mesh mesh) {
        if (mc != null) {
            if (mesh.coordinateSystem == null)
                mesh.coordinateSystem = new Matrix4();
            else
                mesh.coordinateSystem.idt();

            // Put reference system transformation in mesh.coordinateSystem.
            var transform = Mapper.transform.get(entity);
            if (transform.matrix != null) {
                Matrix4 m = new Matrix4();
                transform.matrix.putIn(m);
                mesh.coordinateSystem.mul(m);
            }

            // Apply affine transformations in specific order.
            var body = Mapper.body.get(entity);
            var affine = Mapper.affine.get(entity);
            if (affine != null) {
                var rotate = affine.getRotateTransform();
                var scale = affine.getScaleTransform();
                var translate = affine.getTranslateTransform();

                if (rotate != null) {
                    rotate.apply(mesh.coordinateSystem);
                }

                if (translate != null) {
                    body.pos.set(translate.getVector());
                    translate.apply(mesh.coordinateSystem);
                }

                if (scale != null) {
                    scale.apply(mesh.coordinateSystem);
                }
            }

        }
    }
}
