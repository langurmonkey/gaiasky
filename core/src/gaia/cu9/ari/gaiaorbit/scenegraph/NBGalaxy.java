/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.render.SceneGraphRenderer;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.FovCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.ExtShaderProgram;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public class NBGalaxy extends Particle {

    /** Bmag [-4.6/21.4] - Apparent integral B band magnitude **/
    float bmag;

    /** a26 [arcmin] - Major angular diameter **/
    float a26;

    /** b/a - Apparent axial ratio **/
    float ba;

    /** HRV [km/s] - Heliocentric radial velocity **/
    int hrv;

    /**
     * i [deg] - [0/90] Inclination of galaxy from the face-on (i=0) position
     **/
    int i;

    /** TT [-3/11] - Morphology T-type code **/
    int tt;

    /**
     * Mcl [char] - Dwarf galaxy morphology (BCD, HIcld, Im, Ir, S0em, Sm, Sph,
     * Tr, dE, dEem, or dS0em)
     **/
    String mcl;

    /** Alternative name **/
    String altname;

    public NBGalaxy(Vector3d pos, float appmag, float absmag, float colorbv, String name, float ra, float dec, float bmag, float a26, float ba, int hrv, int i, int tt, String mcl, long starid) {
        super(pos, appmag, absmag, colorbv, name, ra, dec, starid);
        this.bmag = bmag;
        this.a26 = a26;
        this.ba = ba;
        this.hrv = hrv;
        this.i = i;
        this.tt = tt;
        this.mcl = mcl;
    }

    @Override
    public double THRESHOLD_NONE() {
        return (float) 0;
    }

    @Override
    public double THRESHOLD_POINT() {
        return (float) 2E-10;
    }

    @Override
    public double THRESHOLD_QUAD() {
        return (float) 1.7E-12;
    }

    @Override
    protected void setDerivedAttributes() {
        double flux = Math.pow(10, -absmag / 2.5f);
        setRGB(colorbv);

        // Calculate size - This contains arbitrary boundary values to make
        // things nice on the render side
        size = (float) (Math.log(Math.pow(flux, 10.0)) * Constants.PC_TO_U);
        computedSize = 0;
    }

    /**
     * Re-implementation of update method of {@link CelestialBody} and
     * {@link SceneGraphNode}.
     */
    @Override
    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera, float opacity) {
        if (appmag <= GlobalConf.runtime.LIMIT_MAG_RUNTIME) {
            TH_OVER_FACTOR = (float) (THRESHOLD_POINT() / GlobalConf.scene.LABEL_NUMBER_FACTOR);
            translation.set(parentTransform).add(pos);
            distToCamera = translation.len();

            this.opacity = opacity;

            if (!copy) {
                viewAngle = (radius / distToCamera) / camera.getFovFactor();
                viewAngleApparent = viewAngle * GlobalConf.scene.STAR_BRIGHTNESS;

                addToRenderLists(camera);
            }

        }
    }

    protected boolean addToRender(IRenderable renderable, RenderGroup rg) {
        SceneGraphRenderer.render_lists.get(rg.ordinal()).add(renderable);
        return true;
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (opacity != 0) {
            if (camera.getCurrent() instanceof FovCamera) {
                // Render as point, do nothing
            } else {
                addToRender(this, RenderGroup.BILLBOARD_GAL);
            }
            if (renderText() && camera.isVisible(GaiaSky.instance.time, this)) {
                addToRender(this, RenderGroup.FONT_LABEL);
            }
        }

    }

    @Override
    public void render(ExtShaderProgram shader, float alpha, IntMesh mesh, ICamera camera) {
        compalpha = alpha;
        float size = getFuzzyRenderSize(camera) * GlobalConf.scene.STAR_POINT_SIZE * 1.5f;

        Vector3 aux = aux3f1.get();
        shader.setUniformf("u_pos", translation.put(aux));
        shader.setUniformf("u_size", size);

        shader.setUniformf("u_color", ccPale[0], ccPale[1], ccPale[2], alpha);
        shader.setUniformf("u_alpha", alpha * opacity);
        shader.setUniformf("u_distance", (float) distToCamera);
        shader.setUniformf("u_apparent_angle", (float) viewAngleApparent);
        shader.setUniformf("u_time", (float) GaiaSky.instance.getT() / 5f);

        shader.setUniformf("u_sliders", (tt + 3.4f) / 14f, 0.1f, 0f, i / 180f);

        shader.setUniformf("u_radius", (float) getRadius());

        // Sprite.render
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
    }

    @Override
    protected float labelFactor() {
        return 1.2e1f;
    }

    @Override
    protected float labelMax() {
        return 0.00004f;
    }

    @Override
    public float textScale() {
        return 0.15f;
    }

}
