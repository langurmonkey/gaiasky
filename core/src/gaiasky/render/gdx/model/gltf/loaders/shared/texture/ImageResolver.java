/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.loaders.shared.texture;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.render.gdx.model.gltf.data.texture.GLTFImage;
import gaiasky.render.gdx.model.gltf.loaders.shared.data.DataFileResolver;

public class ImageResolver implements Disposable {
	
	private final Array<Pixmap> pixmaps = new Array<Pixmap>();
	
	private final DataFileResolver dataFileResolver;
	
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
