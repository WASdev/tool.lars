/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
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
 *******************************************************************************/
package com.ibm.ws.lars.rest;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;

import org.junit.Test;

import com.ibm.ws.lars.rest.Condition.Operation;
import com.ibm.ws.lars.rest.SortOptions.SortOrder;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * This is a set of basic unit tests for the search logic in
 * {@link PersistenceBean#retrieveAllAssets(java.util.Map, String, PaginationOptions, SortOptions)}
 * , where those tests don't require replicating any database logic. Tests that do require database
 * logic are written as FAT tests in {@link PersistenceBeanTest}.
 */
public class PersistenceBeanBasicSearchTest {

    @Mocked
    com.mongodb.DB db;

    @Mocked
    Logger logger;

    /**
     * There isn't an easy way to magically inject the database into the persistence bean, as it is
     * normally injected by the container, so this is a utility method to inject it
     */
    private PersistenceBean createTestBean() {
        PersistenceBean bean = new PersistenceBean();
        Deencapsulation.setField(bean, "db", db);
        return bean;
    }

    /**
     * Test that an empty list + null search string results in a call to retrieve everything
     */
    @Test
    public void testRetrieveAllAssetsEmptyParams(final @Mocked DBCollection collection) {

        new Expectations() {
            {
                collection.find();
            }
        };

        createTestBean().retrieveAllAssets(new HashMap<String, List<Condition>>(), null, null, null);
    }

    /**
     * Test that a list of filters and no search string result in a database query with no search
     * parameter.
     *
     * @param collection
     */
    @Test
    public void testRetrieveAllAssetsNoSearch(final @Mocked DBCollection collection) {

        new Expectations() {
            {

                collection.find((DBObject) withNotNull(), (DBObject) withNull());
            }
        };

        HashMap<String, List<Condition>> filters = new HashMap<String, List<Condition>>();
        filters.put("key1", Arrays.asList(new Condition[] { new Condition(Operation.EQUALS, "value1") }));
        createTestBean().retrieveAllAssets(filters, null, null, null);
    }

    /**
     * Test that providing a PaginationOptions object results in the correct skip() and limit()
     * methods being called on the result cursor.
     */
    @Test
    public void testRetrieveAllAssetsPaginated(final @Mocked DBCollection collection, final @Injectable DBCursor cursor) {

        new Expectations() {
            {

                collection.find((DBObject) withNotNull(), (DBObject) withNull());
                result = cursor;
                cursor.skip(20);
                cursor.limit(10);
            }
        };

        HashMap<String, List<Condition>> filters = new HashMap<String, List<Condition>>();
        filters.put("key1", Arrays.asList(new Condition[] { new Condition(Operation.EQUALS, "value1") }));
        PaginationOptions pagination = new PaginationOptions(20, 10);
        createTestBean().retrieveAllAssets(filters, null, pagination, null);
    }

    /**
     * Test that providing a SortOptions object results in the correct sort() method being called on
     * the result cursor
     */
    public void testRetrieveAllAssetsSorted(final @Mocked DBCollection collection, final @Injectable DBCursor cursor) {
        final DBObject sortObject = new BasicDBObject("key2", -1);
        new Expectations() {
            {
                collection.find((DBObject) withNotNull(), (DBObject) withNull());
                result = cursor;
                cursor.sort(sortObject);
            }
        };

        HashMap<String, List<Condition>> filters = new HashMap<String, List<Condition>>();
        filters.put("key1", Arrays.asList(new Condition[] { new Condition(Operation.EQUALS, "value1") }));
        SortOptions sortOptions = new SortOptions("key2", SortOrder.DESCENDING);
        createTestBean().retrieveAllAssets(filters, null, null, sortOptions);
    }

    /**
     * Test that an empty list of filters and a simple search string result in a database query with
     * a sort condition, and then a sort.
     *
     */
    @Test
    public void testRetrieveAllAssetsWithSearch(final @Mocked DBCollection collection, final @Mocked DBCursor cursor) {
        BasicDBList list = new BasicDBList();
        list.add(new BasicDBObject("$text", new BasicDBObject("$search", "foo")));
        final BasicDBObject searchObject = new BasicDBObject("$and", list);
        final BasicDBObject sortObject = new BasicDBObject("score", new BasicDBObject("$meta", "textScore"));
        new Expectations() {
            {
                collection.find(searchObject, sortObject);
                cursor.sort(sortObject);
            }
        };

        HashMap<String, List<Condition>> filters = new HashMap<String, List<Condition>>();
        createTestBean().retrieveAllAssets(filters, "foo", null, null);
    }

    /**
     * Test that a non empty list of filters and a simple search string result in a database query
     * with a sort condition, followed by a sort.
     *
     */
    @Test
    public void testRetrieveAllAssetsWithSearchAndFilter(final @Mocked DBCollection collection, final @Mocked DBCursor cursor) {
        final BasicDBObject sortObject = new BasicDBObject("score", new BasicDBObject("$meta", "textScore"));

        new Expectations() {
            {
                collection.find((DBObject) withNotNull(), sortObject);
                cursor.sort(sortObject);
            }
        };

        HashMap<String, List<Condition>> filters = new HashMap<String, List<Condition>>();
        filters.put("key1", Arrays.asList(new Condition[] { new Condition(Operation.EQUALS, "value1") }));
        createTestBean().retrieveAllAssets(filters, "foo", null, null);
    }

    /**
     * Test that a searchTerm and SortingOptions result in a database query with no projection
     * object and a correct sort.
     */
    @Test
    public void testRetrieveAllAssetsWithSearchAndSort(final @Mocked DBCollection collection, final @Mocked DBCursor cursor) {
        final BasicDBObject sortObject = new BasicDBObject("key2", -1);

        new Expectations() {
            {
                collection.find((DBObject) withNotNull(), (DBObject) withNull());
                cursor.sort(sortObject);
            }
        };

        HashMap<String, List<Condition>> filters = new HashMap<String, List<Condition>>();
        filters.put("key1", Arrays.asList(new Condition[] { new Condition(Operation.EQUALS, "value1") }));
        SortOptions sortOptions = new SortOptions("key2", SortOrder.DESCENDING);
        createTestBean().retrieveAllAssets(filters, "foo", null, sortOptions);
    }

    @Test
    public void testCountAllAssets(final @Mocked DBCollection collection, final @Injectable DBCursor cursor) {
        BasicDBList list = new BasicDBList();
        list.add(new BasicDBObject("key1", "value1"));
        final BasicDBObject searchObject = new BasicDBObject("$and", list);

        new Expectations() {
            {
                collection.find(searchObject);
                result = cursor;

                cursor.count();
                result = 3;
            }
        };

        HashMap<String, List<Condition>> filters = new HashMap<>();
        filters.put("key1", Arrays.asList(new Condition[] { new Condition(Operation.EQUALS, "value1") }));
        int count = createTestBean().countAllAssets(filters, null);
        assertEquals(3, count);
    }

    @Test
    public void testCountAllAssetsWithSearch(final @Mocked DBCollection collection, final @Injectable DBCursor cursor) {
        BasicDBList list = new BasicDBList();
        list.add(new BasicDBObject("key1", "value1"));
        list.add(new BasicDBObject("$text", new BasicDBObject("$search", "foo")));
        final BasicDBObject searchObject = new BasicDBObject("$and", list);

        new Expectations() {
            {
                collection.find(searchObject);
                result = cursor;

                cursor.count();
                result = 3;
            }
        };

        HashMap<String, List<Condition>> filters = new HashMap<>();
        filters.put("key1", Arrays.asList(new Condition[] { new Condition(Operation.EQUALS, "value1") }));
        int count = createTestBean().countAllAssets(filters, "foo");
        assertEquals(3, count);
    }

}
