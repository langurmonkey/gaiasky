package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.I3DTextRenderable;
import gaia.cu9.ari.gaiaorbit.render.IShapeRenderable;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.render.system.FontRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

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
        EventManager.instance.subscribe(this, Events.UI_THEME_RELOAD_INFO);

        LabelStyle headerStyle = GlobalResources.skin.get("header", LabelStyle.class);
        labelColour[0] = headerStyle.fontColor.r;
        labelColour[1] = headerStyle.fontColor.g;
        labelColour[2] = headerStyle.fontColor.b;
    }

    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        super.updateLocal(time, camera);

        // Propagate upwards if necessary
        setParentOpacity();

        this.viewAngle = 80f;
        this.viewAngleApparent = this.viewAngle;
    }

    protected void setParentOpacity() {
        if (this.opacity > 0 && this.parent instanceof Text2D) {
            // If our parent is a text2d, we update its opacity
            Text2D parent = (Text2D) this.parent;
            parent.opacity *= (1 - this.opacity);
            parent.setParentOpacity();
        }
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (renderText()) {
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
        return this.opacity > 0 && !GlobalConf.program.CUBEMAP360_MODE;
    }

    @Override
    public void render(ShapeRenderer shapeRenderer, RenderingContext rc, float alpha, ICamera camera) {
        float lenwtop = 0.5f * scale * rc.w();
        float x0top = (rc.w() - lenwtop) / 2f;
        float x1top = x0top + lenwtop;

        float lenwbottom = 0.6f * scale * rc.w();
        float x0bottom = (rc.w() - lenwbottom) / 2f;
        float x1bottom = x0bottom + lenwbottom;

        float ytop = (60f + 15f * scale) * GlobalConf.SCALE_FACTOR;
        float ybottom = (60f - lineHeight * scale + 10f * scale) * GlobalConf.SCALE_FACTOR;

        // Resize batch
        shapeRenderer.setProjectionMatrix(shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, rc.w(), rc.h()));

        // Lines
        shapeRenderer.setColor(1f, 1f, 1f, 0.7f * opacity * alpha);
        shapeRenderer.line(x0top, ytop, x1top, ytop);
        shapeRenderer.line(x0bottom, ybottom, x1bottom, ybottom);

    }

    /**
     * Label rendering
     */
    @Override
    public void render(SpriteBatch batch, ShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        shader.setUniformf("u_viewAngle", (float) viewAngleApparent);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thOverFactor", 1f);
        shader.setUniformf("u_thOverFactorScl", 1f);

        // Resize batch
        batch.setProjectionMatrix(batch.getProjectionMatrix().setToOrtho2D(0, 0, rc.w(), rc.h()));

        // Text
        render2DLabel(batch, shader, rc, sys.fontTitles, camera, text(), 0, 60f * GlobalConf.SCALE_FACTOR, scale * GlobalConf.SCALE_FACTOR, align);

        lineHeight = sys.fontTitles.getLineHeight();
    }

    @Override
    public float[] textColour() {
        return labelColour;
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
        return name;
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
    public void notify(Events event, Object... data) {
        switch (event) {
        case UI_THEME_RELOAD_INFO:
            Skin skin = (Skin) data[0];
            // Get new theme color and put it in the label colour
            LabelStyle headerStyle = skin.get("header", LabelStyle.class);
            labelColour[0] = headerStyle.fontColor.r;
            labelColour[1] = headerStyle.fontColor.g;
            labelColour[2] = headerStyle.fontColor.b;
            break;
        default:
            break;
        }

    }

}
