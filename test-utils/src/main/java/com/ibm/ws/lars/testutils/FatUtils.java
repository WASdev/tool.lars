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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.ibm.ws.lars.testutils.fixtures.LarsRepositoryFixture;
import com.ibm.ws.lars.testutils.fixtures.MassiveRepositoryFixture;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;

public class FatUtils {

    public static final String SCRIPT;

    public static final String DB_PORT;
    public static final String LIBERTY_PORT_HTTP;
    public static final String LIBERTY_PORT_HTTPS;
    public static final String TEST_DB_NAME;
    public static final String LARS_APPLICATION_ROOT;
    public static final String BASEURL_LIBERTY_PORT_HTTP;

    static {
        if (System.getProperty("os.name").startsWith("Windows")) {
            SCRIPT = "client/bin/larsClient.bat";
        } else {
            SCRIPT = "client/bin/larsClient";
        }

        Properties defaultConfig = new Properties();
        InputStream propsStream = FatUtils.class.getClassLoader().getResourceAsStream("config.properties");
        try {
            defaultConfig.load(propsStream);
        } catch (IOException e) {
            // Can't recover from this
            throw new RuntimeException("Unable to find config.properties file for database/server configuration");
        }

        DB_PORT = defaultConfig.getProperty("testMongoPort");
        LIBERTY_PORT_HTTP = defaultConfig.getProperty("testLibertyPortHTTP");
        LIBERTY_PORT_HTTPS = defaultConfig.getProperty("testLibertyPortHTTPS");
        TEST_DB_NAME = defaultConfig.getProperty("testDbName");
        LARS_APPLICATION_ROOT = defaultConfig.getProperty("larsApplicationRoot");
        BASEURL_LIBERTY_PORT_HTTP = defaultConfig.getProperty("baseUrlLibertyPortHTTP");

    }

    public static final String SERVER_URL = "http://localhost:" + LIBERTY_PORT_HTTP + LARS_APPLICATION_ROOT;

    public static final String DEFAULT_HOST_AND_PORT = "localhost:" + DB_PORT;

    private static final String ADMIN_USERNAME = "admin";

    private static final String ADMIN_PASSWORD = "passw0rd";

    private static final String USER_ROLE_USERNAME = "user";

    private static final String USER_ROLE_PASSWORD = "passw0rd";

    public static final String BASEURL_SERVER_URL = "http://localhost:" + BASEURL_LIBERTY_PORT_HTTP + LARS_APPLICATION_ROOT;

    public static final String BLUEMIX_HTTP_URL =
            "http://localhost:" + LIBERTY_PORT_HTTP + "/bluemix" + LARS_APPLICATION_ROOT;

    public static final String BLUEMIX_HTTPS_URL =
            "https://localhost:" + LIBERTY_PORT_HTTPS + "/bluemix" + LARS_APPLICATION_ROOT;

    public static final RepositoryFixture FAT_REPO = LarsRepositoryFixture.createFixture(SERVER_URL, "1", ADMIN_USERNAME, ADMIN_PASSWORD, USER_ROLE_USERNAME, USER_ROLE_PASSWORD);

    public static int countLines(String input) {
        int lines = 0;
        // If the last character isn't \n, add one, as that will make
        // the count match what you would normally expect
        if (input.charAt(input.length() - 1) != '\n') {
            lines = 1;
        }
        while (true) {
            int nlIndex = input.indexOf('\n');
            if (nlIndex == -1) {
                break;
            }
            lines++;
            input = input.substring(nlIndex + 1);
        }
        return lines;
    }

    public static Object[][] getRestFixtureParameters() {
        List<Object[]> objectList = new ArrayList<Object[]>();

        objectList.add(new Object[] { FAT_REPO });

        MassiveRepositoryFixture massiveRepo = getMassiveFixture();
        if (massiveRepo != null) {
            objectList.add(new Object[] { massiveRepo });
        }

        return objectList.toArray(new Object[objectList.size()][]);
    }

    public static MassiveRepositoryFixture getMassiveFixture() {
        String massiveServerString = System.getProperty("massiveServer");
        if (massiveServerString == null) {
            return null;
        }

        String[] parts = massiveServerString.split(",", -1);
        if (parts.length != 4) {
            throw new RuntimeException("Invalid massiveServer property set");
        }

        MassiveRepositoryFixture fixture = MassiveRepositoryFixture.createFixture(parts[0], parts[1], parts[2], parts[3]);
        return fixture;
    }
}
