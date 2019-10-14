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
        switch (operation.toLowerCase()) {
        case "or":
            this.operation = new OperationOr();
            break;
        case "and":
        default:
            this.operation = new OperationAnd();
            break;
        }

    }

    public boolean evaluate(ParticleBean pb) {
        return operation.evaluate(rules, pb);
    }

    private interface IOperation {
        boolean evaluate(FilterRule[] rules, ParticleBean pb);
    }

    private class OperationAnd implements IOperation {
        @Override
        public boolean evaluate(FilterRule[] rules, ParticleBean bean) {
            boolean result = true;
            for (FilterRule rule : rules) {
                result = result && rule.evaluate(bean);
            }
            return result;
        }
    }

    private class OperationOr implements IOperation {
        @Override
        public boolean evaluate(FilterRule[] rules, ParticleBean bean) {
            boolean result = false;
            for (FilterRule rule : rules) {
                result = result || rule.evaluate(bean);
            }
            return result;
        }
    }
}
