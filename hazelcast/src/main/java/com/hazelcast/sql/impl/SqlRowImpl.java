/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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


package com.hazelcast.sql.impl;

import com.hazelcast.internal.serialization.Data;
import com.hazelcast.sql.SqlColumnMetadata;
import com.hazelcast.sql.SqlRow;
import com.hazelcast.sql.SqlRowMetadata;
import com.hazelcast.sql.impl.row.Row;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.StringJoiner;

/**
 * Default implementation of the SQL row which is exposed to users. We merely wrap the internal row, but add more checks which
 * is important for user-facing code, but which could cause performance degradation if implemented in the internal classes.
 */
public class SqlRowImpl implements SqlRow {

    private final SqlRowMetadata rowMetadata;
    private final Row row;
    private final LazyDeserializer lazyDeserializer;

    public SqlRowImpl(SqlRowMetadata rowMetadata, Row row, LazyDeserializer lazyDeserializer) {
        this.rowMetadata = rowMetadata;
        this.row = row;
        this.lazyDeserializer = lazyDeserializer;
    }

    @Nullable
    @Override
    public <T> T getObject(int columnIndex) {
        checkIndex(columnIndex);

        return getObject0(columnIndex, true);
    }

    @Nullable
    @Override
    public <T> T getObject(@Nonnull String columnName) {
        int columnIndex = resolveIndex(columnName);

        return getObject0(columnIndex, true);
    }

    /**
     * Get column's value without forcing of the deserialization {@link Data}.
     */
    public <T> T getObjectRaw(int columnIndex) {
        checkIndex(columnIndex);

        return getObject0(columnIndex, false);
    }

    @SuppressWarnings("unchecked")
    private <T> T getObject0(int columnIndex, boolean deserialize) {
        Object res = row.get(columnIndex);

        if (res instanceof LazyTarget) {
            LazyTarget res0 = (LazyTarget) res;

            if (deserialize) {
                res = lazyDeserializer.deserialize(res0);
            } else {
                res = res0.getSerialized();

                if (res == null) {
                    res = res0.getDeserialized();
                }
            }
        } else if (deserialize) {
            res = lazyDeserializer.deserialize(res);
        }

        return (T) res;
    }

    private int resolveIndex(String columnName) {
        int index = rowMetadata.findColumn(columnName);

        if (index == SqlRowMetadata.COLUMN_NOT_FOUND) {
            throw new IllegalArgumentException("Column \"" + columnName + "\" doesn't exist");
        }

        return index;
    }

    @Nonnull
    @Override
    public SqlRowMetadata getMetadata() {
        return rowMetadata;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= rowMetadata.getColumnCount()) {
            throw new IndexOutOfBoundsException("Column index is out of range: " + index);
        }
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");

        for (int i = 0; i < rowMetadata.getColumnCount(); i++) {
            SqlColumnMetadata columnMetadata = rowMetadata.getColumn(i);
            Object columnValue = row.get(i);

            joiner.add(columnMetadata.getName() + ' ' + columnMetadata.getType() + '=' + columnValue);
        }

        return joiner.toString();
    }
}
