/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.MaterialComponent;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.Constants;
import gaiasky.util.coord.StaticCoordinates;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders billboard galaxies with no texture, just blobs
 */
public class BillboardGalaxy extends Billboard {

    protected static double TH_ANGLE_POINT_M = Math.toRadians(0.9);

    public BillboardGalaxy(){
       super();
    }

    public BillboardGalaxy(String[] names, double alphaDeg, double deltaDeg, double distPc, double sizePc, String tex) {
        super();
        setNames(names);
        setColor(new double[] { 1, 1, 1, 1 });
        setSizepc(sizePc);
        setCt("Galaxies");
        setParent("NBG");
        StaticCoordinates sc = new StaticCoordinates();
        sc.setEquatorial(new double[] { alphaDeg, deltaDeg, distPc });
        setCoordinates(sc);
        ModelComponent mc = new ModelComponent(true);
        mc.setType("twofacedbillboard");
        mc.setStaticlight(1.0);
        Map<String, Object> params = new HashMap<>();
        params.put("diameter", 1.0);
        mc.setParams(params);
        MaterialComponent mtc = new MaterialComponent();
        mtc.setDiffuse(tex);
        mc.setMaterial(mtc);
        setModel(mc);
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
        super.updateLocalValues(time, camera);
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender()) {
            double thPoint = (TH_ANGLE_POINT_M * camera.getFovFactor()) / sizeScaleFactor;
            if (viewAngleApparent >= thPoint) {
                addToRender(this, RenderGroup.MODEL_DIFFUSE);
            } else if (opacity > 0) {
                addToRender(this, RenderGroup.BILLBOARD_GAL);
            }

            if (renderText()) {
                addToRender(this, RenderGroup.FONT_LABEL);
            }

        }
    }

    @Override
    public void render(ExtShaderProgram shader, float alpha, IntMesh mesh, ICamera camera) {
        compalpha = alpha;

        float size = (float) (getFuzzyRenderSize(camera) / Constants.DISTANCE_SCALE_FACTOR);

        Vector3 aux = F31.get();
        shader.setUniformf("u_pos", translation.put(aux));
        shader.setUniformf("u_size", size);

        shader.setUniformf("u_color", ccPale[0], ccPale[1], ccPale[2], alpha);
        shader.setUniformf("u_alpha", alpha * opacity);
        shader.setUniformf("u_distance", (float) distToCamera);
        shader.setUniformf("u_apparent_angle", (float) viewAngleApparent);
        shader.setUniformf("u_time", (float) GaiaSky.instance.getT() / 5f);

        shader.setUniformf("u_radius", size);

        // Sprite.render
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
    }

    @Override
    public boolean renderText() {
        return names != null && GaiaSky.instance.isOn(ComponentType.Labels) && (opacity > 0 || fadeOpacity > 0);
    }

}
