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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests to check any additional requirements needed to support Installation Manager
 */
public class ImCompatibilityTest {

    @Rule
    public RepositoryContext context = RepositoryContext.createAsUser();

    @Test
    public void testRepositoryConfig() throws Exception {
        byte[] response = context.doGetAsByteArray("/repository.config", 200);

        Properties props = new Properties();
        props.load(new ByteArrayInputStream(response));
        assertEquals("repository.config file was not an empty properties file", 0, props.size());

        String responseString = new String(response, "UTF-8");
        assertThat("repository.config did not contain liberty repository marker", responseString, containsString("repository.type=liberty"));
    }
}
