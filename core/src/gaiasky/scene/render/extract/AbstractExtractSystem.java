package gaiasky.scene.render.extract;

import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.component.Base;
import gaiasky.util.Settings;

/**
 * Contains some utils common to all extract systems.
 */
public abstract class AbstractExtractSystem extends IteratingSystem {
    protected Array<Array<IRenderable>> renderLists;

    public AbstractExtractSystem(Family family, int priority) {
        super(family, priority);
    }

    public void setRenderLists(Array<Array<IRenderable>> renderLists) {
        this.renderLists = renderLists;
    }

    protected boolean shouldRender(Base base) {
        return GaiaSky.instance.isOn(base.ct) && base.opacity > 0 && (base.visible || msSinceStateChange(base) < Settings.settings.scene.fadeMs);
    }

    protected long msSinceStateChange(Base base) {
        return (long) (GaiaSky.instance.getT() * 1000f) - base.lastStateChangeTimeMs;
    }

    /**
     * Adds the given renderable to the given render group list.
     *
     * @param renderable The renderable to add.
     * @param rg         The render group that identifies the renderable list.
     *
     * @return True if added, false otherwise.
     */
    protected boolean addToRender(IRenderable renderable, RenderGroup rg) {
        try {
            renderLists.get(rg.ordinal()).add(renderable);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Removes the given renderable from the given render group list.
     *
     * @param renderable The renderable to remove.
     * @param rg         The render group to remove from.
     *
     * @return True if removed, false otherwise.
     */
    protected boolean removeFromRender(IRenderable renderable, RenderGroup rg) {
        return renderLists.get(rg.ordinal()).removeValue(renderable, true);
    }

    protected boolean isInRender(IRenderable renderable, RenderGroup rg) {
        return renderLists.get(rg.ordinal()).contains(renderable, true);
    }

    protected boolean isInRender(IRenderable renderable, RenderGroup... rgs) {
        boolean is = false;
        for (RenderGroup rg : rgs)
            is = is || renderLists.get(rg.ordinal()).contains(renderable, true);
        return is;
    }
}
