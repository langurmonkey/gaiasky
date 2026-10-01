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
import gaiasky.util.parse.Parser;

import java.util.Arrays;
import java.util.Map;

/**
 * Handles the go-to action, implemented as a smooth camera transition to the target object.
 */
public class ActionGoto implements ActionHandler {
    private static final Logger.Log logger = Logger.getLogger(ActionGoto.class);

    /** Names of the parameter that contains the target name. **/
    public static final String[] PARAM_TARGET = new String[]{"target", "object", "name", "id"};

    /**
     * Position transition time parameter.
     */
    public static final String PARAM_POS_DURATION = "pos_duration";
    /** Orientation transition time parameter. **/
    public static final String PARAM_ORI_DURATION = "ori_duration";

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

        // Position transition duration.
        double pos_duration = 20.0;
        String posParam = getParameterValue(params, PARAM_POS_DURATION);
        if (posParam != null) {
            var parsed = Parser.parseDouble(posParam);
            if (Double.isFinite(parsed)) {
                pos_duration = parsed;
            }
        }
        // Orientation transition duration.
        double ori_duration = 10.0;
        String oriParam = getParameterValue(params, PARAM_ORI_DURATION);
        if (oriParam != null) {
            var parsed = Parser.parseDouble(oriParam);
            if (Double.isFinite(parsed)) {
                ori_duration = parsed;
            }
        }

        double pos_d = pos_duration;
        double ori_d = ori_duration;
        var api = GaiaSky.instance.scripting().apiv2().camera;
        GaiaSky.instance.getExecutorService().execute(() -> api.go_to_object(objectParam, pos_d, ori_d));
    }
}
