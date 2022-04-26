package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

public class RenderFlags implements Component {

    /**
     * Whether to render this entity as a quad.
     */
    public boolean renderQuad;

    public void setRenderquad(String renderQuad) {
        this.renderQuad = Boolean.getBoolean(renderQuad);
    }

    public void setRenderquad(Boolean renderQuad) {
        this.renderQuad = renderQuad;
    }
}
