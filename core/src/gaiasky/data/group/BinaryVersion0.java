package gaiasky.data.group;

/**
 * Original binary version (0), used in DR1 and DR2.
 * Contains 9 doubles, 4 floats, 1 integer (hip), 3 integers for the tycho identifiers, a long (id) and the name.
 **/
public class BinaryVersion0 extends BinaryIOBase {

    public BinaryVersion0() {
        super(9, 4, true);
    }

}
