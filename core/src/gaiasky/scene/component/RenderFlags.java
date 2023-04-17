package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

public class RenderFlags implements Component {

    /**
     * Whether to render this entity as a quad.
     */
    public boolean renderQuad;

    public void setRenderQuad(String renderQuad) {
        this.renderQuad = Boolean.getBoolean(renderQuad);
    }

    public void setRenderQuad(Boolean renderQuad) {
        this.renderQuad = renderQuad;
    }

    public void setRenderquad(Boolean renderQuad) {
        setRenderQuad(renderQuad);
    }
}
