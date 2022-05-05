package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.ITransform;
import gaiasky.util.Constants;
import gaiasky.util.camera.Proximity;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;

/**
 * Updates model objects.
 */
public class ModelUpdater extends IteratingSystem implements EntityUpdater {

    // At what distance the light has the maximum intensity
    private static final double LIGHT_X0 = 0.1 * Constants.AU_TO_U;
    // At what distance the light is 0
    private static final double LIGHT_X1 = 5e4 * Constants.AU_TO_U;

    private ICamera camera;
    private Vector3 F31;

    public ModelUpdater(Family family, int priority) {
        super(family, priority);
        this.camera = GaiaSky.instance.cameraManager;
        this.F31 = new Vector3();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {

        Body body = Mapper.body.get(entity);
        Model model = Mapper.model.get(entity);
        GraphNode graph = Mapper.graph.get(entity);
        ModelScaffolding scaffolding = Mapper.modelScaffolding.get(entity);

        // Update light with global position
        if (model.model != null && body.distToCamera <= LIGHT_X1) {
            for (int i = 0; i < Constants.N_DIR_LIGHTS; i++) {
                IFocus lightSource = camera.getCloseLightSource(i);
                if (lightSource != null) {
                    if (lightSource instanceof Proximity.NearbyRecord) {
                        graph.translation.put(model.model.directional(i).direction);
                        Proximity.NearbyRecord nr = (Proximity.NearbyRecord) lightSource;
                        if (nr.isStar() || nr.isStarGroup()) {
                            float[] col = nr.getColor();
                            double closestDist = nr.getClosestDistToCamera();
                            float colFactor = (float) Math.pow(MathUtilsd.lint(closestDist, LIGHT_X0, LIGHT_X1, 1.0, 0.0), 2.0);
                            model.model.directional(i).direction.sub(nr.pos.put(F31));
                            model.model.directional(i).color.set(col[0] * colFactor, col[1] * colFactor, col[2] * colFactor, colFactor);
                        } else {
                            Vector3b campos = camera.getPos();
                            model.model.directional(i).direction.add(campos.x.floatValue(), campos.y.floatValue(), campos.z.floatValue());
                            model.model.directional(i).color.set(1f, 1f, 1f, 1f);
                        }
                    }
                } else {
                    // Disable light
                    model.model.directional(i).color.set(0f, 0f, 0f, 0f);
                }
            }
        }
        updateLocalTransform(entity, body, graph, scaffolding);
    }

    protected void updateLocalTransform(Entity entity, Body body, GraphNode graph, ModelScaffolding scaffolding) {
        setToLocalTransform(entity, body, graph, scaffolding.sizeScaleFactor, graph.localTransform, true);
    }

    public void setToLocalTransform(Entity entity, Body body, GraphNode graph, float sizeFactor, Matrix4 localTransform, boolean forceUpdate) {
        setToLocalTransform(entity, graph, body.size, sizeFactor, localTransform, forceUpdate);
    }

    public void setToLocalTransform(Entity entity, GraphNode graph, float size, float sizeFactor, Matrix4 localTransform, boolean forceUpdate) {
        if (sizeFactor != 1 || forceUpdate) {
            var rotation = Mapper.rotation.get(entity);
            var scaffolding = Mapper.modelScaffolding.get(entity);
            if (rotation.rc != null) {
                graph.translation.getMatrix(localTransform).scl(size * sizeFactor).mul(Coordinates.getTransformF(scaffolding.refPlaneTransform)).rotate(0, 1, 0, (float) rotation.rc.ascendingNode).rotate(0, 0, 1, (float) (rotation.rc.inclination + rotation.rc.axialTilt)).rotate(0, 1, 0, (float) rotation.rc.angle);
                graph.orientation.idt().mul(Coordinates.getTransformD(scaffolding.refPlaneTransform)).rotate(0, 1, 0, (float) rotation.rc.ascendingNode).rotate(0, 0, 1, (float) (rotation.rc.inclination + rotation.rc.axialTilt));
            } else {
                graph.translation.getMatrix(localTransform).scl(size * sizeFactor).mul(Coordinates.getTransformF(scaffolding.refPlaneTransform));
                graph.orientation.idt().mul(Coordinates.getTransformD(scaffolding.refPlaneTransform));
            }
        } else {
            localTransform.set(graph.localTransform);
        }

        // Apply transformations
        AffineTransformations affine = Mapper.affine.get(entity);
        if (affine != null && affine.transformations != null)
            for (ITransform tc : affine.transformations)
                tc.apply(localTransform);
    }
}
