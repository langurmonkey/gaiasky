/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.GLOnlyTextureData;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;

import java.nio.IntBuffer;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * One depth texture per volume, holding the distance to that volume's own first occluding sample,
 * plus the machinery to compose those distances into the scene depth buffer.
 * <p>
 * The pre-pass must write the dust front into the scene depth so the star, label and point passes are
 * hidden behind it, but that front is still there when the colour pass runs, so testing against it
 * compares the volume with itself (the colour pass may run at half resolution and dithers). Storing
 * each front separately lets the colour pass compare the buffer with a value written by that same
 * pass: equal or farther means this volume is nearest, nearer means something occludes it.
 */
public class VolumeDepthMask implements Disposable {

    /** A fullscreen quad, in clip space. **/
    private static final float[] QUAD_VERTICES = {-1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f};
    private static final short[] QUAD_INDICES = {0, 1, 2, 2, 3, 0};

    // GLSL 3.30 for texelFetch; libGDX prepends no version header.
    private static final String COMPOSE_VERTEX = """
            #version 330 core
            in vec2 a_position;
            void main() {
                gl_Position = vec4(a_position, 0.0, 1.0);
            }
            """;
    private static final String COMPOSE_FRAGMENT = """
            #version 330 core
            uniform sampler2D u_volumeDepth;
            void main() {
                // The stored depth goes straight into the buffer: no arithmetic, no bias.
                gl_FragDepth = texelFetch(u_volumeDepth, ivec2(gl_FragCoord.xy), 0).r;
            }
            """;

    private static Texture placeholder;

    private final Map<Object, Target> targets = new IdentityHashMap<>();
    private ShaderProgram composeProgram;
    private Mesh quad;
    private int u_volumeDepth;

    private final IntBuffer savedFbo = BufferUtils.newIntBuffer(1);
    private final IntBuffer savedViewport = BufferUtils.newIntBuffer(4);
    /** Whether the scene's framebuffer and viewport have been saved and not yet put back. **/
    private boolean stateSaved;

    /**
     * Binds a cleared depth target for this volume; the scene framebuffer and viewport are saved and
     * put back by {@link #restore()}.
     *
     * @param key The volume the target belongs to.
     *
     * @return The target's depth texture, or null if depth textures are not available.
     */
    public Texture begin(Object key) {
        if (!Gdx.graphics.isGL30Available()) {
            return null;
        }
        if (!stateSaved) {
            // Saved once for the whole pre-pass, not per volume.
            Gdx.gl.glGetIntegerv(GL30.GL_DRAW_FRAMEBUFFER_BINDING, savedFbo);
            Gdx.gl.glGetIntegerv(GL20.GL_VIEWPORT, savedViewport);
            stateSaved = true;
        }
        int width = savedViewport.get(2);
        int height = savedViewport.get(3);
        if (width <= 0 || height <= 0) {
            return null;
        }

        var target = targets.get(key);
        if (target == null || target.width != width || target.height != height) {
            if (target != null) {
                target.dispose();
            }
            target = new Target(width, height);
            targets.put(key, target);
        }

        Gdx.gl.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.fboHandle);
        Gdx.gl.glViewport(0, 0, width, height);
        // Only depth is written, so only depth is cleared.
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        return target.texture;
    }

    /** Puts back the framebuffer and viewport saved by {@link #begin(Object)}. */
    public void restore() {
        if (!stateSaved) {
            return;
        }
        Gdx.gl.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFbo.get(0));
        Gdx.gl.glViewport(savedViewport.get(0), savedViewport.get(1), savedViewport.get(2), savedViewport.get(3));
        stateSaved = false;
    }

    /**
     * @param key The volume the target belongs to.
     *
     * @return The volume's own front, or null if it has never been rendered.
     */
    public Texture texture(Object key) {
        var target = targets.get(key);
        return target == null ? null : target.texture;
    }

    /**
     * Min-composes one front into the depth buffer restored by {@link #restore()}: a fullscreen quad
     * writing the stored value, with a {@code GL_LEQUAL} test.
     *
     * @param front The distance to compose, from {@link #texture(Object)}.
     */
    public void compose(Texture front) {
        if (front == null) {
            return;
        }
        ensureComposeResources();

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glColorMask(false, false, false, false);

        composeProgram.bind();
        front.bind(0);
        composeProgram.setUniformi(u_volumeDepth, 0);
        quad.render(composeProgram, GL20.GL_TRIANGLES);

        Gdx.gl.glColorMask(true, true, true, true);
    }

    /**
     * A 1x1 black texture bound until the real mask exists, so the shader compiles with the mask
     * code in it. Black is the nearest possible depth, so it never clips anything.
     *
     * @return The placeholder, created on first use.
     */
    public static Texture placeholder() {
        if (placeholder == null) {
            var pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(0f, 0f, 0f, 1f);
            pixmap.fill();
            placeholder = new Texture(pixmap);
            placeholder.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            placeholder.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
            pixmap.dispose();
        }
        return placeholder;
    }

    private void ensureComposeResources() {
        if (composeProgram != null) {
            return;
        }
        composeProgram = new ShaderProgram(COMPOSE_VERTEX, COMPOSE_FRAGMENT);
        if (!composeProgram.isCompiled()) {
            throw new IllegalStateException("The volume depth mask compose shader did not compile: " + composeProgram.getLog());
        }
        u_volumeDepth = composeProgram.getUniformLocation("u_volumeDepth");
        quad = new Mesh(true, 4, 6, new VertexAttribute(Usage.Position, 2, "a_position"));
        quad.setVertices(QUAD_VERTICES);
        quad.setIndices(QUAD_INDICES);
    }

    @Override
    public void dispose() {
        for (var target : targets.values()) {
            target.dispose();
        }
        targets.clear();
    }

    /** Frees the compose shader and quad as well. Call once, when the renderer goes away. **/
    public void disposeAll() {
        dispose();
        if (quad != null) {
            quad.dispose();
            quad = null;
        }
        if (composeProgram != null) {
            composeProgram.dispose();
            composeProgram = null;
        }
    }

    /** A depth-only framebuffer: one depth texture attachment, no colour. **/
    private static final class Target implements Disposable {
        final int fboHandle;
        final Texture texture;
        final int width, height;

        Target(int width,
               int height) {
            var gl = Gdx.gl30;
            this.width = width;
            this.height = height;

            // A depth texture of its own, so the front can be read and composed later.
            var data = new GLOnlyTextureData(width, height, 0, GL30.GL_DEPTH_COMPONENT24, GL30.GL_DEPTH_COMPONENT, GL30.GL_UNSIGNED_INT);
            texture = new Texture(data);
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);

            fboHandle = gl.glGenFramebuffer();
            gl.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboHandle);
            gl.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL20.GL_TEXTURE_2D,
                                      texture.getTextureObjectHandle(), 0);
            // No colour attachment, so both draw and read buffers must be NONE.
            var none = BufferUtils.newIntBuffer(1);
            none.put(0, GL30.GL_NONE);
            gl.glDrawBuffers(1, none);
            gl.glReadBuffer(GL30.GL_NONE);
            var status = gl.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            gl.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                dispose();
                throw new IllegalStateException("The volume depth mask framebuffer is incomplete: 0x" + Integer.toHexString(status));
            }
        }

        @Override
        public void dispose() {
            texture.dispose();
            Gdx.gl30.glDeleteFramebuffer(fboHandle);
        }
    }
}
