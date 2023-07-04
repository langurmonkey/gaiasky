/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.model;

import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.model.*;
import gaiasky.util.gdx.model.gltf.scene3d.animation.NodeAnimationHack;

public class ModelInstanceHack extends IntModelInstance {

    public ModelInstanceHack(IntModel model) {
        super(model);
    }

    public ModelInstanceHack(IntModel model, Matrix4 localTransform) {
        super(model, localTransform);
    }

    public ModelInstanceHack(IntModel model, final String... rootNodeIds) {
        super(model, rootNodeIds);
    }

    public void copyAnimation(IntAnimation anim, boolean shareKeyframes) {
        IntAnimation animation = new IntAnimation();
        animation.id = anim.id;
        animation.duration = anim.duration;
        for (final IntNodeAnimation nanim : anim.nodeAnimations) {
            final IntNode node = getNode(nanim.node.id);
            if (node == null) continue;
            NodeAnimationHack nodeAnim = new NodeAnimationHack();
            nodeAnim.node = node;

            nodeAnim.translationMode = ((NodeAnimationHack) nanim).translationMode;
            nodeAnim.rotationMode = ((NodeAnimationHack) nanim).rotationMode;
            nodeAnim.scalingMode = ((NodeAnimationHack) nanim).scalingMode;
            nodeAnim.weightsMode = ((NodeAnimationHack) nanim).weightsMode;

            if (shareKeyframes) {
                nodeAnim.translation = nanim.translation;
                nodeAnim.rotation = nanim.rotation;
                nodeAnim.scaling = nanim.scaling;
                nodeAnim.weights = ((NodeAnimationHack) nanim).weights;
            } else {
                if (nanim.translation != null) {
                    nodeAnim.translation = new Array<>();
                    for (final NodeKeyframe<Vector3> kf : nanim.translation)
                        nodeAnim.translation.add(new NodeKeyframe<>(kf.keytime, kf.value));
                }
                if (nanim.rotation != null) {
                    nodeAnim.rotation = new Array<>();
                    for (final NodeKeyframe<Quaternion> kf : nanim.rotation)
                        nodeAnim.rotation.add(new NodeKeyframe<>(kf.keytime, kf.value));
                }
                if (nanim.scaling != null) {
                    nodeAnim.scaling = new Array<>();
                    for (final NodeKeyframe<Vector3> kf : nanim.scaling)
                        nodeAnim.scaling.add(new NodeKeyframe<>(kf.keytime, kf.value));
                }
                if (((NodeAnimationHack) nanim).weights != null) {
                    ((NodeAnimationHack) nanim).weights = new Array<>();
                    for (final NodeKeyframe<WeightVector> kf : ((NodeAnimationHack) nanim).weights)
                        ((NodeAnimationHack) nanim).weights.add(new NodeKeyframe<>(kf.keytime, kf.value));
                }
            }
            if (nodeAnim.translation != null || nodeAnim.rotation != null || nodeAnim.scaling != null || ((NodeAnimationHack) nanim).weights != null)
                animation.nodeAnimations.add(nodeAnim);
        }
        if (animation.nodeAnimations.size > 0) animations.add(animation);
    }

    @Override
    public IntRenderable getRenderable(final IntRenderable out, final IntNode node, final IntNodePart nodePart) {
        super.getRenderable(out, node, nodePart);
        if (nodePart instanceof NodePartPlus) {
            out.userData = ((NodePartPlus) nodePart).morphTargets;
        }
        return out;
    }


}
