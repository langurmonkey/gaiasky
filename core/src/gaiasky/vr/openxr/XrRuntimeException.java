/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.vr.openxr;

public class XrRuntimeException extends RuntimeException {
    public final int result;

    public XrRuntimeException(int result, String s) {
        super(s);
        this.result = result;
    }
}
