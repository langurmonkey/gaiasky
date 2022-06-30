package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.data.AssetBean;
import gaiasky.render.RenderingContext;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Mesh;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;

public class MeshInitializer extends InitSystem {
    public MeshInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var model = Mapper.model.get(entity);
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

        label.textScale = 0.2f;
        label.labelFactor = 0.8e-3f;
        label.labelMax = 1f;
        label.renderConsumer = (LabelEntityRenderSystem rs, LabelView l, ExtSpriteBatch b, ExtShaderProgram s, FontRenderSystem f, RenderingContext r, ICamera c)
                -> rs.renderMesh(l, b, s, f, r, c);

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

            // REFSYS ROTATION
            var transform = Mapper.transform.get(entity);
            if(transform.matrix != null) {
                Matrix4 m = new Matrix4();
                transform.matrix.putIn(m);
                mesh.coordinateSystem.mul(m);
            }

            var body = Mapper.body.get(entity);
            var affine = Mapper.affine.get(entity);
            if(affine != null && affine.transformations != null) {
                affine.apply(mesh.coordinateSystem);

                // Set translation to position
                Vector3 translation = new Vector3();
                mesh.coordinateSystem.getTranslation(translation);
                body.pos.set(translation);
            }


        }
    }
}
