package gaiasky.scene.view;

import gaiasky.scene.Mapper;
import gaiasky.scene.component.Model;

public class ModelView extends BaseView {

    /** Model component. **/
    public Model model;

    public ModelView() {

    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.model = Mapper.model.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.model = null;
    }

    /** Checks whether the current model has a sparse virtual texture. **/
    public boolean hasSVT() {
        return model.model != null && model.model.mtc != null && model.model.mtc.hasSVT();
    }
}
