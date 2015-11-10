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
package com.ibm.ws.repository.common.utils.internal;

import java.util.Locale;

/**
 * A collection of utilities for using in the repository.
 */
public class RepositoryCommonUtils {

    /**
     * Creates a locale based on a String of the form language_country_variant,
     * either of the last two parts can be omitted.
     * 
     * @param localeString The locale string
     * @return The locale
     */
    public static Locale localeForString(String localeString) {
        if (localeString == null || localeString.isEmpty()) {
            return null;
        }
        Locale locale;
        String[] localeParts = localeString.split("_");
        switch (localeParts.length) {
            case 1:
                locale = new Locale(localeParts[0]);
                break;
            case 2:
                locale = new Locale(localeParts[0], localeParts[1]);
                break;
            default:
                // Use default for 3 and above, merge the parts back and put them all in the varient
                StringBuilder varient = new StringBuilder(localeParts[2]);
                for (int i = 3; i < localeParts.length; i++) {
                    varient.append("_");
                    varient.append(localeParts[i]);
                }
                locale = new Locale(localeParts[0], localeParts[1], varient.toString());
                break;
        }
        return locale;
    }
}
