/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter;

import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.filter.attrib.IAttribute;
import gaiasky.util.parse.Parser;

import java.util.Comparator;

public class FilterRule {
    private static final Comparator<String> stringComparator = String::compareTo;

    private Object value;
    private IComparator comparator;
    private IAttribute attribute;


    /**
     * Creates a new filter with the given attribute, value and comparator function
     *
     * @param comp The comparator function: '&gt;', '&ge;', '&lt;', '&le;', '==', '!='
     * @param attr The attribute to compare.
     * @param val  The value to compare to.
     */
    public FilterRule(String comp, IAttribute attr, Object val) {
        this.attribute = attr;
        this.value = val;

        this.comparator = getComparatorFromString(comp);
    }

    public boolean evaluate(IParticleRecord bean) {
        return comparator.evaluate(attribute.get(bean), value);
    }

    public FilterRule copy() {
        return new FilterRule(comparator.toString(), attribute, value);
    }

    public Object getValue() {
        return value;
    }

    public double getDoubleValue() {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return Double.NaN;
    }

    public void setValue(Object value) {
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
        return switch (c) {
            default -> new ComparatorG();
            case ">=" -> new ComparatorGeq();
            case "<" -> new ComparatorL();
            case "<=" -> new ComparatorLeq();
            case "==" -> new ComparatorEq();
            case "!=" -> new ComparatorNeq();
        };
    }

    // COMPARATORS
    public interface IComparator {

        boolean evaluate(Object val1, Object val2);

        String toString();

    }

    public static class ComparatorGeq implements IComparator {
        @Override
        public boolean evaluate(Object val1, Object val2) {
            if (val1 instanceof Number n1 && val2 instanceof Number n2)
                return n1.doubleValue() >= n2.doubleValue();
            else if (val1 instanceof String s1 && val2 instanceof String s2) {
                return stringComparator.compare(s1, s2) >= 0;
            } else if (val1 instanceof String s && val2 instanceof Number n) {
                try {
                    return Parser.parseDoubleException(s) >= n.doubleValue();
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return ">=";
        }
    }

    public static class ComparatorG implements IComparator {
        @Override
        public boolean evaluate(Object val1, Object val2) {
            if (val1 instanceof Number n1 && val2 instanceof Number n2)
                return n1.doubleValue() > n2.doubleValue();
            else if (val1 instanceof String s1 && val2 instanceof String s2) {
                return stringComparator.compare(s1, s2) >= 0;
            } else if (val1 instanceof String s && val2 instanceof Number n) {
                try {
                    return Parser.parseDoubleException(s) > n.doubleValue();
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return ">";
        }
    }

    public static class ComparatorLeq implements IComparator {
        @Override
        public boolean evaluate(Object val1, Object val2) {
            if (val1 instanceof Number n1 && val2 instanceof Number n2)
                return n1.doubleValue() <= n2.doubleValue();
            else if (val1 instanceof String s1 && val2 instanceof String s2) {
                return stringComparator.compare(s1, s2) <= 0;
            } else if (val1 instanceof String s && val2 instanceof Number n) {
                try {
                    return Parser.parseDoubleException(s) <= n.doubleValue();
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "<=";
        }
    }

    public static class ComparatorL implements IComparator {
        @Override
        public boolean evaluate(Object val1, Object val2) {
            if (val1 instanceof Number n1 && val2 instanceof Number n2)
                return n1.doubleValue() < n2.doubleValue();
            else if (val1 instanceof String s1 && val2 instanceof String s2) {
                return stringComparator.compare(s1, s2) < 0;
            } else if (val1 instanceof String s && val2 instanceof Number n) {
                try {
                    return Parser.parseDoubleException(s) < n.doubleValue();
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "<";
        }
    }

    public static class ComparatorEq implements IComparator {
        @Override
        public boolean evaluate(Object val1, Object val2) {
            if (val1 instanceof Number n1 && val2 instanceof Number n2)
                return n1.doubleValue() == n2.doubleValue();
            else if (val1 instanceof String s1 && val2 instanceof String s2) {
                return s1.equalsIgnoreCase(s2);
            } else if (val1 instanceof String s && val2 instanceof Number n) {
                try {
                    return Parser.parseDoubleException(s) == n.doubleValue();
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "==";
        }
    }

    public static class ComparatorNeq implements IComparator {
        @Override
        public boolean evaluate(Object val1, Object val2) {
            if (val1 instanceof Number n1 && val2 instanceof Number n2)
                return n1.doubleValue() != n2.doubleValue();
            else if (val1 instanceof String s1 && val2 instanceof String s2) {
                return !s1.equalsIgnoreCase(s2);
            } else if (val1 instanceof String s && val2 instanceof Number n) {
                try {
                    return Parser.parseDoubleException(s) != n.doubleValue();
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "!=";
        }
    }
}
