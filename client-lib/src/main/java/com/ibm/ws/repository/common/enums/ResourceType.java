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

package com.ibm.ws.repository.common.enums;

public enum ResourceType {
    /**
     * Samples that only use product code
     */
    PRODUCTSAMPLE(ResourceTypeLabel.PRODUCTSAMPLE, "com.ibm.websphere.ProductSample", "samples"),
    /**
     * Samples that also use open source code
     */
    OPENSOURCE(ResourceTypeLabel.OPENSOURCE, "com.ibm.websphere.OpenSource", "opensource"),
    /**
     * A liberty install jar file
     */
    INSTALL(ResourceTypeLabel.INSTALL, "com.ibm.websphere.Install", "runtimes"),
    /**
     * Extended functionality for the install
     */
    ADDON(ResourceTypeLabel.ADDON, "com.ibm.websphere.Addon", "addons"),
    /**
     * ESA's representing features
     */
    FEATURE(ResourceTypeLabel.FEATURE, "com.ibm.websphere.Feature", "features"),
    /**
     * Ifixes
     */
    IFIX(ResourceTypeLabel.IFIX, "com.ibm.websphere.Ifix", "ifixes"),
    /**
     * AdminScripts
     */
    ADMINSCRIPT(ResourceTypeLabel.ADMINSCRIPT, "com.ibm.websphere.AdminScript", "scripts"),
    /**
     * Config snippets
     */
    CONFIGSNIPPET(ResourceTypeLabel.CONFIGSNIPPET, "com.ibm.websphere.ConfigSnippet", "snippets"),
    /**
     * Tools
     */
    TOOL(ResourceTypeLabel.TOOL, "com.ibm.websphere.Tool", "tools");

    private final ResourceTypeLabel _typeLabel;
    private final String _type;
    private final String _nameForUrl;

    private ResourceType(ResourceTypeLabel label, String type, String nameForUrl) {
        _typeLabel = label;
        _type = type;
        _nameForUrl = nameForUrl;
    }

    public ResourceTypeLabel getTypeLabel() {
        return _typeLabel;
    }

    public String getURLForType() {
        return _nameForUrl;
    }

    // get the long name of a the type ie for FEATURE returns "com.ibm.websphere.Feature"
    public String getValue() {
        return _type;
    }

    public static ResourceType forValue(String value) {
        for (ResourceType ty : ResourceType.values()) {
            if (ty.getValue().equals(value)) {
                return ty;
            }
        }
        return null;
    }
}
