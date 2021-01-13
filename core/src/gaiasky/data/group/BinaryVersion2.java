package gaiasky.data.group;

/**
 * Binary version 2, used in latter eDR3 runs.
 * Contains 6 doubles, 7 floats and 1 int (hip). It is more compact than version 0 and 1,
 * for only positions and velocity vectors are stored as doubles.
 */
public class BinaryVersion2 extends BinaryIOBase {

    protected BinaryVersion2() {
        super(6, 7, false);
    }

}
