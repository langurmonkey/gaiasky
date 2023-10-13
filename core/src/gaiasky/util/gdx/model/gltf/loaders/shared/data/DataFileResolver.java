/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.shared.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import gaiasky.util.gdx.model.gltf.data.GLTF;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFImage;

import java.nio.ByteBuffer;

public interface DataFileResolver {
	void load(FileHandle file);
	GLTF getRoot();
	ByteBuffer getBuffer(int buffer);
	Pixmap load(GLTFImage glImage);
}
