/*******************************************************************************
 * Copyright 2012 bmanuel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;

/**
 * Screen space reflections filter.
 */
public class SSRFilter extends RaymarchingFilter {

    public SSRFilter(int viewportWidth, int viewportHeight) {
        super("raymarching/screenspace", "ssr", viewportWidth, viewportHeight);
    }

    public void setNormalTexture(Texture tex) {
        setTexture2(tex);
    }

    public void setReflectionTexture(Texture tex) {
        setTexture3(tex);
    }
}
