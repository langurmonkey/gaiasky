/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.glb;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.LittleEndianInputStream;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.util.gdx.model.gltf.data.GLTF;
import gaiasky.util.gdx.model.gltf.data.data.GLTFBufferView;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFImage;
import gaiasky.util.gdx.model.gltf.loaders.exceptions.GLTFIllegalException;
import gaiasky.util.gdx.model.gltf.loaders.exceptions.GLTFRuntimeException;
import gaiasky.util.gdx.model.gltf.loaders.shared.GLTFLoaderBase;
import gaiasky.util.gdx.model.gltf.loaders.shared.data.DataFileResolver;
import gaiasky.util.gdx.model.gltf.loaders.shared.texture.PixmapBinaryLoaderHack;
import net.jafama.FastMath;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BinaryDataFileResolver implements DataFileResolver
{
	private final ObjectMap<Integer, ByteBuffer> bufferMap = new ObjectMap<>();
	private GLTF glModel;
	
	@Override
	public void load(FileHandle file) {
		load(file.read());
	}
	
	public void load(byte[] bytes){
		load(new ByteArrayInputStream(bytes));
	}

	public void load(InputStream stream) {
		load(new LittleEndianInputStream(stream));
	}
	
	public void load(LittleEndianInputStream stream) {
		try {
			loadInternal(stream);
			
		} catch (IOException e) {
			throw new GLTFRuntimeException(e);
		}
	}
	
	private void loadInternal(LittleEndianInputStream stream) throws IOException {
		long magic = stream.readInt(); // & 0xFFFFFFFFL;
		if(magic != 0x46546C67) throw new GLTFIllegalException("bad magic");
		int version = stream.readInt();
		if(version != 2) throw new GLTFIllegalException("bad version");
		long length = stream.readInt();// & 0xFFFFFFFFL;
		
		byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
		String jsonData = null;
		for(int i=12 ; i<length ; ){
			int chunkLen = stream.readInt();
			int chunkType = stream.readInt();
			i += 8;			// chunkLen % 4;
			if(chunkType == 0x4E4F534A){
				byte[] data = new byte[(int)chunkLen];
				stream.readFully(data, 0, chunkLen);
				jsonData = new String(data);
			}else if(chunkType == 0x004E4942){
				ByteBuffer bufferData = ByteBuffer.allocate(chunkLen);
				bufferData.order(ByteOrder.LITTLE_ENDIAN);
				int bytesToRead = chunkLen;
				int bytesRead;
				while (bytesToRead > 0 && (bytesRead = stream.read(buffer, 0, FastMath.min(buffer.length, bytesToRead))) != -1) {
					bufferData.put(buffer, 0, bytesRead);
					bytesToRead -= bytesRead;
				}
				if(bytesToRead > 0) throw new GLTFIllegalException("premature end of file");
				bufferData.flip();
				bufferMap.put(bufferMap.size, bufferData);
			}else{
				Gdx.app.log(GLTFLoaderBase.TAG, "skip buffer type " + chunkType);
				if(chunkLen > 0){
					stream.skip(chunkLen);
				}
			}
			i += chunkLen;
		}
		
		glModel = new Json().fromJson(GLTF.class, jsonData);
	}
	
	@Override
	public GLTF getRoot() {
		return glModel;
	}

	@Override
	public ByteBuffer getBuffer(int buffer) {
		return bufferMap.get(buffer);
	}
	
	@Override
	public Pixmap load(GLTFImage glImage) {
		if(glImage.bufferView != null){
			GLTFBufferView bufferView = glModel.bufferViews.get(glImage.bufferView);
			ByteBuffer buffer = bufferMap.get(bufferView.buffer);
			buffer.position(bufferView.byteOffset);
			byte [] data = new byte[bufferView.byteLength];
			buffer.get(data);
			return PixmapBinaryLoaderHack.load(data, 0, data.length);
		}else{
			throw new GLTFIllegalException("GLB image should have bufferView");
		}
	}
}