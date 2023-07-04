/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.exceptions;

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
