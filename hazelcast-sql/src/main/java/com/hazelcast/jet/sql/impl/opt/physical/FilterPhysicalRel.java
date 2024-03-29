/*
 * Copyright 2021 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.sql.impl.opt.physical;

import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.sql.impl.opt.cost.CostUtils;
import com.hazelcast.sql.impl.QueryParameterMetadata;
import com.hazelcast.sql.impl.expression.Expression;
import com.hazelcast.sql.impl.plan.node.PlanNodeSchema;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexNode;

public class FilterPhysicalRel extends Filter implements PhysicalRel {

    FilterPhysicalRel(
            RelOptCluster cluster,
            RelTraitSet traits,
            RelNode input,
            RexNode condition
    ) {
        super(cluster, traits, input, condition);
    }

    public Expression<Boolean> filter(QueryParameterMetadata parameterMetadata) {
        return filter(schema(parameterMetadata), condition, parameterMetadata);
    }

    @Override
    public PlanNodeSchema schema(QueryParameterMetadata parameterMetadata) {
        return ((PhysicalRel) getInput()).schema(parameterMetadata);
    }

    @Override
    public Vertex accept(CreateDagVisitor visitor) {
        return visitor.onFilter(this);
    }

    @Override
    public final RelWriter explainTerms(RelWriter pw) {
        return super.explainTerms(pw);
    }

    @Override
    public final RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
        double inputRows = mq.getRowCount(getInput());

        double rows = CostUtils.adjustFilteredRowCount(inputRows, mq.getSelectivity(this, condition));
        double cpu = inputRows;

        return planner.getCostFactory().makeCost(rows, cpu, 0);
    }

    @Override
    public final Filter copy(RelTraitSet traitSet, RelNode input, RexNode condition) {
        return new FilterPhysicalRel(getCluster(), traitSet, input, condition);
    }
}
