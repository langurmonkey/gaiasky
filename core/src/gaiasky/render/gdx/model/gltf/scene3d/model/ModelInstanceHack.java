/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.model;

import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.gdx.IntRenderable;
import gaiasky.render.gdx.model.*;
import gaiasky.render.gdx.model.gltf.scene3d.animation.NodeAnimationHack;

public class ModelInstanceHack extends IntModelInstance {

    public ModelInstanceHack(IntModel model) {
        super(model);
    }

    public ModelInstanceHack(IntModel model, Matrix4 localTransform) {
        super(model, localTransform);
    }

    public ModelInstanceHack(IntModel model, String... rootNodeIds) {
        super(model, rootNodeIds);
    }

    public void copyAnimation(IntAnimation anim, boolean shareKeyframes) {
        IntAnimation animation = new IntAnimation();
        animation.id = anim.id;
        animation.duration = anim.duration;
        for (IntNodeAnimation nanim : anim.nodeAnimations) {
            IntNode node = getNode(nanim.node.id);
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
                    for (NodeKeyframe<Vector3> kf : nanim.translation)
                        nodeAnim.translation.add(new NodeKeyframe<>(kf.keytime, kf.value));
                }
                if (nanim.rotation != null) {
                    nodeAnim.rotation = new Array<>();
                    for (NodeKeyframe<Quaternion> kf : nanim.rotation)
                        nodeAnim.rotation.add(new NodeKeyframe<>(kf.keytime, kf.value));
                }
                if (nanim.scaling != null) {
                    nodeAnim.scaling = new Array<>();
                    for (NodeKeyframe<Vector3> kf : nanim.scaling)
                        nodeAnim.scaling.add(new NodeKeyframe<>(kf.keytime, kf.value));
                }
                if (((NodeAnimationHack) nanim).weights != null) {
                    ((NodeAnimationHack) nanim).weights = new Array<>();
                    for (NodeKeyframe<WeightVector> kf : ((NodeAnimationHack) nanim).weights)
                        ((NodeAnimationHack) nanim).weights.add(new NodeKeyframe<>(kf.keytime, kf.value));
                }
            }
            if (nodeAnim.translation != null || nodeAnim.rotation != null || nodeAnim.scaling != null || ((NodeAnimationHack) nanim).weights != null)
                animation.nodeAnimations.add(nodeAnim);
        }
        if (animation.nodeAnimations.size > 0) animations.add(animation);
    }

    @Override
    public IntRenderable getRenderable(IntRenderable out, IntNode node, IntNodePart nodePart) {
        super.getRenderable(out, node, nodePart);
        if (nodePart instanceof NodePartPlus) {
            out.userData = ((NodePartPlus) nodePart).morphTargets;
        }
        return out;
    }


}
