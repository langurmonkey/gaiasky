/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaiasky.util.gdx.shader.attribute;

public class IntAttribute extends Attribute {
    public static final String CullFaceAlias = "cullface";
    public static final int CullFace = register(CullFaceAlias);
    public int value;

    public IntAttribute(int index) {
        super(index);
    }

    public IntAttribute(int index, int value) {
        super(index);
        this.value = value;
    }

    /**
     * create a cull face attribute to be used in a material
     *
     * @param value cull face value, possible values are GL_FRONT_AND_BACK, GL_BACK, GL_FRONT, or -1 to inherit default
     *
     * @return an attribute
     */
    public static IntAttribute createCullFace(int value) {
        return new IntAttribute(CullFace, value);
    }

    @Override
    public Attribute copy() {
        return new IntAttribute(index, value);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 983 * result + value;
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (index != o.index)
            return index - o.index;
        return value - ((IntAttribute) o).value;
    }
}
