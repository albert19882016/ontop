package it.unibz.inf.ontop.owlrefplatform.core.optimization;

import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.model.ImmutableSubstitution;
import it.unibz.inf.ontop.model.ImmutableTerm;
import it.unibz.inf.ontop.model.Variable;
import it.unibz.inf.ontop.pivotalrepr.IntermediateQuery;
import it.unibz.inf.ontop.pivotalrepr.QueryNode;

import java.util.Optional;

/**
 *  Retrieve bindings from the sub-tree,
 *  possible different implementations based on the search depth
 *
 *  TODO: explain
 */
public interface BindingExtractor {

    interface Extraction {
        Optional<ImmutableSubstitution<ImmutableTerm>> getOptionalSubstitution();
        ImmutableSet<Variable> getVariablesWithConflictingBindings();
    }

    Extraction extractInSubTree(IntermediateQuery query, QueryNode subTreeRootNode) ;

}