package gaia.cu9.ari.gaiaorbit.util.scene2d;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;

/**
 * A collapsible pane with a detach-to-window button.
 * 
 * @author Toni Sagrista
 *
 */
public class CollapsiblePane extends Table {

    CollapsibleWindow dialogWindow;
    ImageButton expandIcon, detachIcon;
    float lastx = -1, lasty = -1;
    Actor content;
    String labelText;
    Skin skin;
    Stage stage;
    float space;
    Cell<?> contentCell = null;

    /** Collapse speed in pixels per second **/
    protected float collapseSpeed;
    float targetHeight;
    boolean expanding = false;
    boolean collapsing = false;

    /**
     * Creates a collapsible pane.
     * 
     * @param stage
     *            The main stage.
     * @param labelText
     *            The text of the label.
     * @param content
     *            The content actor.
     * @param skin
     *            The skin to use.
     * @param labelStyle
     *            The style of the label.
     * @param expandButtonStyle
     *            The style of the expand icon.
     * @param detachButtonStyle
     *            The style of the detach icon.
     * @param topIcons
     *            List of top icons that will be added between the label and the
     *            expand/detach icons.
     */
    public CollapsiblePane(final Stage stage, final String labelText, final Actor content, final Skin skin,
	    String labelStyle, String expandButtonStyle, String detachButtonStyle, boolean expanded,
	    Actor... topIcons) {
	super();
	this.stage = stage;
	this.labelText = labelText;
	this.content = content;
	this.skin = skin;
	this.space = 4 * GlobalConf.SCALE_FACTOR;
	this.collapseSpeed = 1000;

	Label mainLabel = new Label(labelText, skin, labelStyle);

	// Expand icon
	expandIcon = new OwnImageButton(skin, expandButtonStyle);
	expandIcon.setName("expand-collapse");
	expandIcon.addListener(event -> {
	if (event instanceof ChangeEvent) {
		toggleExpandCollapse();
		return true;
	}
	return false;
	});
	expandIcon.addListener(new TextTooltip(I18n.bundle.get("gui.tooltip.expandcollapse.group"), skin));

	// Detach icon
	detachIcon = new OwnImageButton(skin, detachButtonStyle);
	detachIcon.setName("expand-collapse");
	detachIcon.setChecked(false);
	detachIcon.addListener(event -> {
	if (event instanceof ChangeEvent) {
		detach();
		return true;
	}
	return false;
	});
	detachIcon.addListener(new TextTooltip(I18n.bundle.get("gui.tooltip.detach.group"), skin));

	Table headerTable = new Table();
	HorizontalGroup headerGroupLeft = new HorizontalGroup();
	headerGroupLeft.space(space).align(Align.left);
	headerGroupLeft.addActor(mainLabel);

	if (topIcons != null && topIcons.length > 0) {
	    for (Actor topIcon : topIcons) {
		if (topIcon != null)
		    headerGroupLeft.addActor(topIcon);
	    }
	}

	HorizontalGroup headerGroupRight = new HorizontalGroup();
	headerGroupRight.space(space).align(Align.right);
	headerGroupRight.addActor(expandIcon);
	headerGroupRight.addActor(detachIcon);

	headerTable.add(headerGroupLeft).left().space(4 * GlobalConf.SCALE_FACTOR);
	headerTable.add().expandX();
	headerTable.add(headerGroupRight).right();

	add(headerTable).spaceBottom(this.space).prefWidth(195 * GlobalConf.SCALE_FACTOR).row();
	contentCell = add().prefHeight(0).prefWidth(195 * GlobalConf.SCALE_FACTOR);

	if (expanded)
	    contentCell.setActor(content);

	layout();
	targetHeight = getHeight();

    }

    public void expandPane() {
	if (!expandIcon.isChecked()) {
	    expandIcon.setChecked(true);
	    expanding = true;
	    collapsing = false;
	}
    }

    public void collapsePane() {
	if (expandIcon.isChecked()) {
	    expandIcon.setChecked(false);
	    expanding = false;
	    collapsing = true;
	}
    }

    private void toggleExpandCollapse() {
	if (expandIcon.isChecked() && dialogWindow == null) {
	    contentCell.setActor(content);
	    expanding = true;
	    collapsing = false;
	} else {
	    contentCell.clearActor();
	    expanding = false;
	    collapsing = true;
	}
	EventManager.instance.post(Events.RECALCULATE_OPTIONS_SIZE);
    }

    public void act(float dt) {
	super.act(dt);

	if (expanding) {

	} else if (collapsing) {

	}
    }

    public void detach() {
	dialogWindow = createWindow(labelText, content, skin, stage, lastx, lasty);

	// Display
	if (!stage.getActors().contains(dialogWindow, true))
	    stage.addActor(dialogWindow);

	expandIcon.setChecked(false);
	expandIcon.setDisabled(true);
	detachIcon.setDisabled(true);
    }

    /**
     * Creates a collapsible pane.
     * 
     * @param stage
     *            The main stage.
     * @param labelText
     *            The text of the label.
     * @param content
     *            The content actor.
     * @param skin
     *            The skin to use.
     * @param topIcons
     *            List of top icons that will be added between the label and the
     *            expand/detach icons.
     */
    public CollapsiblePane(Stage stage, String labelText, final Actor content, Skin skin, boolean expanded,
	    Actor... topIcons) {
	this(stage, labelText, content, skin, "header", "expand-collapse", "detach", expanded, topIcons);
    }

    private CollapsibleWindow createWindow(String labelText, final Actor content, Skin skin, Stage stage, float x,
	    float y) {
	final CollapsibleWindow window = new CollapsibleWindow(labelText, skin);
	window.align(Align.center);

	window.add(content).row();

	/** Close button **/
	TextButton close = new OwnTextButton(I18n.bundle.get("gui.close"), skin, "default");
	close.setName("close");
	close.addListener(new EventListener() {
	    @Override
	    public boolean handle(Event event) {
		if (event instanceof ChangeEvent) {
		    lastx = window.getX();
		    lasty = window.getY();
		    window.remove();
		    dialogWindow = null;
		    expandIcon.setDisabled(false);
		    detachIcon.setDisabled(false);
		    return true;
		}

		return false;
	    }

	});
	Container<Button> closeContainer = new Container<Button>(close);
	close.setSize(70, 20);
	closeContainer.align(Align.right);

	window.add(closeContainer).pad(5, 0, 0, 0).bottom().right();
	window.getTitleTable().align(Align.left);
	window.align(Align.left);
	window.pack();

	x = x < 0 ? stage.getWidth() / 2f - window.getWidth() / 2f : x;
	y = y < 0 ? stage.getHeight() / 2f - window.getHeight() / 2f : y;
	window.setPosition(Math.round(x), Math.round(y));
	window.pack();

	return window;
    }

}
