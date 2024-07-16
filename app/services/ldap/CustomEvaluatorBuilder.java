/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
/*
 *  Original source: ApacheDS "EvaluatorBuilder" class.
 *  This code has been modified to support OBJECTCLASS assertionTypes.
 */
package services.ldap;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.*;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.exception.NotImplementedException;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.xdbm.search.evaluator.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CustomEvaluatorBuilder {
    private final Store db;
    private final SchemaManager schemaManager;
    private static final EmptyEvaluator EMPTY_EVALUATOR = new EmptyEvaluator();

    public CustomEvaluatorBuilder(Store db, SchemaManager schemaManager) {
        this.db = db;
        this.schemaManager = schemaManager;
    }

    public <T> Evaluator<? extends ExprNode> build(PartitionTxn partitionTxn, ExprNode node) throws LdapException {
        Object count = node.get("count");
        if (count != null && (Long)count == 0L) {
            return EMPTY_EVALUATOR;
        } else {
            switch (node.getAssertionType()) {
                case APPROXIMATE:
                    return new ApproximateEvaluator((ApproximateNode)node, this.db, this.schemaManager);
                case EQUALITY:
                    return new EqualityEvaluator((EqualityNode)node, this.db, this.schemaManager);
                case GREATEREQ:
                    return new GreaterEqEvaluator((GreaterEqNode)node, this.db, this.schemaManager);
                case LESSEQ:
                    return new LessEqEvaluator((LessEqNode)node, this.db, this.schemaManager);
                case PRESENCE:
                    return new PresenceEvaluator((PresenceNode)node, this.db, this.schemaManager);
                case SCOPE:
                    if (((ScopeNode)node).getScope() == SearchScope.ONELEVEL) {
                        return new OneLevelScopeEvaluator(this.db, (ScopeNode)node);
                    }

                    return new SubtreeScopeEvaluator(partitionTxn, this.db, (ScopeNode)node);
                case SUBSTRING:
                    return new SubstringEvaluator((SubstringNode)node, this.db, this.schemaManager);
                case AND:
                    return this.buildAndEvaluator(partitionTxn, (AndNode)node);
                case NOT:
                    return new NotEvaluator((NotNode)node, this.build(partitionTxn, ((NotNode)node).getFirstChild()));
                case OR:
                    return this.buildOrEvaluator(partitionTxn, (OrNode)node);
                case UNDEFINED:
                    return new EmptyEvaluator();
                case ASSERTION:
                case EXTENSIBLE:
                    throw new NotImplementedException();
                case OBJECTCLASS:
                    return new PassThroughEvaluator(db);
                default:
                    throw new IllegalStateException(I18n.err(I18n.ERR_260, new Object[]{node.getAssertionType()}));
            }
        }
    }

    private <T> Evaluator<? extends ExprNode> buildAndEvaluator(PartitionTxn partitionTxn, AndNode node) throws LdapException {
        List<ExprNode> children = node.getChildren();
        List<Evaluator<? extends ExprNode>> evaluators = this.buildList(partitionTxn, children);
        int size = evaluators.size();
        switch (size) {
            case 0:
                return EMPTY_EVALUATOR;
            case 1:
                return (Evaluator)evaluators.get(0);
            default:
                return new AndEvaluator(node, evaluators);
        }
    }

    private <T> Evaluator<? extends ExprNode> buildOrEvaluator(PartitionTxn partitionTxn, OrNode node) throws LdapException {
        List<ExprNode> children = node.getChildren();
        List<Evaluator<? extends ExprNode>> evaluators = this.buildList(partitionTxn, children);
        int size = evaluators.size();
        switch (size) {
            case 0:
                return EMPTY_EVALUATOR;
            case 1:
                return (Evaluator)evaluators.get(0);
            default:
                return new OrEvaluator(node, evaluators);
        }
    }

    private List<Evaluator<? extends ExprNode>> buildList(PartitionTxn partitionTxn, List<ExprNode> children) throws LdapException {
        List<Evaluator<? extends ExprNode>> evaluators = new ArrayList(children.size());
        Iterator var4 = children.iterator();

        while(var4.hasNext()) {
            ExprNode child = (ExprNode)var4.next();
            Evaluator<? extends ExprNode> evaluator = this.build(partitionTxn, child);
            if (evaluator != null) {
                evaluators.add(evaluator);
            }
        }

        return evaluators;
    }

    public SchemaManager getSchemaManager() {
        return this.schemaManager;
    }
}
