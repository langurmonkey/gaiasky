/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.loaders.shared.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import gaiasky.render.gdx.model.gltf.data.GLTF;
import gaiasky.render.gdx.model.gltf.data.texture.GLTFImage;

import java.nio.ByteBuffer;

public interface DataFileResolver {
	void load(FileHandle file);
	GLTF getRoot();
	ByteBuffer getBuffer(int buffer);
	Pixmap load(GLTFImage glImage);
}
