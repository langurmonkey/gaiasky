/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.shared.texture;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.util.gdx.model.gltf.loaders.exceptions.GLTFRuntimeException;
import gaiasky.util.gdx.model.gltf.loaders.exceptions.GLTFUnsupportedException;

public class PixmapBinaryLoaderHack {

	public static Pixmap load(byte [] encodedData, int offset, int len){
		if(Gdx.app.getType() == ApplicationType.WebGL){
			throw new GLTFUnsupportedException("load pixmap from bytes not supported for WebGL");
		}else{
			// call new Pixmap(encodedData, offset, len); via reflection to
			// avoid compilation error with GWT.
			try {
				return (Pixmap)ClassReflection.getConstructor(Pixmap.class, byte[].class, int.class, int.class).newInstance(encodedData, offset, len);
			} catch (ReflectionException e) {
				throw new GLTFRuntimeException(e);
			}
		}
	}
}
