/*******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.Logger;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

/**
 * Raymarching filter.
 */
public class RaymarchingFilter extends Filter3<RaymarchingFilter> {
    private final Vector2 viewport;
    private final Vector2 zfark;
    private final Vector3 pos;
    private final float[] additional;
    private Matrix4 frustumCorners, combined, projection, invProjection;
    private float timeSecs;

    /**
     * Additional textures. Texture1 is typically used for the depth buffer. The
     * others may be used for any purpose.
     */
    private Texture texture1, texture2, texture3, texture4;

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        Texture1("u_texture1", 0),
        Texture2("u_texture2", 0),
        Texture3("u_texture3", 0),
        Texture4("u_texture4", 0),
        Time("u_time", 1),
        Viewport("u_viewport", 2),
        ZfarK("u_zfark", 2),
        Pos("u_pos", 3),
        Projection("u_projection", 16),
        InvProjection("u_invProjection", 16),
        Combined("u_modelView", 16),
        FrustumCorners("u_frustumCorners", 16),
        Additional("u_additional", 4);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String mnemonic, int arrayElementSize) {
            this.mnemonic = mnemonic;
            this.elementSize = arrayElementSize;
        }

        @Override
        public String mnemonic() {
            return this.mnemonic;
        }

        @Override
        public int arrayElementSize() {
            return this.elementSize;
        }
    }

    /**
     * Creates a filter with the given viewport size
     *
     * @param vertexShader   The name of the vertex shader file, without extension
     * @param fragmentShader The name of the fragment shader file, without extension
     * @param viewportWidth  The viewport width in pixels.
     * @param viewportHeight The viewport height in pixels.
     */
    public RaymarchingFilter(String vertexShader, String fragmentShader, int viewportWidth, int viewportHeight) {
        this(vertexShader, fragmentShader, new Vector2((float) viewportWidth, (float) viewportHeight));
    }

    /**
     * Creates a filter with the given viewport size and using the default raymarching vertex shader.
     *
     * @param fragmentShader Name of fragment shader file without extension
     * @param viewportWidth  The viewport width in pixels.
     * @param viewportHeight The viewport height in pixels.
     */
    public RaymarchingFilter(String fragmentShader, int viewportWidth, int viewportHeight) {
        this("raymarching/screenspace", fragmentShader, new Vector2((float) viewportWidth, (float) viewportHeight));
    }

    /**
     * Creates a filter with the given viewport size and using the default raymarching vertex shader.
     *
     * @param fragmentShader Name of fragment shader file without extension
     * @param viewportSize   The viewport size in pixels.
     */
    public RaymarchingFilter(String fragmentShader, Vector2 viewportSize) {
        this("raymarching/screenspace", fragmentShader, viewportSize);
    }

    /**
     * Creates a filter with the given viewport size.
     *
     * @param vertexShader   The name of the vertex shader file, without extension
     * @param fragmentShader Name of fragment shader file without extension
     * @param viewportSize   The viewport size in pixels.
     */
    public RaymarchingFilter(String vertexShader, String fragmentShader, Vector2 viewportSize) {
        super(ShaderLoader.fromFile(vertexShader, fragmentShader));
        this.viewport = viewportSize;
        this.zfark = new Vector2();
        this.pos = new Vector3();
        this.frustumCorners = new Matrix4();
        this.projection = new Matrix4();
        this.invProjection = new Matrix4();
        this.combined = new Matrix4();
        this.additional = new float[4];
        rebind();
    }

    public void setFrustumCorners(Matrix4 fc) {
        this.frustumCorners = fc;
        setParam(Param.FrustumCorners, this.frustumCorners);
    }

    public void setProjection(Matrix4 proj) {
        this.projection.set(proj);
        this.invProjection.set(proj).inv();
        setParam(Param.Projection, this.projection);
        setParam(Param.InvProjection, this.invProjection);
    }

    public void setCombined(Matrix4 mv) {
        this.combined.set(mv);
        setParam(Param.Combined, this.combined);
    }

    public void setPos(Vector3 pos) {
        this.pos.set(pos);
        setParam(Param.Pos, this.pos);
    }

    public void setTime(float seconds) {
        this.timeSecs = seconds;
        setParam(Param.Time, timeSecs);
    }

    public void setViewportSize(float width, float height) {
        this.viewport.set(width, height);
        setParam(Param.Viewport, this.viewport);
    }

    public void setZfarK(float zfar, float k) {
        this.zfark.set(zfar, k);
        setParam(Param.ZfarK, this.zfark);
    }

    public void setTexture1(Texture tex) {
        this.texture1 = tex;
        setParam(Param.Texture1, u_texture1);
    }

    public void setDepthTexture(Texture tex) {
        setTexture1(tex);
    }

    public void setTexture2(Texture tex) {
        this.texture2 = tex;
        setParam(Param.Texture2, u_texture2);
    }

    public void setTexture3(Texture tex) {
        this.texture3 = tex;
        setParam(Param.Texture3, u_texture3);
    }

    public void setTexture4(Texture tex) {
        this.texture4 = tex;
        setParam(Param.Texture4, u_texture4);
    }

    public void setAdditional(float[] additional) {
        int len = Math.min(additional.length, 4);
        System.arraycopy(additional, 0, this.additional, 0, len);
        setParamv(Param.Additional, this.additional, 0, 4);
    }

    public void setAdditional(float a, float b, float c, float d) {
        this.additional[0] = a;
        this.additional[1] = b;
        this.additional[2] = c;
        this.additional[3] = d;
        setParamv(Param.Additional, this.additional, 0, 4);
    }

    public void setAdditional(int index, float value) {
        if (index >= 0 && index < 4) {
            this.additional[index] = value;
            setParamv(Param.Additional, this.additional, 0, 4);
        } else {
            Logger.getLogger(RaymarchingFilter.class).error("Additional index must be in [0, 3]: " + index);
        }
    }

    public Vector2 getViewportSize() {
        return viewport;
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture0, u_texture0);
        setParams(Param.Texture1, u_texture1);
        setParams(Param.Texture2, u_texture2);
        setParams(Param.Texture3, u_texture3);
        setParams(Param.Texture4, u_texture4);
        setParams(Param.Viewport, viewport);
        setParams(Param.ZfarK, zfark);
        setParams(Param.FrustumCorners, frustumCorners);
        setParams(Param.Combined, this.combined);
        setParams(Param.Projection, this.projection);
        setParams(Param.InvProjection, this.invProjection);
        setParams(Param.Pos, this.pos);
        setParams(Param.Time, timeSecs);
        setParamsv(Param.Additional, this.additional, 0, 4);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        if (inputTexture != null)
            inputTexture.bind(u_texture0);
        if (texture1 != null)
            texture1.bind(u_texture1);
        if (texture2 != null)
            texture2.bind(u_texture2);
        if (texture3 != null)
            texture3.bind(u_texture3);
        if (texture4 != null)
            texture4.bind(u_texture4);
    }
}
