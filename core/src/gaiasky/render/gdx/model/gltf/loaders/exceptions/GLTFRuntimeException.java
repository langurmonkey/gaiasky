/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.loaders.exceptions;

import java.io.Serial;

public class GLTFRuntimeException extends RuntimeException {
	
	@Serial
	private static final long serialVersionUID = -8571720960735308661L;

	public GLTFRuntimeException (String message) {
		super(message);
	}

	public GLTFRuntimeException (Throwable t) {
		super(t);
	}

	public GLTFRuntimeException (String message, Throwable t) {
		super(message, t);
	}
}
