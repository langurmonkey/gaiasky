/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.data;

import com.badlogic.gdx.graphics.g3d.model.data.ModelTexture;

public class OwnModelTexture extends ModelTexture {

	public final static int USAGE_METALLIC = 11;
	public final static int USAGE_HEIGHT = 12;
	public final static int USAGE_ROUGHNESS = 13;
	public final static int USAGE_AO = 14;
	// PBR packed texture.
	public final static int USAGE_AO_ROUGHNESS_METALLIC = 15;
}
