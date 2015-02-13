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

package com.ibm.ws.massive.test;

import static com.ibm.ws.lars.testutils.BasicChecks.checkCopyFields;
import static com.ibm.ws.lars.testutils.FatUtils.FAT_REPO;
import static com.ibm.ws.lars.testutils.ReflectionTricks.reflectiveCallNoPrimitives;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.RepositoryFixture;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.resources.IfixResource;
import com.ibm.ws.massive.resources.MassiveResource.DisplayPolicy;
import com.ibm.ws.massive.resources.MassiveResource.DownloadPolicy;

public class MassiveIfixResourceTest {

    @Rule
    public RepositoryFixture repo = FAT_REPO;
    private final LoginInfoEntry _loginInfoEntry = repo.getLoginInfoEntry();
    private final IfixResource _testObject = new IfixResource(_loginInfoEntry);

    @Test
    public void testIsDownloadable() {
        assertEquals("Ifixes should not be downloadable",
                     DownloadPolicy.ALL, _testObject.getDownloadPolicy());
    }

    @Test
    public void testDisplayPolicy() throws Exception {
        assertEquals("Ifixes display policy should be hidden by default",
                     DisplayPolicy.HIDDEN, _testObject.getDisplayPolicy());
        assertEquals("Ifixes web display policy should be hidden by default", DisplayPolicy.HIDDEN,
                     reflectiveCallNoPrimitives(_testObject, "getWebDisplayPolicy", (Object[]) null));
    }

    @Test
    public void testCopyFields() throws IllegalArgumentException, IllegalAccessException, InstantiationException, NoSuchMethodException,
                    SecurityException, InvocationTargetException, IOException {
        checkCopyFields(new IfixResource(_loginInfoEntry), new IfixResource(_loginInfoEntry));
    }
}
