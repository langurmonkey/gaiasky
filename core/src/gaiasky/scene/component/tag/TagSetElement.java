package gaiasky.scene.component.tag;

import com.badlogic.ashley.core.Component;

/**
 * This tag marks the element as a part of a set. Therefore, it should
 * not be processed in isolation. Elements tagged with this are skipped
 * by all update systems.
 */
public class TagSetElement implements Component {
}
