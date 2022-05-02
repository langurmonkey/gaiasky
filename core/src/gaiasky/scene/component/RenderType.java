package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.render.SceneGraphRenderer.RenderGroup;

public class RenderType implements Component {
    public RenderGroup renderGroup;

    public void setRendergroup(String rg) {
        this.renderGroup = RenderGroup.valueOf(rg);
    }
}