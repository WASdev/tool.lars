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

package com.ibm.ws.lars.testutils;

import static org.junit.Assert.assertEquals;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.resources.MassiveResource;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

/**
 * A test fixture for a repository server and mongo database
 * <p>
 * When used as a test rule, this fixture will ensure that the database is available, delete all
 * records and check that the repository server is available and contains no resources.
 */
public class RepositoryFixture implements TestRule {

    private static Map<String, DB> cache;

    private final ServerAddress dbHost;
    private final String dbName;

    private final LoginInfo adminLoginInfo;
    private final LoginInfoEntry adminLoginInfoEntry;

    private final LoginInfoEntry userLoginInfoEntry;

    /**
     * Creates a test fixture for the repository server and corresponding mongo database
     *
     * @param serverUrl the URL of the repository server
     * @param dbHost the hostname and port number of the mongodb server to connect to
     * @param dbName the database name to access on the mongodb server
     * @param adminRoleUsername the name of a user who is granted the Administrator
     * @param adminRolePassword the password associated with adminRoleUserName
     * @param userRoleUsername the name of a user who is granted the User role
     * @param userRolePassword the password associated with useRoleUsername
     */
    public RepositoryFixture(String serverUrl,
                             String dbHost,
                             String dbName,
                             String adminRoleUsername,
                             String adminRolePassword,
                             String userRoleUsername,
                             String userRolePassword) {
        this.adminLoginInfoEntry = new LoginInfoEntry(adminRoleUsername, adminRolePassword, "1", serverUrl);
        this.adminLoginInfo = new LoginInfo(adminLoginInfoEntry);

        // If userRoleUsername and userRolePassword are null then this LoginInfo will access the
        // repository as the UNAUTHENTICATED user
        this.userLoginInfoEntry = new LoginInfoEntry(userRoleUsername, userRolePassword, "1", serverUrl);

        this.dbName = dbName;
        try {
            this.dbHost = new ServerAddress(dbHost);
        } catch (UnknownHostException e) {
            throw new AssertionError("The host " + dbHost + " is not known", e);
        }
    }

    /**
     * Creates a test fixture for the repository server and corresponding mongo database
     *
     * @param serverUrl the URL of the repository server
     * @param dbHost the hostname and port number of the mongodb server to connect to
     * @param dbName the database name to access on the mongodb server
     * @param adminRoleUsername the name of a user who is granted the Administrator
     * @param adminRolePassword the password associated with adminRoleUserName
     * @param userRoleUsername the name of a user who is granted the User role
     * @param userRolePassword the password associated with useRoleUsername
     */
    public RepositoryFixture(String serverUrl,
                             String dbHost,
                             String dbName,
                             String adminRoleUsername,
                             String adminRolePassword) {
        this(serverUrl, dbHost, dbName, adminRoleUsername, adminRolePassword, null, null);
    }

    /**
     * Return the DB object for this database
     *
     * @return the database object
     */
    public DB getDb() {
        return getDatabase(dbHost, dbName);
    }

    /**
     * Return a <code>LoginInfo</code> object for accessing the repository server. The user
     * associated with this LoginInfo has the Administrator role.
     *
     * @return the LoginInfo object
     */
    public LoginInfo getLoginInfo() {
        return adminLoginInfo;
    }

    /**
     * Return a <code>LoginInfoEntry</code> object for accessing the repository server. The user
     * associated with this LoginInfoEntry has the Administrator role.
     *
     * @return the LoginInfo object
     */
    public LoginInfoEntry getLoginInfoEntry() {
        return adminLoginInfoEntry;
    }

    /**
     * Returns a <code>LoginInfoEntry</code> object whose user has the User role but is *not* an
     * Administrator.
     */
    public LoginInfoEntry getUserLoginInfoEntry() {
        return this.userLoginInfoEntry;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        final Statement baseStatement = base;
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                // Before running the test, clear the database
                DB db = getDb();
                for (String collectionName : db.getCollectionNames()) {
                    if (!collectionName.startsWith("system.")) {
                        DBCollection c = db.getCollection(collectionName);
                        c.remove(new BasicDBObject());
                    }
                }

                // Now check that the repository contains no resources
                Collection<MassiveResource> resources = MassiveResource.getAllResources(adminLoginInfo);
                assertEquals("Wrong number of resources in the server", 0, resources.size());

                // Finally continue with executing the test
                baseStatement.evaluate();
            }
        };
    }

    /**
     * Get a database handle for the given host and dbName, retrieving it from the cache if it has
     * been requested previously or creating a new handle otherwise.
     *
     * @param host the server address to connect to
     * @param dbName the database name
     * @return a handle to the database
     */
    private static DB getDatabase(ServerAddress host, String dbName) {
        String key = host + "/" + dbName;
        if (cache == null) {
            cache = new HashMap<String, DB>();
        }

        DB db = cache.get(key);
        if (db == null) {
            MongoClient client = new MongoClient(host);
            db = client.getDB(dbName);
            cache.put(key, db);
        }

        return db;
    }

}
