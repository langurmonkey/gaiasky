/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.exceptions;

import java.io.Serial;

public class GLTFUnsupportedException extends GLTFRuntimeException {
    @Serial
    private static final long serialVersionUID = 2530359716452090852L;

    public GLTFUnsupportedException(String message) {
        super(message);
    }

}
