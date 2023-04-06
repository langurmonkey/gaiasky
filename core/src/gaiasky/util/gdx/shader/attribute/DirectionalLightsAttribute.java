package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.utils.Array;

/**
 * An {@link Attribute} which can be used to send an {@link Array} of {@link DirectionalLight} instances to the {@link Shader}.
 * The lights are stored by reference, the {@link #copy()} or {@link #DirectionalLightsAttribute(DirectionalLightsAttribute)}
 * method will not create new lights.
 *
 * @author Xoppa
 */
public class DirectionalLightsAttribute extends Attribute {
    public final static String Alias = "directionalLights";
    public final static int Type = register(Alias);

    public final Array<DirectionalLight> lights;

    public DirectionalLightsAttribute() {
        super(Type);
        lights = new Array<>(1);
    }

    public DirectionalLightsAttribute(final DirectionalLightsAttribute copyFrom) {
        this();
        lights.addAll(copyFrom.lights);
    }

    public DirectionalLightsAttribute(com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute other) {
        this();
        lights.addAll(other.lights);
    }

    @Override
    public DirectionalLightsAttribute copy() {
        return new DirectionalLightsAttribute(this);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        for (DirectionalLight light : lights)
            result = 1229 * result + (light == null ? 0 : light.hashCode());
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (index != o.index)
            return index < o.index ? -1 : 1;
        return 0; // FIXME implement comparing
    }
}
