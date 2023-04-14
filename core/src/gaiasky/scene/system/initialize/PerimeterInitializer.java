package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;
import gaiasky.scene.system.update.GraphUpdater;

public class PerimeterInitializer extends AbstractInitSystem {
    public PerimeterInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var perimeter = Mapper.perimeter.get(entity);
        var graph = Mapper.graph.get(entity);
        var line = Mapper.line.get(entity);

        // Lines.
        line.lineWidth = 0.5f;
        line.renderConsumer = LineEntityRenderSystem::renderPerimeter;

        body.setColor(new float[] { 0.8f, 0.8f, 0.f, 1f });
        graph.localTransform = new Matrix4();
        graph.mustUpdateFunction = GraphUpdater::mustUpdatePerimeter;

        perimeter.maxlonlat = new Vector2(-1000, -1000);
        perimeter.minlonlat = new Vector2(1000, 1000);

        perimeter.cart0 = new Vector3();

        perimeter.loc3d = new float[perimeter.loc2d.length][][];
        for (int lineIndex = 0; lineIndex < perimeter.loc2d.length; lineIndex++) {
            perimeter.loc3d[lineIndex] = new float[perimeter.loc2d[lineIndex].length][3];
        }
    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
