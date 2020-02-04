/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.util;

import com.badlogic.gdx.utils.Array;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

class HipNamesTest {

    public static void main(String[] args) {
        HipNames hn = new HipNames();
        Path folder = Paths.get("/home/tsagrista/git/gaiasky/assets/assets-bak/data/");

        hn.load(folder);

        Map<Integer, Array<String>> hipNames = hn.getHipNames();

        hipNames.keySet().stream().sorted().forEach(hip -> {
            Array<String> nms = hipNames.get(hip);
            System.out.print("HIP " + hip + " ->\t ");

            for (int i = 0; i < nms.size; i++) {
                System.out.print(nms.get(i));
                if (i < nms.size - 1)
                    System.out.print(" | ");
            }

            System.out.println();
        });

    }
}