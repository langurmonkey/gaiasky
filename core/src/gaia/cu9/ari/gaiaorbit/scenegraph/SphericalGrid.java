/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaia.cu9.ari.gaiaorbit.render.IAnnotationsRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ModelComponent;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * A spherical grid
 */
public class SphericalGrid extends BackgroundModel implements IAnnotationsRenderable {
    private static final float ANNOTATIONS_ALPHA = 0.8f;

    private static final int divisionsU = 36;
    private static final int divisionsV = 18;

    private BitmapFont font;
    public ModelComponent mc;
    private Vector3 auxf;
    private Vector3d auxd;
    private Matrix4 annotTransform;

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
        } else {
            // Equatorial, nothing
        }
        font = GlobalResources.skin.getFont("grid-annotation");

    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        // Render group never changes
        // Add to toRender list
        addToRender(this, RenderGroup.MODEL_GRID);
        addToRender(this, RenderGroup.FONT_ANNOTATION);
    }


    /**
     * Annotation rendering
     */
    @Override
    public void render(SpriteBatch spriteBatch, ICamera camera, float alpha) {

        // Horizon
        float stepAngle = 360 / divisionsU;
        alpha *= ANNOTATIONS_ALPHA;

        font.setColor(labelColour[0], labelColour[1], labelColour[2], labelColour[3] * alpha);

        for (int angle = 0; angle < 360; angle += stepAngle) {
            auxf.set(Coordinates.sphericalToCartesian(Math.toRadians(angle), 0, 1f, auxd).valuesf()).mul(annotTransform).nor();
            effectsPos(auxf, camera);
            if (auxf.dot(camera.getCamera().direction.nor()) > 0) {
                auxf.add(camera.getCamera().position);
                camera.getCamera().project(auxf);
                font.draw(spriteBatch, Integer.toString(angle), auxf.x, auxf.y);
            }

        }
        // North-south line
        stepAngle = 180 / divisionsV;
        for (int angle = -90; angle <= 90; angle += stepAngle) {
            if (angle != 0) {
                auxf.set(Coordinates.sphericalToCartesian(0, Math.toRadians(angle), 1f, auxd).valuesf()).mul(annotTransform).nor();
                effectsPos(auxf, camera);
                if (auxf.dot(camera.getCamera().direction.nor()) > 0) {
                    auxf.add(camera.getCamera().position);
                    camera.getCamera().project(auxf);
                    font.draw(spriteBatch, Integer.toString(angle), auxf.x, auxf.y);
                }
                auxf.set(Coordinates.sphericalToCartesian(0, Math.toRadians(-angle), -1f, auxd).valuesf()).mul(annotTransform).nor();
                effectsPos(auxf, camera);
                if (auxf.dot(camera.getCamera().direction.nor()) > 0) {
                    auxf.add(camera.getCamera().position);
                    camera.getCamera().project(auxf);
                    font.draw(spriteBatch, Integer.toString(angle), auxf.x, auxf.y);
                }
            }
        }

    }

    private void effectsPos(Vector3 auxf, ICamera camera) {
        relativisticPos(auxf, camera);
        gravwavePos(auxf);
    }

    private void relativisticPos(Vector3 auxf, ICamera camera) {
        if (GlobalConf.runtime.RELATIVISTIC_ABERRATION) {
            auxd.set(auxf);
            GlobalResources.applyRelativisticAberration(auxd, camera);
            auxd.put(auxf);
        }
    }

    private void gravwavePos(Vector3 auxf) {
        if (GlobalConf.runtime.GRAVITATIONAL_WAVES) {
            auxd.set(auxf);
            RelativisticEffectsManager.getInstance().gravitationalWavePos(auxd);
            auxd.put(auxf);
        }
    }

}
