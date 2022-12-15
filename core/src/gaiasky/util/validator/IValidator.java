/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

/**
 * Generic interface to be implemented by all input validators.
 */
public interface IValidator {
    boolean validate(String value);
}
