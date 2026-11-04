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

    /**
     * How a rule compares its two sides.
     */
    public enum Mode {
        /**
         * Both sides are numbers, or are parsed as numbers: what every rule did before categorical
         * attributes existed.
         */
        NUMERIC,
        /**
         * Both sides are text, compared case-insensitively for equality only. A category has no
         * order, so the ordering comparators are not available in this mode.
         */
        CATEGORICAL
    }

    private Object value;
    private IComparator comparator;
    private IAttribute attribute;
    private Mode mode = Mode.NUMERIC;

    /**
     * Creates a new filter with the given attribute, value and comparator function
     *
     * @param comp The comparator function: '&gt;', '&ge;', '&lt;', '&le;', '==', '!='
     * @param attr The attribute to compare.
     * @param val  The value to compare to.
     */
    public FilterRule(String comp, IAttribute attr, Object val) {
        this(Mode.NUMERIC, comp, attr, val);
    }

    /**
     * Creates a new filter with the given mode, attribute, value and comparator function.
     *
     * @param mode The comparison mode.
     * @param comp The comparator function: '&gt;', '&ge;', '&lt;', '&le;', '==', '!='. Only the two
     *             equality comparators are kept in {@link Mode#CATEGORICAL}.
     * @param attr The attribute to compare.
     * @param val  The value to compare to.
     */
    public FilterRule(Mode mode, String comp, IAttribute attr, Object val) {
        this.attribute = attr;
        this.value = val;
        this.mode = mode != null ? mode : Mode.NUMERIC;

        this.comparator = getComparatorFromString(comp);
    }

    public boolean evaluate(IParticleRecord bean) {
        Object attrValue = attribute.get(bean);
        return mode == Mode.CATEGORICAL ? evaluateCategorical(attrValue, value) : comparator.evaluate(attrValue, value);
    }

    /**
     * Equality test of two categories.
     * <p>
     * Blank cells are common in catalogue columns, so every flavour of them - absent column, null,
     * empty text, text that is only spaces - reads as the empty category. It is unequal to every real
     * category, so {@code !=} accepts it and {@code ==} with an empty value selects it. That is the
     * opposite of what the numeric mode does with a missing value, and the reason the two modes
     * cannot share a path.
     *
     * @param attrValue The value of the attribute in the record.
     * @param ruleValue The value the rule compares against.
     *
     * @return The result of the comparison.
     */
    private boolean evaluateCategorical(Object attrValue, Object ruleValue) {
        boolean equal = asText(attrValue).equalsIgnoreCase(asText(ruleValue));
        return comparator instanceof ComparatorNeq ? !equal : equal;
    }

    /**
     * Renders a value as the text a category is stored as. The filter window shows this same text, so
     * that the rule reads as what it compares.
     * <p>
     * Surrounding whitespace is dropped: a padded cell and a clean one are the same category, and a
     * cell that holds a single space is a blank cell.
     *
     * @param o The value, possibly null.
     *
     * @return The value as text.
     */
    public static String asText(Object o) {
        if (o == null) {
            return "";
        }
        if (o instanceof Number n) {
            double d = n.doubleValue();
            // 5 and "5" name the same category; "5.0" would not.
            if (!Double.isInfinite(d) && d == Math.rint(d) && Math.abs(d) < 1e15) {
                return Long.toString((long) d);
            }
        }
        return o.toString().trim();
    }

    public FilterRule copy() {
        return new FilterRule(mode, comparator.toString(), attribute, value);
    }

    public Mode getMode() {
        return mode;
    }

    /**
     * Sets the comparison mode. Switching to {@link Mode#CATEGORICAL} replaces an ordering comparator
     * with equality, as there is no sensible ordering of categories.
     *
     * @param mode The new mode, or null for {@link Mode#NUMERIC}.
     */
    public void setMode(Mode mode) {
        this.mode = mode != null ? mode : Mode.NUMERIC;
        if (this.mode == Mode.CATEGORICAL) {
            this.comparator = asCategoricalComparator(this.comparator);
        }
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
        this.comparator = mode == Mode.CATEGORICAL ? asCategoricalComparator(comp) : comp;
    }

    public IAttribute getAttribute() {
        return attribute;
    }

    public void setAttribute(IAttribute attr) {
        this.attribute = attr;
    }

    /**
     * Gets the comparator for the given operator, in the current mode. In
     * {@link Mode#CATEGORICAL} an ordering operator yields {@code ==}, since {@code >} has no meaning
     * for a category.
     *
     * @param c The operator: '&gt;', '&ge;', '&lt;', '&le;', '==', '!='.
     *
     * @return The comparator.
     */
    public IComparator getComparatorFromString(String c) {
        var comp = comparatorFromString(c);
        return mode == Mode.CATEGORICAL ? asCategoricalComparator(comp) : comp;
    }

    private static IComparator comparatorFromString(String c) {
        return switch (c) {
            case ">=" -> new ComparatorGeq();
            case "<" -> new ComparatorL();
            case "<=" -> new ComparatorLeq();
            case "==" -> new ComparatorEq();
            case "!=" -> new ComparatorNeq();
            default -> new ComparatorG();
        };
    }

    private static IComparator asCategoricalComparator(IComparator comp) {
        return comp instanceof ComparatorNeq ? new ComparatorNeq() : new ComparatorEq();
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
                return stringComparator.compare(s1, s2) > 0;
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
