/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.animation;

import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.model.IntAnimation;
import gaiasky.util.gdx.model.IntAnimationController;
import gaiasky.util.gdx.model.IntAnimationController.AnimationDesc;
import gaiasky.util.gdx.model.gltf.scene3d.scene.Scene;

public class AnimationsPlayer {

	private final Scene scene;
	
	private final Array<IntAnimationController> controllers = new Array<>();

	public AnimationsPlayer(Scene scene) {
		this.scene = scene;
	}
	
	public void addAnimations(Array<AnimationDesc> animations){
		for(AnimationDesc animation : animations){
			addAnimation(animation);
		}
	}
	public void addAnimation(AnimationDesc animation){
		AnimationControllerHack c = new AnimationControllerHack(scene.modelInstance);
		c.calculateTransforms = false;
		c.setAnimationDesc(animation);
		controllers.add(c);
	}
	public void removeAnimation(IntAnimation animation){
		for(int i=controllers.size-1 ; i>=0 ; i--){
			if(controllers.get(i).current != null && controllers.get(i).current.animation == animation){
				controllers.removeIndex(i);
			}
		}
	}
	
	public void clearAnimations(){
		controllers.clear();
		if(scene.animationController != null){
			scene.animationController.setAnimation(null);
		}
	}
	
	public void playAll(){
		playAll(false);
	}
	public void loopAll(){
		playAll(true);
	}
	public void playAll(boolean loop){
		clearAnimations();
		for(int i=0, n=scene.modelInstance.animations.size ; i<n ; i++){
			AnimationControllerHack c = new AnimationControllerHack(scene.modelInstance);
			c.calculateTransforms = false;
			c.setAnimation(scene.modelInstance.animations.get(i), loop ? -1 : 1);
			controllers.add(c);
		}
	}
	
	public void stopAll(){
		clearAnimations();
	}
	
	public void update(float delta){
		if(controllers.size > 0){
			for(IntAnimationController controller : controllers){
				controller.update(delta);
			}
			scene.modelInstance.calculateTransforms();
		}else{
			if(scene.animationController != null){
				scene.animationController.update(delta);
			}
		}
	}

}
