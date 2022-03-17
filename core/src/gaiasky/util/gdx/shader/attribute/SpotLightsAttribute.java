
package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.badlogic.gdx.utils.Array;

/** An {@link Attribute} which can be used to send an {@link Array} of {@link SpotLight} instances to the {@link Shader}. The
 * lights are stored by reference, the {@link #copy()} or {@link #SpotLightsAttribute(SpotLightsAttribute)} method
 * will not create new lights.
 * @author Xoppa */
public class SpotLightsAttribute extends Attribute {
	public final static String Alias = "spotLights";
	public final static int Type = register(Alias);

	public final Array<SpotLight> lights;

	public SpotLightsAttribute () {
		super(Type);
		lights = new Array<>(1);
	}

	public SpotLightsAttribute (final SpotLightsAttribute copyFrom) {
		this();
		lights.addAll(copyFrom.lights);
	}

	@Override
	public SpotLightsAttribute copy () {
		return new SpotLightsAttribute(this);
	}

	@Override
	public int hashCode () {
		int result = super.hashCode();
		for (SpotLight light : lights)
			result = 1237 * result + (light == null ? 0 : light.hashCode());
		return result;
	}
	
	@Override
	public int compareTo (Attribute o) {
		if (index != o.index) return index < o.index ? -1 : 1;
		return 0; // FIXME implement comparing
	}
}
