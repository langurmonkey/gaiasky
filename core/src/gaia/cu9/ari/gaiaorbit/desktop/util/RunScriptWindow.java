package gaia.cu9.ari.gaiaorbit.desktop.util;

import java.io.File;

import org.python.core.PyCode;
import org.python.core.PySyntaxError;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.interfce.GenericDialog;
import gaia.cu9.ari.gaiaorbit.script.JythonFactory;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnImageButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnScrollPane;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextIconButton;

/**
 * The run script window, which allows the user to choose a script to run.
 * 
 * @author tsagrista
 *
 */
public class RunScriptWindow extends GenericDialog {
    private static final String INTERNAL_PREFIX = "internal/";

    private PyCode code;

    private Label outConsole;
    private Actor scriptsList;

    private Array<FileHandle> scripts = null;
    private FileHandle selectedScript = null;

    private float pad;

    public RunScriptWindow(Stage stg, Skin skin) {
        super(txt("gui.script.title"), skin, stg);

        pad = 5 * GlobalConf.SCALE_FACTOR;

        setAcceptText(txt("gui.camera.run"));
        setCancelText(txt("gui.cancel"));

        buildSuper();

        this.setPosition(Math.round(stage.getWidth() / 2f - this.getWidth() / 2f), Math.round(stage.getHeight() / 2f - this.getHeight() / 2f));
    }

    @Override
    protected void build() {
        content.clear();

        HorizontalGroup titlegroup = new HorizontalGroup();
        titlegroup.space(pad);
        ImageButton tooltip = new OwnImageButton(skin, "tooltip");
        tooltip.addListener(new TextTooltip(txt("gui.tooltip.script", SysUtils.getDefaultScriptDir()), skin));
        Label choosetitle = new OwnLabel(txt("gui.script.choose"), skin, "help-title");
        titlegroup.addActor(choosetitle);
        titlegroup.addActor(tooltip);
        content.add(titlegroup).align(Align.left).padTop(pad * 2);
        content.row();
        
        ScrollPane scriptsScroll = new OwnScrollPane(generateFileList(), skin, "minimalist");
        scriptsScroll.setName("scripts list scroll");
        scriptsScroll.setFadeScrollBars(false);
        scriptsScroll.setScrollingDisabled(true, false);

        scriptsScroll.setHeight(200 * GlobalConf.SCALE_FACTOR);
        scriptsScroll.setWidth(300 * GlobalConf.SCALE_FACTOR);

        content.add(scriptsScroll).align(Align.center).pad(pad);
        content.row();

        Image reloadImg = new Image(skin.getDrawable("reload"));
        Button reload = new OwnTextIconButton("", reloadImg, skin);
        reload.setName("reload script files");
        reload.addListener(new TextTooltip(txt("gui.script.reload"), skin));
        reload.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                scriptsScroll.setActor(generateFileList());
            }
            return false;
        });
        content.add(reload).right().row();
        
        
        // Compile results
        outConsole = new OwnLabel("...", skin);
        content.add(outConsole).align(Align.center).pad(pad);
        content.row();

        pack();
    }

    @Override
    protected void accept() {
        if (code != null) {
            EventManager.instance.post(Events.RUN_SCRIPT_PYCODE, code, GlobalConf.program.SCRIPT_LOCATION, true);
        }
    }

    @Override
    protected void cancel() {
        selectedScript = null;
    }

    private Actor generateFileList() {
        // Choose script
        FileHandle scriptFolder1 = Gdx.files.internal(GlobalConf.program.SCRIPT_LOCATION);
        FileHandle scriptFolder2 = Gdx.files.absolute(SysUtils.getDefaultScriptDir().getPath());

        if (scripts != null)
            scripts.clear();
        else
            scripts = new Array<FileHandle>();

        if (scriptFolder1.exists())
            scripts = GlobalResources.listRec(scriptFolder1, scripts, ".py");

        if (scriptFolder2.exists())
            scripts = GlobalResources.listRec(scriptFolder2, scripts, ".py");

        scripts.sort((fh1, fh2) -> {
            return fh1.name().compareTo(fh2.name());
        });


        final com.badlogic.gdx.scenes.scene2d.ui.List<FileHandle> scriptsList = new com.badlogic.gdx.scenes.scene2d.ui.List<FileHandle>(skin, "normal");
        scriptsList.setName("scripts list");

        Array<String> names = new Array<String>();
        for (FileHandle fh : scripts) {
            if (fh.file().getPath().startsWith(GlobalConf.program.SCRIPT_LOCATION))
                names.add(INTERNAL_PREFIX + fh.name());
            else
                names.add(fh.name());
        }
        names.sort();

        scriptsList.setItems(names);
        scriptsList.pack();
        scriptsList.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    ChangeEvent ce = (ChangeEvent) event;
                    Actor actor = ce.getTarget();
                    String name = ((List<String>) actor).getSelected();

                    if (name != null) {
                        boolean internal = name.startsWith(INTERNAL_PREFIX);
                        if (internal)
                            name = name.substring(INTERNAL_PREFIX.length(), name.length());
                        for (FileHandle fh : scripts) {
                            if ((internal && fh.file().getPath().startsWith(GlobalConf.program.SCRIPT_LOCATION)) || (!internal && fh.file().getPath().startsWith(SysUtils.getDefaultScriptDir().getAbsolutePath()))) {
                                if (fh.name().equals(name)) {
                                    selectedScript = fh;
                                    break;
                                }
                            }
                        }
                        if (selectedScript != null) {
                            select(selectedScript);
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        // Select first
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (scripts.size > 0) {
                    scriptsList.setSelectedIndex(0);
                    select(scripts.get(0));
                }
            }
        });

        if (this.scriptsList != null)
            this.scriptsList.remove();
        this.scriptsList = scriptsList;
        return scriptsList;
    }

    private void select(FileHandle selectedScript) {
        File choice = selectedScript.file();
        try {
            code = JythonFactory.getInstance().compileJythonScript(choice);
            outConsole.setText(txt("gui.script.ready"));
            outConsole.setColor(0, 1, 0, 1);
            acceptButton.setDisabled(false);
            me.pack();
        } catch (PySyntaxError e1) {
            outConsole.setText(txt("gui.script.error", e1.type, e1.value));
            outConsole.setColor(1, 0, 0, 1);
            acceptButton.setDisabled(true);
            me.pack();
        } catch (Exception e2) {
            outConsole.setText(txt("gui.script.error2", e2.getMessage()));
            outConsole.setColor(1, 0, 0, 1);
            acceptButton.setDisabled(true);
            me.pack();
        }
    }

}
