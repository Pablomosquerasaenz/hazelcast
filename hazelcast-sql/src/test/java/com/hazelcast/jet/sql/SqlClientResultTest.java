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

package com.hazelcast.jet.sql;

import com.hazelcast.sql.HazelcastSqlException;
import com.hazelcast.sql.SqlResult;
import com.hazelcast.sql.SqlRow;
import com.hazelcast.sql.SqlStatement;
import com.hazelcast.sql.impl.SqlErrorCode;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({QuickTest.class, ParallelJVMTest.class})
public class SqlClientResultTest extends SqlTestSupport {

    private static final String MAP_NAME = "map";
    private static final String SQL_GOOD = "SELECT * FROM " + MAP_NAME;
    private static final String SQL_BAD = "SELECT * FROM " + MAP_NAME + "_bad";

    @BeforeClass
    public static void setUpClass() {
        initializeWithClient(1, null, null);
    }

    @Before
    public void before() {
        createMapping(MAP_NAME, int.class, int.class);
        Map<Integer, Integer> map = instance().getMap(MAP_NAME);
        map.put(0, 0);
        map.put(1, 1);
    }

    @Test
    public void testQuery() {
        try (SqlResult result = execute(SQL_GOOD)) {
            assertEquals(2, result.getRowMetadata().getColumnCount());

            assertEquals(-1, result.updateCount());
            assertTrue(result.isRowSet());

            Iterator<SqlRow> iterator = result.iterator();
            iterator.next();
            iterator.next();
            assertFalse(iterator.hasNext());

            checkIllegalStateException(result::iterator, "Iterator can be requested only once");
        }
    }

    @Test
    public void testBadQuery() {
        checkSqlException(() -> execute(SQL_BAD), SqlErrorCode.OBJECT_NOT_FOUND, "Object 'map_bad' not found");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testCloseBetweenFetches() {
        try (SqlResult result = execute(SQL_GOOD)) {
            assertEquals(2, result.getRowMetadata().getColumnCount());

            assertEquals(-1, result.updateCount());
            assertTrue(result.isRowSet());

            Iterator<SqlRow> iterator = result.iterator();
            iterator.next();

            result.close();

            checkSqlException(iterator::hasNext, SqlErrorCode.CANCELLED_BY_USER, "Query was cancelled by the user");
            checkSqlException(iterator::next, SqlErrorCode.CANCELLED_BY_USER, "Query was cancelled by the user");
        }
    }

    private void checkSqlException(Runnable task, int expectedCode, String expectedMessage) {
        HazelcastSqlException err = assertThrows(HazelcastSqlException.class, task);

        assertEquals(expectedCode, err.getCode());
        assertTrue(err.getMessage(), err.getMessage().contains(expectedMessage));
    }

    private void checkIllegalStateException(Runnable task, String expectedMessage) {
        IllegalStateException err = assertThrows(IllegalStateException.class, task);

        assertEquals(expectedMessage, err.getMessage());
    }

    private SqlResult execute(String sql) {
        return client().getSql().execute(new SqlStatement(sql).setCursorBufferSize(1));
    }
}
