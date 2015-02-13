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

import java.util.List;
import java.util.Map;

public interface AppliesToHeaderHelper {
    /**
     * @return a list of the Editions that should be used, if the productEdition attribute on the header is empty or absent.
     *         Return null if this helper does not wish to alter the default processing.
     */
    public List<String> getEditionListForAbsentProductEditionsHeader();

    /**
     * @return a map of productEdition values that should be overridden, productEdition attribute values not in this map will be used as-is.
     *         This map is NOT used to process any edition names returned via getEditionListForAbsentProductEditionsHeader.
     *         Return null if this helper does not wish to alter the default processing.
     */
    public Map<String, String> getEditionNameOverrideMap();

    /**
     * Obtain a "label" for the filter version, when given a version string (value from productVersion attribute).
     *
     * @param versionString the value of the productVersion attribute
     * @return the label to use, or null if this helper does not wish to alter the default processing.
     */
    public String getLabelForProductVersionString(String versionString);
}
