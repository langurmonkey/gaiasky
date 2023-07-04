/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.shared.texture;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFImage;
import gaiasky.util.gdx.model.gltf.loaders.shared.data.DataFileResolver;

public class ImageResolver implements Disposable {
	
	private Array<Pixmap> pixmaps = new Array<Pixmap>();	
	
	private DataFileResolver dataFileResolver;
	
	public ImageResolver(DataFileResolver dataFileResolver) {
		super();
		this.dataFileResolver = dataFileResolver;
	}

	public void load(Array<GLTFImage> glImages) {
		if(glImages != null){
			for(int i=0 ; i<glImages.size ; i++){
				GLTFImage glImage = glImages.get(i);
				Pixmap pixmap = dataFileResolver.load(glImage);
				pixmaps.add(pixmap);
			}
		}
	}
	
	public Pixmap get(int index) {
		return pixmaps.get(index);
	}
	
	@Override
	public void dispose() {
		for(Pixmap pixmap : pixmaps){
			pixmap.dispose();
		}
		pixmaps.clear();
	}

	public void clear() {
		pixmaps.clear();
	}

	public Array<Pixmap> getPixmaps(Array<Pixmap> array) {
		array.addAll(pixmaps);
		return array;
	}
}
