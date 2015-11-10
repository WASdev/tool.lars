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

package com.ibm.ws.repository.transport.model.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.FilterVersion;

/**
 * Tests for {@link AppliesToFilterInfo}
 */
public class AppliesToFilterInfoTest {

    /**
     * Test that hasMaxVersion defaults to false
     */
    @Test
    public void testHasMaxVersionDefault() {
        AppliesToFilterInfo testObject = new AppliesToFilterInfo();
        assertFalse("The default should be false", Boolean.valueOf(testObject.getHasMaxVersion()));
    }

    /**
     * Test that setting max version also sets hasMaxVersion
     */
    @Test
    public void testSetMaxVersion() {
        AppliesToFilterInfo testObject = new AppliesToFilterInfo();
        testObject.setMaxVersion(new FilterVersion());
        testObject.setHasMaxVersion("true");
        assertTrue("The has max version should now be set", Boolean.valueOf(testObject.getHasMaxVersion()));
    }

    /**
     * Test that resetting max version also resets hasMaxVersion
     */
    @Test
    public void testReSetMaxVersion() {
        AppliesToFilterInfo testObject = new AppliesToFilterInfo();
        testObject.setMaxVersion(new FilterVersion());
        testObject.setHasMaxVersion("true");
        assertTrue("The has max version should now be set", Boolean.valueOf(testObject.getHasMaxVersion()));
        testObject.setMaxVersion(null);
        testObject.setHasMaxVersion("false");
        assertFalse("The has max version should now be reset", Boolean.valueOf(testObject.getHasMaxVersion()));
    }
}
