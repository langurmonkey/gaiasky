package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.data.group.PointDataProvider;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.component.BillboardSet;
import gaiasky.scene.entity.BillboardSetRadio;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.util.List;

public class BillboardSetInitializer extends AbstractInitSystem {

    private final Vector3d D31;

    public BillboardSetInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        D31 = new Vector3d();
    }

    @Override
    public void initializeEntity(Entity entity) {
        var billboard = Mapper.billboardSet.get(entity);
        var label = Mapper.label.get(entity);

        label.label = true;
        label.textScale = 3;
        label.labelMax = (float) (2e-3 / Constants.DISTANCE_SCALE_FACTOR);
        label.labelFactor = 1;
        label.renderConsumer = LabelEntityRenderSystem::renderBillboardSet;
        label.renderFunction = LabelView::renderTextBase;
        label.depthBufferConsumer = LabelView::noTextDepthBuffer;

        reloadData(billboard);
    }

    @Override
    public void setUpEntity(Entity entity) {

        transformData(entity);

        EventManager.instance.subscribe(new BillboardSetRadio(entity), Event.GRAPHICS_QUALITY_UPDATED);
    }

    public boolean reloadData(BillboardSet billboard) {
        try {
            var provider = new PointDataProvider();
            boolean reload = false;
            for (BillboardDataset dataset : billboard.datasets) {
                boolean reloadNeeded = dataset.initialize(provider, reload);
                reload = reload || reloadNeeded;
            }
            return reload;
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }
        return false;
    }

    public void transformData(Entity entity) {
        var body = Mapper.body.get(entity);
        var coord = Mapper.coordinates.get(entity);
        var transform = Mapper.transform.get(entity);
        var set = Mapper.billboardSet.get(entity);

        // Set static coordinates to position
        coord.coordinates.getEquatorialCartesianCoordinates(null, body.pos);

        // Model
        Vector3d aux = D31;
        Vector3b pos3b = body.pos;

        // Transform all
        for (BillboardDataset bd : set.datasets) {
            List<IParticleRecord> a = bd.data;
            if (a != null) {
                for (int i = 0; i < a.size(); i++) {
                    IParticleRecord pr = a.get(i);

                    aux.set((float) pr.x(), (float) pr.z(), (float) pr.y());
                    aux.scl(body.size).rotate(-90, 0, 1, 0).mul(transform.matrix).add(pos3b);
                    pr.setPos(aux.x, aux.y, aux.z);
                }

            }
        }
    }
}
