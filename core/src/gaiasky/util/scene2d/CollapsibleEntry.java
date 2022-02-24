package gaiasky.util.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class CollapsibleEntry extends OwnButton {
    private Cell<?> contentCell, actorCell;
    private OwnImageButton collapse;
    private Actor title, content;

    public CollapsibleEntry(Actor title, Actor content, Skin skin, String styleName, boolean changeCursor) {
        super(skin, styleName, changeCursor);
        this.title = title;
        this.content = content;
        createActor(title, skin);
    }

    public CollapsibleEntry(Actor title, Actor content, Skin skin) {
        this(title, content, skin, "dataset-nofocus", false);
    }

    public void setWidth(float width) {
        contentCell.width(width);
        actorCell.width(width - 40f);
        super.setWidth(width);
    }

    public void collapse() {
        contentCell.setActor(null);
        me.pack();
    }

    public void expand() {
        contentCell.setActor(content);
        me.pack();
    }

    private void createActor(Actor title, Skin skin) {
        // Unchecked = expand
        // Checked   = collapse
        pad(5f);
        collapse = new OwnImageButton(skin, "expand-collapse");
        collapse.setCheckedNoFire(false); // start collapsed (show expand icon)
        add(collapse).top().left().padBottom(10f).padRight(15f);
        actorCell = add(title).top().left().padBottom(10f);
        actorCell.row();
        contentCell = add().top().left().colspan(2).expandX();

        collapse.addListener(event -> {
            if (event instanceof ChangeEvent) {
                boolean checked = collapse.isChecked();
                if (!checked) {
                    collapse();
                } else {
                    expand();
                }
                return true;
            }
            return false;
        });
        title.addListener(new ClickListener(){
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if(event.getButton() == Buttons.LEFT){
                    boolean checked = collapse.isChecked();
                    if (checked) {
                        collapse();
                    } else {
                        expand();
                    }
                    collapse.setCheckedNoFire(!checked);
                }
                // Bubble up
                super.touchUp(event, x, y, pointer, button);
            }
        });
        title.addListener(event -> {
            if (event instanceof InputEvent) {
                Type type = ((InputEvent) event).getType();
                if (type == Type.enter) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Hand);
                } else if (type == Type.exit) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                }
                return true;
            }
            return false;
        });
    }
}
