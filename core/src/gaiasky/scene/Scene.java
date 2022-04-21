package gaiasky.scene;

import com.artemis.World;
import com.artemis.WorldConfiguration;
import com.artemis.WorldConfigurationBuilder;
import gaiasky.scene.system.HelloWorldSystem;

/**
 * Represents a scene, contains and manages the world. The world contains
 * and manages all entities and systems.
 */
public class Scene {
    public World world;

    public Scene(){

    }

    public void initialize(){
        // 1. Register any plugins, set up the world.
        WorldConfiguration setup = new WorldConfigurationBuilder()
                .with(new HelloWorldSystem())
                .build();

        // 2. Create the world
        world = new World(setup);
    }
}
