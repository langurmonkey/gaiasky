/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.attributes;

import com.badlogic.gdx.graphics.Color;
import gaiasky.util.gdx.shader.attribute.Attribute;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;

public class PBRColorAttribute extends ColorAttribute
{
	public final static String BaseColorFactorAlias = "BaseColorFactor";
	public final static int BaseColorFactor = register(BaseColorFactorAlias);

	public static PBRColorAttribute createBaseColorFactor(Color color){
		return new PBRColorAttribute(BaseColorFactor, color);
	}

	public PBRColorAttribute(int index, Color color) {
		super(index, color);
	}
	
	@Override
	public Attribute copy() {
		return new PBRColorAttribute(index, color);
	}
	
}
