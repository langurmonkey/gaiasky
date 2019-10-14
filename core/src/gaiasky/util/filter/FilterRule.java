/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter;

import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.util.filter.attrib.IAttribute;

public class FilterRule {
    // Value in the same units as the one internal units
    double value;
    IComparator comparator;
    IAttribute attribute;

    /**
     * Creates a new filter with the given attribute, value and comparator function
     * @param comp The comparator function: '>', '>=', '<', '<=', '==', '!='
     * @param attr The attribute to compare
     * @param val The value to compare to
     */
    public FilterRule(String comp, IAttribute attr, double val){
        this.attribute = attr;
        this.value = val;

        switch(comp){
        case ">":
            this.comparator = new ComparatorG();
            break;
        case ">=":
            this.comparator = new ComparatorGeq();
            break;
        case "<":
            this.comparator = new ComparatorL();
            break;
        case "<=":
            this.comparator = new ComparatorLeq();
            break;
        case "==":
            this.comparator = new ComparatorEq();
            break;
        case "!=":
            this.comparator = new ComparatorNeq();
            break;
        }
    }

    public boolean evaluate(ParticleBean bean){
        return comparator.evaluate(attribute.get(bean), value);
    }

    // COMPARATORS
    public interface IComparator {
        boolean evaluate(double val1, double val2);
    }

    public class ComparatorGeq implements IComparator{
        @Override
        public boolean evaluate(double val1, double val2) {
            return val1 >= val2;
        }
    }
    public class ComparatorG implements IComparator{
        @Override
        public boolean evaluate(double val1, double val2) {
            return val1 > val2;
        }
    }
    public class ComparatorLeq implements IComparator{
        @Override
        public boolean evaluate(double val1, double val2) {
            return val1 <= val2;
        }
    }
    public class ComparatorL implements IComparator{
        @Override
        public boolean evaluate(double val1, double val2) {
            return val1 < val2;
        }
    }
    public class ComparatorEq implements IComparator{
        @Override
        public boolean evaluate(double val1, double val2) {
            return val1 == val2;
        }
    }
    public class ComparatorNeq implements IComparator{
        @Override
        public boolean evaluate(double val1, double val2) {
            return val1 != val2;
        }
    }
}
