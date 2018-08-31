package gaia.cu9.ari.gaiaorbit.interfce;

import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnScrollPane;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextArea;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextButton;

public class DatasetsWidget {

    private Skin skin;
    private String assetsLoc;
    public OwnTextButton[] cbs;
    public Map<Button, String> candidates;

    public DatasetsWidget(Skin skin, String assetsLoc) {
        super();
        this.skin = skin;
        this.assetsLoc = assetsLoc;
        candidates = new HashMap<Button, String>();
    }

    public Array<FileHandle> buildCatalogFiles() {
        // Discover datasets, add as buttons
        Array<FileHandle> catalogLocations = new Array<FileHandle>();
        catalogLocations.add(Gdx.files.absolute(assetsLoc + File.separatorChar + "data"));
        for (String loc : GlobalConf.data.CATALOG_LOCATIONS) {
            catalogLocations.add(Gdx.files.absolute(loc.trim()));
        }
        Array<FileHandle> catalogFiles = new Array<FileHandle>();

        for (FileHandle catalogLocation : catalogLocations) {
            FileHandle[] cfs = catalogLocation.list(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().startsWith("catalog-") && pathname.getName().endsWith(".json");
                }
            });
            catalogFiles.addAll(cfs);
        }
        return catalogFiles;
    }

    public Actor buildDatasetsWidget(Array<FileHandle> catalogFiles) {
        return buildDatasetsWidget(catalogFiles, true);
    }

    public Actor buildDatasetsWidget(Array<FileHandle> catalogFiles, boolean scrollOn) {
        float pad = 3 * GlobalConf.SCALE_FACTOR;
        float tawidth = 300 * GlobalConf.SCALE_FACTOR;
        float taheight = GlobalConf.SCALE_FACTOR > 1 ? 50 : 35;

        JsonReader reader = new JsonReader();

        // Sort by name
        Comparator<FileHandle> byName = (FileHandle a, FileHandle b) -> a.name().compareTo(b.name());
        catalogFiles.sort(byName);

        // Containers
        Table dstable = new Table(skin);

        Actor result;

        if (scrollOn) {
            OwnScrollPane scroll = new OwnScrollPane(dstable, skin, "minimalist-nobg");
            scroll.setHeight(300 * GlobalConf.SCALE_FACTOR);
            scroll.setWidth(600 * GlobalConf.SCALE_FACTOR);
            scroll.setFadeScrollBars(false);
            scroll.setScrollingDisabled(true, false);
            scroll.setSmoothScrolling(true);

            result = scroll;
        } else {
            result = dstable;
        }

        cbs = new OwnTextButton[catalogFiles.size];
        int i = 0;
        String[] currentSetting = GlobalConf.data.CATALOG_JSON_FILES.split("\\s*,\\s*");
        for (FileHandle catalogFile : catalogFiles) {
            String path = catalogFile.path();
            boolean internal = path.contains(assetsLoc);

            String candidate = internal ? path.substring(assetsLoc.length(), catalogFile.path().length()) : path;

            String name = null;
            String desc = null;
            try {
                JsonValue val = reader.parse(catalogFile);
                if (val.has("description"))
                    desc = val.get("description").asString();
                if (val.has("name"))
                    name = val.get("name").asString();
            } catch (Exception e) {
            }
            if (desc == null)
                desc = candidate;
            if (name == null)
                name = catalogFile.nameWithoutExtension();

            OwnLabel type = new OwnLabel(internal ? "[internal]" : "[custom]", skin);
            if (internal) {
                type.setColor(.4f, .4f, 1f, 1f);
            } else {
                type.setColor(.4f, 1f, .4f, 1f);
            }
            type.addListener(new TextTooltip(path, skin));

            OwnTextButton cb = new OwnTextButton(name, skin, "toggle-big");
            cb.bottom().left();

            cb.setChecked(contains(catalogFile.path(), currentSetting));
            cb.addListener(new TextTooltip(candidate, skin));

            dstable.add(cb).left().top().padRight(pad);
            dstable.add(type).left().top().padRight(pad);

            // Description
            TextArea description = new OwnTextArea(desc, skin.get("regular", TextFieldStyle.class));
            description.setDisabled(true);
            description.setPrefRows(2);
            description.setWidth(tawidth);
            description.setHeight(taheight);
            dstable.add(description).left().top().padTop(pad).padLeft(pad).row();

            candidates.put(cb, candidate);

            cbs[i++] = cb;

        }
        ButtonGroup<OwnTextButton> bg = new ButtonGroup<OwnTextButton>();
        bg.setMinCheckCount(0);
        bg.setMaxCheckCount(catalogFiles.size);
        bg.add(cbs);

        float maxw = 0;
        for (Button b : cbs) {
            if (b.getWidth() > maxw)
                maxw = b.getWidth();
        }
        for (Button b : cbs)
            b.setWidth(maxw + 10 * GlobalConf.SCALE_FACTOR);

        return result;
    }

    private boolean contains(String name, String[] list) {
        for (String candidate : list)
            if (candidate != null && !candidate.isEmpty() && name.contains(candidate))
                return true;
        return false;
    }
}
