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

import java.util.Collection;
import java.util.Collections;

/**
 * The attributes that you can filter an ESA resource by.
 */
public enum FilterableAttribute {
    TYPE("type"),
    PRODUCT_ID("wlpInformation.appliesToFilterInfo.productId"),
    /** When filtering by VISIBILITY only one value should be provided. */
    VISIBILITY("wlpInformation.visibility", "wlpInformation2.visibility", Collections.singleton(Visibility.INSTALL.toString())),
    PRODUCT_MIN_VERSION("wlpInformation.appliesToFilterInfo.minVersion.value"),
    PRODUCT_HAS_MAX_VERSION("wlpInformation.appliesToFilterInfo.hasMaxVersion"),
    SYMBOLIC_NAME("wlpInformation.provideFeature"),
    SHORT_NAME("wlpInformation.shortName"),
    LOWER_CASE_SHORT_NAME("wlpInformation.lowerCaseShortName");
    private final String attributeName;
    private final String secondaryAttributeName;
    private final Collection<String> valuesInSecondaryAttributeName;

    private FilterableAttribute(final String attributeName) {
        this.attributeName = attributeName;
        this.secondaryAttributeName = null;
        this.valuesInSecondaryAttributeName = null;
    }

    private FilterableAttribute(final String attributeName, final String secondaryAttributeName, final Collection<String> valuesInSecondaryAttributeName) {
        this.attributeName = attributeName;
        this.secondaryAttributeName = secondaryAttributeName;
        this.valuesInSecondaryAttributeName = valuesInSecondaryAttributeName;
    }

    /**
     * @return the attributeName
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * @return the secondaryAttributeName
     */
    public String getSecondaryAttributeName() {
        return secondaryAttributeName;
    }

    /**
     * @return the valuesInSecondaryAttributeName
     */
    public Collection<String> getValuesInSecondaryAttributeName() {
        return valuesInSecondaryAttributeName;
    }
}