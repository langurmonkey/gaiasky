package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;

/**
 * Extra attributes for stars and particles.
 */
public class ParticleExtra implements Component, ICopy {

    public double computedSize;
    public double radius;
    public double primitiveRenderScale;

    public void setPrimitiveRenderScale(Double primitiveRenderScale) {
        this.primitiveRenderScale = primitiveRenderScale;
    }

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(this.getClass());
        copy.computedSize = computedSize;
        copy.radius = radius;
        copy.primitiveRenderScale = primitiveRenderScale;
        return copy;
    }
}
