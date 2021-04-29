package gaiasky.data.group;

/**
 * Binary version 1, used in eDR3 mainly. Same as version 0 ({@link BinaryVersion0}), but
 * without tycho identifiers.
 */
public class BinaryVersion1 extends BinaryVersion0 {

    public BinaryVersion1() {
        super();
        this.tychoIds = false;
    }

}
