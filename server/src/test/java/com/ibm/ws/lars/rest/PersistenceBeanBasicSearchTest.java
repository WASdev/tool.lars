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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;

import org.junit.Test;

import com.ibm.ws.lars.rest.Condition.Operation;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * This is a set of basic unit tests for the search logic in
 * {@link PersistenceBean#retrieveAllAssets(java.util.Map, String)} , where those tests don't
 * require replicating any database logic. Tests that do require database logic are written as FAT
 * tests in {@link PersistenceBeanTest}.
 */
public class PersistenceBeanBasicSearchTest {

    @Mocked
    com.mongodb.DB db;

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

        createTestBean().retrieveAllAssets(new HashMap<String, List<Condition>>(), null);
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
        createTestBean().retrieveAllAssets(filters, null);
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
        createTestBean().retrieveAllAssets(filters, "foo");
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
        createTestBean().retrieveAllAssets(filters, "foo");
    }
}
