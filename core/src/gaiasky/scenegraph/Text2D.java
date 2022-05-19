/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.api.I3DTextRenderable;
import gaiasky.render.api.IShapeRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.RenderGroup;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Settings;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

public class Text2D extends FadeNode implements I3DTextRenderable, IShapeRenderable, IObserver {

    private float scale = 1f;
    private int align;
    private boolean lines = false;
    private float lineHeight = 0f;

    public Text2D() {
        super();
    }

    @Override
    public void initialize() {
        EventManager.instance.subscribe(this, Event.UI_THEME_RELOAD_INFO);

        LabelStyle headerStyle = GaiaSky.instance.getGlobalResources().getSkin().get("header", LabelStyle.class);
        labelcolor[0] = headerStyle.fontColor.r;
        labelcolor[1] = headerStyle.fontColor.g;
        labelcolor[2] = headerStyle.fontColor.b;
    }

    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        super.updateLocal(time, camera);

        // Propagate upwards if necessary
        setParentOpacity();

        this.viewAngle = 80f;
        this.viewAngleApparent = this.viewAngle / camera.getFovFactor();
    }

    protected void setParentOpacity() {
        if (this.opacity > 0 && this.parent instanceof Text2D) {
            // If our parent is a Text2D, we update its opacity
            Text2D parent = (Text2D) this.parent;
            parent.opacity *= (1 - this.opacity);
            parent.setParentOpacity();
        }
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender() && this.renderText()) {
            addToRender(this, RenderGroup.FONT_LABEL);
            if (lines) {
                addToRender(this, RenderGroup.SHAPE);
            }
        }
    }

    @Override
    public double getDistToCamera() {
        return 0;
    }

    @Override
    public boolean renderText() {
        return !Settings.settings.program.modeCubemap.active;
    }

    @Override
    public void render(ShapeRenderer shapeRenderer, RenderingContext rc, float alpha, ICamera camera) {
        float lenwtop = 0.5f * scale * rc.w();
        float x0top = (rc.w() - lenwtop) / 2f;
        float x1top = x0top + lenwtop;

        float lenwbottom = 0.6f * scale * rc.w();
        float x0bottom = (rc.w() - lenwbottom) / 2f;
        float x1bottom = x0bottom + lenwbottom;

        float ytop = (60f + 15f * scale) * 1.6f;
        float ybottom = (60f - lineHeight * scale + 10f * scale) * 1.6f;

        // Resize batch
        shapeRenderer.setProjectionMatrix(shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, rc.w(), rc.h()));

        // Lines
        shapeRenderer.setColor(1f, 1f, 1f, opacity * alpha);
        shapeRenderer.line(x0top, ytop, x1top, ytop);
        shapeRenderer.line(x0bottom, ybottom, x1bottom, ybottom);

    }

    /**
     * Label rendering
     */
    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        shader.setUniformf("u_viewAngle", (float) viewAngleApparent);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", 1f);

        // Resize batch
        batch.setProjectionMatrix(batch.getProjectionMatrix().setToOrtho2D(0, 0, rc.w(), rc.h()));

        // Text
        render2DLabel(batch, shader, rc, sys.fontTitles, camera, text(), 0, 96f, scale * 1.6f, align);

        lineHeight = sys.fontTitles.getLineHeight();
    }

    @Override
    public float[] textColour() {
        return labelcolor;
    }

    @Override
    public float textSize() {
        return 10;
    }

    @Override
    public float textScale() {
        return 1;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
    }

    @Override
    public String text() {
        return getLocalizedName();
    }

    @Override
    public void textDepthBuffer() {
    }

    @Override
    public boolean isLabel() {
        return false;
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {

    }

    public void setScale(Double scale) {
        this.scale = scale.floatValue();
    }

    public void setAlign(Long align) {
        this.align = align.intValue();
    }

    public void setLines(String linesText) {
        lines = Boolean.parseBoolean(linesText);
    }

    @Override
    public float getTextOpacity(){
        return getOpacity();
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case UI_THEME_RELOAD_INFO:
            Skin skin = (Skin) data[0];
            // Get new theme color and put it in the label colour
            LabelStyle headerStyle = skin.get("header", LabelStyle.class);
            labelcolor[0] = headerStyle.fontColor.r;
            labelcolor[1] = headerStyle.fontColor.g;
            labelcolor[2] = headerStyle.fontColor.b;
            break;
        default:
            break;
        }

    }

}
