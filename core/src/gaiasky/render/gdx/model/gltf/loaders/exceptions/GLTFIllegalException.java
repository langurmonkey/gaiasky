/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.loaders.exceptions;

import java.io.Serial;

public class GLTFIllegalException extends GLTFRuntimeException {

	@Serial
	private static final long serialVersionUID = 5253133784286484602L;

	public GLTFIllegalException(String message) {
		super(message);
	}
	
}
