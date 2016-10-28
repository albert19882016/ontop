package it.unibz.inf.ontop.owlrefplatform.core.optimization;

import it.unibz.inf.ontop.pivotalrepr.*;
import it.unibz.inf.ontop.pivotalrepr.proposal.TrueNodeRemovalProposal;
import it.unibz.inf.ontop.pivotalrepr.proposal.impl.TrueNodeRemovalProposalImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.unibz.inf.ontop.pivotalrepr.NonCommutativeOperatorNode.ArgumentPosition.RIGHT;

/**
 * Removes TrueNodes whenever possible.
 * <p>
 * For each TrueNode n in the query,
 * let p be its parent node.
 * <p>
 * If p is neither a (left or inner) join node or a construction node without projected variable,
 * then no action is taken.
 * <p>
 * If p is a left join node,
 * and n is its left child,
 * then no action is taken either.
 * <p>
 * If p is a construction node without projected variable,
 * then n is removed.
 * <p>
 * If p is an inner join node,
 * or if p is a left join node and n is not its left child,
 * then n is removed,
 * and:
 * - if p has exactly one remaining child n2,
 * then p is replaced by n2
 * - if p has more than one remaining children,
 * nothing happens
 * <p>
 * <p>
 * Several iterations over the whole tree may need to be applied,
 * until no more TrueNode can be removed.
 * The process terminates if no TrueNode has been removed during the latest tree traversal.
 */


public class TrueNodesRemovalOptimizer extends NodeCentricDepthFirstOptimizer<TrueNodeRemovalProposal> {

    private final Logger log = LoggerFactory.getLogger(TrueNodesRemovalOptimizer.class);

    public TrueNodesRemovalOptimizer() {
        super(false);
    }

    @Override
    protected Optional<TrueNodeRemovalProposal> evaluateNode(QueryNode currentNode, IntermediateQuery currentQuery) {
        return Optional.of(currentNode).
                filter(n -> n instanceof TrueNode).
                map(n -> (TrueNode) n).
                filter(n -> isRemovableTrueNode(n, currentQuery)).
                map(TrueNodeRemovalProposalImpl::new);
    }

    private boolean isRemovableTrueNode(TrueNode node, IntermediateQuery query) {
        Optional<QueryNode> parentNode = query.getParent(node);

        return parentNode.isPresent() &&
                (parentNode.get() instanceof InnerJoinNode ||
                        parentNode.get() instanceof ConstructionNode ||
                        parentNode.get() instanceof TrueNode ||
                            (parentNode.get() instanceof LeftJoinNode &&
                                        query.getOptionalPosition(node).get() == RIGHT));
    }

    /**
     * Uses the index of TrueNodes in the current query, instead of the inherited tree traversal method
     * NodeCentricDepthFirstOptimizer.optimizeQuery()
     */
    @Override
    protected IntermediateQuery optimizeQuery(IntermediateQuery intermediateQuery) throws EmptyQueryException {
        boolean iterate = true;
        while (iterate) {
            List<TrueNode> trueNodes = intermediateQuery.getTrueNodes().collect(Collectors.toList());
            iterate = false;
            for (TrueNode trueNode : trueNodes) {
                Optional<TrueNodeRemovalProposal> optionalProposal = evaluateNode(trueNode, intermediateQuery);
                if (optionalProposal.isPresent()) {
                    intermediateQuery.applyProposal(optionalProposal.get());
                    iterate = true;
                }
            }
        }
        return intermediateQuery;
    }
}