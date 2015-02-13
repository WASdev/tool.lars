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
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.RepositoryFixture;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.resources.ConfigSnippetResource;
import com.ibm.ws.massive.resources.MassiveResource.DownloadPolicy;

public class MassiveConfigSnippetResourceTest {

    @Rule
    public RepositoryFixture repo = FAT_REPO;
    private final LoginInfoEntry _loginInfoEntry = repo.getLoginInfoEntry();

    @Test
    public void testIsDownloadable() throws IOException {
        assertEquals("Admin scripts should be downloadable",
                     DownloadPolicy.ALL, new ConfigSnippetResource(_loginInfoEntry).getDownloadPolicy());
    }

    @Test
    public void testCopyFields() throws IllegalArgumentException, IllegalAccessException, InstantiationException, NoSuchMethodException,
                    SecurityException, InvocationTargetException, IOException {
        checkCopyFields(new ConfigSnippetResource(_loginInfoEntry), new ConfigSnippetResource(_loginInfoEntry));
    }
}