/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.util.Pair;

public class ModePopupInfo {

    public String title;
    public String header;
    public Array<Pair<String[], String>> mappings;


    public void initMappings(){
        if(mappings == null){
            mappings = new Array();
        }
    }

    public void addMapping(String action, String... keys){
        initMappings();
        mappings.add(new Pair<>(keys, action));
    }
}
