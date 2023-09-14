/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.model.data;

import com.badlogic.gdx.graphics.g3d.model.data.ModelAnimation;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class IntModelData {
    public final short[] version = new short[2];
    public final Array<IntModelMesh> meshes = new Array<>();
    public final Array<OwnModelMaterial> materials = new Array<>();
    public final Array<IntModelNode> nodes = new Array<>();
    public final Array<ModelAnimation> animations = new Array<>();
    public String id;

    public void addMesh(IntModelMesh mesh) {
        for (IntModelMesh other : meshes) {
            if (other.id.equals(mesh.id)) {
                throw new GdxRuntimeException("Mesh with id '" + other.id + "' already in model");
            }
        }
        meshes.add(mesh);
    }
}
