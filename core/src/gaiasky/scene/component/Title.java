package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

public class Title implements Component {

    public float scale = 1f;
    public int align;
    public boolean lines = false;
    public float lineHeight = 0f;

    public void setScale(Double scale) {
        this.scale = scale.floatValue();
    }

    public void setLines(Boolean lines) {
        this.lines = lines;
    }

    public void setLines(String linesText) {
        this.lines = Boolean.parseBoolean(linesText);
    }

    public void setAlign(Long align) {
        this.align = align.intValue();
    }
}
