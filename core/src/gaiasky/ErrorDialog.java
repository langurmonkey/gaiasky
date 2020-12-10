/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import gaiasky.util.GlobalConf;

public class ErrorDialog implements ApplicationListener {

    private String title, message;
    private Stage ui;

    public ErrorDialog(String title, String message) {
        this.title = title;
        this.message = message;
    }

    @Override
    public void create() {
        ui = new Stage();
        FileHandle fh = Gdx.files.internal("skins/" + GlobalConf.program.UI_THEME + "/" + GlobalConf.program.UI_THEME + ".json");
        if (!fh.exists()) {
            // Default to dark-green
            GlobalConf.program.UI_THEME = "dark-green";
            fh = Gdx.files.internal("skins/" + GlobalConf.program.UI_THEME + "/" + GlobalConf.program.UI_THEME + ".json");
        }
        Skin skin = new Skin(fh);

        Table t = new Table(skin);
        t.setFillParent(true);
        ui.addActor(t);

        Label l = new Label(message, skin, "ui-15");
        Button b = new TextButton("Close", skin, "big");
        b.addListener((event) -> {
            if(event instanceof ChangeListener.ChangeEvent){
                Gdx.app.exit();
            }
            return true;
        });
        b.pad(10);
        t.add(l).center().padBottom(50).row();
        t.add(b).center().row();

        Gdx.input.setInputProcessor(ui);
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        ui.act(Gdx.graphics.getDeltaTime());
        ui.draw();

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {

    }

}
