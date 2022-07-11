package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Billboard;
import gaiasky.scene.component.Celestial;
import gaiasky.scene.component.GraphNode;

public class BillboardView extends BaseView {

    public Billboard billboard;
    public GraphNode graph;
    public Celestial celestial;

    @Override
    protected void entityCheck(Entity entity) {
        super.entityCheck(entity);
        check(entity, Mapper.billboard, Billboard.class);
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        billboard = Mapper.billboard.get(entity);
        graph = Mapper.graph.get(entity);
        celestial = Mapper.celestial.get(entity);
    }
}
