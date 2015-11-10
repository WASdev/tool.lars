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
package com.ibm.ws.repository.resources.writeable;

import java.util.Collection;

import com.ibm.ws.repository.resources.ConfigSnippetResource;

/**
 * Represents a Config Snippet Resource in a repository.
 * <p>
 * This interface allows read access to fields which are specific to config snippets.
 */
public interface ConfigSnippetResourceWritable extends ConfigSnippetResource, RepositoryResourceWritable, ApplicableToProductWritable {

    /**
     * Sets the list of required features for this config snippet
     * 
     * @param requireFeature the list of symbolic names of required features
     */
    public void setRequireFeature(Collection<String> requireFeature);

}