/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.attributes;

import com.badlogic.gdx.graphics.Color;
import gaiasky.render.gdx.shader.attribute.Attribute;
import gaiasky.render.gdx.shader.attribute.ColorAttribute;

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
