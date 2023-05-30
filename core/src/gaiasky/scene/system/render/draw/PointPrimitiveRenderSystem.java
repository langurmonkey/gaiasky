/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IPointRenderable;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.ImmediateModeRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.view.PointView;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.Vector3d;
import org.lwjgl.opengl.GL30;

import java.util.List;

public class PointPrimitiveRenderSystem extends ImmediateModeRenderSystem {
    protected static final Log logger = Logger.getLogger(PointPrimitiveRenderSystem.class);

    private final PointView pointView;
    private final int glType;
    private int sizeOffset;
    private final Vector3d D31 = new Vector3d();

    public PointPrimitiveRenderSystem(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        this.pointView = new PointView();
        this.glType = GL20.GL_POINTS;
    }

    @Override
    protected void initShaderProgram() {
        Gdx.gl.glEnable(GL30.GL_VERTEX_PROGRAM_POINT_SIZE);
    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
        initVertices(meshIdx++);
    }

    private void initVertices(int index) {
        if (index >= meshes.size) {
            meshes.setSize(index + 1);
        }
        if (meshes.get(index) == null) {
            if (index > 0)
                logger.info("Capacity too small, creating new meshdata: " + curr.capacity);
            curr = new MeshData();
            meshes.set(index, curr);

            curr.capacity = 1000;

            VertexAttribute[] attribs = buildVertexAttributes();
            curr.mesh = new IntMesh(false, curr.capacity, 0, attribs);

            curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
            curr.vertices = new float[curr.capacity * curr.vertexSize];
            curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
            sizeOffset = curr.mesh.getVertexAttribute(Usage.Generic).offset / 4;
        } else {
            curr = meshes.get(index);
        }
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ExtShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Generic, 1, "a_size"));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        ExtShaderProgram shaderProgram = getShaderProgram();
        shaderProgram.begin();
        shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
        addEffectsUniforms(shaderProgram, camera);

        renderables.forEach(r -> {
            Render render = (Render) r;
            pointView.setEntity(render.entity);
            render(render.entity, pointView, getAlpha(render.entity));

            pointView.blend();
            pointView.depth();

            for (int md = 0; md < meshIdx; md++) {
                MeshData meshd = meshes.get(md);
                meshd.mesh.setVertices(meshd.vertices, 0, meshd.vertexIdx);
                meshd.mesh.render(shaderProgram, glType);

                meshd.clear();
            }
        });
        shaderProgram.end();

        // Reset indices
        meshIdx = 1;
        curr = meshes.get(0);

    }

    private void render(Entity entity, PointView pointView, float alpha) {
        // Render points CPU
        var body = Mapper.body.get(entity);
        var verts = Mapper.verts.get(entity);
        var graph = Mapper.graph.get(entity);

        var pointCloudData = verts.pointCloudData;
        var cc = body.color;

        Vector3d v = D31;
        for (int i = 0; i < pointCloudData.getNumPoints(); i++) {
            pointCloudData.loadPoint(v, i);
            v.add(graph.translation);
            addPoint(pointView, (float) v.x, (float) v.y, (float) v.z, verts.primitiveSize, cc[0], cc[1], cc[2], alpha * cc[3]);
        }
    }

    public void addPoint(IPointRenderable pr, double x, double y, double z, float pointSize, float r, float g, float b, float a) {
        // Check if 3 more indices fit
        if (curr.numVertices + 1 >= curr.capacity) {
            // Create new mesh data
            initVertices(meshIdx++);
        }

        color(r, g, b, a);
        size(pointSize);
        vertex((float) x, (float) y, (float) z);
    }

    public void size(float pointSize) {
        curr.vertices[curr.vertexIdx + sizeOffset] = pointSize;
    }

}
