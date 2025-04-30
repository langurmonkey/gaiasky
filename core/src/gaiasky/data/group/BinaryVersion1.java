/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

/**
 * Version 1, in contrast with 0, excludes Tycho identifiers from the format.
 */
public class BinaryVersion1 extends BinaryVersion0 {

    public BinaryVersion1() {
        super();
        this.tychoIds = false;
    }

}
