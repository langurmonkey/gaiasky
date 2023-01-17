package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scene.record.CloudComponent;

import java.util.Map;

import static gaiasky.scene.record.MaterialComponent.convertToComponent;

public class Cloud implements Component {

    public CloudComponent cloud;

    public void setCloudSVT(Map<Object, Object> virtualTexture) {
        if (this.cloud != null) {
            this.cloud.setDiffuseSVT(convertToComponent(virtualTexture));
        }
    }

}
