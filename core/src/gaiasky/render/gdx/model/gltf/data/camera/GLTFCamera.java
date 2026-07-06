/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.data.camera;

import gaiasky.render.gdx.model.gltf.data.GLTFEntity;

public class GLTFCamera extends GLTFEntity {
	public String type;
	public GLTFPerspective perspective;
	public GLTFOrthographic orthographic;
}
