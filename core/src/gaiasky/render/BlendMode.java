/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

public enum BlendMode {
    /** Uses GL_ONE and GL_ONE for source and destination in blend equation. **/
    ADDITIVE,
    /** Uses GL_SRC_ALPHA and GL_ONE_MINUS_SRC_ALPHA for source and destination in blend equation. **/
    ALPHA,
    /** Uses GL_ONE and GL_ONE_MINUS_SRC_COLOR for source and destination in blend equation. **/
    COLOR,
    /** Disable blending **/
    NONE
}
