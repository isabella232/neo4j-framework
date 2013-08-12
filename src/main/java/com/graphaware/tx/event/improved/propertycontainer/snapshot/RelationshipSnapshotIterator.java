/*
 * Copyright (c) 2013 GraphAware
 *
 * This file is part of GraphAware.
 *
 * GraphAware is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.tx.event.improved.propertycontainer.snapshot;

import com.graphaware.tx.event.improved.data.TransactionDataContainer;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.collection.PrefetchingIterator;

import java.util.Iterator;

/**
 * {@link NodeSnapshot}'s {@link org.neo4j.graphdb.Relationship} iterator.
 */
public class RelationshipSnapshotIterator extends PrefetchingIterator<Relationship> implements Iterator<Relationship>, Iterable<Relationship> {

    private final Iterator<Relationship> wrappedIterator;
    private final TransactionDataContainer transactionDataContainer;
    private final Iterator<Relationship> deletedRelationshipIterator;

    public RelationshipSnapshotIterator(Node wrappedNode, TransactionDataContainer transactionDataContainer) {
        this.wrappedIterator = wrappedNode.getRelationships().iterator();
        this.transactionDataContainer = transactionDataContainer;
        this.deletedRelationshipIterator = transactionDataContainer.getRelationshipTransactionData().getDeleted(wrappedNode).iterator();
    }

    public RelationshipSnapshotIterator(Node wrappedNode, TransactionDataContainer transactionDataContainer, RelationshipType... types) {
        this.wrappedIterator = wrappedNode.getRelationships(types).iterator();
        this.transactionDataContainer = transactionDataContainer;
        this.deletedRelationshipIterator = transactionDataContainer.getRelationshipTransactionData().getDeleted(wrappedNode, types).iterator();
    }

    public RelationshipSnapshotIterator(Node wrappedNode, TransactionDataContainer transactionDataContainer, Direction direction, RelationshipType... types) {
        this.wrappedIterator = wrappedNode.getRelationships(direction, types).iterator();
        this.transactionDataContainer = transactionDataContainer;
        this.deletedRelationshipIterator = transactionDataContainer.getRelationshipTransactionData().getDeleted(wrappedNode, direction, types).iterator();
    }

    public RelationshipSnapshotIterator(Node wrappedNode, TransactionDataContainer transactionDataContainer, Direction direction) {
        this.wrappedIterator = wrappedNode.getRelationships(direction).iterator();
        this.transactionDataContainer = transactionDataContainer;
        this.deletedRelationshipIterator = transactionDataContainer.getRelationshipTransactionData().getDeleted(wrappedNode, direction).iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Relationship> iterator() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Relationship fetchNextOrNull() {
        while (wrappedIterator.hasNext()) {
            Relationship next = wrappedIterator.next();
            if (transactionDataContainer.getRelationshipTransactionData().hasBeenCreated(next)) {
                //just created - wasn't there before the TX started
                continue;
            }

            if (transactionDataContainer.getRelationshipTransactionData().hasBeenChanged(next)) {
                return transactionDataContainer.getRelationshipTransactionData().getChanged(next).getPrevious();
            }

            return new RelationshipSnapshot(next, transactionDataContainer);
        }

        if (deletedRelationshipIterator.hasNext()) {
            return deletedRelationshipIterator.next();
        }

        return null;
    }
}