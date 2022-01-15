/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.*;
import gaiasky.desktop.GaiaSkyDesktop;
import gaiasky.util.I18n;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.datadesc.DataDescriptor;
import gaiasky.util.datadesc.DataDescriptorUtils;
import gaiasky.util.datadesc.DatasetDesc;
import gaiasky.util.datadesc.DatasetType;
import gaiasky.util.scene2d.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static gaiasky.interafce.DatasetManagerWindow.getIcon;

/**
 * Widget which lists all detected catalogs and offers a way to select them.
 */
public class DatasetsWidget {

    private final Stage ui;
    private final Skin skin;
    public OwnCheckBox[] cbs;
    public Map<Button, String> candidates;
    public DataDescriptor localDatasets;
    private float padFactor = 1f;

    public DatasetsWidget(final Stage ui, final Skin skin) {
        super();
        this.ui = ui;
        this.skin = skin;
        candidates = new HashMap<>();
    }

    public void reloadLocalCatalogs() {
        localDatasets = DataDescriptorUtils.instance().buildLocalDatasets(null);
    }

    public Actor buildDatasetsWidget() {
        return buildDatasetsWidget(true);
    }

    public Actor buildDatasetsWidget(final boolean scrollOn) {
        return buildDatasetsWidget(scrollOn, 40, 420f);
    }

    /**
     * This function sets the padding factor of this widget. Call before {@link #buildDatasetsWidget()}.
     * @param factor The pad factor
     */
    public void setPadFactor(final float factor){
        this.padFactor = factor;
    }

    public Actor buildDatasetsWidget(final boolean scrollOn, final int maxCharsDescription, final float descriptionWidth) {
        final float pad = 4.8f * padFactor;
        final float padLarge = 14.4f * padFactor;

        Actor result;

        final Table datasetsTable = new Table(skin);
        datasetsTable.align(Align.topLeft);

        OwnScrollPane scroll = null;
        if (scrollOn) {
            scroll = new OwnScrollPane(datasetsTable, skin, "minimalist-nobg");
            scroll.setFadeScrollBars(false);
            scroll.setScrollingDisabled(true, false);
            scroll.setSmoothScrolling(true);

            result = scroll;
        } else {
            result = datasetsTable;
        }

        cbs = new OwnCheckBox[localDatasets.datasets.size()];
        java.util.List<String> currentSetting = Settings.settings.data.catalogFiles;

        int i = 0;
        for (DatasetType type : localDatasets.types) {
            String typeKey = "gui.download.type." + type.typeStr;
            try{
                I18n.txt(typeKey);
            } catch (Exception e){
               typeKey = "gui.download.type.unknown";
            }
            OwnLabel dsType = new OwnLabel(I18n.txt(typeKey), skin, "hud-header");

            OwnImageButton expandIcon = new OwnImageButton(skin, "expand-collapse");
            expandIcon.setChecked(true);
            HorizontalGroup titleGroup = new HorizontalGroup();
            titleGroup.space(padLarge);
            titleGroup.addActor(expandIcon);
            titleGroup.addActor(dsType);
            datasetsTable.add(titleGroup).top().left().padBottom(pad * 3f).padTop(padLarge * 2f).row();

            Table t = new Table(skin);
            Cell<Table> c = datasetsTable.add(t).colspan(2).left();
            c.row();

            expandIcon.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    if (c.getActor() == null) {
                        c.setActor(t);
                    } else {
                        c.setActor(null);
                    }
                    datasetsTable.pack();
                    return true;
                }
                return false;
            });

            // Sort datasets
            Comparator<DatasetDesc> byName = Comparator.comparing(datasetDesc -> datasetDesc.name.toLowerCase());
            type.datasets.sort(byName);

            for (DatasetDesc dataset : type.datasets) {

                // Type
                Image typeImage = new OwnImage(skin.getDrawable(getIcon(dataset.type)));
                float scl = 0.7f;
                float iw = typeImage.getWidth();
                float ih = typeImage.getHeight();
                typeImage.setSize(iw * scl, ih * scl);
                typeImage.setScaling(Scaling.none);
                typeImage.addListener(new OwnTextTooltip(dataset.type, skin, 10));
                t.add(typeImage).left().padLeft(padLarge * 2f).padRight(pad * 4f).padBottom(pad);

                // Select checkbox
                final OwnCheckBox cb = new OwnCheckBox(TextUtils.capString(dataset.name, 33), skin, "large", pad * 2f);
                boolean gsVersionTooSmall = false;
                if (dataset.minGsVersion > GaiaSkyDesktop.SOURCE_VERSION) {
                    // Can't select! minimum GS version larger than current
                    cb.setChecked(false);
                    cb.setDisabled(true);
                    cb.setColor(ColorUtils.gRedC);
                    cb.addListener(new OwnTextTooltip("Required Gaia Sky version (" + dataset.minGsVersion + ") larger than current (" + GaiaSkyDesktop.SOURCE_VERSION + ")", skin, 10));
                    cb.getStyle().disabledFontColor = ColorUtils.gRedC;
                    gsVersionTooSmall = true;
                } else {
                    cb.setChecked(TextUtils.contains(dataset.catalogFile.path(), currentSetting));
                    cb.addListener(new OwnTextTooltip(dataset.path.toString(), skin));
                }
                cb.setMinWidth(370f);
                cb.bottom().left();

                t.add(cb).left().padRight(pad * 6f).padBottom(pad);

                // Description
                HorizontalGroup descGroup = new HorizontalGroup();
                descGroup.space(pad * 2f);
                String shortDesc = TextUtils.capString(dataset.description != null ? dataset.description : "", maxCharsDescription);
                OwnLabel description = new OwnLabel(shortDesc, skin);
                if(gsVersionTooSmall)
                    description.setColor(ColorUtils.gRedC);
                description.addListener(new OwnTextTooltip(dataset.description, skin, 10));
                description.setWidth(descriptionWidth);
                // Info
                OwnImageButton imgTooltip = new OwnImageButton(skin, "tooltip");
                imgTooltip.addListener(new OwnTextTooltip(dataset.description, skin, 10));
                descGroup.addActor(imgTooltip);
                descGroup.addActor(description);
                t.add(descGroup).left().padRight(pad * 2f).padBottom(pad);

                // Link
                if (dataset.link != null) {
                    LinkButton imgLink = new LinkButton(dataset.link, skin);
                   t.add(imgLink).left().padRight(pad * 6f).padBottom(pad);
                } else {
                    t.add().left().padRight(pad * 6f).padBottom(pad);
                }

                // Version
                String version = "v-0";
                if (dataset.myVersion >= 0) {
                    version = "v-" + dataset.myVersion;
                }
                OwnLabel versionLabel = new OwnLabel(version, skin);
                t.add(versionLabel).left().padRight(pad * 6f).padBottom(pad);

                // Size
                OwnLabel sizeLabel = new OwnLabel(dataset.size, skin);
                sizeLabel.setWidth(85f);
                sizeLabel.addListener(new OwnTextTooltip(I18n.txt("gui.dschooser.size.tooltip"), skin, 10));
                t.add(sizeLabel).left().padRight(pad * 4f).padBottom(pad);

                // # objects
                OwnLabel nObjectsLabel = new OwnLabel(dataset.nObjectsStr, skin);
                nObjectsLabel.addListener(new OwnTextTooltip(I18n.txt("gui.dschooser.nobjects.tooltip"), skin, 10));
                t.add(nObjectsLabel).left().padBottom(pad).row();

                localDatasets.datasets.add(dataset);
                candidates.put(cb, dataset.catalogFile.path());

                cbs[i++] = cb;
            }

        }

        ButtonGroup<OwnCheckBox> bg = new ButtonGroup<>();
        bg.setMinCheckCount(0);
        bg.setMaxCheckCount(localDatasets.datasets.size());
        bg.add(cbs);

        datasetsTable.pack();
        if (scroll != null) {

            scroll.setWidth(Math.min(1520f, datasetsTable.getWidth() + pad * 15f));
            scroll.setHeight(Math.min(ui.getHeight() * 0.7f, 1500f));
        }

        // No files
        if (localDatasets.datasets.size() == 0) {
            datasetsTable.add(new OwnLabel(I18n.txt("gui.dschooser.nodatasets"), skin)).center();
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

    /**
     * Finds the dataset with the given descriptor file in the dataset descriptor list.
     *
     * @param descriptorFile The filename of the descriptor file.
     * @return The dataset descriptor or null if it was not found.
     */
    public DatasetDesc findDatasetByDescriptor(Path descriptorFile) throws IOException {
        if (localDatasets != null && localDatasets.datasets != null && Files.exists(descriptorFile))
            for (DatasetDesc dd : localDatasets.datasets) {
                if (Files.exists(dd.path) && Files.isSameFile(dd.path, descriptorFile))
                    return dd;
            }
        return null;
    }
}
