/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.IRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.FovCamera;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.color.Colormap;
import gaiasky.util.comp.DistToCameraComparator;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;

/**
 * Adds some utils to build quads as a couple of triangles. This should
 * be used by point clouds that render their particles as
 * GL_TRIANGLES.
 */
public abstract class PointCloudTriRenderSystem extends PointCloudRenderSystem implements IObserver {

    // Positions per vertex index
    protected Pair<Float, Float>[] vertPos;
    // UV coordinates per vertex index (0,1,2,4)
    protected Pair<Float, Float>[] vertUV;

    public PointCloudTriRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(rg, alphas, shaders);

        vertPos = new Pair[4];
        vertPos[0] = new Pair<>(1f, 1f);
        vertPos[1] = new Pair<>(1f, -1f);
        vertPos[2] = new Pair<>(-1f, -1f);
        vertPos[3] = new Pair<>(-1f, 1f);

        vertUV = new Pair[4];
        vertUV[0] = new Pair<>(1f, 1f);
        vertUV[1] = new Pair<>(1f, 0f);
        vertUV[2] = new Pair<>(0f, 0f);
        vertUV[3] = new Pair<>(0f, 1f);
    }

    protected void index(int idx) {
        tempIndices[curr.indexIdx++] = idx;
    }

    /**
     * Adds the indices to make two triangles into
     * a quad, given the four vertices in vertPos.
     * @param current The current mesh.
     */
    protected void quadIndices(MeshData current) {
        index(current.numVertices - 4);
        index(current.numVertices - 3);
        index(current.numVertices - 2);

        index(current.numVertices - 2);
        index(current.numVertices - 1);
        index(current.numVertices - 4);
    }
}
