/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.*;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.TextUtils;
import gaiasky.util.datadesc.DatasetDesc;
import gaiasky.util.datadesc.DatasetType;
import gaiasky.util.scene2d.*;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.*;

import static gaiasky.interafce.DownloadDataWindow.getIcon;

/**
 * Widget which lists all detected catalogs and offers a way to select them.
 *
 * @author tsagrista
 */
public class DatasetsWidget {

    private final Stage ui;
    private final Skin skin;
    public OwnCheckBox[] cbs;
    public Map<Button, String> candidates;

    public DatasetsWidget(Stage ui, Skin skin) {
        super();
        this.ui = ui;
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
        float pad = 4.8f;

        JsonReader reader = new JsonReader();

        // Containers
        Table dsTable = new Table(skin);
        dsTable.align(Align.top);

        Actor result;

        OwnScrollPane scroll = null;
        if (scrollOn) {
            scroll = new OwnScrollPane(dsTable, skin, "minimalist-nobg");
            scroll.setFadeScrollBars(false);
            scroll.setScrollingDisabled(true, false);
            scroll.setSmoothScrolling(true);

            result = scroll;
        } else {
            result = dsTable;
        }

        Map<String, DatasetType> typeMap = new HashMap<>();
        List<DatasetType> types = new ArrayList<>();
        for (FileHandle catalogFile : catalogFiles) {
            JsonValue val = reader.parse(catalogFile);
            DatasetDesc dd = new DatasetDesc(reader, val);
            dd.path = Path.of(catalogFile.path());
            dd.catalogFile = catalogFile;

            if (dd.description == null)
                dd.description = dd.path.toString();
            if (dd.name == null)
                dd.name = dd.catalogFile.nameWithoutExtension();

            DatasetType dt;
            if (typeMap.containsKey(dd.type)) {
                dt = typeMap.get(dd.type);
            } else {
                dt = new DatasetType(dd.type);
                typeMap.put(dd.type, dt);
                types.add(dt);
            }

            dt.datasets.add(dd);
        }

        Comparator<DatasetType> byType = Comparator.comparing(datasetType -> DownloadDataWindow.getTypeWeight(datasetType.typeStr));
        Collections.sort(types, byType);

        cbs = new OwnCheckBox[catalogFiles.size];
        Array<String> currentSetting = GlobalConf.data.CATALOG_JSON_FILES;

        int i = 0;
        for (DatasetType type : types) {
            OwnLabel dsType = new OwnLabel(I18n.txt("gui.download.type." + type.typeStr), skin, "hud-header");
            dsTable.add(dsType).colspan(5).left().padTop(pad * 4f).padBottom(pad * 4f).row();

            // Sort datasets
            Comparator<DatasetDesc> byName = Comparator.comparing(datasetDesc -> datasetDesc.name.toLowerCase());
            Collections.sort(type.datasets, byName);

            for (DatasetDesc dataset : type.datasets) {
                OwnCheckBox cb = new OwnCheckBox(dataset.name, skin, "title", pad * 2f);
                cb.bottom().left();

                cb.setChecked(contains(dataset.catalogFile.path(), currentSetting));
                cb.addListener(new OwnTextTooltip(dataset.path.toString(), skin));

                dsTable.add(cb).left().padRight(pad * 6f).padBottom(pad);

                // Description
                HorizontalGroup descGroup = new HorizontalGroup();
                descGroup.space(pad * 2f);
                String shortDesc = TextUtils.capString(dataset.description != null ? dataset.description : "", maxCharsDescription);
                OwnLabel description = new OwnLabel(shortDesc, skin);
                description.addListener(new OwnTextTooltip(dataset.description, skin, 10));
                // Info
                OwnImageButton imgTooltip = new OwnImageButton(skin, "tooltip");
                imgTooltip.addListener(new OwnTextTooltip(dataset.description, skin, 10));
                descGroup.addActor(imgTooltip);
                descGroup.addActor(description);
                dsTable.add(descGroup).left().padRight(pad * 6f).padBottom(pad);

                // Link
                if (dataset.link != null) {
                    LinkButton imgLink = new LinkButton(dataset.link, skin);
                    dsTable.add(imgLink).left().padRight(pad * 6f).padBottom(pad);
                } else {
                    dsTable.add().left().padRight(pad * 6f).padBottom(pad);
                }

                // Version
                String vers = "v-0";
                if (dataset.myVersion >= 0) {
                    vers = "v-" + dataset.myVersion;
                }
                OwnLabel versionLabel = new OwnLabel(vers, skin);
                dsTable.add(versionLabel).left().padRight(pad * 6f).padBottom(pad);

                // Type
                Image typeImage = new OwnImage(skin.getDrawable(getIcon(dataset.type)));
                float scl = 0.7f;
                float iw = typeImage.getWidth();
                float ih = typeImage.getHeight();
                typeImage.setSize(iw * scl, ih * scl);
                typeImage.setScaling(Scaling.none);
                typeImage.addListener(new OwnTextTooltip(dataset.type, skin, 10));
                dsTable.add(typeImage).left().padRight(pad * 4f).padBottom(pad);

                // Size
                OwnLabel sizeLabel = new OwnLabel(dataset.size, skin);
                sizeLabel.addListener(new OwnTextTooltip(I18n.txt("gui.dschooser.size.tooltip"), skin, 10));
                dsTable.add(sizeLabel).left().padRight(pad * 6f).padBottom(pad);

                // # objects
                OwnLabel nobjsLabel = new OwnLabel(dataset.nObjectsStr, skin);
                nobjsLabel.addListener(new OwnTextTooltip(I18n.txt("gui.dschooser.nobjects.tooltip"), skin, 10));
                dsTable.add(nobjsLabel).left().padBottom(pad).row();

                candidates.put(cb, dataset.catalogFile.path());

                cbs[i++] = cb;
            }

        }

        ButtonGroup<OwnCheckBox> bg = new ButtonGroup<>();
        bg.setMinCheckCount(0);
        bg.setMaxCheckCount(catalogFiles.size);
        bg.add(cbs);

        dsTable.pack();
        if (scroll != null) {
            scroll.setWidth(Math.min(1520f, dsTable.getWidth() + pad * 15f));
            scroll.setHeight(Math.min(ui.getHeight() * 0.8f, 850f));
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
            b.setWidth(maxw + 16f);

        return result;
    }

    private boolean contains(String name, Array<String> list) {
        for (String candidate : list)
            if (candidate != null && !candidate.isEmpty() && name.contains(candidate))
                return true;
        return false;
    }
}
