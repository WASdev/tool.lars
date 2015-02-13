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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.resources.MassiveResource;
import com.ibm.ws.massive.resources.Provider;
import com.ibm.ws.massive.resources.RepositoryResourceException;

/**
 * This class includes several static methods for doing various checks within tests.
 */
public class BasicChecks {

    /**
     * Checks that the resource has a main attachment and that the filesize in the asset metadata
     * matches the filesize in the attachment metadata.
     *
     * @param res the resource to check
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    public void checkAttachment(MassiveResource res)
            throws RepositoryBackendException, RepositoryResourceException {
        assertNotNull("No main attachment", res.getMainAttachment());
        assertEquals("Wrong file size", res.getMainAttachmentSize(), res
                .getMainAttachment().getSize());
    }

    /**
     * Checks that <code>left.copyFieldsFrom(right)</code> (protected method) correctly copies
     * fields between massive resources and leaves the two resources equivalent without attachments
     * <p>
     * To do this, it tries to set all fields in <code>left</code> to specific values and then
     * copies fields from <codE>right</code>. It then checks that the two objects are equivalent.
     *
     * @param left the resource to copy into
     * @param right the resource to copy from
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IOException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InvocationTargetException
     */
    public static void checkCopyFields(MassiveResource left, MassiveResource right)
            throws IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, NoSuchMethodException, SecurityException, InvocationTargetException {

        ArrayList<String> methodsToIgnore = new ArrayList<String>();
        methodsToIgnore.add("setState");
        methodsToIgnore.add("setType");
        methodsToIgnore.add("setLoginInfo");
        for (Method m : left.getClass().getMethods()) {
            if (m.getName().startsWith("set")) {
                Class<?>[] parameterss = m.getParameterTypes();

                // Not a normal setter, ignore it
                if (parameterss.length != 1) {
                    continue;
                }

                if (methodsToIgnore.contains(m.getName())) {
                    continue;
                }

                Class<?> param = parameterss[0];

                Object p = null;
                if (param.isEnum()) {
                    p = param.getEnumConstants()[0];
                } else if (param.equals(Collection.class)) {
                    System.out.println("got a collection");
                    p = new ArrayList<Object>();
                } else if (param.isInterface()) {
                    continue;
                } else if (param.isPrimitive()) {
                    p = new Integer(4);
                } else if (param.equals(String.class)) {
                    p = new String("test string");
                } else {
                    p = param.newInstance();
                }

                m.invoke(left, p);
            }
        }

        Method m = null;

        try {
            m = left.getClass().getDeclaredMethod("copyFieldsFrom",
                                                  MassiveResource.class, boolean.class);
        } catch (Exception e) {
            m = left.getClass()
                    .getSuperclass()
                    .getDeclaredMethod("copyFieldsFrom", MassiveResource.class,
                                       boolean.class);
        }
        m.setAccessible(true);
        m.invoke(right, left, true);
        if (!left.equivalentWithoutAttachments(right)) {
            System.out.println("EQUIV FAILED: Left");
            left.dump(System.out);
            System.out.println("EQUIV FAILED: Right");
            right.dump(System.out);
            fail("Resources are not equivalent after copying fields");
        }

        if (!right.equivalentWithoutAttachments(left)) {
            System.out.println("EQUIV FAILED: Left");
            left.dump(System.out);
            System.out.println("EQUIV FAILED: Right");
            right.dump(System.out);
            fail("Resources are not equivalent after copying fields - though they were equivalent the other way around...");
        }
    }

    /**
     * Populates the name, provider, version and description fields of a MassiveResource with static
     * test values.
     *
     * @param res the resource to populate
     */
    public static void populateResource(MassiveResource res) {
        res.setName("test resource");

        Provider prov = new Provider();
        prov.setName("test provider");
        prov.setUrl("http://testhost/testfile");
        prov.dump(System.out);

        res.setProvider(prov);

        res.setVersion("version 1");
        res.setDescription("This is a test resource");
    }

}
