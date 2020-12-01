/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.TextUtils;
import gaiasky.util.scene2d.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Widget which lists all detected catalogs and offers a way to select them.
 *
 * @author tsagrista
 */
public class DatasetsWidget {

    private Skin skin;
    public OwnCheckBox[] cbs;
    public Map<Button, String> candidates;

    public DatasetsWidget(Skin skin) {
        super();
        this.skin = skin;
        candidates = new HashMap<>();
    }

    public Array<FileHandle> buildCatalogFiles() {
        // Discover data sets, add as buttons
        Array<FileHandle> catalogLocations = new Array<>();
        catalogLocations.add(Gdx.files.absolute(GlobalConf.data.DATA_LOCATION));

        Array<FileHandle> catalogFiles = new Array<>();

        for (FileHandle catalogLocation : catalogLocations) {
            FileHandle[] cfs = catalogLocation.list(pathname -> pathname.getName().startsWith("catalog-") && pathname.getName().endsWith(".json"));
            catalogFiles.addAll(cfs);
        }
        return catalogFiles;
    }

    public Actor buildDatasetsWidget(Array<FileHandle> catalogFiles) {
        return buildDatasetsWidget(catalogFiles, true);
    }

    public Actor buildDatasetsWidget(Array<FileHandle> catalogFiles, boolean scrollOn) {
        return buildDatasetsWidget(catalogFiles, scrollOn, 40);
    }

    public Actor buildDatasetsWidget(Array<FileHandle> catalogFiles, boolean scrollOn, int maxCharsDescription) {
        float pad = 3f * GlobalConf.UI_SCALE_FACTOR;

        JsonReader reader = new JsonReader();

        // Sort by name
        Comparator<FileHandle> byName = Comparator.comparing(FileHandle::name);
        catalogFiles.sort(byName);

        // Containers
        Table dsTable = new Table(skin);
        dsTable.align(Align.top);

        Actor result;

        OwnScrollPane scroll = null;
        if (scrollOn) {
            scroll = new OwnScrollPane(dsTable, skin, "minimalist-nobg");
            scroll.setHeight(Math.min(Gdx.graphics.getHeight() * 0.75f, 400f * GlobalConf.UI_SCALE_FACTOR));
            scroll.setFadeScrollBars(false);
            scroll.setScrollingDisabled(true, false);
            scroll.setSmoothScrolling(true);

            result = scroll;
        } else {
            result = dsTable;
        }

        cbs = new OwnCheckBox[catalogFiles.size];
        int i = 0;
        Array<String> currentSetting = GlobalConf.data.CATALOG_JSON_FILES;
        for (FileHandle catalogFile : catalogFiles) {
            String path = catalogFile.path();

            String name = "";
            String desc = "";
            String link = null;
            int version = -1;
            long bytes = -1;
            long nobjects = -1;
            try {
                JsonValue val = reader.parse(catalogFile);
                if (val.has("description"))
                    desc = val.getString("description");
                if (val.has("name"))
                    name = val.getString("name");
                if (val.has("link"))
                    link = val.getString("link");
                if (val.has("size"))
                    bytes = val.getLong("size");
                if (val.has("nobjects"))
                    nobjects = val.getLong("nobjects");
                if (val.has("version"))
                    version = val.getInt("version");
            } catch (Exception e) {
            }
            if (desc == null)
                desc = path;
            if (name == null)
                name = catalogFile.nameWithoutExtension();

            OwnCheckBox cb = new OwnCheckBox(name, skin, "title", pad * 2f);
            cb.bottom().left();

            cb.setChecked(contains(catalogFile.path(), currentSetting));
            cb.addListener(new OwnTextTooltip(path, skin));

            dsTable.add(cb).left().padRight(pad * 6f).padBottom(pad);

            // Description
            HorizontalGroup descGroup = new HorizontalGroup();
            descGroup.space(pad * 2f);
            String shortDesc = TextUtils.capString(desc != null ? desc : "", maxCharsDescription);
            OwnLabel description = new OwnLabel(shortDesc, skin);
            description.addListener(new OwnTextTooltip(desc, skin, 10));
            // Info
            OwnImageButton imgTooltip = new OwnImageButton(skin, "tooltip");
            imgTooltip.addListener(new OwnTextTooltip(desc, skin, 10));
            descGroup.addActor(imgTooltip);
            descGroup.addActor(description);
            dsTable.add(descGroup).left().padRight(pad * 6f).padBottom(pad);

            // Link
            if (link != null) {
                LinkButton imgLink = new LinkButton(link, skin);
                dsTable.add(imgLink).left().padRight(pad * 6f).padBottom(pad);
            } else {
                dsTable.add().left().padRight(pad * 6f).padBottom(pad);
            }

            // Version
            String vers = "v-0";
            if (version >= 0) {
                vers = "v-" + version;
            }
            OwnLabel versionLabel = new OwnLabel(vers, skin);
            dsTable.add(versionLabel).left().padRight(pad * 6f).padBottom(pad);

            // Size
            String size = "";
            try {
                if (bytes > 0)
                    size = GlobalResources.humanReadableByteCount(bytes, true);
                else
                    size = "";
            } catch (IllegalArgumentException e) {
                size = "? MB";
            }
            OwnLabel sizeLabel = new OwnLabel(size, skin);
            sizeLabel.addListener(new OwnTextTooltip(I18n.txt("gui.dschooser.size.tooltip"), skin, 10));
            dsTable.add(sizeLabel).left().padRight(pad * 6f).padBottom(pad);

            // # objects
            String nObjectsStr = I18n.txt("gui.dataset.nobjects", GlobalResources.nObjectsToString(nobjects));
            OwnLabel nobjsLabel = new OwnLabel(nObjectsStr, skin);
            nobjsLabel.addListener(new OwnTextTooltip(I18n.txt("gui.dschooser.nobjects.tooltip"), skin, 10));
            dsTable.add(nobjsLabel).left().padBottom(pad).row();

            candidates.put(cb, path);

            cbs[i++] = cb;

        }
        ButtonGroup<OwnCheckBox> bg = new ButtonGroup<>();
        bg.setMinCheckCount(0);
        bg.setMaxCheckCount(catalogFiles.size);
        bg.add(cbs);

        dsTable.pack();
        if (scroll != null) {
            scroll.setWidth(Math.min(950f * GlobalConf.UI_SCALE_FACTOR, dsTable.getWidth() + pad * 15f));
        }

        // No files
        if (catalogFiles.size == 0) {
            dsTable.add(new OwnLabel(I18n.txt("gui.dschooser.nodatasets"), skin)).center();
        }

        float maxw = 0;
        for (Button b : cbs) {
            if (b.getWidth() > maxw)
                maxw = b.getWidth();
        }
        for (Button b : cbs)
            b.setWidth(maxw + 10f * GlobalConf.UI_SCALE_FACTOR);

        return result;
    }

    private boolean contains(String name, Array<String> list) {
        for (String candidate : list)
            if (candidate != null && !candidate.isEmpty() && name.contains(candidate))
                return true;
        return false;
    }
}
