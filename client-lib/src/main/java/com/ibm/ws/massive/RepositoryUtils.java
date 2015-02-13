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

package com.ibm.ws.massive;

import java.io.IOException;

import com.ibm.ws.massive.sa.client.MassiveClient;
import com.ibm.ws.massive.sa.client.RequestFailureException;

/**
 * A set of utilities for communicating the Liberty Repository.
 */
public class RepositoryUtils {

    /**
     * Method to test if the repository defined by the <code>loginInfo</code> is available.
     *
     * @param loginInfo The {@link LoginInfo} defining the user credentials to log into the repository as well as it's location
     * @return <code>true</code> if a connection to the repository could be established, <code>false</code> otherwise
     */
    public static boolean isRepositoryAvailable(LoginInfoEntry loginInfo) {
        MassiveClient client = new MassiveClient(loginInfo.getClientLoginInfo());
        try {
            client.getAllAssetsMetadata();
        } catch (IOException e) {
            // Something went wrong accessing the repository so return false
            return false;
        } catch (RequestFailureException e) {
            // Something went wrong accessing the repository so return false
            return false;
        }
        return true;
    }

}
