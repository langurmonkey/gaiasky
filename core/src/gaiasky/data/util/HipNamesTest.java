/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.util;

import com.badlogic.gdx.utils.Array;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

class HipNamesTest {

    public static void main(String[] args) throws UnsupportedEncodingException {
        final PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        HipNames hn = new HipNames();
        Path folder = Paths.get(System.getenv("PROJECTS"), "/gaiasky/util/gdx/shader/loader/assets-bak/data/hipnames/");

        hn.load(folder);

        Map<Integer, Array<String>> hipNames = hn.getHipNames();

        hipNames.keySet().stream().sorted().forEach(hip -> {
            Array<String> nms = hipNames.get(hip);
            out.print(hip + ",");

            for (int i = 0; i < nms.size; i++) {
                out.print(nms.get(i));
                if (i < nms.size - 1)
                    out.print("|");
            }

            out.println();
        });

    }
}
