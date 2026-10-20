/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import gaiasky.GaiaSky;
import gaiasky.render.postprocess.filters.SmaaBlendFilter;
import gaiasky.render.postprocess.filters.SmaaEdgesFilter;
import gaiasky.render.postprocess.filters.SmaaWeightsFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

/**
 * SMAA (Subpixel Morphological Antialiasing) post-processing effect.
 * <p>
 * Three passes, each with its own shader:
 * <ol>
 *     <li>Edge detection: marks pixels where a luma edge starts (left/top).</li>
 *     <li>Blending weights: finds the ends of each edge and looks up the area to
 *     blend in the precomputed area and search lookup tables.</li>
 *     <li>Neighbourhood blending: mixes each pixel with its neighbour along the
 *     edge, only where weights exist.</li>
 * </ol>
 * The algorithm is by Jorge Jimenez et al.; see
 * {@code assets/shader/postprocess/smaa-LICENSE.md} for the license and
 * attribution of the shaders and the lookup tables.
 */
public final class Smaa extends Antialiasing {
    private static final String AREA_TEXTURE_PATH = "img/smaa/smaa-area.png";
    private static final String SEARCH_TEXTURE_PATH = "img/smaa/smaa-search.png";

    private final Texture areaTexture;
    private final Texture searchTexture;

    private GaiaSkyFrameBuffer edgesBuffer;
    private GaiaSkyFrameBuffer weightsBuffer;

    private SmaaEdgesFilter edgesFilter;
    private SmaaWeightsFilter weightsFilter;
    private SmaaBlendFilter blendFilter;

    private int width;
    private int height;
    private int quality;

    /**
     * Creates the SMAA effect.
     *
     * @param viewportWidth  The viewport width in pixels.
     * @param viewportHeight The viewport height in pixels.
     * @param quality        The quality in [0,1,2], from worst to best.
     */
    public Smaa(int viewportWidth, int viewportHeight, int quality) {
        areaTexture = loadLookupTexture(AREA_TEXTURE_PATH);
        searchTexture = loadLookupTexture(SEARCH_TEXTURE_PATH);
        disposables.addAll(areaTexture, searchTexture);

        this.width = viewportWidth;
        this.height = viewportHeight;
        createBuffers(viewportWidth, viewportHeight);
        createFilters(quality);
    }

    private static Texture loadLookupTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path), false);
        // The lookup tables are sampled with bilinear filtering and must not
        // wrap around, or the searches bleed into the other half of the table.
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return texture;
    }

    private void createBuffers(int w, int h) {
        edgesBuffer = createColorBuffer(w, h);
        weightsBuffer = createColorBuffer(w, h);
        disposables.addAll(edgesBuffer, weightsBuffer);
    }

    private GaiaSkyFrameBuffer createColorBuffer(int w, int h) {
        GLFrameBuffer.FrameBufferBuilder builder = new GLFrameBuffer.FrameBufferBuilder(w, h);
        builder.addColorTextureAttachment(GL30.GL_RGBA8, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE);
        var buffer = new GaiaSkyFrameBuffer(builder, 0);
        var texture = buffer.getColorBufferTexture();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return buffer;
    }

    private static String getDefines(int quality) {
        StringBuilder sb = new StringBuilder();
        switch (quality) {
            case 0 -> sb.append("#define SMAA_PRESET_LOW");
            case 1 -> sb.append("#define SMAA_PRESET_MEDIUM");
            default -> sb.append("#define SMAA_PRESET_HIGH");
        }
        sb.append('\n');
        if (GaiaSky.settings().graphics.useSRGB) {
            // The scene is rendered in sRGB, so sampling decodes to linear and
            // the edge detection has to re-encode before thresholding.
            sb.append("#define SMAA_SRGB_INPUT");
            sb.append('\n');
        }
        return sb.toString();
    }

    private void createFilters(int quality) {
        this.quality = quality;
        String defines = getDefines(quality);
        edgesFilter = new SmaaEdgesFilter(width, height, defines);
        weightsFilter = new SmaaWeightsFilter(width, height, areaTexture, searchTexture, defines);
        blendFilter = new SmaaBlendFilter(width, height);
    }

    private void disposeFilters() {
        if (edgesFilter != null) {
            edgesFilter.dispose();
            edgesFilter = null;
        }
        if (weightsFilter != null) {
            weightsFilter.dispose();
            weightsFilter = null;
        }
        if (blendFilter != null) {
            blendFilter.dispose();
            blendFilter = null;
        }
    }

    @Override
    public void setViewportSize(int width, int height) {
        if (this.width == width && this.height == height) {
            return;
        }
        this.width = width;
        this.height = height;
        edgesFilter.setViewportSize(width, height);
        weightsFilter.setViewportSize(width, height);
        blendFilter.setViewportSize(width, height);

        disposables.removeValue(edgesBuffer, true);
        disposables.removeValue(weightsBuffer, true);
        edgesBuffer.dispose();
        weightsBuffer.dispose();
        createBuffers(width, height);
    }

    /**
     * Updates the SMAA quality setting. Changing the quality requires new
     * shader programs, so the filters are recreated.
     *
     * @param quality The quality in [0,1,2], from worst to best.
     */
    @Override
    public void updateQuality(int quality) {
        int q = Math.max(0, Math.min(2, quality));
        if (q == this.quality) {
            return;
        }
        disposeFilters();
        createFilters(q);
        rebind();
    }

    @Override
    public void rebind() {
        if (edgesFilter != null) {
            edgesFilter.rebind();
            weightsFilter.rebind();
            blendFilter.rebind();
        }
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        // 1. Edge detection
        edgesFilter.setInput(src).setOutput(edgesBuffer).render();
        // 2. Blending weights
        weightsFilter.setInput(edgesBuffer).setOutput(weightsBuffer).render();
        // 3. Neighbourhood blending. The viewport has to be restored right
        // before this pass: the intermediate passes reset it to the whole
        // back buffer when they end, and dest may be the screen with a
        // viewport smaller than the window.
        restoreViewport(dest);
        blendFilter.setBlendTexture(weightsBuffer.getColorBufferTexture());
        blendFilter.setInput(src).setOutput(dest).render();
    }

    @Override
    public void updateShaders() {
        super.updateShaders();
        if (edgesFilter != null) {
            edgesFilter.updateProgram();
            weightsFilter.updateProgram();
            blendFilter.updateProgram();
        }
    }

    @Override
    public void dispose() {
        disposeFilters();
        super.dispose();
    }
}
