/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hazelcast.sql.impl.calcite.statistics;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.map.impl.MapService;
import com.hazelcast.map.impl.proxy.MapProxyImpl;
import com.hazelcast.replicatedmap.impl.ReplicatedMapProxy;
import com.hazelcast.replicatedmap.impl.ReplicatedMapService;

/**
 * Default implementation of statistics provider.
 */
// TODO: Create a wrapper which will supply cached statistics and update them periodically.
public class DefaultStatisticProvider implements StatisticProvider {
    @Override
    public long getRowCount(DistributedObject container) {
        assert container != null;

        String serviceName = container.getServiceName();

        assert serviceName != null;

        if (serviceName.equals(MapService.SERVICE_NAME)) {
            return ((MapProxyImpl) container).size();
        } else {
            assert serviceName.equals(ReplicatedMapService.SERVICE_NAME) : serviceName;

            return ((ReplicatedMapProxy) container).size();
        }
    }
}
