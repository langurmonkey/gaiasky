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
import gaiasky.scenegraph.ConstellationBoundaries;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;
import uk.ac.starlink.util.DataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConstelBoundariesLoader<T extends SceneGraphNode> implements ISceneGraphLoader {
    private static final String separator = "\\t";
    private static final boolean LOAD_INTERPOLATED = true;
    private static final int INTERPOLATED_MOD = 3;
    private String[] files;

    @Override
    public void initialize(String[] files) {
        this.files = files;
    }

    @Override
    public void initialize(DataSource ds) {
    }

    @Override
    public Array<? extends SceneGraphNode> loadData() {
        Array<ConstellationBoundaries> boundaries = new Array<>();
        int n = 0;
        for (String f : files) {
            try {
                // load boundaries
                FileHandle file = Settings.settings.data.dataFileHandle(f);
                BufferedReader br = new BufferedReader(new InputStreamReader(file.read()));

                try {
                    //Skip first line
                    String line;
                    ConstellationBoundaries boundary = new ConstellationBoundaries();
                    boundary.ct = new ComponentTypes(ComponentType.Boundaries);
                    List<List<Vector3d>> list = new ArrayList<>();
                    List<Vector3d> buffer = new ArrayList<>(4);
                    String lastName = "";
                    int interp = 0;
                    while ((line = br.readLine()) != null) {
                        if (!line.startsWith("#")) {
                            String[] tokens = line.split(separator);

                            String name = tokens[2];
                            String type = tokens.length > 3 ? tokens[3] : "O";

                            if (!name.equals(lastName)) {
                                // New line
                                list.add(buffer);
                                buffer = new ArrayList<>(20);
                                lastName = name;
                            }

                            if (type.equals("I")) {
                                interp++;
                            }

                            if ((type.equals("I") && LOAD_INTERPOLATED && interp % INTERPOLATED_MOD == 0) || type.equals("O")) {
                                // Load the data
                                double ra = Parser.parseDouble(tokens[0].trim()) * 15d;
                                double dec = Parser.parseDouble(tokens[1].trim());

                                double dist = 10 * Constants.AU_TO_U;

                                Vector3d point = Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), dist, new Vector3d());
                                buffer.add(point);
                                n++;
                            }

                        }
                    }
                    list.add(buffer);
                    boundary.setBoundaries(list);
                    boundaries.add(boundary);
                } catch (IOException e) {
                    Logger.getLogger(this.getClass()).error(e);
                }
            } catch (Exception e) {
                Logger.getLogger(this.getClass()).error(e);
            }
        }

        Logger.getLogger(this.getClass()).info(I18n.msg("notif.boundaries.init", n));

        return boundaries;
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
