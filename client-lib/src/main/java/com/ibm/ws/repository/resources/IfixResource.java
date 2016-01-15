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
package com.ibm.ws.repository.resources;

import java.util.Collection;
import java.util.Date;

/**
 * Represents an IFix Resource in a repository.
 * <p>
 * This interface allows read access to fields which are specific to ifixes.
 */
public interface IfixResource extends RepositoryResource, ApplicableToProduct {

    /**
     * Gets the list of APAR IDs which are provided in this ifix resource
     *
     * @return the list of APAR IDs, or null if no APAR IDs are provided
     */
    public Collection<String> getProvideFix();

    /**
     * Gets the modified date of the most recently modified file to be replaced by the update
     *
     * @return the date, or null if it has not been set
     */
    public Date getDate();

}
