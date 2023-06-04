/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.ILineRenderable;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.view.LineView;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import org.lwjgl.opengl.GL30;

import java.util.List;

public class LinePrimitiveRenderer extends LineRenderSystem {
    protected static final Log logger = Logger.getLogger(LinePrimitiveRenderer.class);

    protected ICamera camera;
    protected Vector3 aux2;

    protected LineView view;

    protected ExtShaderProgram shaderProgram;

    public LinePrimitiveRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        view = new LineView();
        aux2 = new Vector3();
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
            Render render = (Render) r;
            view.setEntity(render.entity);

            view.render(this, camera, getAlpha(render));

            Gdx.gl.glLineWidth(view.getLineWidth() * 1.5f * Settings.settings.scene.lineWidth);

            for (int md = 0; md < meshIdx; md++) {
                MeshData meshDouble = meshes.get(md);
                meshDouble.mesh.setVertices(meshDouble.vertices, 0, meshDouble.vertexIdx);
                meshDouble.mesh.render(shaderProgram, getGLPrimitive(render));

                meshDouble.clear();
            }
        });
        shaderProgram.end();

        // Reset indices
        meshIdx = 1;
        curr = meshes.get(0);
    }

    protected int getGLPrimitive(Render r) {
        if (Mapper.verts.has(r.entity)) {
            return Mapper.verts.get(r.entity).glPrimitive;
        } else {
            return GL30.GL_LINES;
        }
    }

    /**
     * Breaks current line of points
     */
    public void breakLine() {

    }

    public void addPoint(ILineRenderable lr, double x, double y, double z, float r, float g, float b, float a) {
        // Check if 3 more indices fit
        if (curr.numVertices + 1 >= curr.capacity) {
            // Create new mesh data
            initVertices(meshIdx++);
        }

        color(r, g, b, a);
        vertex((float) x, (float) y, (float) z);
    }

    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, Color col) {
        addLinePostproc(x0, y0, z0, x1, y1, z1, col.r, col.g, col.b, col.a);
    }

    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, Color col0, Color col1) {
        addLinePostproc(x0, y0, z0, x1, y1, z1, col0.r, col0.g, col0.b, col0.a, col1.r, col1.g, col1.b, col1.a);
    }

    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b, float a) {
        addLinePostproc(x0, y0, z0, x1, y1, z1, r, g, b, a);
    }

    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, double r0, double g0, double b0, double a0, double r1, double g1, double b1, double a1) {
        addLinePostproc(x0, y0, z0, x1, y1, z1, r0, g0, b0, a0, r1, g1, b1, a1);
    }

    public void addLinePostproc(double x0, double y0, double z0, double x1, double y1, double z1, double r, double g, double b, double a) {
        // Check if 3 more indices fit
        if (curr.numVertices + 2 >= curr.capacity) {
            // Create new mesh data
            initVertices(meshIdx++);
        }

        color(r, g, b, a);
        vertex((float) x0, (float) y0, (float) z0);
        color(r, g, b, a);
        vertex((float) x1, (float) y1, (float) z1);
    }

    public void addLinePostproc(double x0, double y0, double z0, double x1, double y1, double z1, double r0, double g0, double b0, double a0, double r1, double g1, double b1, double a1) {
        // Check if 3 more indices fit
        if (curr.numVertices + 2 >= curr.capacity) {
            // Create new mesh data
            initVertices(meshIdx++);
        }

        color(r0, g0, b0, a0);
        vertex((float) x0, (float) y0, (float) z0);
        color(r1, g1, b1, a1);
        vertex((float) x1, (float) y1, (float) z1);
    }

}
