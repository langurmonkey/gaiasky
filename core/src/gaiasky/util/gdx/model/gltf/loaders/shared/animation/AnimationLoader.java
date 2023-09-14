/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.shared.animation;

import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.util.gdx.model.IntAnimation;
import gaiasky.util.gdx.model.IntNode;
import gaiasky.util.gdx.model.IntNodeAnimation;
import gaiasky.util.gdx.model.gltf.data.animation.GLTFAnimation;
import gaiasky.util.gdx.model.gltf.data.animation.GLTFAnimationChannel;
import gaiasky.util.gdx.model.gltf.data.animation.GLTFAnimationSampler;
import gaiasky.util.gdx.model.gltf.data.data.GLTFAccessor;
import gaiasky.util.gdx.model.gltf.loaders.exceptions.GLTFUnsupportedException;
import gaiasky.util.gdx.model.gltf.loaders.shared.GLTFTypes;
import gaiasky.util.gdx.model.gltf.loaders.shared.data.DataResolver;
import gaiasky.util.gdx.model.gltf.loaders.shared.scene.NodeResolver;
import gaiasky.util.gdx.model.gltf.scene3d.animation.NodeAnimationHack;
import gaiasky.util.gdx.model.gltf.scene3d.model.*;

public class AnimationLoader {
	
	public final Array<IntAnimation> animations = new Array<>();
	
	public void load(Array<GLTFAnimation> glAnimations, NodeResolver nodeResolver, DataResolver dataResolver) {
		
		if(glAnimations != null){
			for(int i=0 ; i<glAnimations.size ; i++){
				GLTFAnimation glAnimation = glAnimations.get(i);
				
				IntAnimation animation = load(glAnimation, nodeResolver, dataResolver);
				animation.id = glAnimation.name == null ? "animation" + i : glAnimation.name;
				
				animations.add(animation);
			}
		}
	}
	
	private IntAnimation load(GLTFAnimation glAnimation, NodeResolver nodeResolver, DataResolver dataResolver){
		
		ObjectMap<IntNode, IntNodeAnimation> animMap = new ObjectMap<>();
		
		IntAnimation animation = new IntAnimation();
		
		for(GLTFAnimationChannel glChannel : glAnimation.channels){
			GLTFAnimationSampler glSampler = glAnimation.samplers.get(glChannel.sampler);
			IntNode node = nodeResolver.get(glChannel.target.node);
			
			IntNodeAnimation nodeAnimation = animMap.get(node);
			if(nodeAnimation == null){
				nodeAnimation = new NodeAnimationHack();
				nodeAnimation.node = node;
				animMap.put(node, nodeAnimation);
				animation.nodeAnimations.add(nodeAnimation);
			}
			
			float[] inputData = dataResolver.readBufferFloat(glSampler.input);
			float[] outputData = dataResolver.readBufferFloat(glSampler.output);

			final Interpolation interpolation = GLTFTypes.mapInterpolation(glSampler.interpolation);
			
			// case of cubic spline, we skip anchor vectors if cubic is disabled.
			int dataOffset = 0;
			int dataStride = 1;
			if(interpolation == Interpolation.CUBICSPLINE){
				dataOffset = 1;
				dataStride = 3;
			}
			
			GLTFAccessor inputAccessor = dataResolver.getAccessor(glSampler.input);
			animation.duration = Math.max(animation.duration, inputAccessor.max[0]);
			
			String property = glChannel.target.path;
			if("translation".equals(property)){
				
				((NodeAnimationHack)nodeAnimation).translationMode = interpolation;
				
				nodeAnimation.translation = new Array<NodeKeyframe<Vector3>>();
				if(interpolation == Interpolation.CUBICSPLINE){
					// copy first frame if not at zero time
					if(inputData[0] > 0){
						nodeAnimation.translation.add(new NodeKeyframe<Vector3>(0, GLTFTypes.map(new CubicVector3(), outputData, 0)));
					}
					for(int k=0 ; k<inputData.length ; k++){
						nodeAnimation.translation.add(new NodeKeyframe<Vector3>(inputData[k], GLTFTypes.map(new CubicVector3(), outputData, k*dataStride*3)));
					}
				}else{
					// copy first frame if not at zero time
					if(inputData[0] > 0){
						nodeAnimation.translation.add(new NodeKeyframe<Vector3>(0, GLTFTypes.map(new Vector3(), outputData, dataOffset * 3)));
					}
					for(int k=0 ; k<inputData.length ; k++){
						nodeAnimation.translation.add(new NodeKeyframe<Vector3>(inputData[k], GLTFTypes.map(new Vector3(), outputData, (dataOffset+(k*dataStride))*3)));
					}
				}
			}else if("rotation".equals(property)){
				
				((NodeAnimationHack)nodeAnimation).rotationMode = interpolation;
				
				nodeAnimation.rotation = new Array<NodeKeyframe<Quaternion>>();
				if(interpolation == Interpolation.CUBICSPLINE){
					// copy first frame if not at zero time
					if(inputData[0] > 0){
						nodeAnimation.rotation.add(new NodeKeyframe<Quaternion>(0, GLTFTypes.map(new CubicQuaternion(), outputData, 0)));
					}
					for(int k=0 ; k<inputData.length ; k++){
						nodeAnimation.rotation.add(new NodeKeyframe<Quaternion>(inputData[k], GLTFTypes.map(new CubicQuaternion(), outputData, k*dataStride*4)));
					}
				}else{
					// copy first frame if not at zero time
					if(inputData[0] > 0){
						nodeAnimation.rotation.add(new NodeKeyframe<Quaternion>(0, GLTFTypes.map(new Quaternion(), outputData, dataOffset * 4)));
					}
					for(int k=0 ; k<inputData.length ; k++){
						nodeAnimation.rotation.add(new NodeKeyframe<Quaternion>(inputData[k], GLTFTypes.map(new Quaternion(), outputData, (dataOffset+(k*dataStride))*4)));
					}
				}
			}else if("scale".equals(property)){
				
				((NodeAnimationHack)nodeAnimation).scalingMode = interpolation;
				
				nodeAnimation.scaling = new Array<NodeKeyframe<Vector3>>();
				if(interpolation == Interpolation.CUBICSPLINE){
					// copy first frame if not at zero time
					if(inputData[0] > 0){
						nodeAnimation.scaling.add(new NodeKeyframe<Vector3>(0, GLTFTypes.map(new CubicVector3(), outputData, 0)));
					}
					for(int k=0 ; k<inputData.length ; k++){
						nodeAnimation.scaling.add(new NodeKeyframe<Vector3>(inputData[k], GLTFTypes.map(new CubicVector3(), outputData, k*dataStride*3)));
					}
				}else{
					// copy first frame if not at zero time
					if(inputData[0] > 0){
						nodeAnimation.scaling.add(new NodeKeyframe<Vector3>(0, GLTFTypes.map(new Vector3(), outputData, dataOffset * 3)));
					}
					for(int k=0 ; k<inputData.length ; k++){
						nodeAnimation.scaling.add(new NodeKeyframe<Vector3>(inputData[k], GLTFTypes.map(new Vector3(), outputData, (dataOffset+(k*dataStride))*3)));
					}
				}
			}else if("weights".equals(property)){
				
				((NodeAnimationHack)nodeAnimation).weightsMode = interpolation;
				
				NodeAnimationHack np = (NodeAnimationHack)nodeAnimation;
				int nbWeights = ((NodePlus)node).weights.count;
				np.weights = new Array<NodeKeyframe<WeightVector>>();
				if(interpolation == Interpolation.CUBICSPLINE){
					// copy first frame if not at zero time
					if(inputData[0] > 0){
						np.weights.add(new NodeKeyframe<WeightVector>(0, GLTFTypes.map(new CubicWeightVector(nbWeights), outputData, 0)));
					}
					for(int k=0 ; k<inputData.length ; k++){
						np.weights.add(new NodeKeyframe<WeightVector>(inputData[k], GLTFTypes.map(new CubicWeightVector(nbWeights), outputData, k*dataStride*nbWeights)));
					}
				}else{
					// copy first frame if not at zero time
					if(inputData[0] > 0){
						np.weights.add(new NodeKeyframe<WeightVector>(0, GLTFTypes.map(new WeightVector(nbWeights), outputData, dataOffset * nbWeights)));
					}
					for(int k=0 ; k<inputData.length ; k++){
						np.weights.add(new NodeKeyframe<WeightVector>(inputData[k], GLTFTypes.map(new WeightVector(nbWeights), outputData, (dataOffset+(k*dataStride))*nbWeights)));
					}
				}
			}else{
				throw new GLTFUnsupportedException("unsupported " + property);
			}
		}
		
		return animation;
	}
	
}
