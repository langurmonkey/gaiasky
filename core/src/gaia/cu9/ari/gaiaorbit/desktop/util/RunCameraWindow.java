package gaia.cu9.ari.gaiaorbit.desktop.util;

import java.io.File;
import java.util.Comparator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.scene2d.CollapsibleWindow;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnImageButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnScrollPane;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextButton;

/**
 * The run camera path file window, which allows the user to choose a script to
 * run.
 * 
 * @author tsagrista
 *
 */
public class RunCameraWindow extends CollapsibleWindow {

    final private Stage stage;
    private RunCameraWindow me;
    private Table table;

    private TextButton run;

    private Label outConsole;
    private Color originalColor;

    private Array<FileHandle> scripts = null;
    private FileHandle selectedScript = null;

    private float pad;

    public RunCameraWindow(Stage stg, Skin skin) {
        super(txt("gui.camera.title"), skin);

        this.stage = stg;
        this.me = this;

        pad = 5 * GlobalConf.SCALE_FACTOR;
        table = new Table(skin);

        /** BUTTONS **/
        HorizontalGroup buttonGroup = new HorizontalGroup();
        buttonGroup.space(pad);
        TextButton cancel = new OwnTextButton(txt("gui.cancel"), skin, "default");
        cancel.setName("cancel");
        cancel.setSize(70 * GlobalConf.SCALE_FACTOR, 20 * GlobalConf.SCALE_FACTOR);
        cancel.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    selectedScript = null;
                    me.hide();
                    return true;
                }

                return false;
            }

        });
        TextButton reload = new OwnTextButton(txt("gui.camera.reload"), skin, "default");
        reload.setName("reload");
        reload.setSize(150 * GlobalConf.SCALE_FACTOR, 20 * GlobalConf.SCALE_FACTOR);
        reload.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    me.initialize();
                    return true;
                }

                return false;
            }

        });
        reload.addListener(new TextTooltip(txt("gui.camera.reload"), skin));

        run = new OwnTextButton(txt("gui.camera.run"), skin, "default");
        run.setName("run");
        run.setSize(70 * GlobalConf.SCALE_FACTOR, 20 * GlobalConf.SCALE_FACTOR);
        run.setDisabled(true);
        run.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                boolean async = true;
                if (event instanceof ChangeEvent) {
                    me.hide();
                    if (selectedScript != null) {
                        EventManager.instance.post(Events.PLAY_CAMERA_CMD, selectedScript);
                    }
                    return true;
                }

                return false;
            }

        });
        buttonGroup.addActor(cancel);
        buttonGroup.addActor(reload);
        buttonGroup.addActor(run);

        add(table).pad(pad);
        row();
        add(buttonGroup).pad(pad).bottom().right();
        getTitleTable().align(Align.left);

        this.setPosition(Math.round(stage.getWidth() / 2f - this.getWidth() / 2f), Math.round(stage.getHeight() / 2f - this.getHeight() / 2f));
    }

    private void initialize() {
        table.clear();

        // Choose script
        FileHandle scriptFolder = Gdx.files.absolute(SysUtils.getDefaultCameraDir().getPath());

        scripts = new Array<FileHandle>();

        if (scriptFolder.exists())
            scripts = GlobalResources.listRec(scriptFolder, scripts, ".dat");

        scripts.sort(new FileHandleComparator());

        HorizontalGroup titlegroup = new HorizontalGroup();
        titlegroup.space(pad);
        ImageButton tooltip = new OwnImageButton(skin, "tooltip");
        tooltip.addListener(new TextTooltip(txt("gui.tooltip.camera", SysUtils.getDefaultCameraDir()), skin));
        Label choosetitle = new OwnLabel(txt("gui.camera.choose"), skin, "help-title");
        titlegroup.addActor(choosetitle);
        titlegroup.addActor(tooltip);
        table.add(titlegroup).align(Align.left).padTop(pad * 2);
        table.row();

        final com.badlogic.gdx.scenes.scene2d.ui.List<FileHandle> scriptsList = new com.badlogic.gdx.scenes.scene2d.ui.List<FileHandle>(skin, "normal");
        scriptsList.setName("camera path files list");

        Array<String> names = new Array<String>();
        for (FileHandle fh : scripts)
            names.add(fh.name());

        scriptsList.setItems(names);
        scriptsList.pack();//
        scriptsList.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    ChangeEvent ce = (ChangeEvent) event;
                    Actor actor = ce.getTarget();
                    final String name = ((com.badlogic.gdx.scenes.scene2d.ui.List<String>) actor).getSelected();
                    select(name);
                    return true;
                }
                return false;
            }
        });

        ScrollPane scriptsScroll = new OwnScrollPane(scriptsList, skin, "minimalist");
        scriptsScroll.setName("camera path files list scroll");
        scriptsScroll.setFadeScrollBars(false);
        scriptsScroll.setScrollingDisabled(true, false);

        scriptsScroll.setHeight(200 * GlobalConf.SCALE_FACTOR);
        scriptsScroll.setWidth(300 * GlobalConf.SCALE_FACTOR);

        table.add(scriptsScroll).align(Align.center).pad(pad);
        table.row();

        // Compile results
        outConsole = new OwnLabel("...", skin);
        originalColor = new Color(outConsole.getColor());
        table.add(outConsole).align(Align.center).pad(pad);
        table.row();

        pack();

        // Select first
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (scripts.size > 0) {
                    scriptsList.setSelectedIndex(0);
                    select(scripts.get(0).name());
                }
            }
        });

    }

    private void select(String name) {
        if (name != null) {
            for (FileHandle fh : scripts) {
                if (fh.name().equals(name)) {
                    selectedScript = fh;
                    break;
                }
            }
            if (selectedScript != null) {
                File choice = selectedScript.file();
                try {
                    outConsole.setText(txt("gui.camera.ready"));
                    outConsole.setColor(0, 1, 0, 1);
                    run.setDisabled(false);
                    me.pack();
                } catch (Exception e) {
                    outConsole.setText(txt("gui.camera.error2", e.getMessage()));
                    outConsole.setColor(1, 0, 0, 1);
                    run.setDisabled(true);
                    me.pack();
                }
            }
        }
    }

    public void hide() {
        if (stage.getActors().contains(me, true))
            me.remove();
    }

    public void display() {
        initialize();

        if (!stage.getActors().contains(me, true))
            stage.addActor(this);

        outConsole.setText("...");
        outConsole.setColor(originalColor);
    }

    protected static String txt(String key) {
        return I18n.bundle.get(key);
    }

    protected static String txt(String key, Object... args) {
        return I18n.bundle.format(key, args);
    }

    private class FileHandleComparator implements Comparator<FileHandle> {

        @Override
        public int compare(FileHandle fh0, FileHandle fh1) {
            return fh0.name().compareTo(fh1.name());

        }

    }
}
