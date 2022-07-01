package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.scene.system.render.draw.model.ModelEntityRender;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.Consumers.Consumer10;
import gaiasky.util.gdx.IntModelBatch;

public class Model implements Component {

    /** The model. **/
    public ModelComponent model;

    /** The render consumer. **/
    public Consumer10<ModelEntityRender, Entity, Model, IntModelBatch, Float, Double, RenderingContext, RenderGroup, Boolean, Boolean> renderConsumer;

    public void setModel(ModelComponent model) {
        this.model = model;
    }
}
