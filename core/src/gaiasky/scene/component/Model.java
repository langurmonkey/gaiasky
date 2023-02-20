package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.scene.record.ModelComponent;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.util.Consumers.Consumer10;
import gaiasky.util.gdx.IntModelBatch;

public class Model implements Component {

    /** The model. **/
    public ModelComponent model;
    /**
     * In constructed models, this attribute is used to cache the model size (diameter, size, width, height, depth)
     * in order to compute an accurate solid angle.
     */
    public double modelSize = 1;

    /** The render consumer. **/
    public Consumer10<ModelEntityRenderSystem, Entity, Model, IntModelBatch, Float, Double, RenderingContext, RenderGroup, Boolean, Boolean> renderConsumer;

    public void setModel(ModelComponent model) {
        this.model = model;
    }

    public void updateModel(ModelComponent model) {
        if(this.model != null) {
            this.model.updateWith(model);
        } else {
            setModel(model);
        }
    }

}
