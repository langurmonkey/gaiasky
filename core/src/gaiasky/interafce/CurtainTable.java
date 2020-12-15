/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Timer.Task;
import gaiasky.util.GlobalConf;
import gaiasky.util.math.StdRandom;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextButton;

public class CurtainTable extends Table {
    private static Task tt;

    protected Skin skin;
    protected Stage ui;
    protected Actor content;
    protected Container<Actor> container;
    protected Cell<?> contentCell;
    protected OwnTextButton collapseButton;

    protected boolean collapsing, expanding, collapsed;

    // Pixels per second
    protected float collapseSpeed;
    protected float containerExpandedWidth, tableExpandedWidth;

    protected float pad5, pad10, pad15;

    protected long reft;

    public CurtainTable(Actor content, Stage ui, Skin skin) {
        super(skin);
        this.ui = ui;
        this.skin = skin;
        this.content = content;
        this.collapsed = false;

        this.collapseSpeed = 1500;
        this.collapsing = this.expanding = false;

        this.container = new Container<>(this.content);

        this.pad5 = 8f;
        this.pad10 = 16f;
        this.pad15 = 18f;

        contentCell = add(this.container).top().expandY().pad(0);
        collapseButton = new OwnTextButton(collapsed ? ">" : "<", skin);
        collapseButton.pad(pad5);
        add(collapseButton).top().expandY().pad(0);

        pack();

        collapseButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (collapsed || collapsing) {
                    collapsing = false;
                    expanding = true;
                    reft = System.currentTimeMillis();
                } else {
                    collapsing = true;
                    expanding = false;
                    reft = System.currentTimeMillis();
                }
                return true;
            }
            return false;
        });

        // White bg
        this.container.setBackground(skin.getDrawable("white"));

        recomputeSize();

        if(tt != null){
            tt.cancel();
        }
        tt = new Task() {
            @Override
            public void run() {
                int rd = StdRandom.uniform(20);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < rd; i++)
                    sb.append(i);
                OwnLabel nl = new OwnLabel("n:" + sb.toString(), skin);
                nl.setColor(0, 0, 0, 1);
                setContent(nl);
            }
        };
        //Timer.schedule(tt, 5, 5);
    }

    public void setContent(Actor content) {
        this.container.setActor(content);
        pack();
        recomputeSize();
        System.out.println(containerExpandedWidth + "  " + this.container.getActor());
    }

    public void recomputeSize() {
        containerExpandedWidth = container.getActor().getWidth();
        tableExpandedWidth = containerExpandedWidth + collapseButton.getWidth();

        // Update elements' width
        container.setWidth(containerExpandedWidth);
        contentCell.width(containerExpandedWidth);
        setWidth(tableExpandedWidth);
    }

    @Override
    protected void drawBackground(Batch batch, float parentAlpha, float x, float y) {
        super.drawBackground(batch, parentAlpha, x, y);
    }

    public void act(float delta) {
        super.act(delta);
        if (collapsing || expanding) {
            long t = System.currentTimeMillis();
            float currWidth = container.getWidth();
            float pixels = ((t - reft) / 1000f) * collapseSpeed;
            if (collapsing) {
                // COLLAPSING
                if (currWidth > 0) {
                    contentCell.width(containerExpandedWidth - pixels);
                    setWidth(tableExpandedWidth - pixels);
                } else {
                    contentCell.width(0);
                    setWidth(tableExpandedWidth - containerExpandedWidth);
                    collapsing = false;
                    collapsed = true;
                    collapseButton.setText(">");
                }
            } else if (expanding) {
                // EXPANDING
                if (currWidth < containerExpandedWidth) {
                    contentCell.width(pixels);
                    setWidth(tableExpandedWidth - containerExpandedWidth + pixels);
                } else {
                    contentCell.width(containerExpandedWidth);
                    setWidth(tableExpandedWidth);
                    expanding = false;
                    collapsed = false;
                    collapseButton.setText("<");
                }
            }
        }
    }
    static public class CurtainTableStyle {
        public Drawable background;
        public Drawable expandDrawable, collapseDrawable;

        public CurtainTableStyle () {
        }

        public CurtainTableStyle(Drawable background, Drawable expand, Drawable collapse) {
            this.background = background;
            this.expandDrawable = expand;
            this.collapseDrawable = collapse;
        }
    }
}
