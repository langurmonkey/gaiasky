/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.shared.data;

import gaiasky.util.gdx.model.gltf.data.GLTF;
import gaiasky.util.gdx.model.gltf.data.data.GLTFAccessor;
import gaiasky.util.gdx.model.gltf.data.data.GLTFBufferView;
import gaiasky.util.gdx.model.gltf.loaders.shared.GLTFTypes;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class DataResolver {
	
	private final GLTF glModel;
	private final DataFileResolver dataFileResolver;
	
	public DataResolver(GLTF glModel, DataFileResolver dataFileResolver) {
		super();
		this.glModel = glModel;
		this.dataFileResolver = dataFileResolver;
	}
	
	public GLTFAccessor getAccessor(int accessorID) {
		return glModel.accessors.get(accessorID);
	}

	public float[] readBufferFloat(int accessorID) {
		GLTFAccessor accessor = glModel.accessors.get(accessorID);
		GLTFBufferView bufferView = glModel.bufferViews.get(accessor.bufferView);
		ByteBuffer bytes = dataFileResolver.getBuffer(bufferView.buffer);
		bytes.position(bufferView.byteOffset + accessor.byteOffset);
		float [] data = new float[GLTFTypes.accessorSize(accessor)/4];
		
		int nbFloatsPerVertex = GLTFTypes.accessorTypeSize(accessor);
		int nbBytesToSkip = 0;
		if(bufferView.byteStride != null) nbBytesToSkip = bufferView.byteStride - nbFloatsPerVertex * 4;
		if(nbBytesToSkip == 0){
			bytes.asFloatBuffer().get(data);
		}else{
			for(int i=0 ; i<accessor.count ; i++){
				for(int j=0 ; j<nbFloatsPerVertex ; j++){
					data[i*nbFloatsPerVertex+j] = bytes.getFloat();
				}
				// skip remaining bytes
				bytes.position(bytes.position() + nbBytesToSkip);
			}
		}
		return data;
	}
	
	public int[] readBufferUByte(int accessorID) {
		GLTFAccessor accessor = glModel.accessors.get(accessorID);
		GLTFBufferView bufferView = glModel.bufferViews.get(accessor.bufferView);
		ByteBuffer bytes = dataFileResolver.getBuffer(bufferView.buffer);
		bytes.position(bufferView.byteOffset + accessor.byteOffset);
		int [] data = new int[GLTFTypes.accessorSize(accessor)];
		
		int nbBytesPerVertex = GLTFTypes.accessorTypeSize(accessor);
		int nbBytesToSkip = 0;
		if(bufferView.byteStride != null) nbBytesToSkip = bufferView.byteStride - nbBytesPerVertex;
		if(nbBytesToSkip == 0){
			for(int i=0 ; i<data.length ; i++){
				data[i] = bytes.get() & 0xFF;
			}
		}else{
			for(int i=0 ; i<accessor.count ; i++){
				for(int j=0 ; j<nbBytesPerVertex ; j++){
					data[i*nbBytesPerVertex+j] = bytes.get() & 0xFF;
				}
				// skip remaining bytes
				bytes.position(bytes.position() + nbBytesToSkip);
			}
		}
		return data;
	}
	
	public int[] readBufferUShort(int accessorID) {
		GLTFAccessor accessor = glModel.accessors.get(accessorID);
		GLTFBufferView bufferView = glModel.bufferViews.get(accessor.bufferView);
		ByteBuffer bytes = dataFileResolver.getBuffer(bufferView.buffer);
		bytes.position(bufferView.byteOffset + accessor.byteOffset);
		int [] data = new int[GLTFTypes.accessorSize(accessor)/2];
		
		int nbShortsPerVertex = GLTFTypes.accessorTypeSize(accessor);
		int nbBytesToSkip = 0;
		if(bufferView.byteStride != null) nbBytesToSkip = bufferView.byteStride - nbShortsPerVertex * 2;
		if(nbBytesToSkip == 0){
			ShortBuffer shorts = bytes.asShortBuffer();
			for(int i=0 ; i<data.length ; i++){
				data[i] = shorts.get() & 0xFFFF;
			}
		}else{
			for(int i=0 ; i<accessor.count ; i++){
				for(int j=0 ; j<nbShortsPerVertex ; j++){
					data[i*nbShortsPerVertex+j] = bytes.getShort() & 0xFFFF;
				}
				// skip remaining bytes
				bytes.position(bytes.position() + nbBytesToSkip);
			}
		}
		return data;
	}
	
	public float[] readBufferUShortAsFloat(int accessorID) {
		int[] intBuffer = readBufferUShort(accessorID);
		float[] floatBuffer = new float[intBuffer.length];
		for (int i = 0; i < intBuffer.length; i++) {
			floatBuffer[i] = intBuffer[i] / 65535f;
		}
		return floatBuffer;
	}

	public float[] readBufferUByteAsFloat(int accessorID) {
		int[] intBuffer = readBufferUByte(accessorID);
		float[] floatBuffer = new float[intBuffer.length];
		for (int i = 0; i < intBuffer.length; i++) {
			floatBuffer[i] = intBuffer[i] / 255f;
		}
		return floatBuffer;
	}

	public FloatBuffer getBufferFloat(int accessorID) {
		return getBufferFloat(glModel.accessors.get(accessorID));
	}

	public GLTFBufferView getBufferView(int bufferViewID) {
		return glModel.bufferViews.get(bufferViewID);
	}

	public FloatBuffer getBufferFloat(GLTFAccessor glAccessor) {
		return getBufferByte(glAccessor).asFloatBuffer();
	}

	public IntBuffer getBufferInt(GLTFAccessor glAccessor) {
		return getBufferByte(glAccessor).asIntBuffer();
	}

	public ShortBuffer getBufferShort(GLTFAccessor glAccessor) {
		return getBufferByte(glAccessor).asShortBuffer();
	}

	public ByteBuffer getBufferByte(GLTFAccessor glAccessor) {
		GLTFBufferView bufferView = glModel.bufferViews.get(glAccessor.bufferView);
		ByteBuffer bytes = dataFileResolver.getBuffer(bufferView.buffer);
		bytes.position(bufferView.byteOffset + glAccessor.byteOffset);
		return bytes;
	}

	public ByteBuffer getBufferByte(GLTFBufferView bufferView) {
		ByteBuffer bytes = dataFileResolver.getBuffer(bufferView.buffer);
		bytes.position(bufferView.byteOffset);
		return bytes;
	}
}
