/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter;

import com.badlogic.gdx.utils.Array;
import gaiasky.scene.api.IParticleRecord;

/**
 * A filter on a dataset as a set of rules
 */
public class Filter {
    private final Array<FilterRule> rules;
    private IOperation operation;

    /**
     * Creates a filter with only one rule
     *
     * @param rule
     */
    public Filter(FilterRule rule) {
        this.rules = new Array<>();
        this.rules.add(rule);
        this.operation = new OperationAnd();
    }

    /**
     * Creates a new filter with the given rules and linking operation
     *
     * @param rules     The rules list
     * @param operation The operation: 'and', 'or'
     */
    public Filter(String operation, FilterRule... rules) {
        this.rules = new Array<>(rules);
        this.operation = getOperationFromString(operation);
    }

    public Filter(String operation, Array<FilterRule> rules) {
        this.rules = rules;
        this.operation = getOperationFromString(operation);
    }

    public Filter deepCopy() {
        Array<FilterRule> rulesCopy = new Array<>(false, rules.size);
        for (int i = 0; i < rules.size; i++) {
            rulesCopy.add(rules.get(i).copy());
        }
        Filter copy = new Filter(operation.getOperationString(), rulesCopy);
        return copy;
    }

    public boolean evaluate(IParticleRecord pb) {
        synchronized (this) {
            return operation.evaluate(rules, pb);
        }
    }

    public boolean hasRules() {
        return rules != null && rules.size > 0;
    }

    public Array<FilterRule> getRules() {
        return rules;
    }

    public IOperation getOperation() {
        return operation;
    }

    public void setOperation(String op) {
        this.operation = getOperationFromString(op);
    }

    public String getOperationString() {
        return operation != null ? operation.getOperationString() : null;
    }

    public IOperation getOperationFromString(String op) {
        switch (op.toLowerCase()) {
        case "or":
            return new OperationOr();
        case "xor":
            return new OperationXor();
        case "and":
        default:
            return new OperationAnd();
        }
    }

    public void addRule(FilterRule rule) {
        rules.add(rule);
    }

    public boolean removeRule(FilterRule rule) {
        return rules.removeValue(rule, true);
    }

    private interface IOperation {
        boolean evaluate(Array<FilterRule> rules, IParticleRecord pb);

        String getOperationString();
    }

    private class OperationAnd implements IOperation {
        public String op;

        public OperationAnd() {
            this.op = "and";
        }

        @Override
        public boolean evaluate(Array<FilterRule> rules, IParticleRecord bean) {
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
        public boolean evaluate(Array<FilterRule> rules, IParticleRecord bean) {
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

    private class OperationXor implements IOperation {
        public String op;

        public OperationXor() {
            this.op = "xor";
        }

        @Override
        public boolean evaluate(Array<FilterRule> rules, IParticleRecord bean) {
            boolean result = false;
            for (FilterRule rule : rules) {
                result = result ^ rule.evaluate(bean);
            }
            return result;
        }

        @Override
        public String getOperationString() {
            return op;
        }
    }
}
