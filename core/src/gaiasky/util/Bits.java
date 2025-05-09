/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import net.jafama.FastMath;

import java.util.Arrays;

/**
 * Implements an arbitrarily large array of bits using an array of long integers.
 */
public class Bits {
    public static int DEFAULT_LENGTH = 120;
    long[] bits = { 0 };

    public Bits() {
    }

    /**
     * Creates a bit set whose initial size is large enough to explicitly represent bits with indices in the range 0 through
     * nBits-1.
     *
     * @param nBits the initial size of the bit set
     */
    public Bits(int nBits) {
        checkCapacity(nBits >>> 6);
    }

    public static Bits empty() {
        return empty(DEFAULT_LENGTH);
    }

    public static Bits empty(int nBits) {
        return new Bits(nBits);
    }

    public static Bits from(long mask) {
        Bits b = empty();
        b.setMask(mask);
        return b;
    }

    public static Bits indices(int... idx) {
        Bits b = empty();
        for (int j : idx) {
            b.set(j);
        }
        return b;
    }

    public boolean has(int index) {
        return get(index);
    }

    public boolean is(int index) {
        return get(index);
    }

    /**
     * @param index the index of the bit
     *
     * @return whether the bit is set
     *
     * @throws ArrayIndexOutOfBoundsException if index &lt; 0
     */
    public boolean get(int index) {
        final int word = index >>> 6;
        if (word >= bits.length)
            return false;
        return (bits[word] & (1L << (index & 0x3F))) != 0L;
    }

    /**
     * Returns the bit at the given index and clears it in one go.
     *
     * @param index the index of the bit
     *
     * @return whether the bit was set before invocation
     *
     * @throws ArrayIndexOutOfBoundsException if index &lt; 0
     */
    public boolean getAndClear(int index) {
        final int word = index >>> 6;
        if (word >= bits.length)
            return false;
        long oldBits = bits[word];
        bits[word] &= ~(1L << (index & 0x3F));
        return bits[word] != oldBits;
    }

    /**
     * Returns the bit at the given index and sets it in one go.
     *
     * @param index the index of the bit
     *
     * @return whether the bit was set before invocation
     *
     * @throws ArrayIndexOutOfBoundsException if index &lt; 0
     */
    public boolean getAndSet(int index) {
        final int word = index >>> 6;
        checkCapacity(word);
        long oldBits = bits[word];
        bits[word] |= 1L << (index & 0x3F);
        return bits[word] == oldBits;
    }

    /**
     * @param index the index of the bit to set
     *
     * @throws ArrayIndexOutOfBoundsException if index &lt; 0
     */
    public void set(int index) {
        final int word = index >>> 6;
        checkCapacity(word);
        bits[word] |= 1L << (index & 0x3F);
    }

    public void setMask(long mask) {
        for (int i = 0; i < 64; i++) {
            if ((mask >> i & 1L) == 1) {
                set(i);
            }
        }
    }

    /** @param index the index of the bit to flip */
    public void flip(int index) {
        final int word = index >>> 6;
        checkCapacity(word);
        bits[word] ^= 1L << (index & 0x3F);
    }

    private void checkCapacity(int len) {
        if (len >= bits.length) {
            long[] newBits = new long[len + 1];
            System.arraycopy(bits, 0, newBits, 0, bits.length);
            bits = newBits;
        }
    }

    /**
     * @param index the index of the bit to clear
     *
     * @throws ArrayIndexOutOfBoundsException if index &lt; 0
     */
    public void clear(int index) {
        final int word = index >>> 6;
        if (word >= bits.length)
            return;
        bits[word] &= ~(1L << (index & 0x3F));
    }

    /** Clears the entire bitset */
    public void clear() {
        Arrays.fill(bits, 0);
    }

    /** @return the number of bits currently stored, <b>not</b> the highset set bit! */
    public int numBits() {
        return bits.length << 6;
    }

    /**
     * Returns the "logical size" of this bitset: the index of the highest set bit in the bitset plus one. Returns zero if the
     * bitset contains no set bits.
     *
     * @return the logical size of this bitset
     */
    public int length() {
        long[] bits = this.bits;
        for (int word = bits.length - 1; word >= 0; --word) {
            long bitsAtWord = bits[word];
            if (bitsAtWord != 0) {
                for (int bit = 63; bit >= 0; --bit) {
                    if ((bitsAtWord & (1L << (bit & 0x3F))) != 0L) {
                        return (word << 6) + bit + 1;
                    }
                }
            }
        }
        return 0;
    }

    /** @return true if this bitset contains at least one bit set to true */
    public boolean notEmpty() {
        return !isEmpty();
    }

    /** @return true if this bitset contains no bits that are set to true */
    public boolean isEmpty() {
        long[] bits = this.bits;
        int length = bits.length;
        for (long bit : bits) {
            if (bit != 0L) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the index of the first bit that is set to true that occurs on or after the specified starting index. If no such bit
     * exists then -1 is returned.
     */
    public int nextSetBit(int fromIndex) {
        long[] bits = this.bits;
        int word = fromIndex >>> 6;
        int bitsLength = bits.length;
        if (word >= bitsLength)
            return -1;
        long bitsAtWord = bits[word];
        if (bitsAtWord != 0) {
            for (int i = fromIndex & 0x3f; i < 64; i++) {
                if ((bitsAtWord & (1L << (i & 0x3F))) != 0L) {
                    return (word << 6) + i;
                }
            }
        }
        for (word++; word < bitsLength; word++) {
            if (word != 0) {
                bitsAtWord = bits[word];
                if (bitsAtWord != 0) {
                    for (int i = 0; i < 64; i++) {
                        if ((bitsAtWord & (1L << (i & 0x3F))) != 0L) {
                            return (word << 6) + i;
                        }
                    }
                }
            }
        }
        return -1;
    }

    /** Returns the index of the first bit that is set to false that occurs on or after the specified starting index. */
    public int nextClearBit(int fromIndex) {
        long[] bits = this.bits;
        int word = fromIndex >>> 6;
        int bitsLength = bits.length;
        if (word >= bitsLength)
            return bits.length << 6;
        long bitsAtWord = bits[word];
        for (int i = fromIndex & 0x3f; i < 64; i++) {
            if ((bitsAtWord & (1L << (i & 0x3F))) == 0L) {
                return (word << 6) + i;
            }
        }
        for (word++; word < bitsLength; word++) {
            if (word == 0) {
                return 0;
            }
            bitsAtWord = bits[word];
            for (int i = 0; i < 64; i++) {
                if ((bitsAtWord & (1L << (i & 0x3F))) == 0L) {
                    return (word << 6) + i;
                }
            }
        }
        return bits.length << 6;
    }

    /**
     * Performs a logical <b>AND</b> of this target bit set with the argument bit set. This bit set is modified so that each bit in
     * it has the value true if and only if it both initially had the value true and the corresponding bit in the bit set argument
     * also had the value true.
     *
     * @param other a bit set
     */
    public Bits and(Bits other) {
        int commonWords = FastMath.min(bits.length, other.bits.length);
        for (int i = 0; commonWords > i; i++) {
            bits[i] &= other.bits[i];
        }

        if (bits.length > commonWords) {
            for (int i = commonWords, s = bits.length; s > i; i++) {
                bits[i] = 0L;
            }
        }
        return this;
    }

    /**
     * Clears all the bits in this bit set whose corresponding bit is set in the specified bit set.
     *
     * @param other a bit set
     */
    public Bits andNot(Bits other) {
        for (int i = 0, j = bits.length, k = other.bits.length; i < j && i < k; i++) {
            bits[i] &= ~other.bits[i];
        }
        return this;
    }

    /**
     * Performs a logical <b>OR</b> of this bit set with the bit set argument. This bit set is modified so that a bit in it has the
     * value true if and only if it either already had the value true or the corresponding bit in the bit set argument has the
     * value true.
     *
     * @param other a bit set
     */
    public Bits or(Bits other) {
        int commonWords = FastMath.min(bits.length, other.bits.length);
        for (int i = 0; commonWords > i; i++) {
            bits[i] |= other.bits[i];
        }

        if (commonWords < other.bits.length) {
            checkCapacity(other.bits.length);
            for (int i = commonWords, s = other.bits.length; s > i; i++) {
                bits[i] = other.bits[i];
            }
        }
        return this;
    }

    /**
     * Performs a logical <b>XOR</b> of this bit set with the bit set argument. This bit set is modified so that a bit in it has
     * the value true if and only if one of the following statements holds:
     * <ul>
     * <li>The bit initially has the value true, and the corresponding bit in the argument has the value false.</li>
     * <li>The bit initially has the value false, and the corresponding bit in the argument has the value true.</li>
     * </ul>
     *
     * @param other The other instance.
     */
    public Bits xor(Bits other) {
        int commonWords = FastMath.min(bits.length, other.bits.length);

        for (int i = 0; commonWords > i; i++) {
            bits[i] ^= other.bits[i];
        }

        if (commonWords < other.bits.length) {
            checkCapacity(other.bits.length);
            for (int i = commonWords, s = other.bits.length; s > i; i++) {
                bits[i] = other.bits[i];
            }
        }
        return this;
    }

    /**
     * Returns true if the specified BitSet has any bits set to true that are also set to true in this BitSet.
     *
     * @param other a bit set
     *
     * @return boolean indicating whether this bit set intersects the specified bit set
     */
    public boolean intersects(Bits other) {
        long[] bits = this.bits;
        long[] otherBits = other.bits;
        for (int i = FastMath.min(bits.length, otherBits.length) - 1; i >= 0; i--) {
            if ((bits[i] & otherBits[i]) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this bit set is a super set of the specified set, i.e. it has all bits set to true that are also set to true
     * in the specified BitSet.
     *
     * @param other a bit set
     *
     * @return boolean indicating whether this bit set is a super set of the specified set
     */
    public boolean containsAll(Bits other) {
        long[] bits = this.bits;
        long[] otherBits = other.bits;
        int otherBitsLength = otherBits.length;
        int bitsLength = bits.length;

        for (int i = bitsLength; i < otherBitsLength; i++) {
            if (otherBits[i] != 0) {
                return false;
            }
        }
        for (int i = FastMath.min(bitsLength, otherBitsLength) - 1; i >= 0; i--) {
            if ((bits[i] & otherBits[i]) != otherBits[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int word = length() >>> 6;
        int hash = 0;
        for (int i = 0; word >= i; i++) {
            hash = 127 * hash + Long.hashCode(bits[i]);
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        Bits other = (Bits) obj;
        long[] otherBits = other.bits;

        int commonWords = FastMath.min(bits.length, otherBits.length);
        for (int i = 0; commonWords > i; i++) {
            if (bits[i] != otherBits[i])
                return false;
        }

        if (bits.length == otherBits.length)
            return true;

        return length() == other.length();
    }

    public Bits copy() {
        int n = this.numBits();
        Bits result = new Bits(n);
        for (int i = 0; i < n; i++) {
            if (this.get(i))
                result.set(i);
        }
        return result;
    }

    public int compareTo(Bits other) {
        if (this.bits.length != other.bits.length) {
            return -1;
        }
        int n = this.bits.length;
        for (int i = 0; i < n; i++) {
            if (get(i) && !other.get(i))
                return 1;
            if (!get(i) && other.get(i))
                return -1;
        }
        return 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (long bit : bits) {
            sb.append(bit);
        }
        return sb.toString();
    }
}
