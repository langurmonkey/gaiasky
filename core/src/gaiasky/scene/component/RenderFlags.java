package gaiasky.scene.component;

import com.artemis.Component;

public class RenderFlags extends Component {
    public boolean renderQuad;

    public void setRenderquad(Boolean renderQuad) {
        this.renderQuad = renderQuad;
    }
}
