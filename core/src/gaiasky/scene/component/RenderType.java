package gaiasky.scene.component;

import com.artemis.Component;
import gaiasky.render.SceneGraphRenderer.RenderGroup;

public class RenderType extends Component {
    public RenderGroup renderGroupModel;

    public void setRendergroup(String rg) {
        this.renderGroupModel = RenderGroup.valueOf(rg);
    }
}
