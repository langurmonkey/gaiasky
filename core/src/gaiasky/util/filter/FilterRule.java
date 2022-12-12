/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter;

import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.filter.attrib.IAttribute;

public class FilterRule {
    // Value in the same units as the one internal units
    private double value;
    private IComparator comparator;
    private IAttribute attribute;

    /**
     * Creates a new filter with the given attribute, value and comparator function
     *
     * @param comp The comparator function: '>', '>=', '<', '<=', '==', '!='
     * @param attr The attribute to compare
     * @param val  The value to compare to
     */
    public FilterRule(String comp, IAttribute attr, double val) {
        this.attribute = attr;
        this.value = val;

        this.comparator = getComparatorFromString(comp);
    }

    public boolean evaluate(IParticleRecord bean) {
        return comparator.evaluate(attribute.get(bean), value);
    }

    public FilterRule copy() {
        FilterRule cpy = new FilterRule(comparator.toString(), attribute, value);
        return cpy;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public IComparator getComparator() {
        return comparator;
    }

    public void setComparator(IComparator comp) {
        this.comparator = comp;
    }

    public IAttribute getAttribute() {
        return attribute;
    }

    public void setAttribute(IAttribute attr) {
        this.attribute = attr;
    }

    public IComparator getComparatorFromString(String c) {
        switch (c) {
        case ">":
        default:
            return new ComparatorG();
        case ">=":
            return new ComparatorGeq();
        case "<":
            return new ComparatorL();
        case "<=":
            return new ComparatorLeq();
        case "==":
            return new ComparatorEq();
        case "!=":
            return new ComparatorNeq();
        }
    }

    // COMPARATORS
    public interface IComparator {

        boolean evaluate(double val1, double val2);

        String toString();

    }

    public class ComparatorGeq implements IComparator {
        @Override
        public boolean evaluate(double val1, double val2) {
            return val1 >= val2;
        }

        @Override
        public String toString() {
            return ">=";
        }
    }

    public class ComparatorG implements IComparator {
        @Override
        public boolean evaluate(double val1, double val2) {
            return val1 > val2;
        }

        @Override
        public String toString() {
            return ">";
        }
    }

    public class ComparatorLeq implements IComparator {
        @Override
        public boolean evaluate(double val1, double val2) {
            return val1 <= val2;
        }

        @Override
        public String toString() {
            return "<=";
        }
    }

    public class ComparatorL implements IComparator {
        @Override
        public boolean evaluate(double val1, double val2) {
            return val1 < val2;
        }

        @Override
        public String toString() {
            return "<";
        }
    }

    public class ComparatorEq implements IComparator {
        @Override
        public boolean evaluate(double val1, double val2) {
            return val1 == val2;
        }

        @Override
        public String toString() {
            return "==";
        }
    }

    public class ComparatorNeq implements IComparator {
        @Override
        public boolean evaluate(double val1, double val2) {
            return val1 != val2;
        }

        @Override
        public String toString() {
            return "!=";
        }
    }
}
