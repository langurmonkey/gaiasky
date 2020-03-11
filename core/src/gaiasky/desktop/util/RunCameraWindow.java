/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.interfce.GenericDialog;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.scene2d.*;

import java.nio.file.Files;
import java.nio.file.Path;
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

    private Array<Path> scripts = null;
    private Path selectedScript = null;

    private float pad;

    public RunCameraWindow(Stage stg, Skin skin) {
        super(I18n.txt("gui.camera.title"), skin, stg);

        setAcceptText(I18n.txt("gui.camera.run"));
        setCancelText(I18n.txt("gui.cancel"));

        buildSuper();
    }

    @Override
    protected void build() {
        content.clear();

        HorizontalGroup titlegroup = new HorizontalGroup();
        titlegroup.space(pad);
        ImageButton tooltip = new OwnImageButton(skin, "tooltip");
        tooltip.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.camera", SysUtils.getDefaultCameraDir()), skin));
        Label choosetitle = new OwnLabel(I18n.txt("gui.camera.choose"), skin, "help-title");
        titlegroup.addActor(choosetitle);
        titlegroup.addActor(tooltip);
        content.add(titlegroup).align(Align.left).padTop(pad * 2);
        content.row();

        ScrollPane scroll = new OwnScrollPane(generateFileList(), skin, "minimalist");
        scroll.setName("camera path files list scroll");
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        scroll.setHeight(700 * GlobalConf.UI_SCALE_FACTOR);
        scroll.setWidth(600 * GlobalConf.UI_SCALE_FACTOR);

        content.add(scroll).align(Align.center).pad(pad);
        content.row();

        Button reload = new OwnTextIconButton("", skin, "reload");
        reload.setName("reload camera files");
        reload.addListener(new OwnTextTooltip(I18n.txt("gui.camera.reload"), skin));
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
        Path scriptFolder = SysUtils.getDefaultCameraDir();
        if (scripts == null)
            scripts = new Array<>();
        else
            scripts.clear();
        
        if (Files.exists(scriptFolder))
            scripts = GlobalResources.listRec(scriptFolder, scripts, ".dat", ".gsc");
        scripts.sort(new PathNameComparator());
        
        final com.badlogic.gdx.scenes.scene2d.ui.List<FileHandle> scriptsList = new com.badlogic.gdx.scenes.scene2d.ui.List<FileHandle>(skin, "normal");
        scriptsList.setName("camera path files list");

        Array<String> names = new Array<String>();
        for (Path p : scripts)
            names.add(p.getFileName().toString());

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
        GaiaSky.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (scripts.size > 0) {
                    scriptsList.setSelectedIndex(0);
                    select(scripts.get(0).getFileName().toString());
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
            for (Path p : scripts) {
                if (p.getFileName().toString().equals(name)) {
                    selectedScript = p;
                    break;
                }
            }
            if (selectedScript != null) {
                try {
                    outConsole.setText(I18n.txt("gui.camera.ready"));
                    outConsole.setColor(0, 1, 0, 1);
                    this.acceptButton.setDisabled(false);
                    me.pack();
                } catch (Exception e) {
                    outConsole.setText(I18n.txt("gui.camera.error2", e.getMessage()));
                    outConsole.setColor(1, 0, 0, 1);
                    this.acceptButton.setDisabled(true);
                    me.pack();
                }
            }
        }
    }

    private class PathNameComparator implements Comparator<Path> {
        @Override
        public int compare(Path fh0, Path fh1) {
            return fh0.getFileName().compareTo(fh1.getFileName());

        }

    }
}
