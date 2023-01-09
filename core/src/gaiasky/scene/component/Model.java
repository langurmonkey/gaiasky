package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.scene.record.ModelComponent;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.util.Consumers.Consumer10;
import gaiasky.util.gdx.IntModelBatch;

import java.util.Map;

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

    public void setDiffuseSVT(Map<Object, Object> virtualTexture) {
        if (this.model != null) {
            if (this.model.mtc != null) {
                this.model.mtc.setDiffuseSVT(this.model.mtc.convertToComponent(virtualTexture));
            }
        }
    }

    public void setSpecularSVT(Map<Object, Object> virtualTexture) {
        if (this.model != null) {
            if (this.model.mtc != null) {
                this.model.mtc.setSpecularSVT(this.model.mtc.convertToComponent(virtualTexture));
            }
        }
    }

    public void setHeightSVT(Map<Object, Object> virtualTexture) {
        if (this.model != null) {
            if (this.model.mtc != null) {
                this.model.mtc.setHeightSVT(this.model.mtc.convertToComponent(virtualTexture));
            }
        }
    }

}
