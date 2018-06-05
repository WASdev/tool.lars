/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
package com.ibm.ws.repository.common.utils.test;

import static org.junit.Assert.fail;

import org.junit.Test;

public class Java7Test {

    /**
     * This test checks that the tests are running on a Java 7 VM. This will only be true if
     * a Java 7 VM is available. Effectively the test is checking that the gradle build scripts are
     * invoking the java 7 vm correctly. Gradle will set SHOULD_USE_JAVA_7 if it knows about
     * a Java 7 VM. If the property is true, then this test can check that the VM running the tests is
     * a Java 7 one.
     */
    @Test
    public void testRunningWithJava7() {
        if (Boolean.getBoolean("SHOULD_USE_JAVA_7")) {
            String version = System.getProperty("java.version");
            if (version != null) {
                if (!version.startsWith("1.7")) {
                    fail("The test should run with java 7 but it is " + version);
                }
            }
        }
    }

}
