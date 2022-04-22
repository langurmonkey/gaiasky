/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.render.IAnnotationsRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

/**
 * A spherical grid
 */
public class SphericalGrid extends BackgroundModel implements IAnnotationsRenderable {
    private static final float ANNOTATIONS_ALPHA = 0.8f;

    private static final int divisionsU = 36;
    private static final int divisionsV = 18;

    private final Vector3 auxf;
    private final Vector3d auxd;
    private final Matrix4 annotTransform;

    public SphericalGrid() {
        super();
        annotTransform = new Matrix4();
        auxf = new Vector3();
        auxd = new Vector3d();
    }


    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
        // Initialize transform
        annotTransform.scl(size);
        if (transformName != null) {
            Class<Coordinates> c = Coordinates.class;
            try {
                Method m = ClassReflection.getMethod(c, transformName);
                Matrix4d trf = (Matrix4d) m.invoke(null);
                Matrix4 aux = new Matrix4();
                trf.putIn(aux);
                annotTransform.mul(aux);
            } catch (ReflectionException e) {
                Logger.getLogger(this.getClass()).error("Error getting/invoking method Coordinates." + transformName + "()");
            }
        }

    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if(this.shouldRender()) {
            addToRender(this, RenderGroup.MODEL_VERT_GRID);
            addToRender(this, RenderGroup.FONT_ANNOTATION);
        }
    }


    /**
     * Annotation rendering
     */
    @Override
    public void render(ExtSpriteBatch spriteBatch, ICamera camera, BitmapFont font, float alpha) {

        // Horizon
        float stepAngle = 360f / divisionsU;
        alpha *= ANNOTATIONS_ALPHA;

        font.setColor(labelcolor[0], labelcolor[1], labelcolor[2], labelcolor[3] * alpha);

        Vector3 vrOffset = F34.get();
        if (Settings.settings.runtime.openVr) {
            if (camera.getCurrent() instanceof NaturalCamera) {
                ((NaturalCamera) camera.getCurrent()).vrOffset.put(vrOffset);
                vrOffset.scl((float)(1f / Constants.M_TO_U));
            }
        } else {
            vrOffset.set(0, 0, 0);
        }

        for (int angle = 0; angle < 360; angle += stepAngle) {
            auxf.set(Coordinates.sphericalToCartesian(Math.toRadians(angle), 0f, 1f, auxd).valuesf()).mul(annotTransform).nor();
            effectsPos(auxf, camera);
            if (auxf.dot(camera.getCamera().direction.nor()) > 0) {
                auxf.add(camera.getCamera().position).scl((float) Constants.DISTANCE_SCALE_FACTOR).add(vrOffset);
                camera.getCamera().project(auxf);
                font.draw(spriteBatch, angle(angle), auxf.x, auxf.y);
            }

        }
        // North-south line
        stepAngle = 180f / divisionsV;
        for (int angle = -90; angle <= 90; angle += stepAngle) {
            if (angle != 0) {
                auxf.set(Coordinates.sphericalToCartesian(0, Math.toRadians(angle), 1f, auxd).valuesf()).mul(annotTransform).nor();
                effectsPos(auxf, camera);
                if (auxf.dot(camera.getCamera().direction.nor()) > 0) {
                    auxf.add(camera.getCamera().position).scl((float) Constants.DISTANCE_SCALE_FACTOR).add(vrOffset);
                    camera.getCamera().project(auxf);
                    font.draw(spriteBatch, angleSign(angle), auxf.x, auxf.y);
                }
                auxf.set(Coordinates.sphericalToCartesian(0, Math.toRadians(-angle), -1f, auxd).valuesf()).mul(annotTransform).nor();
                effectsPos(auxf, camera);
                if (auxf.dot(camera.getCamera().direction.nor()) > 0) {
                    auxf.add(camera.getCamera().position).scl((float) Constants.DISTANCE_SCALE_FACTOR).add(vrOffset);
                    camera.getCamera().project(auxf);
                    font.draw(spriteBatch, angleSign(angle), auxf.x, auxf.y);
                }
            }
        }

    }

    private String angle(int angle){
        return angle + "°";
    }
    private String angleSign(int angle){
        return (angle >= 0 ? "+" : "-") + Math.abs(angle) + "°";
    }

    private void effectsPos(Vector3 auxf, ICamera camera) {
        relativisticPos(auxf, camera);
        gravwavePos(auxf);
    }

    private void relativisticPos(Vector3 auxf, ICamera camera) {
        if (Settings.settings.runtime.relativisticAberration) {
            auxd.set(auxf);
            GlobalResources.applyRelativisticAberration(auxd, camera);
            auxd.put(auxf);
        }
    }

    private void gravwavePos(Vector3 auxf) {
        if (Settings.settings.runtime.gravitationalWaves) {
            auxd.set(auxf);
            RelativisticEffectsManager.getInstance().gravitationalWavePos(auxd);
            auxd.put(auxf);
        }
    }

}
