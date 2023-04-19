/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.shader.attribute.BlendingAttribute;

import java.util.Comparator;
import java.util.Objects;

public class DefaultIntRenderableSorter implements IntRenderableSorter, Comparator<IntRenderable> {
    private final Vector3 tmpV1 = new Vector3();
    private final Vector3 tmpV2 = new Vector3();
    private Camera camera;

    @Override
    public void sort(final Camera camera, final Array<IntRenderable> renderables) {
        this.camera = camera;
        renderables.sort(this);
    }

    private void getTranslation(Matrix4 worldTransform, Vector3 center, Vector3 output) {
        if (center.isZero())
            worldTransform.getTranslation(output);
        else if (!worldTransform.hasRotationOrScaling())
            worldTransform.getTranslation(output).add(center);
        else
            output.set(center).mul(worldTransform);
    }

    @Override
    public int compare(final IntRenderable o1, final IntRenderable o2) {
        final boolean b1 = o1.material.has(BlendingAttribute.Type) && ((BlendingAttribute) Objects.requireNonNull(o1.material.get(BlendingAttribute.Type))).blended;
        final boolean b2 = o2.material.has(BlendingAttribute.Type) && ((BlendingAttribute) Objects.requireNonNull(o2.material.get(BlendingAttribute.Type))).blended;
        if (b1 != b2)
            return b1 ? 1 : -1;
        getTranslation(o1.worldTransform, o1.meshPart.center, tmpV1);
        getTranslation(o2.worldTransform, o2.meshPart.center, tmpV2);
        final float dst = (int) (1000f * camera.position.dst2(tmpV1)) - (int) (1000f * camera.position.dst2(tmpV2));
        final int result = dst < 0 ? -1 : (dst > 0 ? 1 : 0);
        return b1 ? -result : result;
    }
}
