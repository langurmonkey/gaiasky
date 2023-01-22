package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scene.record.CloudComponent;
import gaiasky.scene.record.ModelComponent;

import java.util.Map;

import static gaiasky.scene.record.MaterialComponent.convertToComponent;

public class Cloud implements Component {

    public CloudComponent cloud;

    public void setCloud(String diffuseCloud) {
        if (this.cloud != null) {
            this.cloud.setDiffuse(diffuseCloud);
        }
    }

    public void updateCloud(CloudComponent cloud) {
        if(this.cloud != null) {
            this.cloud.updateWith(cloud);
        } else {
            this.cloud = cloud;
        }
    }
}
