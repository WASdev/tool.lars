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

package com.ibm.ws.massive.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.ibm.ws.massive.sa.client.model.AppliesToFilterInfo;

public class AppliesToProcessorTest {

    @Test
    public void testBasicAppliesToHeader() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5.5.0; productEditions=\"BASE\"", null);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "8.5.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testShortVersion() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5; productEditions=\"BASE\"", null);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5", "8.5", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5", "8.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5", "8.5", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5", "8.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testBasicOrderIrrelevant() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productEditions=\"BASE\"; productVersion=8.5.5.0", null);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "8.5.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testUnbounded() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5.5.0+; productEditions=\"BASE\"", null);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNull("filter info should NOT have maxVersion information", atfi.getMaxVersion());
        assertFalse("filter info should not have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testQuotedUnbounded() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=\"8.5.5.0+\"; productEditions=\"BASE\"", null);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNull("filter info should NOT have maxVersion information", atfi.getMaxVersion());
        assertFalse("filter info should not have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testNoEditionInBetaFeature() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=2013.13.13", null);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 2, atfi.getEditions().size());
        assertTrue("filter info should be for base", atfi.getEditions().get(0).equals("Betas") ||
                                                     atfi.getEditions().get(0).equals("Blue mix"));
        assertTrue("filter info should be for base", atfi.getEditions().get(1).equals("Betas") ||
                                                     atfi.getEditions().get(1).equals("Bluemix"));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 2013.13.13", "2013.13.13", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label Betas", "Betas", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 2013.13.13", "2013.13.13", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label Betas", "Betas", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testDateBased() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=2013.13.13; productEditions=\"BASE\"", null);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 2013.13.13", "2013.13.13", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label Betas", "Betas", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 2013.13.13", "2013.13.13", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label Betas", "Betas", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testUnboundedDateBased() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=2013.13.13+; productEditions=\"BASE\"", null);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 2013.13.13", "2013.13.13", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label Betas", "Betas", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNull("filter info should not have maxVersion information", atfi.getMaxVersion());
        assertFalse("filter info should not have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testCoreTranslated() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=2013.13.13; productEditions=\"Core\"", null);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for Liberty Core", "Liberty Core", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 2013.13.13", "2013.13.13", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label Betas", "Betas", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 2013.13.13", "2013.13.13", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label Betas", "Betas", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testListWithCore() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=2013.13.13; productEditions=\"Core,BASE\"", null);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be for two editions", 2, atfi.getEditions().size());
        assertEquals("filter info should be for Liberty Core", "Liberty Core", atfi.getEditions().get(0));
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(1));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 2013.13.13", "2013.13.13", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label Betas", "Betas", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 2013.13.13", "2013.13.13", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label Betas", "Betas", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testMultipleProduct() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=2013.13.13; productEditions=\"Core,BASE\",com.ibm.tinypinkelf; productVersion=8.5.5.0; productEditions=\"Base\"",
                                                                                  null);
        assertEquals("Expected two filter info to be returned", 2, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be for two editions", 2, atfi.getEditions().size());
        assertEquals("filter info should be for Liberty Core", "Liberty Core", atfi.getEditions().get(0));
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(1));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 2013.13.13", "2013.13.13", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label Betas", "Betas", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 2013.13.13", "2013.13.13", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label Betas", "Betas", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
        atfi = atfis.get(1);
        assertEquals("filter info should be for tinypinkelf", "com.ibm.tinypinkelf", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Base", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "8.5.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testOmittedEditions() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5.5.0", null);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be for six editions", 6, atfi.getEditions().size());

        //"Liberty Core", "Base", "Express", "Developers", "ND", "z/OS"
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("Liberty Core"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("Base"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("Express"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("Developers"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("ND"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("z/OS"));

        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "8.5.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testBasicAppliesToHeaderWithInactiveHelperOverride() throws Exception {
        AppliesToHeaderHelper helper = new AppliesToHeaderHelper() {

            @Override
            public String getLabelForProductVersionString(String versionString) {
                return null;
            }

            @Override
            public Map<String, String> getEditionNameOverrideMap() {
                return null;
            }

            @Override
            public List<String> getEditionListForAbsentProductEditionsHeader() {
                return null;
            }
        };

        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5.5.0; productEditions=\"Core\"", helper);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be a single edition", 1, atfi.getEditions().size());
        assertEquals("filter info should be for base", "Liberty Core", atfi.getEditions().get(0));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "8.5.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testBasicAppliesToHeaderWithInactiveHelperOverrideAndAbsentEditions() throws Exception {
        AppliesToHeaderHelper helper = new AppliesToHeaderHelper() {

            @Override
            public String getLabelForProductVersionString(String versionString) {
                return null;
            }

            @Override
            public Map<String, String> getEditionNameOverrideMap() {
                return null;
            }

            @Override
            public List<String> getEditionListForAbsentProductEditionsHeader() {
                return null;
            }
        };

        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5.5.0", helper);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be for six editions", 6, atfi.getEditions().size());
        //"Liberty Core", "Base", "Express", "Developers", "ND", "z/OS"
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("Liberty Core"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("Base"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("Express"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("Developers"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("ND"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("z/OS"));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "8.5.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testBasicAppliesToHeaderWithHelperOverrideAndAbsentEditions() throws Exception {
        AppliesToHeaderHelper helper = new AppliesToHeaderHelper() {

            @Override
            public String getLabelForProductVersionString(String versionString) {
                return null;
            }

            @Override
            public Map<String, String> getEditionNameOverrideMap() {
                return null;
            }

            @Override
            public List<String> getEditionListForAbsentProductEditionsHeader() {
                return Arrays.asList("stiletto", "court", "kitten", "cone", "block");
            }
        };

        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5.5.0", helper);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be for five editions", 5, atfi.getEditions().size());
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("stiletto"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("court"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("kitten"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("cone"));
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("block"));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "8.5.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testBasicAppliesToHeaderWithHelperOverrideAndMappedAbsentEditions() throws Exception {
        AppliesToHeaderHelper helper = new AppliesToHeaderHelper() {
            @Override
            public String getLabelForProductVersionString(String versionString) {
                return null;
            }

            //in a non test environment, we wouldn't want to new this up every time!
            @Override
            public Map<String, String> getEditionNameOverrideMap() {
                Map<String, String> editions = new HashMap<String, String>();
                editions.put("block", "wedge");
                return editions;
            }

            @Override
            public List<String> getEditionListForAbsentProductEditionsHeader() {
                return null;
            }
        };

        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5.5.0; productEditions=\"block\"", helper);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be for one editions", 1, atfi.getEditions().size());
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("wedge"));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "8.5.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testBasicAppliesToHeaderWithHelperOverrideAndMappedButAsIsAbsentEditions() throws Exception {
        AppliesToHeaderHelper helper = new AppliesToHeaderHelper() {
            @Override
            public String getLabelForProductVersionString(String versionString) {
                return null;
            }

            //in a non test environment, we wouldn't want to new this up every time!
            @Override
            public Map<String, String> getEditionNameOverrideMap() {
                Map<String, String> editions = new HashMap<String, String>();
                editions.put("block", "wedge");
                return editions;
            }

            @Override
            public List<String> getEditionListForAbsentProductEditionsHeader() {
                return null;
            }
        };

        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5.5.0; productEditions=\"stiletto\"", helper);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be for one editions", 1, atfi.getEditions().size());
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("stiletto"));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "8.5.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testBasicAppliesToHeaderWithNotApplicableHelperOverrideForLabel() throws Exception {
        AppliesToHeaderHelper helper = new AppliesToHeaderHelper() {
            @Override
            public String getLabelForProductVersionString(String versionString) {
                if (versionString.equals("fish"))
                    return "sharkbait";
                else
                    return null;
            }

            //in a non test environment, we wouldn't want to new this up every time!
            @Override
            public Map<String, String> getEditionNameOverrideMap() {
                return null;
            }

            @Override
            public List<String> getEditionListForAbsentProductEditionsHeader() {
                return null;
            }
        };

        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=8.5.5.0; productEditions=\"stiletto\"", helper);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be for one editions", 1, atfi.getEditions().size());
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("stiletto"));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "8.5.5", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "8.5.5.0", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "8.5.5", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testBasicAppliesToHeaderWithHelperOverrideForLabel() throws Exception {
        AppliesToHeaderHelper helper = new AppliesToHeaderHelper() {
            @Override
            public String getLabelForProductVersionString(String versionString) {
                if (versionString.equals("fish"))
                    return "sharkbait";
                else
                    return null;
            }

            //in a non test environment, we wouldn't want to new this up every time!
            @Override
            public Map<String, String> getEditionNameOverrideMap() {
                return null;
            }

            @Override
            public List<String> getEditionListForAbsentProductEditionsHeader() {
                return null;
            }
        };

        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish; productVersion=fish; productEditions=\"stiletto\"", helper);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info should have edition info", atfi.getEditions());
        assertEquals("filter info should be for one editions", 1, atfi.getEditions().size());
        assertTrue("filter info should contain all editions", atfi.getEditions().contains("stiletto"));
        assertNotNull("filter info should have minVersion information", atfi.getMinVersion());
        assertEquals("filter info minVersion should be for 8.5.5.0", "fish", atfi.getMinVersion().getValue());
        assertEquals("filter info minVersion should have label 8.5.5", "sharkbait", atfi.getMinVersion().getLabel());
        assertTrue("filter info minVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertNotNull("filter info should have maxVersion information", atfi.getMaxVersion());
        assertEquals("filter info maxVersion should be for 8.5.5.0", "fish", atfi.getMaxVersion().getValue());
        assertEquals("filter info maxVersion should have label 8.5.5", "sharkbait", atfi.getMaxVersion().getLabel());
        assertTrue("filter info maxVersion should be inclusive", atfi.getMinVersion().getInclusive());
        assertTrue("filter info should have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }

    @Test
    public void testNoVersion() throws Exception {
        List<AppliesToFilterInfo> atfis = AppliesToProcessor.parseAppliesToHeader("com.ibm.fish", null);
        assertEquals("Expected one filter info to be returned", 1, atfis.size());
        AppliesToFilterInfo atfi = atfis.get(0);
        assertEquals("filter info should be for fish", "com.ibm.fish", atfi.getProductId());
        assertNotNull("filter info not should have some edition info (the defaults)", atfi.getEditions());
        assertNull("filter info should not have minVersion information", atfi.getMinVersion());
        assertNull("filter info should not have maxVersion information", atfi.getMaxVersion());
        assertFalse("filter info should not have a max version", Boolean.valueOf(atfi.getHasMaxVersion()));
    }
}
