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
 * The attributes that you can filter a resource by.
 * NB Not all resources have all attributes.
 */
public enum FilterableAttribute {

    TYPE("type", ResourceType.class),
    PRODUCT_ID("wlpInformation.appliesToFilterInfo.productId", String.class),
    /** When filtering by VISIBILITY only one value should be provided. */
    VISIBILITY("wlpInformation.visibility", "wlpInformation2.visibility", Collections.singleton(Visibility.INSTALL.toString()), Visibility.class),
    PRODUCT_MIN_VERSION("wlpInformation.appliesToFilterInfo.minVersion.value", String.class),
    PRODUCT_HAS_MAX_VERSION("wlpInformation.appliesToFilterInfo.hasMaxVersion", Boolean.class),
    SYMBOLIC_NAME("wlpInformation.provideFeature", String.class),
    SHORT_NAME("wlpInformation.shortName", String.class),
    LOWER_CASE_SHORT_NAME("wlpInformation.lowerCaseShortName", String.class),
    VANITY_URL("wlpInformation.vanityRelativeURL", String.class);
    private final String attributeName;
    private final String secondaryAttributeName;
    private final Collection<String> valuesInSecondaryAttributeName;
    private final Class<?> type;

    private FilterableAttribute(final String attributeName, Class<?> type) {
        this.attributeName = attributeName;
        this.secondaryAttributeName = null;
        this.valuesInSecondaryAttributeName = null;
        this.type = type;
    }

    private FilterableAttribute(final String attributeName, final String secondaryAttributeName,
                                final Collection<String> valuesInSecondaryAttributeName, Class<?> type) {
        this.attributeName = attributeName;
        this.secondaryAttributeName = secondaryAttributeName;
        this.valuesInSecondaryAttributeName = valuesInSecondaryAttributeName;
        this.type = type;
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

    public Class<?> getType() {
        return type;
    }
}