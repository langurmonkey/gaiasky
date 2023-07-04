/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.shaders;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.PBRVertexAttributes;
import gaiasky.util.gdx.model.gltf.scene3d.model.WeightVector;
import gaiasky.util.gdx.shader.DepthIntShader;
import gaiasky.util.gdx.shader.attribute.Attributes;

public class PBRDepthShader extends DepthIntShader
{
	public final long morphTargetsMask;
	
	// morph targets
	private int u_morphTargets1;
	private int u_morphTargets2;
	
	public PBRDepthShader(IntRenderable renderable, Config config, String prefix) {
		super(renderable, config, prefix);
		this.morphTargetsMask = computeMorphTargetsMask(renderable);
	}
	
	protected long computeMorphTargetsMask(IntRenderable renderable){
		int morphTargetsFlag = 0;
		VertexAttributes vertexAttributes = renderable.meshPart.mesh.getVertexAttributes();
		final int n = vertexAttributes.size();
		for (int i = 0; i < n; i++) {
			final VertexAttribute attr = vertexAttributes.get(i);
			if (attr.usage == PBRVertexAttributes.Usage.PositionTarget) morphTargetsFlag |= (1 << attr.unit);
		}
		return morphTargetsFlag;
	}
	
	@Override
	public boolean canRender(IntRenderable renderable) {
		
		if(this.morphTargetsMask != computeMorphTargetsMask(renderable)) return false;
		
		return super.canRender(renderable);
	}
	
	@Override
	public void init() {
		super.init();
		
		u_morphTargets1 = program.fetchUniformLocation("u_morphTargets1", false);
		u_morphTargets2 = program.fetchUniformLocation("u_morphTargets2", false);

	}
	
	@Override
	public void render(IntRenderable renderable, Attributes combinedAttributes) {
		
		if(u_morphTargets1 >= 0){
			if(renderable.userData instanceof WeightVector){
				WeightVector weightVector = (WeightVector)renderable.userData;
				program.setUniformf(u_morphTargets1, weightVector.get(0), weightVector.get(1), weightVector.get(2), weightVector.get(3));
			}else{
				program.setUniformf(u_morphTargets1, 0, 0, 0, 0);
			}
		}
		if(u_morphTargets2 >= 0){
			if(renderable.userData instanceof WeightVector){
				WeightVector weightVector = (WeightVector)renderable.userData;
				program.setUniformf(u_morphTargets2, weightVector.get(4), weightVector.get(5), weightVector.get(6), weightVector.get(7));
			}else{
				program.setUniformf(u_morphTargets2, 0, 0, 0, 0);
			}
		}
		
		super.render(renderable, combinedAttributes);
	}

}
