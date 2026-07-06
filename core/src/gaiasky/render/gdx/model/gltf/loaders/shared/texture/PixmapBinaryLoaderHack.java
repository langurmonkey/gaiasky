/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.loaders.shared.texture;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.render.gdx.model.gltf.loaders.exceptions.GLTFRuntimeException;
import gaiasky.render.gdx.model.gltf.loaders.exceptions.GLTFUnsupportedException;

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
