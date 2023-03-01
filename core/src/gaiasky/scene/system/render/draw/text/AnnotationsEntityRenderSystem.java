package gaiasky.scene.system.render.draw.text;

import com.badlogic.gdx.math.Vector3;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.scene.component.Render;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Settings;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Vector3d;

/**
 * Renders grid annotations.
 */
public class AnnotationsEntityRenderSystem {

    private static final float ANNOTATIONS_ALPHA = 0.8f;

    private static final int divisionsU = 36;
    private static final int divisionsV = 18;

    private final Vector3 F34 = new Vector3();
    private final Vector3 F31 = new Vector3();
    private final Vector3d D31 = new Vector3d();

    /**
     * Annotation rendering
     */
    public void render(Render render, ExtSpriteBatch spriteBatch, ICamera camera, BitmapFont font, float alpha) {
        var entity = render.entity;
        var grid = Mapper.grid.get(entity);
        var body = Mapper.body.get(entity);

        // Horizon
        float stepAngle = 360f / divisionsU;
        alpha *= ANNOTATIONS_ALPHA;

        font.setColor(body.labelColor[0], body.labelColor[1], body.labelColor[2], body.labelColor[3] * alpha);

        Vector3 vrOffset = F34;
        if (Settings.settings.runtime.openXr) {
            if (camera.getCurrent() instanceof NaturalCamera) {
                ((NaturalCamera) camera.getCurrent()).vrOffset.put(vrOffset);
                vrOffset.scl((float) (1f / Constants.M_TO_U));
            }
        } else {
            vrOffset.set(0, 0, 0);
        }

        for (int angle = 0; angle < 360; angle += stepAngle) {
            F31.set(Coordinates.sphericalToCartesian(Math.toRadians(angle), 0f, 1f, D31).valuesf()).mul(grid.annotTransform).nor();
            effectsPos(F31, camera);
            if (F31.dot(camera.getCamera().direction.nor()) > 0) {
                F31.add(camera.getCamera().position).scl((float) Constants.DISTANCE_SCALE_FACTOR).add(vrOffset);
                camera.getCamera().project(F31);
                font.draw(spriteBatch, angle(angle), F31.x, F31.y);
            }

        }
        // North-south line
        stepAngle = 180f / divisionsV;
        for (int angle = -90; angle <= 90; angle += stepAngle) {
            if (angle != 0) {
                F31.set(Coordinates.sphericalToCartesian(0, Math.toRadians(angle), 1f, D31).valuesf()).mul(grid.annotTransform).nor();
                effectsPos(F31, camera);
                if (F31.dot(camera.getCamera().direction.nor()) > 0) {
                    F31.add(camera.getCamera().position).scl((float) Constants.DISTANCE_SCALE_FACTOR).add(vrOffset);
                    camera.getCamera().project(F31);
                    font.draw(spriteBatch, angleSign(angle), F31.x, F31.y);
                }
                F31.set(Coordinates.sphericalToCartesian(0, Math.toRadians(-angle), -1f, D31).valuesf()).mul(grid.annotTransform).nor();
                effectsPos(F31, camera);
                if (F31.dot(camera.getCamera().direction.nor()) > 0) {
                    F31.add(camera.getCamera().position).scl((float) Constants.DISTANCE_SCALE_FACTOR).add(vrOffset);
                    camera.getCamera().project(F31);
                    font.draw(spriteBatch, angleSign(angle), F31.x, F31.y);
                }
            }
        }
    }

    private String angle(int angle) {
        return angle + "°";
    }

    private String angleSign(int angle) {
        return (angle >= 0 ? "+" : "-") + Math.abs(angle) + "°";
    }

    private void effectsPos(Vector3 auxf, ICamera camera) {
        relativisticPos(auxf, camera);
        gravwavePos(auxf);
    }

    private void relativisticPos(Vector3 auxf, ICamera camera) {
        if (Settings.settings.runtime.relativisticAberration) {
            D31.set(auxf);
            GlobalResources.applyRelativisticAberration(D31, camera);
            D31.put(auxf);
        }
    }

    private void gravwavePos(Vector3 auxf) {
        if (Settings.settings.runtime.gravitationalWaves) {
            D31.set(auxf);
            RelativisticEffectsManager.getInstance().gravitationalWavePos(D31);
            D31.put(auxf);
        }
    }
}
