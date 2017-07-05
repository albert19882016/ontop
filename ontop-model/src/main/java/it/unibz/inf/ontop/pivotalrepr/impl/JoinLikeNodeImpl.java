package it.unibz.inf.ontop.pivotalrepr.impl;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import it.unibz.inf.ontop.evaluator.TermNullabilityEvaluator;
import it.unibz.inf.ontop.model.*;
import it.unibz.inf.ontop.model.impl.ImmutabilityTools;
import it.unibz.inf.ontop.owlrefplatform.core.unfolding.ExpressionEvaluator;
import it.unibz.inf.ontop.pivotalrepr.IntermediateQuery;
import it.unibz.inf.ontop.pivotalrepr.JoinLikeNode;
import it.unibz.inf.ontop.utils.ImmutableCollectors;

import java.util.Optional;
import java.util.stream.Stream;

public abstract class JoinLikeNodeImpl extends JoinOrFilterNodeImpl implements JoinLikeNode {

    protected JoinLikeNodeImpl(Optional<ImmutableExpression> optionalJoinCondition,
                               TermNullabilityEvaluator nullabilityEvaluator) {
        super(optionalJoinCondition, nullabilityEvaluator);
    }

    /**
     * TODO: explain
     */
    protected Optional<ExpressionEvaluator.EvaluationResult> computeAndEvaluateNewCondition(
            ImmutableSubstitution<? extends ImmutableTerm> substitution,
            Optional<ImmutableExpression> optionalNewEqualities) {

        Optional<ImmutableExpression> updatedExistingCondition = getOptionalFilterCondition()
                .map(substitution::applyToBooleanExpression);

        Optional<ImmutableExpression> newCondition = ImmutabilityTools.foldBooleanExpressions(
                Stream.concat(
                    Stream.of(updatedExistingCondition),
                    Stream.of(optionalNewEqualities))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .flatMap(e -> e.flattenAND().stream()));

        return newCondition
                .map(cond -> new ExpressionEvaluator().evaluateExpression(cond));
    }

    protected static ImmutableSet<Variable> union(ImmutableSet<Variable> set1, ImmutableSet<Variable> set2) {
        return Stream.concat(
                set1.stream(),
                set2.stream())
                .collect(ImmutableCollectors.toSet());
    }

    @Override
    public ImmutableSet<Variable> getRequiredVariables(IntermediateQuery query) {
        ImmutableMultiset<Variable> childrenVariableBag = query.getChildren(this).stream()
                .flatMap(c -> query.getVariables(c).stream())
                .collect(ImmutableCollectors.toMultiset());

        Stream<Variable> cooccuringVariableStream = childrenVariableBag.entrySet().stream()
                .filter(e -> e.getCount() > 1)
                .map(Multiset.Entry::getElement);

        return Stream.concat(cooccuringVariableStream, getLocallyRequiredVariables().stream())
                .collect(ImmutableCollectors.toSet());
    }
}