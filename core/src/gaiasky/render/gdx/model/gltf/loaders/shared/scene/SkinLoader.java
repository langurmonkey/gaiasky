/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.loaders.shared.scene;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import gaiasky.render.gdx.model.IntNode;
import gaiasky.render.gdx.model.IntNodePart;
import gaiasky.render.gdx.model.gltf.data.scene.GLTFNode;
import gaiasky.render.gdx.model.gltf.data.scene.GLTFSkin;
import gaiasky.render.gdx.model.gltf.loaders.exceptions.GLTFIllegalException;
import gaiasky.render.gdx.model.gltf.loaders.shared.data.DataResolver;
import net.jafama.FastMath;

import java.nio.FloatBuffer;

public class SkinLoader {
	
	private int maxBones;

	public void load(Array<GLTFSkin> glSkins, Array<GLTFNode> glNodes, NodeResolver nodeResolver, DataResolver dataResolver) {
		if(glNodes != null){
			for(int i=0 ; i<glNodes.size ; i++){
				GLTFNode glNode = glNodes.get(i);
				if(glNode.skin != null){
					GLTFSkin glSkin = glSkins.get(glNode.skin);
					load(glSkin, glNode, nodeResolver.get(i), nodeResolver, dataResolver);
				}
			}
		}
	}

	private void load(GLTFSkin glSkin, GLTFNode glNode, IntNode node, NodeResolver nodeResolver, DataResolver dataResolver){
		
		Array<Matrix4> ibms = new Array<Matrix4>();
		Array<Integer> joints = new Array<Integer>();
		
		int bonesCount = glSkin.joints.size;
		maxBones = FastMath.max(maxBones, bonesCount);
		
		FloatBuffer floatBuffer = dataResolver.getBufferFloat(glSkin.inverseBindMatrices);
		
		for(int i=0 ; i<bonesCount ; i++){
			float [] matrixData = new float[16];
			floatBuffer.get(matrixData);
			ibms.add(new Matrix4(matrixData));
		}
		joints.addAll(glSkin.joints);
		
		if(ibms.size > 0){
			for(int i=0 ; i<node.parts.size ; i++){
				IntNodePart nodePart = node.parts.get(i);
				if(nodePart.bones != null){
					// special case when the same mesh is used by several skins.
					// in this case, we need to clone the node part
					IntNodePart newNodPart = new IntNodePart();
					newNodPart.material = nodePart.material;
					newNodPart.meshPart = nodePart.meshPart;
					node.parts.set(i, nodePart = newNodPart);
				}
				nodePart.bones = new Matrix4[ibms.size];
				nodePart.invBoneBindTransforms = new ArrayMap<>();
				for(int n=0 ; n<joints.size ; n++){
					nodePart.bones[n] = new Matrix4().idt();
					int nodeIndex = joints.get(n);
					IntNode key = nodeResolver.get(nodeIndex);
					if(key == null) throw new GLTFIllegalException("node not found for bone: " + nodeIndex);
					nodePart.invBoneBindTransforms.put(key, ibms.get(n));
				}
			}
		}
	}

	public int getMaxBones() {
		return maxBones;
	}

	
}
