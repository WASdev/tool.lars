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

/**
 * Represents an Admin Script Resource in a repository.
 * <p>
 * This interface allows read access to fields which are specific to admin scripts.
 */
public interface AdminScriptResource extends RepositoryResource, ApplicableToProduct {

    /**
     * Get the language that the script is written in
     *
     * @return the language the script is written in, or null if it has not been set
     */
    public String getScriptLanguage();

    /**
     * Gets the list of required features for this admin script
     *
     * @return The list of required features for this admin script, or null if no features are required
     */
    public Collection<String> getRequireFeature();

}
