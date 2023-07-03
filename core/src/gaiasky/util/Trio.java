/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

/**
 * A simple data structure that holds three objects.
 *
 * @param <A> First object type.
 * @param <B> Second object type.
 * @param <C> Third object type.
 */
public class Trio<A, B, C> {

    private A first;
    private B second;
    private C third;

    public Trio(A first, B second, C third) {
        super();
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public int hashCode() {
        int hashFirst = first != null ? first.hashCode() : 0;
        int hashSecond = second != null ? second.hashCode() : 0;
        int hashThird = third != null ? third.hashCode() : 0;

        return (hashFirst + hashSecond + hashThird) * hashThird + (hashFirst + hashSecond) * hashSecond + hashFirst;
    }

    public boolean equals(Object other) {
        if (other instanceof Trio) {
            Trio otherTrio = (Trio) other;
            return ((this.first == null && otherTrio.first == null) || this.first.equals(otherTrio.first)) &&
                    ((this.second == null && otherTrio.second == null) || this.second.equals(otherTrio.second)) &&
                    ((this.third == null && otherTrio.third == null) || this.third.equals(otherTrio.third));
        }

        return false;
    }

    public String toString() {
        return "(" + first + ", " + second + ", " + third + ")";
    }

    public A getFirst() {
        return first;
    }

    public void setFirst(A first) {
        this.first = first;
    }

    public B getSecond() {
        return second;
    }

    public void setSecond(B second) {
        this.second = second;
    }

    public C getThird() {
        return third;
    }

    public void setThird(C third) {
        this.third = third;
    }

}
