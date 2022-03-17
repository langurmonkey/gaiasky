/**
 * This package contains extensions and re-implementations of certain utilities of libgdx.
 * For instance, it contains:
 * <ul>
 *     <li>Re-implementation of the mesh object and auxiliaries so that the indices are 32-bit integers instead of 16-bit shorts. This allows for larger models.</li>
 *     <li>Re-implementation of Attribute and auxiliaries to use a proper bit mask instead of a 64-bit long as the attribute mask. This enables registering more than 64 attributes.</li>
 *     <li>Lots of extensions to <code>gdx.scene2d.ui</code>.</li>
 *     <li>An extension to the wavefront loaders that enables loading more material properties.</li>
 *     <li>Several new object creators like ring, icosphere, octahedronsphere, etc.</li>
 * </ul>
 */
package gaiasky.util.gdx;