package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

public class RenderFlags implements Component {
    public boolean renderQuad;

    public void setRenderquad(Boolean renderQuad) {
        this.renderQuad = renderQuad;
    }
}
