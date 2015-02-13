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

public class FatUtils {

    public static final String SCRIPT;

    static {
        if (System.getProperty("os.name").startsWith("Windows")) {
            SCRIPT = "client/bin/larsClient.bat";
        } else {
            SCRIPT = "client/bin/larsClient";
        }
    }

    public static final String SERVER_URL = "http://localhost:9085/ma/v1";

    public static final String DEFAULT_DB_NAME = "larsDB";

    public static final String DEFAULT_HOST_AND_PORT = "localhost:27020";

    private static final String ADMIN_USERNAME = "admin";

    private static final String ADMIN_PASSWORD = "passw0rd";

    private static final String USER_ROLE_USERNAME = "user";

    private static final String USER_ROLE_PASSWORD = "passw0rd";

    public static final RepositoryFixture FAT_REPO = new RepositoryFixture(SERVER_URL,
            DEFAULT_HOST_AND_PORT,
            DEFAULT_DB_NAME,
            ADMIN_USERNAME,
            ADMIN_PASSWORD,
            USER_ROLE_USERNAME,
            USER_ROLE_PASSWORD);

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

}
