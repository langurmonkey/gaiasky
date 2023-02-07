package gaiasky.scene.view;

import gaiasky.scene.Mapper;
import gaiasky.scene.component.Cloud;
import gaiasky.scene.component.Model;

public class ModelView extends BaseView {

    /** Model component. **/
    public Model model;
    /** Cloud component. **/
    public Cloud cloud;

    public ModelView() {

    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.model = Mapper.model.get(entity);
        this.cloud = Mapper.cloud.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.model = null;
        this.cloud = null;
    }

    /** Checks whether the current model has a sparse virtual texture. **/
    public boolean hasSVT() {
        return (model.model != null && model.model.mtc != null && model.model.mtc.hasSVT())
                || (cloud != null && cloud.cloud != null && cloud.cloud.hasSVT());
    }
    public boolean hasSVTNoCloud() {
        return (model.model != null && model.model.mtc != null && model.model.mtc.hasSVT());
    }

    public boolean hasSVTCloud() {
        return (cloud != null && cloud.cloud != null && cloud.cloud.hasSVT());
    }
}
