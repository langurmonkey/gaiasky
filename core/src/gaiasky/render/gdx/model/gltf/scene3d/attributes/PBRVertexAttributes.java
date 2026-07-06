/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.attributes;

public class PBRVertexAttributes
{
	// based on VertexAttributes maximum (biNormal = 256)
	public static final class Usage {
		public static final int PositionTarget = 512;
		public static final int NormalTarget = 1024;
		public static final int TangentTarget = 2048;
	}
}
