/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.I3DTextRenderable;
import gaiasky.render.IAnnotationsRenderable;
import gaiasky.render.IRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.GlobalConf;
import gaiasky.util.comp.DistToCameraComparator;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;

import java.util.Comparator;
import java.util.List;

public class FontRenderSystem extends AbstractRenderSystem {

    private final ExtSpriteBatch batch;
    public BitmapFont fontDistanceField, font2d, fontTitles;
    private final Comparator<IRenderable> comp;
    private final float[] red;

    public FontRenderSystem(RenderGroup rg, float[] alphas, ExtSpriteBatch batch, ExtShaderProgram program) {
        super(rg, alphas, new ExtShaderProgram[] { program });
        this.batch = batch;
        // Init comparator
        comp = new DistToCameraComparator<>();
        red = new float[] { 1f, 0f, 0f, 1f };
    }

    public FontRenderSystem(RenderGroup rg, float[] alphas, ExtSpriteBatch batch, ExtShaderProgram program, BitmapFont fontDistanceField, BitmapFont font2d, BitmapFont fontTitles) {
        this(rg, alphas, batch, program);

        this.fontDistanceField = fontDistanceField;
        this.font2d = font2d;
        this.fontTitles = fontTitles;

    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        renderables.sort(comp);
        batch.begin();

        int size = renderables.size();
        ExtShaderProgram program = programs[0];
        if (program == null) {
            for (int i = 0; i < size; i++) {
                IAnnotationsRenderable s = (IAnnotationsRenderable) renderables.get(i);
                // Render sprite
                s.render(batch, camera, font2d, getAlpha(s));
            }
        } else {
            renderFont3D(renderables, program, camera, alphas[ComponentType.Labels.ordinal()]);
        }
        batch.end();

    }

    private void renderFont3D(List<IRenderable> renderables, ExtShaderProgram program, ICamera camera, float lalpha) {

        fontDistanceField.getData().setScale(0.6f);
        renderables.forEach(r -> {
            I3DTextRenderable lr = (I3DTextRenderable) r;

            // Label color
            program.setUniform4fv("u_color", GlobalConf.program.isUINightMode() ? red : lr.textColour(), 0, 4);
            // Component alpha
            program.setUniformf("u_componentAlpha", getAlpha(lr) * (!lr.isLabel() ? 1 : lalpha));
            // Font opacity multiplier, take into account element opacity
            program.setUniformf("u_opacity", 0.75f * lr.getTextOpacity());
            // zfar and k
            addDepthBufferUniforms(program, camera);

            lr.render(batch, program, this, rc, camera);
        });
    }

    @Override
    public void resize(int w, int h) {
        super.resize(w, h);
        updateBatchSize(w, h);
    }

    @Override
    public void updateBatchSize(int w, int h) {
        batch.setProjectionMatrix(batch.getProjectionMatrix().setToOrtho2D(0, 0, w, h));
    }

}
