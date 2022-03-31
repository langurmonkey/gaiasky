/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.constel;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.ISceneGraphLoader;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scenegraph.Constellation;
import gaiasky.scenegraph.FadeNode;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.util.i18n.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.parse.Parser;
import uk.ac.starlink.util.DataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class ConstellationsLoader<T extends SceneGraphNode> implements ISceneGraphLoader {
    private static final String separator = "[\\t,]";
    String[] files;

    @Override
    public void initialize(String[] files) {
        this.files = files;
    }

    @Override
    public void initialize(DataSource ds) {
    }

    @Override
    public Array<? extends SceneGraphNode> loadData() {
        Array<SceneGraphNode> constellations = new Array<>();

        for (String f : files) {
            try {
                // Add fade node
                FadeNode constellationsFadeNode = new FadeNode();
                constellationsFadeNode.setPosition(new double[] { 0, 0, 0 });
                constellationsFadeNode.setCt(new String[] { "Constellations" });
                constellationsFadeNode.setFadeout(new double[] { 1.0e2, 2.0e4 });
                constellationsFadeNode.setParent(SceneGraphNode.ROOT_NAME);
                constellationsFadeNode.setName("Constellations");
                constellations.add(constellationsFadeNode);

                // load constellations
                FileHandle file = Settings.settings.data.dataFileHandle(f);
                BufferedReader br = new BufferedReader(new InputStreamReader(file.read()));

                try {
                    //Skip first line
                    String lastName = "";
                    Array<int[]> partial = null;
                    int lastid = -1;
                    String line;
                    String name = null;
                    ComponentTypes ct = new ComponentTypes(ComponentType.Constellations);

                    while ((line = br.readLine()) != null) {
                        if (!line.startsWith("#")) {
                            String[] tokens = line.split(separator);
                            name = tokens[0].trim();

                            if (!lastName.isEmpty() && !name.equals("JUMP") && !name.equals(lastName)) {
                                // We finished a constellation object
                                Constellation cons = new Constellation(lastName, "Constellations");
                                cons.ct = ct;
                                cons.ids = partial;
                                constellations.add(cons);
                                partial = null;
                                lastid = -1;
                            }

                            if (partial == null) {
                                partial = new Array<>();
                            }

                            // Break point sequence
                            if (name.equals("JUMP") && tokens[1].trim().equals("JUMP")) {
                                lastid = -1;
                            } else {

                                int newid = Parser.parseInt(tokens[1].trim());
                                if (lastid > 0) {
                                    partial.add(new int[] { lastid, newid });
                                }
                                lastid = newid;

                                lastName = name;
                            }
                        }
                    }
                    // Add last
                    if (!lastName.isEmpty() && !name.equals("JUMP")) {
                        // We finished a constellation object
                        Constellation cons = new Constellation(lastName, "Constellations");
                        cons.ct = ct;
                        cons.ids = partial;
                        constellations.add(cons);
                    }
                } catch (IOException e) {
                    Logger.getLogger(this.getClass()).error(e);
                }

            } catch (Exception e) {
                Logger.getLogger(this.getClass()).error(e);
            }
        }

        Logger.getLogger(this.getClass()).info(I18n.txt("notif.constellations.init", constellations.size));
        return constellations;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public void setDescription(String description) {
    }

    @Override
    public void setParams(Map<String, Object> params) {

    }
}
