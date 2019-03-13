package gaia.cu9.ari.gaiaorbit.desktop.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.interfce.GenericDialog;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnImageButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnScrollPane;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextIconButton;

import java.util.Comparator;

/**
 * The run camera path file window, which allows the user to choose a script to
 * run.
 * 
 * @author tsagrista
 *
 */
public class RunCameraWindow extends GenericDialog {

    private Label outConsole;
    private Actor scriptsList;

    private Array<FileHandle> scripts = null;
    private FileHandle selectedScript = null;

    private float pad;

    public RunCameraWindow(Stage stg, Skin skin) {
        super(txt("gui.camera.title"), skin, stg);

        setAcceptText(txt("gui.camera.run"));
        setCancelText(txt("gui.cancel"));

        buildSuper();
    }

    @Override
    protected void build() {
        content.clear();

        HorizontalGroup titlegroup = new HorizontalGroup();
        titlegroup.space(pad);
        ImageButton tooltip = new OwnImageButton(skin, "tooltip");
        tooltip.addListener(new TextTooltip(txt("gui.tooltip.camera", SysUtils.getDefaultCameraDir()), skin));
        Label choosetitle = new OwnLabel(txt("gui.camera.choose"), skin, "help-title");
        titlegroup.addActor(choosetitle);
        titlegroup.addActor(tooltip);
        content.add(titlegroup).align(Align.left).padTop(pad * 2);
        content.row();

        ScrollPane scroll = new OwnScrollPane(generateFileList(), skin, "minimalist");
        scroll.setName("camera path files list scroll");
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        scroll.setHeight(200 * GlobalConf.SCALE_FACTOR);
        scroll.setWidth(300 * GlobalConf.SCALE_FACTOR);

        content.add(scroll).align(Align.center).pad(pad);
        content.row();

        Button reload = new OwnTextIconButton("", skin, "reload");
        reload.setName("reload camera files");
        reload.addListener(new TextTooltip(txt("gui.camera.reload"), skin));
        reload.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                scroll.setActor(generateFileList());
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
    protected void cancel() {
        selectedScript = null;
    }

    @Override
    protected void accept() {
        if (selectedScript != null) {
            EventManager.instance.post(Events.PLAY_CAMERA_CMD, selectedScript);
        }
    }

    private Actor generateFileList() {
        // Init files
        FileHandle scriptFolder = Gdx.files.absolute(SysUtils.getDefaultCameraDir().getPath());
        if (scripts == null)
            scripts = new Array<>();
        else
            scripts.clear();
        
        if (scriptFolder.exists())
            scripts = GlobalResources.listRec(scriptFolder, scripts, ".dat", ".gsc");
        scripts.sort(new FileHandleComparator());
        
        final com.badlogic.gdx.scenes.scene2d.ui.List<FileHandle> scriptsList = new com.badlogic.gdx.scenes.scene2d.ui.List<FileHandle>(skin, "normal");
        scriptsList.setName("camera path files list");

        Array<String> names = new Array<String>();
        for (FileHandle fh : scripts)
            names.add(fh.name());

        scriptsList.setItems(names);
        scriptsList.pack();//
        scriptsList.addListener(event -> {
            if (event instanceof ChangeEvent) {
                ChangeEvent ce = (ChangeEvent) event;
                Actor actor = ce.getTarget();
                @SuppressWarnings("unchecked")
                final String name = ((List<String>) actor).getSelected();
                select(name);
                return true;
            }
            return false;
        });
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
        if (this.scriptsList != null)
            this.scriptsList.remove();
        this.scriptsList = scriptsList;
        return scriptsList;
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
                try {
                    outConsole.setText(txt("gui.camera.ready"));
                    outConsole.setColor(0, 1, 0, 1);
                    this.acceptButton.setDisabled(false);
                    me.pack();
                } catch (Exception e) {
                    outConsole.setText(txt("gui.camera.error2", e.getMessage()));
                    outConsole.setColor(1, 0, 0, 1);
                    this.acceptButton.setDisabled(true);
                    me.pack();
                }
            }
        }
    }

    private class FileHandleComparator implements Comparator<FileHandle> {
        @Override
        public int compare(FileHandle fh0, FileHandle fh1) {
            return fh0.name().compareTo(fh1.name());

        }

    }
}
