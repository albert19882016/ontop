package it.unibz.inf.ontop.model.term.functionsymbol.db.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.exception.MinorOntopInternalBugException;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.model.type.DBTermType;
import it.unibz.inf.ontop.model.type.TermType;
import it.unibz.inf.ontop.utils.ImmutableCollectors;

import java.util.stream.IntStream;
import java.util.stream.Stream;


public abstract class AbstractDBStrictEqNeqFunctionSymbol extends DBBooleanFunctionSymbolImpl {

    private final boolean isEq;

    protected AbstractDBStrictEqNeqFunctionSymbol(String name, int arity, boolean isEq,
                                                  TermType rootTermType, DBTermType dbBooleanTermType) {
        super(name + arity, IntStream.range(0, arity)
                .boxed()
                .map(i -> rootTermType)
                .collect(ImmutableCollectors.toList()), dbBooleanTermType);
        this.isEq = isEq;
    }

    @Override
    public boolean isInjective(ImmutableList<? extends ImmutableTerm> arguments, ImmutableSet<Variable> nonNullVariables) {
        return false;
    }

    @Override
    public boolean canBePostProcessed(ImmutableList<? extends ImmutableTerm> arguments) {
        return true;
    }

    @Override
    protected ImmutableTerm buildTermAfterEvaluation(ImmutableList<ImmutableTerm> newTerms,
                                                     boolean isInConstructionNodeInOptimizationPhase,
                                                     TermFactory termFactory) {

        if (newTerms.size() < 2)
            throw new IllegalArgumentException("newTerms must have at least two elements");

        ImmutableSet<ImmutableTerm> nonNullTerms = newTerms.stream()
                .filter(t -> !t.isNull())
                .collect(ImmutableCollectors.toSet());

        if (newTerms.stream()
                .anyMatch(ImmutableTerm::isNull)) {

            if (nonNullTerms.size() <= 1)
                return termFactory.getNullConstant();
            else {
                ImmutableExpression newExpression = isEq
                        ? termFactory.getFalseOrNullFunctionalTerm(ImmutableList.of(termFactory.getStrictEquality(nonNullTerms)))
                        : termFactory.getTrueOrNullFunctionalTerm(ImmutableList.of(termFactory.getStrictNEquality(nonNullTerms)));

                // Indirectly recursive
                return newExpression.simplify(isInConstructionNodeInOptimizationPhase);
            }
        }
        else if (nonNullTerms.size() == 1) {
            return termFactory.getDBBooleanConstant(isEq);
        }
        else {
            return simplifyNonNullTerms(ImmutableList.copyOf(nonNullTerms), termFactory);
        }
    }

    private ImmutableTerm simplifyNonNullTerms(ImmutableList<ImmutableTerm> nonNullTerms,
                                               TermFactory termFactory) {

        ImmutableSet.Builder<ImmutableTerm> remainingTermBuilder = ImmutableSet.builder();
        ImmutableSet.Builder<ImmutableExpression> otherExpressionBuilder = ImmutableSet.builder();

        int size = nonNullTerms.size();

        for(int i=0; i < size; i++) {
            ImmutableTerm term = nonNullTerms.get(i);

            // Non-final
            boolean keepTerm = true;

            for (int j=i+1; (j < size) && keepTerm; j++) {
                ImmutableTerm otherTerm = nonNullTerms.get(j);
                EvaluationResult evaluation = term.evaluateStrictEq(otherTerm);

                switch(evaluation.getStatus()) {
                    case SAME_EXPRESSION:
                        break;
                    case SIMPLIFIED_EXPRESSION:
                        otherExpressionBuilder.add(evaluation.getSimplifiedExpression().get());
                        keepTerm = false;
                        break;
                    case IS_NULL:
                        throw new MinorOntopInternalBugException("Was not expected an equality to be evaluated " +
                                "as NULL which the two arguments were supposed not to be nulls");
                    case IS_FALSE:
                        return termFactory.getDBBooleanConstant(!isEq);
                    case IS_TRUE:
                        keepTerm = false;
                        break;
                }
            }
            if (keepTerm)
                remainingTermBuilder.add(term);
        }

        ImmutableSet<ImmutableTerm> remainingTerms = remainingTermBuilder.build();
        ImmutableSet<ImmutableExpression> otherExpressions = otherExpressionBuilder.build();

        if (remainingTerms.isEmpty())
            return otherExpressions.isEmpty()
                    ? termFactory.getDBBooleanConstant(isEq)
                    : combineExpressions(otherExpressions.stream(), termFactory);

        return combineExpressions(
                Stream.concat(
                        Stream.of(termFactory.getStrictEquality(remainingTerms)),
                        otherExpressions.stream()),
                termFactory);
    }

    /**
     * Non-empty stream
     */
    private ImmutableExpression combineExpressions(Stream<ImmutableExpression> expressions, TermFactory termFactory) {
        return termFactory.getConjunction(expressions)
                .map(c -> isEq ? c : c.negate(termFactory))
                .orElseThrow(() -> new IllegalArgumentException("expressions must not be empty"));
    }

    @Override
    public boolean blocksNegation() {
        return false;
    }

    @Override
    public ImmutableExpression negate(ImmutableList<? extends ImmutableTerm> subTerms, TermFactory termFactory) {
        throw new RuntimeException("TODO: implement the switch of operators");
    }
}
