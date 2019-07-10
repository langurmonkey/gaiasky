/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce.components;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.Align;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;

/** 
 * A GUI component
 * @author Toni Sagrista
 *
 */
public abstract class GuiComponent {

    protected Actor component;
    protected Skin skin;
    protected Stage stage;

    protected float pad, space8, space6, space4, space3, space2, space1;

    public GuiComponent(Skin skin, Stage stage) {
        this.skin = skin;
        this.stage = stage;
        pad = 5 * GlobalConf.SCALE_FACTOR;
        space8 = 8 * GlobalConf.SCALE_FACTOR;
        space6 = 6 * GlobalConf.SCALE_FACTOR;
        space4 = 4 * GlobalConf.SCALE_FACTOR;
        space3 = 3 * GlobalConf.SCALE_FACTOR;
        space2 = 2 * GlobalConf.SCALE_FACTOR;
        space1 = 1 * GlobalConf.SCALE_FACTOR;
    }

    /**
     * Initialises the component
     */
    public abstract void initialize();

    public Actor getActor() {
        return component;
    }

    /**
     * Disposes the component
     */
    public abstract void dispose();

    protected VerticalGroup vgroup(Actor ac1, Actor ac2, float sp){
        VerticalGroup vg = new VerticalGroup().align(Align.left).columnAlign(Align.left);
        vg.space(sp);
        vg.addActor(ac1);
        vg.addActor(ac2);
        return vg;
    }
}
