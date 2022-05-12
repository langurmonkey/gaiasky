package gaiasky.scene.render.extract;

import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.component.Base;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Settings;

/**
 * Contains some utils common to all extract systems.
 */
public abstract class AbstractExtractSystem extends IteratingSystem {

    protected final ICamera camera;
    protected Array<Array<IRenderable>> renderLists;

    public AbstractExtractSystem(Family family, int priority) {
        super(family, priority);
        this.camera = GaiaSky.instance.cameraManager;;
    }

    public void setRenderLists(Array<Array<IRenderable>> renderLists) {
        this.renderLists = renderLists;
    }

    protected boolean shouldRender(Base base) {
        return GaiaSky.instance.isOn(base.ct) && base.opacity > 0 && (base.visible || base.msSinceStateChange() < Settings.settings.scene.fadeMs);
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
            Array<IRenderable> renderList = renderLists.get(rg.ordinal());
            synchronized (renderList) {
                renderList.add(renderable);
            }
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
