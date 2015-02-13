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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.ws.massive.sa.client.model.AppliesToFilterInfo;
import com.ibm.ws.massive.sa.client.model.FilterVersion;

/**
 * This class parses the appliesTo header, the only documentation
 * for this header is the existing parse logic within
 *
 * wlp.lib.ProductMatch
 *
 * which is sadly too tied to its existing usage to be reusable here.
 * This class MUST be kept in sync with that logic.
 */
public class AppliesToProcessor {

    private final static List<String> allEditions = Arrays.asList("Liberty Core", "Base", "Express", "Developers", "ND", "z/OS");
    private final static List<String> betaEditions = Arrays.asList("Betas", "Bluemix");

    private final static Map<String, String> editionsMap = new HashMap<String, String>();
    private final static String VERSION_ATTRIB_NAME = "productVersion";
    private final static String EDITION_ATTRIB_NAME = "productEdition";
    private final static String INSTALL_TYPE_ATTRIB_NAME = "productInstallType";
    private final static String EARLY_ACCESS_LABEL = "Betas";
    static {
        editionsMap.put("Core", "Liberty Core");
        editionsMap.put("CORE", "Liberty Core");
        editionsMap.put("LIBERTY_CORE", "Liberty Core");
        editionsMap.put("BASE", "Base");
        editionsMap.put("DEVELOPERS", "Developers");
        editionsMap.put("EXPRESS", "Express");
        editionsMap.put("EARLY_ACCESS", "Betas");
        editionsMap.put("zOS", "z/OS");
    }
    public final static String BETA_REGEX = "[2-9][0-9][0-9][0-9][.].*";

    private static String getValue(String substring) {
        int index = substring.indexOf('=');
        substring = substring.substring(index + 1).trim();
        if (substring.charAt(0) == '"') {
            return substring.substring(1, substring.length() - 1);
        } else {
            return substring;
        }
    }

    private static void add(AppliesToFilterInfo atfi, String substring, Map<String, String> helperMap, AppliesToHeaderHelper helper) {
        substring = substring.trim();
        if (atfi.getProductId() == null) {
            atfi.setProductId(substring);
        } else if (substring.startsWith(VERSION_ATTRIB_NAME)) {
            String version = getValue(substring);
            boolean unbounded = version.endsWith("+");
            if (unbounded) {
                version = version.substring(0, version.length() - 1);
            }
            //version+ == unbounded
            //version  == exact
            //year.month.day == Early Access

            //is this an early access?
            String label = null;
            String compatibilityLabel = null;
            if (helper != null) {
                //let the helper say what the label should be.
                label = helper.getLabelForProductVersionString(version);
            }
            //if the helper declined, or there was no helper, default to the below.
            if (label == null) {
                if (version.matches("^" + BETA_REGEX)) {
                    label = EARLY_ACCESS_LABEL;
                    compatibilityLabel = EARLY_ACCESS_LABEL;
                } else {
                    compatibilityLabel = version;
                    //select first 3 dot components of string.
                    int cutpoint = 0;
                    int count = 0;
                    while (cutpoint < version.length() && count < 3) {
                        if (version.charAt(cutpoint++) == '.')
                            count++;
                    }
                    //if there were less, just use the entire string.
                    if (cutpoint == version.length() && count != 3) {
                        label = version;
                    } else {
                        label = version.substring(0, cutpoint - 1);
                    }
                }
            }
            FilterVersion minVersion = new FilterVersion();
            minVersion.setLabel(label);
            minVersion.setInclusive(true);
            minVersion.setValue(version);
            minVersion.setCompatibilityLabel(compatibilityLabel);
            atfi.setMinVersion(minVersion);
            if (!unbounded) {
                //use a new instance of filterversion, just incase anyone decides to edit it later
                FilterVersion maxVersion = new FilterVersion();
                maxVersion.setLabel(label);
                maxVersion.setInclusive(true);
                maxVersion.setValue(version);
                maxVersion.setCompatibilityLabel(compatibilityLabel);
                atfi.setMaxVersion(maxVersion);
            }
        } else if (substring.startsWith(EDITION_ATTRIB_NAME)) {
            String editionStr = getValue(substring);
            List<String> editions = new ArrayList<String>();
            atfi.setEditions(editions);
            List<String> rawEditions = new ArrayList<String>();
            atfi.setRawEditions(rawEditions);
            for (int startIndex = 0, endIndex = editionStr.indexOf(',');; startIndex = endIndex, endIndex = editionStr.indexOf(',', ++startIndex)) {
                String edition = editionStr.substring(startIndex, endIndex == -1 ? editionStr.length() : endIndex);

                // Store original edition in rawEditions
                rawEditions.add(edition);

                //map entries to happier names..
                if (helperMap.containsKey(edition)) {
                    edition = helperMap.get(edition);
                }
                editions.add(edition);
                if (endIndex == -1) {
                    break;
                }
            }
        } else if (substring.startsWith(INSTALL_TYPE_ATTRIB_NAME)) {
            String installTypeString = getValue(substring);
            atfi.setInstallType(installTypeString);
        }
    }

    public static List<AppliesToFilterInfo> parseAppliesToHeader(String appliesTo, AppliesToHeaderHelper helper) {
        List<AppliesToFilterInfo> result = new ArrayList<AppliesToFilterInfo>();

        List<String> editionsList = allEditions;
        if (helper != null) {
            List<String> helperEditions = helper.getEditionListForAbsentProductEditionsHeader();
            if (helperEditions != null) {
                editionsList = helperEditions;
            }
        }

        Map<String, String> editionNameMap = editionsMap;
        if (helper != null) {
            Map<String, String> helperMap = helper.getEditionNameOverrideMap();
            if (helperMap != null) {
                editionNameMap = helperMap;
            }
        }

        boolean quoted = false;
        int index = 0;
        AppliesToFilterInfo match = new AppliesToFilterInfo();
        for (int i = 0; i < appliesTo.length(); i++) {
            char c = appliesTo.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            }
            if (!quoted) {
                if (c == ',') {
                    add(match, appliesTo.substring(index, i), editionNameMap, helper);
                    index = i + 1;
                    result.add(match);
                    match = new AppliesToFilterInfo();
                } else if (c == ';') {
                    add(match, appliesTo.substring(index, i), editionNameMap, helper);
                    index = i + 1;
                }
            }
        }
        add(match, appliesTo.substring(index), editionNameMap, helper);
        result.add(match);

        //no editions? means all..
        List<String> editions = match.getEditions();
        if (editions == null || editions.isEmpty()) {
            if (editions == null) {
                editions = new ArrayList<String>();
                match.setEditions(editions);
            }
            if ((match.getMinVersion() != null) && (match.getMinVersion().getCompatibilityLabel() != null) &&
                (match.getMinVersion().getCompatibilityLabel().equals(EARLY_ACCESS_LABEL))) {
                editions.addAll(betaEditions);
            } else {
                editions.addAll(editionsList);
            }
        }

        return result;
    }
}