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

import java.lang.reflect.InvocationTargetException;
import com.ibm.ws.lars.testutils.ReflectionTricks;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

/**
 * Unit tests for the {@link Configuration} class
 */
public class ConfigurationTest {
    
    @Test
    public void testComputeRestAppURLBase() throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        String methodName = "computeRestBaseUri";
        assertEquals("http://example.org/ma/v1/", ReflectionTricks.reflectiveCallNoPrimitives(Configuration.class, methodName, "http://example.org"));
        assertEquals("http://example.org/ma/v1/", ReflectionTricks.reflectiveCallNoPrimitives(Configuration.class, methodName, "http://example.org/"));
        assertEquals("http://example.org/wibble/ma/v1/", ReflectionTricks.reflectiveCallNoPrimitives(Configuration.class, methodName, "http://example.org/wibble"));
        assertEquals("http://example.org/wibble/ma/v1/", ReflectionTricks.reflectiveCallNoPrimitives(Configuration.class, methodName, "http://example.org/wibble/"));
    }

    @Test
    public void testStripDefaultPort() throws URISyntaxException,SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        String methodName = "stripDefaultPort";

        assertEquals(new URI("http://example.com/test"), ReflectionTricks.reflectiveCallNoPrimitives(Configuration.class, methodName, new URI("http://example.com:80/test")));
        assertEquals(new URI("http://example.com:9080/test"), ReflectionTricks.reflectiveCallNoPrimitives(Configuration.class, methodName, new URI("http://example.com:9080/test")));
        assertEquals(new URI("https://example.com/test"), ReflectionTricks.reflectiveCallNoPrimitives(Configuration.class, methodName, new URI("https://example.com:443/test")));
        assertEquals(new URI("https://example.com:9443/test"), ReflectionTricks.reflectiveCallNoPrimitives(Configuration.class, methodName, new URI("https://example.com:9443/test")));

        assertEquals(new URI("HTTP://example.com/test"), ReflectionTricks.reflectiveCallNoPrimitives(Configuration.class, methodName, new URI("HTTP://example.com:80/test")));
        assertEquals(new URI("HTTP://example.com:9080/test"), ReflectionTricks.reflectiveCallNoPrimitives(Configuration.class, methodName, new URI("HTTP://example.com:9080/test")));

        assertEquals(new URI("http://example.com:443/test"), ReflectionTricks.reflectiveCallNoPrimitives(Configuration.class, methodName, new URI("http://example.com:443/test")));
        assertEquals(new URI("https://example.com:80/test"), ReflectionTricks.reflectiveCallNoPrimitives(Configuration.class, methodName, new URI("https://example.com:80/test")));
    }

}
