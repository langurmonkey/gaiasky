/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.urlprotocol;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.GaiaSky;
import gaiasky.util.Logger;

import java.util.Arrays;
import java.util.Map;

/**
 * Handles the focus action, implemented as a camera focus change.
 */
public class ActionFocus implements ActionHandler {
    private static final Logger.Log logger = Logger.getLogger(ActionFocus.class);

    /** Names of the parameter that contains the target name. **/
    public static final String[] PARAM_TARGET = new String[]{"target", "object", "name", "id"};

    @Override
    public void process(String url,
                        Skin skin,
                        Stage stage,
                        boolean hotLoad,
                        Map<String, String> params) {

        String objectParam = getParameterValue(params, PARAM_TARGET);
        if (objectParam == null || objectParam.isBlank()) {
            logger.error("Dataset URL is missing the '" + Arrays.toString(PARAM_TARGET) + "' parameter: " + url);
            return;
        }

        var api = GaiaSky.instance.scripting().apiv2().camera;
        GaiaSky.instance.getExecutorService().execute(() -> api.focus_mode(objectParam));
    }
}
