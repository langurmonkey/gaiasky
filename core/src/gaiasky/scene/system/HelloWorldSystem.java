package gaiasky.scene.system;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import gaiasky.scene.component.Base;

@All({Base.class})
public class HelloWorldSystem extends IteratingSystem {

    protected ComponentMapper<Base> mBase;

    private long runs = 0;

    @Override
    protected void process(int id) {
        //System.out.println(mBase.get(id).id);
        runs++;
    }
}
