/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter;

import gaiasky.scenegraph.ParticleGroup.ParticleBean;

/**
 * A filter on a dataset as a set of rules
 */
public class Filter {
    private FilterRule[] rules;
    private IOperation operation;

    /**
     * Creates a filter with only one rule
     *
     * @param rule
     */
    public Filter(FilterRule rule) {
        this.rules = new FilterRule[] { rule };
        this.operation = new OperationAnd();
    }

    /**
     * Creates a new filter with the given rules and linking operation
     *
     * @param rules     The rules list
     * @param operation The operation: 'and', 'or'
     */
    public Filter(String operation, FilterRule... rules) {
        this.rules = rules;
        this.operation = getOperationFromString(operation);
    }

    public boolean evaluate(ParticleBean pb) {
        return operation.evaluate(rules, pb);
    }

    public FilterRule[] getRules() {
        return rules;
    }

    public IOperation getOperation() {
        return operation;
    }

    public String getOperationString() {
        return operation != null ? operation.getOperationString() : null;
    }

    public IOperation getOperationFromString(String op) {
        switch (op.toLowerCase()) {
        case "or":
            return new OperationOr();
        case "and":
        default:
            return new OperationAnd();
        }
    }

    private interface IOperation {
        boolean evaluate(FilterRule[] rules, ParticleBean pb);

        String getOperationString();
    }

    private class OperationAnd implements IOperation {
        public String op;

        public OperationAnd() {
            this.op = "and";
        }

        @Override
        public boolean evaluate(FilterRule[] rules, ParticleBean bean) {
            boolean result = true;
            for (FilterRule rule : rules) {
                result = result && rule.evaluate(bean);
            }
            return result;
        }

        @Override
        public String getOperationString() {
            return op;
        }
    }

    private class OperationOr implements IOperation {
        public String op;

        public OperationOr() {
            this.op = "or";
        }

        @Override
        public boolean evaluate(FilterRule[] rules, ParticleBean bean) {
            boolean result = false;
            for (FilterRule rule : rules) {
                result = result || rule.evaluate(bean);
            }
            return result;
        }

        @Override
        public String getOperationString() {
            return op;
        }
    }
}
