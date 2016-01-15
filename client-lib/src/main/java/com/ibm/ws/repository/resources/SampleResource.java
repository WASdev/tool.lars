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

import com.ibm.ws.repository.common.enums.ResourceType;

/**
 * Represents a Sample Resource in a repository.
 * <p>
 * This interface allows read access to fields which are specific to samples.
 * <p>
 * Samples represented by this interface can either be of type {@link ResourceType#PRODUCTSAMPLE} or {@link ResourceType#OPENSOURCE}.
 */
public interface SampleResource extends RepositoryResource, ApplicableToProduct {

    /**
     * Gets the list of required features for this sample
     *
     * @return The list of required features for this sample, or null if it has not been set
     */
    public Collection<String> getRequireFeature();

    /**
     * Gets the short name for this sample
     * <p>
     * The short name should be the name of the server included in the sample.
     *
     * @return The short name for this sample, or null if it has not been set
     */
    public String getShortName();

}
