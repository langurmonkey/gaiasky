package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.scene.Mapper;
import gaiasky.scene.entity.FocusHit;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.system.update.GraphUpdater;
import gaiasky.scene.view.FocusView;
import gaiasky.scene.view.LabelView;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.Constants;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

/**
 * Initializes location mark entities.
 */
public class LocInitializer extends AbstractInitSystem {

    public LocInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var loc = Mapper.loc.get(entity);
        var label = Mapper.label.get(entity);

        graph.mustUpdateFunction = GraphUpdater::mustUpdateLoc;

        label.label = false;
        label.labelMax = 1;
        label.textScale = 1e-7f;
        label.renderConsumer = LabelEntityRenderSystem::renderLocation;
        label.depthBufferConsumer = LabelView::noTextDepthBuffer;
        label.renderFunction = LabelView::renderTextLocation;
        label.labelPosition = new Vector3b();

        body.color = new float[] { 1f, 1f, 1f, 1f };
        body.size *= Constants.KM_TO_U;

        loc.location3d = new Vector3();
        loc.sizeKm = (float) (body.size * Constants.U_TO_KM);
        loc.displayName = "˟ " + base.getLocalizedName();
    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
