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

import java.util.logging.Level;
import java.util.logging.Logger;

import mockit.Expectations;
import mockit.Mocked;

import org.junit.Test;

public class RepositoryExceptionMapperLoggingTest {

    @Test
    public void testLogging(@Mocked final Logger logger) {

        new Expectations() {
            {
                logger.isLoggable(Level.SEVERE);
                result = true;
                logger.severe("Internal server error\n" + "No message");

                // check for logging of the stack trace. Just look for the first line, as
                // that should be stable and not change
                logger.severe(withSubstring("at com.ibm.ws.lars.rest.RepositoryExceptionMapperLoggingTest.testLogging"));
            }
        };

        RepositoryExceptionMapper mapper = new RepositoryExceptionMapper();
        mapper.toResponse(new RepositoryException("No message"));
    }
}
