/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.tree.OctreeNode;
import org.lwjgl.opengl.GL30;

import java.util.List;

public class LineQuadstripRenderer extends LinePrimitiveRenderer {
    protected static final Log logger = Logger.getLogger(LinePrimitiveRenderer.class);

    final static double baseWidthAngle = Math.toRadians(.13);
    final static double baseWidthAngleTan = Math.tan(baseWidthAngle);
    protected ICamera camera;

    public LineQuadstripRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        shaderProgram = getShaderProgram();
        shaderProgram.begin();
        shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
        shaderProgram.setUniformf("u_alpha", 1f);

        // Rel, grav, z-buffer
        addEffectsUniforms(shaderProgram, camera);

        this.camera = camera;
        renderables.forEach(r -> {
            int primitive = GL30.GL_LINES;
            if (r instanceof Render render) {
                view.setEntity(render.entity);

                view.render(this, camera, getAlpha(render));
                primitive = getGLPrimitive(render);
            } else if (r instanceof OctreeNode octant) {
                octant.render(this, camera, getAlpha(octant));
            }

            shaderProgram.setUniformf("u_lineWidthTan", (float) (view.getLineWidth() * 0.8f * baseWidthAngleTan * Settings.settings.scene.renderer.line.width * camera.getFovFactor()));

            for (int md = 0; md < meshIdx; md++) {
                MeshData meshDouble = meshes.get(md);
                meshDouble.mesh.setVertices(meshDouble.vertices, 0, meshDouble.vertexIdx);
                meshDouble.mesh.render(shaderProgram, primitive);

                meshDouble.clear();
            }
        });
        shaderProgram.end();

        // Reset indices
        meshIdx = 1;
        curr = meshes.get(0);
    }

}
